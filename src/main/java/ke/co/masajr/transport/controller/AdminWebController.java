package ke.co.masajr.transport.controller;

import ke.co.masajr.transport.entity.Role;
import ke.co.masajr.transport.repository.AppUserRepository;
import ke.co.masajr.transport.repository.StageRepository;
import ke.co.masajr.transport.repository.TenantRepository;
import ke.co.masajr.transport.service.TenantService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller
public class AdminWebController {

    private final TenantService tenantService;
    private final TenantRepository tenantRepository;
    private final AppUserRepository userRepository;
    private final StageRepository stageRepository;

    public AdminWebController(TenantService tenantService,
                              TenantRepository tenantRepository,
                              AppUserRepository userRepository,
                              StageRepository stageRepository) {
        this.tenantService = tenantService;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.stageRepository = stageRepository;
    }

    @GetMapping("/admin/tenants")
    public String tenants(Model model) {
        model.addAttribute("tenants", tenantService.listTenants());
        return "pages/admin/tenants";
    }

    @PostMapping("/admin/tenants")
    public String createTenant(@RequestParam String name,
                               @RequestParam String mpesaShortcode,
                               @RequestParam String consumerKey,
                               @RequestParam String consumerSecret,
                               @RequestParam String passkey,
                               RedirectAttributes ra) {
        try {
            tenantService.createTenant(name, mpesaShortcode, consumerKey, consumerSecret, passkey);
            ra.addFlashAttribute("success", "Tenant '" + name + "' created.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/tenants";
    }

    @GetMapping("/admin/users")
    public String users() {
        // The page loads its data via JS from REST endpoints (/api/admin/users, /api/admin/tenants)
        return "pages/admin/users";
    }

    @PostMapping("/admin/users")
    public String createUser(@RequestParam String username,
                             @RequestParam String password,
                             @RequestParam String role,
                             @RequestParam(required = false) Long tenantId,
                             @RequestParam(required = false) Long stageId,
                             RedirectAttributes ra) {
        try {
            tenantService.createUser(username, password, Role.valueOf(role), tenantId, stageId);
            ra.addFlashAttribute("success", "User '" + username + "' created.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/trips")
    public String trips() { return "redirect:/trips"; }

    @GetMapping("/admin/vehicles")
    public String vehicles() { return "redirect:/vehicles"; }

    @GetMapping("/admin/stages")
    public String stages() { return "redirect:/stages"; }

    @GetMapping("/admin/fares")
    public String fares() { return "redirect:/trips"; }
}
