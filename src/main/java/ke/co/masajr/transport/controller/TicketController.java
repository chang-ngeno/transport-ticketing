package ke.co.masajr.transport.controller;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import ke.co.masajr.transport.entity.AppUser;
import ke.co.masajr.transport.entity.BookingEntity;
import ke.co.masajr.transport.service.TicketBookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<BookingEntity> book(@RequestBody Map<String, Object> body,
                                               @AuthenticationPrincipal AppUser caller) {
        Long tripId = Long.parseLong(body.get("tripId").toString());
        String phone = body.get("phoneNumber").toString();
        BookingEntity booking = bookingService.bookTicket(caller.getTenantId(), tripId, phone);
        return ResponseEntity.ok(booking);
    }

    @PreAuthorize("hasAnyRole('STAGE_ATTENDANT','STAGE_HEAD','TENANT_ADMIN','SUPER_ADMIN')")
    @RateLimiter(name = "booking")
    @PostMapping("/book/batch")
    public ResponseEntity<List<BookingEntity>> bookBatch(@RequestBody Map<String, Object> body,
                                                          @AuthenticationPrincipal AppUser caller) throws Exception {
        Long tripId = Long.parseLong(body.get("tripId").toString());
        @SuppressWarnings("unchecked")
        List<String> phones = (List<String>) body.get("phoneNumbers");
        List<BookingEntity> bookings = bookingService.processBatchBookings(
                caller.getTenantId(), tripId, phones.toArray(String[]::new));
        return ResponseEntity.ok(bookings);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{ticketId}")
    public ResponseEntity<?> getTicket(@PathVariable String ticketId) {
        return bookingService.findByTicketId(ticketId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<List<BookingEntity>> listBookings(@AuthenticationPrincipal AppUser caller) {
        return ResponseEntity.ok(bookingService.findByTenant(caller.getTenantId()));
    }
}
