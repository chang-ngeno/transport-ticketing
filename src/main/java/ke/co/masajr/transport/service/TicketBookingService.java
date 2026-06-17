package ke.co.masajr.transport.service;

import ke.co.masajr.transport.entity.BookingEntity;
import ke.co.masajr.transport.entity.BookingEntity.PaymentMethod;
import ke.co.masajr.transport.entity.Fare;
import ke.co.masajr.transport.entity.Trip;
import ke.co.masajr.transport.entity.Vehicle;
import ke.co.masajr.transport.repository.BookingRepository;
import ke.co.masajr.transport.repository.FareRepository;
import ke.co.masajr.transport.repository.TripRepository;
import ke.co.masajr.transport.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
    private final VehicleRepository vehicleRepository;
    private final MpesaService mpesaService;
    private final SmsService smsService;
    private TicketBookingService self;

    public TicketBookingService(TripRepository tripRepository,
                                FareRepository fareRepository,
                                BookingRepository bookingRepository,
                                VehicleRepository vehicleRepository,
                                MpesaService mpesaService,
                                SmsService smsService) {
        this.tripRepository = tripRepository;
        this.fareRepository = fareRepository;
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
        this.mpesaService = mpesaService;
        this.smsService = smsService;
        this.self = this;
    }

    @Autowired
    public void setSelf(@Lazy TicketBookingService self) {
        this.self = self;
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
        return bookTicketInternal(tenantId, tripId, phoneNumber, PaymentMethod.MPESA.name());
    }

    @Transactional
    public BookingEntity bookTicket(Long tenantId, Long tripId, String phoneNumber, String paymentMethod) {
        return bookTicketInternal(tenantId, tripId, phoneNumber, paymentMethod);
    }

    private BookingEntity bookTicketInternal(Long tenantId, Long tripId, String phoneNumber, String paymentMethod) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        if (!trip.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Trip does not belong to tenant");
        }

        Vehicle vehicle = vehicleRepository.findById(trip.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for trip: " + tripId));

        if (trip.getBookedSeats() >= trip.getTotalSeats()) {
            throw new IllegalStateException("Vehicle " + vehicle.getRegistrationNumber() + " is full");
        }

        PaymentMethod method = PaymentMethod.valueOf(paymentMethod.toUpperCase());
        if (method == PaymentMethod.MPESA && (phoneNumber == null || phoneNumber.isBlank())) {
            throw new IllegalArgumentException("Mobile number is required for M-PESA bookings");
        }

        BigDecimal price = resolvePrice(tripId, LocalDateTime.now());
        String ticketId = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        BookingEntity booking = new BookingEntity();
        booking.setTenantId(tenantId);
        booking.setTripId(tripId);
        booking.setTicketId(ticketId);
        booking.setPhoneNumber(phoneNumber == null ? "" : phoneNumber);
        booking.setPaymentMethod(method);
        booking.setAmount(price);
        booking.setPricePaid(price);
        booking.setStatus(method == PaymentMethod.CASH ? "PAID" : "PENDING");
        bookingRepository.save(booking);

        trip.setBookedSeats(trip.getBookedSeats() + 1);
        tripRepository.save(trip);

        if (method == PaymentMethod.CASH) {
            if (phoneNumber != null && !phoneNumber.isBlank()) {
                smsService.sendSms(phoneNumber, String.format(
                        "Your ticket %s has been booked and paid in cash. Enjoy your trip!", ticketId));
            }
        } else {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                var stkTask = scope.fork(() ->
                        mpesaService.initiateStk(tenantId, phoneNumber, price.doubleValue(), ticketId));

                scope.fork(() -> {
                    smsService.sendSms(phoneNumber, String.format(
                            "Ticket %s booked. Amount: KES %.2f. Awaiting M-PESA payment.",
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
        }

        return booking;
    }

    public List<BookingEntity> processBatchBookingsWithDefaultPayment(Long tenantId, Long tripId,
                                                                         String... phoneNumbers) throws Exception {
        return processBatchBookings(tenantId, tripId, PaymentMethod.MPESA.name(), phoneNumbers);
    }

    public List<BookingEntity> processBatchBookings(Long tenantId, Long tripId,
                                                    String paymentMethod, String... phoneNumbers) throws Exception {
        if (phoneNumbers == null) return List.of();

        List<BookingEntity> results = new ArrayList<>();

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            @SuppressWarnings("unchecked")
            List<StructuredTaskScope.Subtask<BookingEntity>> tasks = Arrays.stream(phoneNumbers)
                    .filter(Objects::nonNull)
                    .map(phone -> (StructuredTaskScope.Subtask<BookingEntity>) scope.fork(() -> self.bookTicket(tenantId, tripId, phone, paymentMethod)))
                    .toList();

            scope.join();
            scope.throwIfFailed();

            for (var task : tasks) {
                results.add(task.get());
            }
        }

        return results;
    }

    public void processStrictBatchWithDefaultPayment(Long tenantId, Long tripId,
                                                      String... phoneNumbers) throws Exception {
        processStrictBatch(tenantId, tripId, PaymentMethod.MPESA.name(), phoneNumbers);
    }

    public void processStrictBatch(Long tenantId, Long tripId,
                                   String paymentMethod, String... phoneNumbers) throws Exception {
        if (phoneNumbers == null) return;

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            Arrays.stream(phoneNumbers)
                    .filter(Objects::nonNull)
                    .forEach(phone -> scope.fork(() -> {
                        self.bookTicket(tenantId, tripId, phone, paymentMethod);
                        return null;
                    }));
            scope.join();
            scope.throwIfFailed();
        }
    }

    public void processWithVirtualThreadsWithDefaultPayment(Long tenantId, Long tripId, String... phoneNumbers) {
        processWithVirtualThreads(tenantId, tripId, PaymentMethod.MPESA.name(), phoneNumbers);
    }

    public void processWithVirtualThreads(Long tenantId, Long tripId, String paymentMethod, String... phoneNumbers) {
        if (phoneNumbers == null) return;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Arrays.stream(phoneNumbers)
                    .filter(Objects::nonNull)
                    .forEach(phone -> executor.submit(() -> {
                        mpesaRateLimit.acquire();
                        try {
                            self.bookTicket(tenantId, tripId, phone, paymentMethod);
                        } finally {
                            mpesaRateLimit.release();
                        }
                        return null;
                    }));
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
