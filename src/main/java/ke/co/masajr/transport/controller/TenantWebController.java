package ke.co.masajr.transport.controller;

import ke.co.masajr.transport.entity.AppUser;
import ke.co.masajr.transport.entity.Role;
import ke.co.masajr.transport.repository.AppUserRepository;
import ke.co.masajr.transport.repository.StageRepository;
import ke.co.masajr.transport.repository.TenantRepository;
import ke.co.masajr.transport.repository.TripRepository;
import ke.co.masajr.transport.repository.VehicleRepository;
import ke.co.masajr.transport.service.TenantService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Controller
public class TenantWebController {

    private final TenantService tenantService;
    private final StageRepository stageRepository;
    private final AppUserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;

    public TenantWebController(TenantService tenantService,
                               StageRepository stageRepository,
                               AppUserRepository userRepository,
                               TenantRepository tenantRepository,
                               TripRepository tripRepository,
                               VehicleRepository vehicleRepository) {
        this.tenantService = tenantService;
        this.stageRepository = stageRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.tripRepository = tripRepository;
        this.vehicleRepository = vehicleRepository;
    }

    // ── Redirect shims ────────────────────────────────────────────────────────

    @GetMapping("/tenant/trips")
    public String trips() { return "redirect:/trips"; }

    @GetMapping("/tenant/stages")
    public String stages() { return "redirect:/stages"; }

    @GetMapping("/tenant/vehicles")
    public String vehicles() { return "redirect:/vehicles"; }

    // ── Stages ────────────────────────────────────────────────────────────────

    @PostMapping("/tenant/stages")
    public String createStage(@RequestParam String name,
                              @RequestParam(required = false) String location,
                              @RequestParam(required = false) Long tenantId,
                              @AuthenticationPrincipal AppUser caller,
                              RedirectAttributes ra) {
        try {
            Long tid = caller.getTenantId() != null ? caller.getTenantId() : tenantId;
            if (tid == null) throw new IllegalArgumentException("Tenant is required.");
            tenantService.createStage(tid, name, location);
            ra.addFlashAttribute("success", "Stage '" + name + "' created.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stages";
    }

    // ── Trips ─────────────────────────────────────────────────────────────────

    @PostMapping("/tenant/trips")
    public String createTrip(@RequestParam Long fromStageId,
                             @RequestParam String toDestination,
                             @RequestParam(required = false) String route,
                             @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime departureTime,
                             @RequestParam int totalSeats,
                             @RequestParam BigDecimal basePrice,
                             @RequestParam(required = false) Long tenantId,
                             @AuthenticationPrincipal AppUser caller,
                             RedirectAttributes ra) {
        try {
            Long tid = caller.getTenantId() != null ? caller.getTenantId() : tenantId;
            if (tid == null) throw new IllegalArgumentException("Tenant is required.");
            tenantService.createTrip(tid, fromStageId, toDestination,
                    route, departureTime, totalSeats, basePrice);
            ra.addFlashAttribute("success", "Trip to '" + toDestination + "' created.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/trips";
    }

    // ── Fares ─────────────────────────────────────────────────────────────────

    @GetMapping("/tenant/trips/{tripId}/fares")
    public String faresPage(@PathVariable Long tripId, Model model) {
        model.addAttribute("trip", tripRepository.findById(tripId)
                .orElseGet(() -> {
                    var t = new ke.co.masajr.transport.entity.Trip();
                    t.setId(tripId); return t;
                }));
        model.addAttribute("fares", tenantService.listFares(tripId));
        return "pages/fares";
    }

    @PostMapping("/tenant/trips/{tripId}/fares")
    public String createFare(@PathVariable Long tripId,
                             @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime effectiveFrom,
                             @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime effectiveTo,
                             @RequestParam BigDecimal price,
                             @AuthenticationPrincipal AppUser caller,
                             RedirectAttributes ra) {
        try {
            tenantService.createFare(tripId, effectiveFrom, effectiveTo, price, caller.getId());
            ra.addFlashAttribute("success", "Fare window added.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tenant/trips/" + tripId + "/fares";
    }

    // ── Vehicles ──────────────────────────────────────────────────────────────

    @PostMapping("/stage/vehicles")
    public String createVehicle(@RequestParam String registrationNumber,
                                @RequestParam int capacity,
                                @RequestParam(required = false) Long stageId,
                                @AuthenticationPrincipal AppUser caller,
                                RedirectAttributes ra) {
        try {
            Long sid = caller.getStageId() != null ? caller.getStageId() : stageId;
            if (sid == null) throw new IllegalArgumentException("No stage assigned.");
            tenantService.createVehicle(sid, registrationNumber, capacity);
            ra.addFlashAttribute("success", "Vehicle " + registrationNumber + " registered.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/vehicles";
    }

    @PostMapping("/stage/vehicles/{id}/toggle")
    public String toggleVehicle(@PathVariable Long id, RedirectAttributes ra) {
        try {
            var v = vehicleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Vehicle not found: " + id));
            tenantService.toggleVehicle(id, !v.getIsActive());
            ra.addFlashAttribute("success", "Vehicle updated.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/vehicles";
    }

    // ── Users (Tenant) ────────────────────────────────────────────────────────

    @GetMapping("/tenant/users")
    public String tenantUsers(Model model, @AuthenticationPrincipal AppUser caller) {
        Long tenantId = caller.getTenantId();
        model.addAttribute("users", userRepository.findAll().stream()
                .filter(u -> tenantId == null || tenantId.equals(u.getTenantId()))
                .toList());
        model.addAttribute("stages", tenantId != null
                ? stageRepository.findByTenantId(tenantId)
                : stageRepository.findAll());
        return "pages/tenant/users";
    }

    @PostMapping("/tenant/users")
    public String createUser(@RequestParam String username,
                             @RequestParam String password,
                             @RequestParam String role,
                             @RequestParam(required = false) Long stageId,
                             @AuthenticationPrincipal AppUser caller,
                             RedirectAttributes ra) {
        try {
            tenantService.createUser(username, password, Role.valueOf(role), caller.getTenantId(), stageId);
            ra.addFlashAttribute("success", "User '" + username + "' created.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tenant/users";
    }
}
