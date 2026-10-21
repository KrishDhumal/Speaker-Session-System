package com.college.speaker.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;

/**
 * One-click sign-in for the three demo accounts, so a demonstration does not
 * stop to type a password.
 *
 * <p>This bypasses password checking by design, so it is limited to the three
 * known demo usernames and can be switched off entirely with
 * app.demo-login.enabled=false — which any real deployment should do.
 */
@Controller
@ConditionalOnProperty(name = "app.demo-login.enabled", havingValue = "true", matchIfMissing = true)
public class DemoLoginController {

    private static final Set<String> DEMO_USERS = Set.of("speaker", "reviewer", "organizer");

    private final UserDetailsService userDetailsService;

    public DemoLoginController(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/demo-login")
    public String signInAs(@RequestParam String username, HttpServletRequest request) {
        if (!DEMO_USERS.contains(username)) {
            return "redirect:/login?error";
        }
        UserDetails user = userDetailsService.loadUserByUsername(username);
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        // A new session id for the new login, then store the context in it.
        request.changeSessionId();
        request.getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return "redirect:/dashboard";
    }
}
