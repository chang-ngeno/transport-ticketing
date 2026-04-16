package ke.co.masajr.transport.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminWebController {

    @GetMapping("/admin/tenants")
    public String tenants() { return "pages/admin/tenants"; }

    @GetMapping("/admin/users")
    public String users() { return "pages/admin/users"; }

    @GetMapping("/admin/trips")
    public String trips() { return "redirect:/trips"; }

    @GetMapping("/admin/vehicles")
    public String vehicles() { return "redirect:/vehicles"; }

    @GetMapping("/admin/stages")
    public String stages() { return "redirect:/stages"; }

    @GetMapping("/admin/fares")
    public String fares() { return "redirect:/trips"; }
}
