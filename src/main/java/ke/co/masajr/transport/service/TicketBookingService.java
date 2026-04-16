package ke.co.masajr.transport.service;

import ke.co.masajr.transport.entity.BookingEntity;
import ke.co.masajr.transport.entity.Fare;
import ke.co.masajr.transport.entity.Trip;
import ke.co.masajr.transport.repository.BookingRepository;
import ke.co.masajr.transport.repository.FareRepository;
import ke.co.masajr.transport.repository.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.StructuredTaskScope;

/**
 * Core ticket booking service.
 *
 * Implements three concurrency strategies from the Engineering Guide:
 *
 *  1. processBatchBookings()     — StructuredTaskScope.ShutdownOnFailure
 *                                  "all-or-nothing" atomic batch (Java 21)
 *  2. processWithVirtualThreads() — Virtual thread executor + Semaphore throttle
 *                                   for high-throughput, rate-limited batches (Java 21)
 *  3. processStrictBatch()        — Strict StructuredTaskScope: first failure
 *                                   cancels all sibling tasks immediately (Java 21)
 *
 * All varargs methods guard against: null array, null elements (Objects::nonNull).
 */
@Service
public class TicketBookingService {

    private static final Logger log = LoggerFactory.getLogger(TicketBookingService.class);

    /**
     * Semaphore: max 50 concurrent M-PESA STK calls at any time.
     * Mirrors the Banking API rate-limit backpressure pattern from §3 of the guide.
     */
    private final Semaphore mpesaRateLimit = new Semaphore(50);

    private final TripRepository tripRepository;
    private final FareRepository fareRepository;
    private final BookingRepository bookingRepository;
    private final MpesaService mpesaService;
    private final SmsService smsService;

    public TicketBookingService(TripRepository tripRepository,
                                FareRepository fareRepository,
                                BookingRepository bookingRepository,
                                MpesaService mpesaService,
                                SmsService smsService) {
        this.tripRepository = tripRepository;
        this.fareRepository = fareRepository;
        this.bookingRepository = bookingRepository;
        this.mpesaService = mpesaService;
        this.smsService = smsService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dynamic pricing
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolves the effective price for a trip at its departure time.
     * Picks the fare window whose [effectiveFrom, effectiveTo) bracket covers
     * the departure time. Falls back to the trip's base price if none match.
     */
    public BigDecimal resolvePrice(Long tripId, LocalDateTime at) {
        return fareRepository.findActiveFare(tripId, at)
                .map(Fare::getPricePerSeat)
                .orElseGet(() -> tripRepository.findById(tripId)
                        .map(Trip::getPricePerSeat)
                        .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Single booking
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Books a single ticket.
     * STK push + SMS run in parallel via StructuredTaskScope on virtual threads.
     */
    @Transactional
    public BookingEntity bookTicket(Long tenantId, Long tripId, String phoneNumber) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));

        if (!trip.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Trip does not belong to tenant");
        }

        int available = trip.getTotalSeats() - trip.getBookedSeats();
        if (available <= 0) {
            throw new RuntimeException("No seats available on trip " + tripId);
        }

        BigDecimal price = resolvePrice(tripId, trip.getDepartureTime());
        String ticketId = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        BookingEntity booking = new BookingEntity();
        booking.setTenantId(tenantId);
        booking.setTripId(tripId);
        booking.setTicketId(ticketId);
        booking.setPhoneNumber(phoneNumber);
        booking.setAmount(price);
        booking.setPricePaid(price);
        booking.setStatus("PENDING");
        bookingRepository.save(booking);

        trip.setBookedSeats(trip.getBookedSeats() + 1);
        tripRepository.save(trip);

        // Parallel STK + SMS via structured concurrency
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var stkTask = scope.fork(() ->
                    mpesaService.initiateStk(tenantId, phoneNumber, price.doubleValue(), ticketId));

            scope.fork(() -> {
                smsService.sendSms(phoneNumber, String.format(
                        "Your ticket %s is confirmed. Amount: KES %.2f. Awaiting payment.",
                        ticketId, price));
                return null;
            });

            scope.join().throwIfFailed();

            booking.setCheckoutRequestId(stkTask.get());
            bookingRepository.save(booking);

        } catch (Exception e) {
            log.error("STK/SMS failed for ticket {}: {}", ticketId, e.getMessage());
            // Booking stays PENDING – M-PESA callback will update status
        }

