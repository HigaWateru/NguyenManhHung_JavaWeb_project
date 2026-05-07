package demo.project.controller;

import demo.project.dto.LoginDto;
import demo.project.dto.RegisterDto;
import demo.project.model.User;
import demo.project.model._enum.Role;
import demo.project.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping({"/", "/home"})
    public String index(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            var authorities = authentication.getAuthorities();
            if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                return "redirect:/admin/dashboard";
            } else if (authorities.stream().anyMatch(a -> a.getAuthority().equals("DOCTOR"))) {
                return "redirect:/doctor/dashboard";
            } else {
                return "redirect:/patient/home";
            }
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("loginForm", new LoginDto());
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@Valid @ModelAttribute("loginForm") LoginDto dto, BindingResult result) {
        if (result.hasErrors()) return "login";
        return "forward:/process-login";
    }

    @PostMapping("/login-manual-backup")
    public String loginManual(@Valid @ModelAttribute("loginForm") LoginDto dto, BindingResult result,
                              HttpSession session) {
        if (result.hasErrors()) return "login";
        User foundUser = userRepository.findByUsername(dto.getUsername());
        if (foundUser == null) {
            result.rejectValue("username", "error.username", "Tên đăng nhập không tồn tại");
            return "login";
        }

        if (!passwordEncoder.matches(dto.getPassword(), foundUser.getPassword())) {
            result.rejectValue("password", "error.password", "Mật khẩu không chính xác");
            return "login";
        }

        session.setAttribute("username", foundUser.getUsername());
        session.setAttribute("loginUser", foundUser);
        if (foundUser.getRole().equals(Role.ADMIN)) return "redirect:/admin/dashboard";
        if (foundUser.getRole().equals(Role.DOCTOR)) return "redirect:/doctor/dashboard";

        return "redirect:/home";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registerForm", new RegisterDto());
        return "register";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/403";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterDto dto, BindingResult result) {
        if (result.hasErrors()) return "register";

        if (userRepository.findByUsername(dto.getUsername()) != null) {
            result.rejectValue("username", "error.username", "Tên đăng nhập đã tồn tại");
            return "register";
        }

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.confirmPassword", "Mật khẩu xác nhận không khớp");
            return "register";
        }

        User user = User.builder().username(dto.getUsername()).password(passwordEncoder.encode(dto.getPassword())).
                role(Role.PATIENT).isActive(true).build();

        userRepository.save(user);
        return "redirect:/login";
    }
}
