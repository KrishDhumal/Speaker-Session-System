package com.college.speaker.web;

import com.college.speaker.domain.SessionStatus;
import com.college.speaker.domain.SpeakerSession;
import com.college.speaker.domain.Track;
import com.college.speaker.service.IllegalTransitionException;
import com.college.speaker.service.SpeakerSessionService;
import com.college.speaker.service.StatusWorkflow;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/sessions")
public class SessionController {

    private final SpeakerSessionService service;
    private final StatusWorkflow workflow;

    public SessionController(SpeakerSessionService service, StatusWorkflow workflow) {
        this.service = service;
        this.workflow = workflow;
    }

    @ModelAttribute("allTracks")
    public Track[] allTracks() {
        return Track.values();
    }

    @ModelAttribute("allStatuses")
    public SessionStatus[] allStatuses() {
        return SessionStatus.values();
    }

    /** List + search are the same screen; empty filters mean "show everything". */
    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) SessionStatus status,
                       @RequestParam(required = false) Track track,
                       Model model) {
        List<SpeakerSession> results = service.search(keyword, status, track);
        model.addAttribute("sessions", results);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("track", track);
        model.addAttribute("resultCount", results.size());
        return "sessions/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        SpeakerSession session = new SpeakerSession();
        session.setStartTime(LocalDateTime.now().plusDays(7).withHour(10).withMinute(0).withSecond(0).withNano(0));
        model.addAttribute("speakerSession", session);
        model.addAttribute("formMode", "create");
        return "sessions/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("speakerSession") SpeakerSession session,
                         BindingResult binding,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("formMode", "create");
            return "sessions/form";
        }
        SpeakerSession saved = service.create(session, CurrentUser.username(authentication));
        redirect.addFlashAttribute("message", "Session created as a draft.");
        return "redirect:/sessions/" + saved.getId();
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Authentication authentication, Model model) {
        SpeakerSession session = service.get(id);
        model.addAttribute("speakerSession", session);
        model.addAttribute("transitions",
                workflow.allowedTransitions(session.getStatus(), CurrentUser.roles(authentication)));
        return "sessions/view";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("speakerSession", service.get(id));
        model.addAttribute("formMode", "edit");
        return "sessions/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("speakerSession") SpeakerSession session,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("formMode", "edit");
            return "sessions/form";
        }
        service.update(id, session);
        redirect.addFlashAttribute("message", "Session updated.");
        return "redirect:/sessions/" + id;
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam SessionStatus target,
                               @RequestParam(required = false) String note,
                               Authentication authentication,
                               RedirectAttributes redirect) {
        try {
            service.changeStatus(id, target, CurrentUser.roles(authentication), note);
            redirect.addFlashAttribute("message", "Status changed to " + target.getLabel() + ".");
        } catch (IllegalTransitionException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/sessions/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        service.delete(id);
        redirect.addFlashAttribute("message", "Session deleted.");
        return "redirect:/sessions";
    }
}