        return booking;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // §3 — High-throughput batch: Virtual Threads + Semaphore throttle
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Books tickets for multiple phone numbers using a virtual-thread-per-task
     * executor throttled by a Semaphore (max 50 concurrent M-PESA calls).
     *
     * <p>Null-safety (§2 of guide):
     * <ul>
     *   <li>Null array → early return (no-op)</li>
     *   <li>Null elements → filtered via {@link Objects#nonNull}</li>
     * </ul>
     *
     * @param tenantId    owning tenant
     * @param tripId      target trip
     * @param phoneNumbers varargs – zero or more phone numbers; nulls are ignored
     */
    public void processWithVirtualThreads(Long tenantId, Long tripId, String... phoneNumbers) {
        // §2 guard: null array
        if (phoneNumbers == null) return;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Arrays.stream(phoneNumbers)
                    .filter(Objects::nonNull)                          // §2 guard: null elements
                    .forEach(phone -> executor.submit(() -> {
                        mpesaRateLimit.acquire();                      // §3 backpressure
                        try {
                            bookTicket(tenantId, tripId, phone);
                        } finally {
                            mpesaRateLimit.release();
                        }
                        return null;
                    }));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // §4 — Atomic batch: StructuredTaskScope.ShutdownOnFailure
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Books tickets as an atomic batch: if any single booking fails, all
     * sibling sub-tasks are cancelled immediately (ShutdownOnFailure).
     *
     * <p>Returns the list of created {@link BookingEntity} objects on full success.
     *
     * <p>Null-safety: null array → empty list; null elements → skipped.
     *
     * @param tenantId     owning tenant
     * @param tripId       target trip
     * @param phoneNumbers varargs phone numbers
     * @throws Exception   propagated from the first failed sub-task
     */
    public List<BookingEntity> processBatchBookings(Long tenantId, Long tripId,
                                                    String... phoneNumbers) throws Exception {
        // §2 guard: null array
        if (phoneNumbers == null) return List.of();

        List<BookingEntity> results = new ArrayList<>();

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            List<StructuredTaskScope.Subtask<BookingEntity>> tasks = Arrays.stream(phoneNumbers)
                    .filter(Objects::nonNull)                          // §2 guard: null elements
                    .map(phone -> scope.fork(() -> bookTicket(tenantId, tripId, phone)))
                    .toList();

            scope.join();            // wait for all sub-tasks
            scope.throwIfFailed();   // 4: propagate first error, cancel siblings

            for (var task : tasks) {
                results.add(task.get());
            }
        }

        return results;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // §4 variant — Strict batch (void, throws on first failure)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Strict all-or-nothing batch that mirrors the guide's {@code processStrictBatch}
     * pattern exactly. Void return; throws if any sub-task fails.
     *
     * @param tenantId     owning tenant
     * @param tripId       target trip
     * @param phoneNumbers varargs; null array or null elements are safely ignored
     * @throws Exception   first sub-task exception, others cancelled
     */
    public void processStrictBatch(Long tenantId, Long tripId,
                                   String... phoneNumbers) throws Exception {
        if (phoneNumbers == null) return;

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            for (String phone : phoneNumbers) {
                if (phone == null) continue;                           // §2 null element guard
                scope.fork(() -> {
                    bookTicket(tenantId, tripId, phone);
                    return null;
                });
            }
            scope.join();            // wait for all threads
            scope.throwIfFailed();   // propagate the first encountered error
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // M-PESA callback handler
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void handleMpesaCallback(String checkoutRequestId, boolean success) {
        bookingRepository.findByCheckoutRequestId(checkoutRequestId).ifPresentOrElse(booking -> {
            booking.setStatus(success ? "PAID" : "FAILED");
            bookingRepository.save(booking);

            if (success) {
                smsService.sendSms(booking.getPhoneNumber(),
                        String.format("✅ Payment received for ticket %s. Enjoy your trip!",
                                booking.getTicketId()));
            } else {
                smsService.sendSms(booking.getPhoneNumber(),
                        String.format("❌ Payment failed for ticket %s. Please try again.",
                                booking.getTicketId()));
                // Release the seat
                tripRepository.findById(booking.getTripId()).ifPresent(trip -> {
                    trip.setBookedSeats(Math.max(0, trip.getBookedSeats() - 1));
                    tripRepository.save(trip);
                });
            }
            log.info("Booking {} → {} for checkout {}",
                    booking.getTicketId(), booking.getStatus(), checkoutRequestId);
        }, () -> log.warn("No booking for CheckoutRequestID: {}", checkoutRequestId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────────────────────────────

    public Optional<BookingEntity> findByTicketId(String ticketId) {
        return bookingRepository.findByTicketId(ticketId);
    }

    public List<BookingEntity> findByTenant(Long tenantId) {
        return bookingRepository.findByTenantId(tenantId);
    }
}
