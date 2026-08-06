package com.sodi.metareports.shared.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    String home() {
        return "redirect:/admin";
    }

    @GetMapping("/login") String login() { return "auth/login"; }
    @GetMapping("/access-denied") String denied() { return "auth/access-denied"; }
}
