package demo.project.controller;

import demo.project.model.Doctor;
import demo.project.model.Profile;
import demo.project.model.User;
import demo.project.repository.SpecialtyRepository;
import demo.project.repository.UserRepository;
import demo.project.service.ProfileService;
import demo.project.service.UploadService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;
    private final UserRepository userRepository;
    private final UploadService uploadService;
    private final SpecialtyRepository specialtyRepository;

    @GetMapping
    public String viewProfile(Model model, Principal principal, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null && principal != null) {
            user = userRepository.findByUsername(principal.getName());
            session.setAttribute("loginUser", user);
        }

        if (user == null) return "redirect:/login";

        Profile profile = profileService.getProfileByUser(user);
        model.addAttribute("profile", profile);
        model.addAttribute("user", user);
        model.addAttribute("backLink", "/home");

        if (user.getRole().name().equals("DOCTOR")) {
            Doctor doctor = profileService.getDoctorByUser(user);
            model.addAttribute("doctor", doctor);
            model.addAttribute("specialties", specialtyRepository.findAll());
            model.addAttribute("backLink", "/doctor/dashboard");
        }

        return "profile";
    }

    @PostMapping("/update")
    public String updateProfile(@ModelAttribute Profile profile,
                                @RequestParam(value = "description", required = false) String description,
                                @RequestParam(value = "specialtyId", required = false) Long specialtyId,
                                @RequestParam(value = "image", required = false) MultipartFile image,
                                HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) return "redirect:/login";

        if (image != null && !image.isEmpty()) {
            String photoUrl = uploadService.upload(image);
            profile.setPhoto(photoUrl);
        }

        profileService.updateProfile(user, profile, description, specialtyId);
        
        // Update session
        session.setAttribute("profile", profileService.getProfileByUser(user));
        
        return "redirect:/profile?success";
    }
}
