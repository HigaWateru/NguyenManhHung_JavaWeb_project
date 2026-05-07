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

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login-manual-backup")
    public String loginManual(@Valid @ModelAttribute("loginForm") LoginDto dto, BindingResult result, HttpSession session) {
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

        User user = User.builder().username(dto.getUsername()).password(passwordEncoder.encode(dto.getPassword())).role(Role.PATIENT).isActive(true).build();

        userRepository.save(user);
        return "redirect:/login";
    }
}
