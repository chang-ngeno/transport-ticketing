package ke.co.masajr.transport.service;

import ke.co.masajr.transport.dto.ReceiptResponse;
import ke.co.masajr.transport.dto.TripManifestResponse;
import ke.co.masajr.transport.entity.*;
import ke.co.masajr.transport.entity.BookingEntity.PaymentMethod;
import ke.co.masajr.transport.repository.*;
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
 */
@Service
public class TicketBookingService {

    private static final Logger log = LoggerFactory.getLogger(TicketBookingService.class);

    /**
     * Semaphore: max 50 concurrent M-PESA STK calls at any time.
     */
    private final Semaphore mpesaRateLimit = new Semaphore(50);

    private final TripRepository tripRepository;
    private final FareRepository fareRepository;
    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final StageRepository stageRepository;
    private final TenantRepository tenantRepository;
    private final MpesaService mpesaService;
    private final SmsService smsService;
    private TicketBookingService self;

    public TicketBookingService(TripRepository tripRepository,
                                FareRepository fareRepository,
                                BookingRepository bookingRepository,
                                VehicleRepository vehicleRepository,
                                StageRepository stageRepository,
                                TenantRepository tenantRepository,
                                MpesaService mpesaService,
                                SmsService smsService) {
        this.tripRepository = tripRepository;
        this.fareRepository = fareRepository;
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
        this.stageRepository = stageRepository;
        this.tenantRepository = tenantRepository;
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

    @Transactional
    public BookingEntity bookTicket(Long tenantId, Long tripId, String phoneNumber) {
        return bookTicketInternal(tenantId, tripId, phoneNumber, PaymentMethod.MPESA.name(), 1);
    }

    @Transactional
    public BookingEntity bookTicket(Long tenantId, Long tripId, String phoneNumber, String paymentMethod) {
        return bookTicketInternal(tenantId, tripId, phoneNumber, paymentMethod, 1);
    }

    @Transactional
    public BookingEntity bookTicket(Long tenantId, Long tripId, String phoneNumber, String paymentMethod, int passengerCount) {
        return bookTicketInternal(tenantId, tripId, phoneNumber, paymentMethod, passengerCount);
    }

    private BookingEntity bookTicketInternal(Long tenantId, Long tripId, String phoneNumber, String paymentMethod, int passengerCount) {
        if (passengerCount <= 0) {
            passengerCount = 1;
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        if (tenantId != null && !trip.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Trip does not belong to tenant");
        }

        Vehicle vehicle = vehicleRepository.findById(trip.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for trip: " + tripId));

        if (trip.getBookedSeats() + passengerCount > trip.getTotalSeats()) {
            throw new IllegalStateException("Vehicle " + vehicle.getRegistrationNumber() + " does not have enough seats available");
        }

        PaymentMethod method = PaymentMethod.valueOf(paymentMethod.toUpperCase());
        if (method == PaymentMethod.MPESA && (phoneNumber == null || phoneNumber.isBlank())) {
            throw new IllegalArgumentException("Mobile number is required for M-PESA bookings");
        }

        BigDecimal unitPrice = resolvePrice(tripId, LocalDateTime.now());
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(passengerCount));

        // Generate prefix depending on payment mode
        String prefix = method == PaymentMethod.CASH ? "CSH-" : "MPA-";
        String ticketId = prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        BookingEntity booking = new BookingEntity();
        booking.setTenantId(trip.getTenantId());
        booking.setTripId(tripId);
        booking.setTicketId(ticketId);
        booking.setPhoneNumber(phoneNumber == null ? "" : phoneNumber);
        booking.setPaymentMethod(method);
        booking.setAmount(totalPrice);
        booking.setPricePaid(totalPrice);
        booking.setPassengerCount(passengerCount);
        booking.setStatus(method == PaymentMethod.CASH ? "PAID" : "PENDING");
        bookingRepository.save(booking);

        trip.setBookedSeats(trip.getBookedSeats() + passengerCount);
        tripRepository.save(trip);

        if (method == PaymentMethod.CASH) {
            if (phoneNumber != null && !phoneNumber.isBlank()) {
                smsService.sendSms(phoneNumber, String.format(
                        "Your ticket %s for %d passenger(s) has been booked and paid in cash. Enjoy your trip!",
                        ticketId, passengerCount));
            }
        } else {
            // Capture effectively final variables for lambda
            final int finalPassengerCount = passengerCount;
            final BigDecimal finalTotalPrice = totalPrice;
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                var stkTask = scope.fork(() ->
                        mpesaService.initiateStk(trip.getTenantId(), phoneNumber, finalTotalPrice.doubleValue(), ticketId));

                scope.fork(() -> {
                    smsService.sendSms(phoneNumber, String.format(
                            "Ticket %s booked for %d passenger(s). Amount: KES %.2f. Awaiting M-PESA payment.",
                            ticketId, finalPassengerCount, finalTotalPrice));
                    return null;
                });

                scope.join().throwIfFailed();
                booking.setCheckoutRequestId(stkTask.get());
                bookingRepository.save(booking);
            } catch (Exception e) {
                log.error("STK/SMS failed for ticket {}: {}", ticketId, e.getMessage());
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
                    .map(phone -> (StructuredTaskScope.Subtask<BookingEntity>) scope.fork(() -> self.bookTicket(tenantId, tripId, phone, paymentMethod, 1)))
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
                        self.bookTicket(tenantId, tripId, phone, paymentMethod, 1);
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
                            self.bookTicket(tenantId, tripId, phone, paymentMethod, 1);
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
                // Release the seats
                tripRepository.findById(booking.getTripId()).ifPresent(trip -> {
                    trip.setBookedSeats(Math.max(0, trip.getBookedSeats() - booking.getPassengerCount()));
                    tripRepository.save(trip);
                });
            }
            log.info("Booking {} → {} for checkout {}",
                    booking.getTicketId(), booking.getStatus(), checkoutRequestId);
        }, () -> log.warn("No booking for CheckoutRequestID: {}", checkoutRequestId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reports
    // ─────────────────────────────────────────────────────────────────────────

    public ReceiptResponse getReceipt(String ticketId) {
        BookingEntity booking = bookingRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + ticketId));
        Trip trip = tripRepository.findById(booking.getTripId())
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + booking.getTripId()));
        Vehicle vehicle = vehicleRepository.findById(trip.getVehicleId()).orElse(null);

