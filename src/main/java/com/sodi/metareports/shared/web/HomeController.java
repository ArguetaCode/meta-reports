package com.sodi.metareports.shared.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class HomeController {
    @GetMapping("/")
    String home() {
        return "redirect:/admin";
    }

    @GetMapping("/login") String login() { return "auth/login"; }
    @RequestMapping(value = "/access-denied", method = {RequestMethod.GET, RequestMethod.POST})
    String denied() { return "auth/access-denied"; }
}
