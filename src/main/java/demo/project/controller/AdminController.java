package demo.project.controller;

import demo.project.dto.DoctorDto;
import demo.project.model.Doctor;
import demo.project.model.Medicine;
import demo.project.model.Profile;
import demo.project.model.User;
import demo.project.model._enum.Role;
import demo.project.repository.DoctorRepository;
import demo.project.repository.MedicineRepository;
import demo.project.repository.ProfileRepository;
import demo.project.repository.SpecialtyRepository;
import demo.project.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;
    private final ProfileRepository profileRepository;
    private final MedicineRepository medicineRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        if (username != null) {
            User user = userRepository.findByUsername(username);
            session.setAttribute("loginUser", user);
        }
        return "admin/dashboard";
    }

    // --- MEDICINE MANAGEMENT ---

    @GetMapping("/medicines")
    public String listMedicines(Model model) {
        model.addAttribute("medicines", medicineRepository.findAll());
        return "admin/medicines/list";
    }

    @GetMapping("/medicines/add")
    public String addMedicineForm(Model model) {
        model.addAttribute("medicine", new Medicine());
        return "admin/medicines/add";
    }

    @PostMapping("/medicines/add")
    public String addMedicine(@ModelAttribute Medicine medicine, RedirectAttributes redirectAttributes) {
        medicineRepository.save(medicine);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm thuốc mới thành công!");
        return "redirect:/admin/medicines";
    }

    @GetMapping("/medicines/edit/{id}")
    public String editMedicineForm(@PathVariable Long id, Model model) {
        Medicine medicine = medicineRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid medicine Id:" + id));
        model.addAttribute("medicine", medicine);
        return "admin/medicines/edit";
    }

    @PostMapping("/medicines/edit/{id}")
    public String updateMedicine(@PathVariable Long id, @ModelAttribute Medicine medicine, RedirectAttributes redirectAttributes) {
        medicine.setId(id);
        medicineRepository.save(medicine);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thuốc thành công!");
        return "redirect:/admin/medicines";
    }

    @GetMapping("/medicines/delete/{id}")
    public String deleteMedicine(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        medicineRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa thuốc thành công!");
        return "redirect:/admin/medicines";
    }

    // --- DOCTOR MANAGEMENT ---

    @GetMapping("/doctors/add")
    public String addDoctorForm(Model model) {
        model.addAttribute("doctorForm", new DoctorDto());
        model.addAttribute("specialties", specialtyRepository.findAll());
        return "admin/add-doctor";
    }

    @PostMapping("/doctors/add")
    public String addDoctor(@Valid @ModelAttribute("doctorForm") DoctorDto dto, BindingResult result, Model model,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("specialties", specialtyRepository.findAll());
            return "admin/add-doctor";
        }

        if (userRepository.findByUsername(dto.getUsername()) != null) {
            result.rejectValue("username", "error.username", "Tên đăng nhập đã tồn tại");
            model.addAttribute("specialties", specialtyRepository.findAll());
            return "admin/add-doctor";
        }

        User user = User.builder().username(dto.getUsername()).password(passwordEncoder.encode(dto.getPassword()))
                .role(Role.DOCTOR).isActive(true).build();
        userRepository.save(user);

        Profile profile = Profile.builder().user(user).fullName(dto.getFullName()).phone(dto.getPhone())
                .address(dto.getAddress()).build();
        profileRepository.save(profile);

        Doctor doctor = Doctor.builder().user(user).description(dto.getDescription()).build();
        doctor.setSpecialty(specialtyRepository.findById(dto.getSpecialtyId()).orElse(null));
        doctorRepository.save(doctor);

        redirectAttributes.addFlashAttribute("successMessage", "Thêm bác sĩ thành công!");
        return "redirect:/admin/dashboard";
    }
}
