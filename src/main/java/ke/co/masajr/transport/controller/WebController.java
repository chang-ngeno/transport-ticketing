package ke.co.masajr.transport.controller;

import ke.co.masajr.transport.entity.AppUser;
import ke.co.masajr.transport.entity.BookingEntity;
import ke.co.masajr.transport.entity.Trip;
import ke.co.masajr.transport.repository.BookingRepository;
import ke.co.masajr.transport.repository.TripRepository;
import ke.co.masajr.transport.repository.VehicleRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WebController {

    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;

    public WebController(BookingRepository bookingRepository,
                         TripRepository tripRepository,
                         VehicleRepository vehicleRepository) {
        this.bookingRepository = bookingRepository;
        this.tripRepository = tripRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @GetMapping("/")
    public String index() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = auth != null && auth.isAuthenticated() &&
                (auth.getPrincipal() != "anonymousUser");
        return loggedIn ? "redirect:/dashboard" : "redirect:/login";
    }

    @GetMapping("/login")
    public String login() { return "pages/login"; }

    // Serve favicon by redirecting to our SVG app icon
    @GetMapping("/favicon.ico")
    public String favicon() { return "redirect:/icons/icon-192.png.svg"; }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Determine scope from authenticated principal
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long tenantId = null;
        Long stageId = null;
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AppUser user) {
            tenantId = user.getTenantId();
            stageId = user.getStageId();
        }

        // Build stats map with safe defaults and scoping
        long total;
        long paid;
        long pending;
        long failed;
        long activeTrips;
        long activeVehicles;

        LocalDateTime now = LocalDateTime.now();

        if (tenantId != null) {
            total = bookingRepository.countByTenantId(tenantId);
            paid = bookingRepository.countByTenantIdAndStatus(tenantId, "PAID");
            pending = bookingRepository.countByTenantIdAndStatus(tenantId, "PENDING");
            failed = bookingRepository.countByTenantIdAndStatus(tenantId, "FAILED");
            activeTrips = tripRepository.countByTenantIdAndDepartureTimeAfter(tenantId, now);
        } else {
            total = bookingRepository.count();
            paid = bookingRepository.countByStatus("PAID");
            pending = bookingRepository.countByStatus("PENDING");
            failed = bookingRepository.countByStatus("FAILED");
            activeTrips = tripRepository.countByDepartureTimeAfter(now);
        }

        // Vehicles: prefer stage-scoped count if available; fallback to global active
        if (stageId != null) {
            activeVehicles = vehicleRepository.countByStageIdAndIsActive(stageId, true);
        } else {
            activeVehicles = vehicleRepository.countByIsActive(true);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBookings", total);
        stats.put("paidBookings", paid);
        stats.put("pendingBookings", pending);
        stats.put("failedBookings", failed);
        stats.put("activeTrips", activeTrips);
        stats.put("activeVehicles", activeVehicles);

        // Recent items (scoped if tenant available)
        List<Trip> recentTrips = (tenantId != null)
                ? tripRepository.findTop10ByTenantIdAndDepartureTimeAfterOrderByDepartureTimeAsc(tenantId, now)
                : tripRepository.findTop10ByDepartureTimeAfterOrderByDepartureTimeAsc(now);

        List<BookingEntity> recentBookings = (tenantId != null)
                ? bookingRepository.findTop10ByTenantIdOrderByCreatedAtDesc(tenantId)
                : bookingRepository.findTop10ByOrderByCreatedAtDesc();

        model.addAttribute("stats", stats);
        model.addAttribute("recentTrips", recentTrips);
        model.addAttribute("recentBookings", recentBookings);

        return "pages/dashboard";
    }

    @GetMapping("/bookings")
    public String bookings() { return "pages/bookings"; }

    @GetMapping("/tickets")
    public String tickets() { return "pages/tickets"; }

    @GetMapping("/stages")
    public String stages() { return "pages/stages"; }

    @GetMapping("/trips")
    public String trips() { return "pages/trips"; }

    @GetMapping("/fares")
    public String fares() { return "pages/fares"; }

    @GetMapping("/vehicles")
    public String vehicles() { return "pages/vehicles"; }

    @GetMapping("/book")
    public String book() { return "pages/book"; }

    // Preferred route used by templates and manifest shortcuts
    @GetMapping("/tickets/book")
    public String bookTicket() { return "pages/book"; }

    @GetMapping("/offline")
    public String offline() { return "pages/offline"; }
}
