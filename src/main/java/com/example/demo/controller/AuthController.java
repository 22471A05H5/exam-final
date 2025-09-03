package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.entity.User.UserRole;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        User user = userService.findByUsername(username).orElse(null);
        if (user != null) {
            UserRole role = user.getRole();
            switch (role) {
                case SUPERADMIN:
                    return "redirect:/superadmin/dashboard";
                case PRINCIPAL:
                    return "redirect:/principal/dashboard";
                case HOD:
                    return "redirect:/hod/dashboard";
                case ASSISTANT:
                    return "redirect:/assistant/dashboard";
                case FACULTY:
                    return "redirect:/faculty/dashboard";
                case STUDENT:
                    return "redirect:/student/dashboard";
                default:
                    return "redirect:/login?error=true";
            }
        }
        return "redirect:/login?error=true";
    }
}
