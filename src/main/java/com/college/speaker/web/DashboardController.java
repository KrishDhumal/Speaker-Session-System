package com.college.speaker.web;

import com.college.speaker.service.SpeakerSessionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final SpeakerSessionService service;

    public DashboardController(SpeakerSessionService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("total", service.total());
        model.addAttribute("countsByStatus", service.countsByStatus());
        model.addAttribute("countsByTrack", service.countsByTrack());
        model.addAttribute("upcoming", service.upcomingScheduled());
        return "dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
