package ke.co.masajr.transport.controller;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import ke.co.masajr.transport.dto.BatchBookingRequest;
import ke.co.masajr.transport.dto.BookingRequest;
import ke.co.masajr.transport.entity.AppUser;
import ke.co.masajr.transport.entity.BookingEntity;
import ke.co.masajr.transport.service.TicketBookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketBookingService bookingService;

    public TicketController(TicketBookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PreAuthorize("hasAnyRole('STAGE_ATTENDANT','STAGE_HEAD','TENANT_ADMIN','SUPER_ADMIN')")
    @RateLimiter(name = "booking")
    @PostMapping("/book")
    public ResponseEntity<BookingEntity> book(@Valid @RequestBody BookingRequest request,
                                               @AuthenticationPrincipal AppUser caller) {
        if (request.paymentMethod().equalsIgnoreCase("MPESA") &&
                (request.phoneNumber() == null || request.phoneNumber().isBlank())) {
            throw new IllegalArgumentException("Mobile number is required for M-PESA bookings");
        }

        int count = request.passengerCount() != null ? request.passengerCount() : 1;
        BookingEntity booking = bookingService.bookTicket(
                caller.getTenantId(),
                request.tripId(),
                request.phoneNumber(),
                request.paymentMethod(),
                count
        );
        return ResponseEntity.ok(booking);
    }

    @PreAuthorize("hasAnyRole('STAGE_ATTENDANT','STAGE_HEAD','TENANT_ADMIN','SUPER_ADMIN')")
    @RateLimiter(name = "booking")
    @PostMapping("/book/batch")
    public ResponseEntity<List<BookingEntity>> bookBatch(@Valid @RequestBody BatchBookingRequest request,
                                                          @AuthenticationPrincipal AppUser caller) throws Exception {
        List<String> phoneNumbers = request.phoneNumbers().stream()
            .filter(phone -> phone != null && !phone.isBlank())
            .toList();

        if (request.paymentMethod().equalsIgnoreCase("MPESA") && phoneNumbers.isEmpty()) {
            throw new IllegalArgumentException("At least one mobile number is required for M-PESA batch bookings");
        }

        List<BookingEntity> bookings = bookingService.processBatchBookings(
                caller.getTenantId(),
                request.tripId(),
                request.paymentMethod(),
                phoneNumbers.toArray(String[]::new)
        );
        return ResponseEntity.ok(bookings);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{ticketId}")
    public ResponseEntity<?> getTicket(@PathVariable String ticketId) {
        return bookingService.findByTicketId(ticketId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{ticketId}/receipt")
    public ResponseEntity<?> getTicketReceipt(@PathVariable String ticketId) {
        return ResponseEntity.ok(bookingService.getReceipt(ticketId));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<List<BookingEntity>> listBookings(@AuthenticationPrincipal AppUser caller) {
        return ResponseEntity.ok(bookingService.findByTenant(caller.getTenantId()));
    }
}
