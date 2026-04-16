package ke.co.masajr.transport.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TenantWebController {

    @GetMapping("/tenant/trips")
    public String trips() { return "redirect:/trips"; }

    @GetMapping("/tenant/stages")
    public String stages() { return "redirect:/stages"; }

    @GetMapping("/tenant/vehicles")
    public String vehicles() { return "redirect:/vehicles"; }

    @GetMapping("/tenant/users")
    public String users() { return "pages/tenant/users"; }
}
