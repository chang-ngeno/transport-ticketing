package ke.co.masajr.transport.controller;

import ke.co.masajr.transport.entity.AppUser;
import ke.co.masajr.transport.entity.BookingEntity;
import ke.co.masajr.transport.entity.Trip;
import ke.co.masajr.transport.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WebController {

    private final BookingRepository bookingRepository;
    private final StageRepository stageRepository;
    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;
    private final AppUserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final FareRepository fareRepository;

    public WebController(BookingRepository bookingRepository,
                         StageRepository stageRepository,
                         TripRepository tripRepository,
                         VehicleRepository vehicleRepository,
                         AppUserRepository userRepository,
                         TenantRepository tenantRepository,
                         FareRepository fareRepository) {
        this.bookingRepository = bookingRepository;
        this.stageRepository = stageRepository;
        this.tripRepository = tripRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.fareRepository = fareRepository;
    }

    private AppUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getPrincipal() instanceof AppUser u) ? u : null;
    }

    @GetMapping("/")
    public String index() {
        return "pages/landing";
    }

    @GetMapping("/login")
    public String login() { return "pages/login"; }

    @GetMapping("/favicon.ico")
    public String favicon() { return "redirect:/icons/icon-192.png.svg"; }

    @GetMapping("/offline")
    public String offline() { return "pages/offline"; }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        AppUser user = currentUser();
        if (user == null) return "redirect:/login";
        Long tenantId = user.getTenantId();
        Long stageId  = user.getStageId();
        LocalDateTime now = LocalDateTime.now();

        long total, paid, pending, failed, activeTrips;
        if (tenantId != null) {
            total       = bookingRepository.countByTenantId(tenantId);
            paid        = bookingRepository.countByTenantIdAndStatus(tenantId, "PAID");
            pending     = bookingRepository.countByTenantIdAndStatus(tenantId, "PENDING");
            failed      = bookingRepository.countByTenantIdAndStatus(tenantId, "FAILED");
            activeTrips = tripRepository.countByTenantIdAndTripStartTimeAfter(tenantId, now);
        } else {
            total       = bookingRepository.count();
            paid        = bookingRepository.countByStatus("PAID");
            pending     = bookingRepository.countByStatus("PENDING");
            failed      = bookingRepository.countByStatus("FAILED");
            activeTrips = tripRepository.countByTripStartTimeAfter(now);
        }

        long activeVehicles = stageId != null
                ? vehicleRepository.countByStageIdAndIsActive(stageId, true)
                : vehicleRepository.countByIsActive(true);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBookings",  total);
        stats.put("paidBookings",   paid);
        stats.put("pendingBookings", pending);
        stats.put("failedBookings", failed);
        stats.put("activeTrips",    activeTrips);
        stats.put("activeVehicles", activeVehicles);

        List<Trip> recentTrips = tenantId != null
                ? tripRepository.findTop10ByTenantIdAndTripStartTimeAfterOrderByTripStartTimeAsc(tenantId, now)
                : tripRepository.findTop10ByTripStartTimeAfterOrderByTripStartTimeAsc(now);

        List<BookingEntity> recentBookings = tenantId != null
                ? bookingRepository.findTop10ByTenantIdOrderByCreatedAtDesc(tenantId)
                : bookingRepository.findTop10ByOrderByCreatedAtDesc();

        model.addAttribute("stats", stats);
        model.addAttribute("recentTrips", recentTrips);
        model.addAttribute("recentBookings", recentBookings);
        return "pages/dashboard";
    }

    @GetMapping("/stages")
    public String stages(Model model) {
        AppUser user = currentUser();
        if (user == null) return "redirect:/login";
        Long tenantId = user.getTenantId();
        model.addAttribute("stages", tenantId != null
                ? stageRepository.findByTenantId(tenantId)
                : stageRepository.findAll());
        model.addAttribute("tenants", tenantRepository.findAll());
        return "pages/stages";
    }

    @GetMapping("/trips")
    public String trips(Model model) {
        AppUser user = currentUser();
        if (user == null) return "redirect:/login";
        Long tenantId = user.getTenantId();
        model.addAttribute("trips", tenantId != null
                ? tripRepository.findByTenantId(tenantId)
                : tripRepository.findAll());
        model.addAttribute("stages", tenantId != null
                ? stageRepository.findByTenantId(tenantId)
                : stageRepository.findAll());
        model.addAttribute("tenants", tenantRepository.findAll());
        return "pages/trips";
    }

    @GetMapping("/vehicles")
    public String vehicles(Model model) {
        AppUser user = currentUser();
        if (user == null) return "redirect:/login";
        Long stageId = user.getStageId();
        model.addAttribute("vehicles", stageId != null
                ? vehicleRepository.findByStageId(stageId)
                : vehicleRepository.findAll());
        model.addAttribute("stages", stageId != null ? List.of() : stageRepository.findAll());
        return "pages/vehicles";
    }

    @GetMapping("/tickets")
    public String tickets(Model model,
                          @RequestParam(required = false) String search,
                          @RequestParam(required = false) String status) {
        AppUser user = currentUser();
        if (user == null) return "redirect:/login";
        Long tenantId = user.getTenantId();
        List<BookingEntity> all = tenantId != null
                ? bookingRepository.findByTenantId(tenantId)
                : bookingRepository.findAll();

        List<BookingEntity> filtered = all.stream()
                .filter(b -> search == null || search.isBlank()
                        || b.getTicketId().toLowerCase().contains(search.toLowerCase())
                        || b.getPhoneNumber().contains(search))
                .filter(b -> status == null || status.isBlank() || b.getStatus().equals(status))
                .toList();

        model.addAttribute("bookings", filtered);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        return "pages/tickets";
    }

    // /bookings redirects to /tickets (same data, avoid duplication)
    @GetMapping("/bookings")
    public String bookings() { return "redirect:/tickets"; }

    @GetMapping({"/book", "/tickets/book"})
    public String bookTicket(Model model, @RequestParam(required = false) Long tripId) {
        AppUser user = currentUser();
        if (user == null) return "redirect:/login";
        Long tenantId = user.getTenantId();
        model.addAttribute("trips", tenantId != null
                ? tripRepository.findByTenantId(tenantId)
                : tripRepository.findAll());
        model.addAttribute("selectedTripId", tripId);
        return "pages/book";
    }

    @GetMapping("/fares")
    public String fares() { return "redirect:/trips"; }

    // Note: Admin Users page route is provided by AdminWebController to avoid duplicates
}