        String fromStageName = "Unknown";
        if (trip.getFromStageId() != null) {
            fromStageName = stageRepository.findById(trip.getFromStageId()).map(Stage::getName).orElse("Unknown");
        }

        String toStageName = "Unknown";
        if (trip.getToStageId() != null) {
            toStageName = stageRepository.findById(trip.getToStageId()).map(Stage::getName).orElse("Unknown");
        }

        String tenantName = "Unknown";
        if (trip.getTenantId() != null) {
            tenantName = tenantRepository.findById(trip.getTenantId()).map(Tenant::getName).orElse("Unknown");
        }

        return new ReceiptResponse(
            booking.getTicketId(),
            booking.getPhoneNumber(),
            booking.getPaymentMethod().name(),
            booking.getAmount(),
            booking.getStatus(),
            booking.getCreatedAt(),
            booking.getPassengerCount(),
            trip.getId(),
            fromStageName,
            toStageName,
            trip.getToDestination(),
            trip.getRoute(),
            trip.getTripStartTime(),
            vehicle != null ? vehicle.getRegistrationNumber() : "N/A",
            tenantName
        );
    }

    public TripManifestResponse getTripManifest(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        Vehicle vehicle = vehicleRepository.findById(trip.getVehicleId()).orElse(null);

        String fromStageName = "Unknown";
        if (trip.getFromStageId() != null) {
            fromStageName = stageRepository.findById(trip.getFromStageId()).map(Stage::getName).orElse("Unknown");
        }

        List<BookingEntity> bookings = bookingRepository.findByTripId(tripId);
        BigDecimal totalAmount = bookings.stream()
                .filter(b -> "PAID".equalsIgnoreCase(b.getStatus()))
                .map(BookingEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TripManifestResponse.PassengerManifestItem> passengerItems = bookings.stream()
                .map(b -> new TripManifestResponse.PassengerManifestItem(
                        b.getTicketId(),
                        b.getPhoneNumber(),
                        b.getPassengerCount(),
                        b.getAmount(),
                        b.getStatus(),
                        b.getPaymentMethod().name()
                ))
                .toList();

        return new TripManifestResponse(
            trip.getId(),
            vehicle != null ? vehicle.getRegistrationNumber() : "N/A",
            trip.getTotalSeats(),
            trip.getBookedSeats(),
            trip.getPricePerSeat(),
            totalAmount,
            fromStageName,
            trip.getToDestination(),
            trip.getTripStartTime(),
            passengerItems
        );
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
