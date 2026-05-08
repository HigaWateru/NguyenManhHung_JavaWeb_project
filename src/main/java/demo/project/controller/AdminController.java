package demo.project.controller;

import demo.project.dto.DoctorDto;
import demo.project.model.Doctor;
import demo.project.model.Medicine;
import demo.project.model.Prescription;
import demo.project.model.PrescriptionDetail;
import demo.project.model.Profile;
import demo.project.model.Specialty;
import demo.project.model.User;
import demo.project.model._enum.Role;
import demo.project.model._enum.Status;
import demo.project.repository.DoctorRepository;
import demo.project.repository.MedicineRepository;
import demo.project.repository.PrescriptionDetailRepository;
import demo.project.repository.PrescriptionRepository;
import demo.project.repository.ProfileRepository;
import demo.project.repository.SpecialtyRepository;
import demo.project.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;
    private final ProfileRepository profileRepository;
    private final MedicineRepository medicineRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionDetailRepository prescriptionDetailRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        if (username != null) {
            User user = userRepository.findByUsername(username);
            session.setAttribute("loginUser", user);
        }
        long approvedCount = prescriptionRepository.countByStatus(Status.CONFIRMED)
                + prescriptionRepository.countByStatus(Status.COMPLETED);
        model.addAttribute("totalMedicineStock", medicineRepository.sumStock());
        model.addAttribute("approvedPrescriptionCount", approvedCount);
        model.addAttribute("pendingPrescriptionCount", prescriptionRepository.countByStatus(Status.PENDING));
        model.addAttribute("specialtyCount", specialtyRepository.count());
        model.addAttribute("recentPrescriptions", prescriptionRepository.findTop5ByOrderByIdDesc());
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
    public String addMedicine(@ModelAttribute("medicine") Medicine medicine, BindingResult result,
                              RedirectAttributes redirectAttributes) {
        normalizeMedicineName(medicine);
        if (isDuplicateMedicineName(medicine.getName(), null)) {
            result.rejectValue("name", "error.medicine", "Tên thuốc đã tồn tại");
            return "admin/medicines/add";
        }

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
    public String updateMedicine(@PathVariable Long id, @ModelAttribute("medicine") Medicine medicine,
                                 BindingResult result, RedirectAttributes redirectAttributes) {
        normalizeMedicineName(medicine);
        if (isDuplicateMedicineName(medicine.getName(), id)) {
            result.rejectValue("name", "error.medicine", "Tên thuốc đã tồn tại");
            medicine.setId(id);
            return "admin/medicines/edit";
        }

        medicine.setId(id);
        medicineRepository.save(medicine);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thuốc thành công!");
        return "redirect:/admin/medicines";
    }

    private void normalizeMedicineName(Medicine medicine) {
        if (medicine.getName() != null) {
            medicine.setName(medicine.getName().trim());
        }
    }

    private boolean isDuplicateMedicineName(String name, Long currentMedicineId) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return currentMedicineId == null
                ? medicineRepository.existsByNameIgnoreCaseTrimmed(name)
                : medicineRepository.existsByNameIgnoreCaseTrimmedAndIdNot(name, currentMedicineId);
    }

    @GetMapping("/medicines/delete/{id}")
    public String deleteMedicine(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        medicineRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa thuốc thành công!");
        return "redirect:/admin/medicines";
    }

    // --- PRESCRIPTION APPROVAL ---
    @GetMapping("/prescriptions")
    public String listPrescriptions(Model model) {
        model.addAttribute("prescriptions", prescriptionRepository.findAll());
        return "admin/prescriptions";
    }

    @Transactional
    @PostMapping("/prescriptions/{id}/approve")
    public String approvePrescription(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid prescription Id:" + id));
        if (prescription.getStatus() != Status.PENDING) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đơn thuốc này đã được xử lý.");
            return "redirect:/admin/prescriptions";
        }

        List<PrescriptionDetail> details = prescriptionDetailRepository.findByPrescription(prescription);
        for (PrescriptionDetail detail : details) {
            Medicine medicine = detail.getMedicine();
            int stock = medicine.getStock() != null ? medicine.getStock() : 0;
            int quantity = detail.getQuantity() != null ? detail.getQuantity() : 0;
            if (stock < quantity) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Không đủ tồn kho cho thuốc " + medicine.getName() + ". Còn " + stock + ", cần " + quantity + ".");
                return "redirect:/admin/prescriptions";
            }
        }

        for (PrescriptionDetail detail : details) {
            Medicine medicine = detail.getMedicine();
            int stock = medicine.getStock() != null ? medicine.getStock() : 0;
            int quantity = detail.getQuantity() != null ? detail.getQuantity() : 0;
            medicine.setStock(stock - quantity);
            medicineRepository.save(medicine);
        }

        prescription.setStatus(Status.CONFIRMED);
        prescriptionRepository.save(prescription);
        redirectAttributes.addFlashAttribute("successMessage", "Duyệt đơn thuốc thành công!");
        return "redirect:/admin/prescriptions";
    }

    @PostMapping("/prescriptions/{id}/reject")
    public String rejectPrescription(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid prescription Id:" + id));
        prescription.setStatus(Status.CANCELLED);
        prescriptionRepository.save(prescription);
        redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối đơn thuốc!");
        return "redirect:/admin/prescriptions";
    }

    // --- SPECIALTY MANAGEMENT ---
    @GetMapping("/specialties")
    public String listSpecialties(Model model) {
        model.addAttribute("specialties", specialtyRepository.findAll());
        model.addAttribute("specialty", new Specialty());
        return "admin/specialties";
    }

    @PostMapping("/specialties/add")
    public String addSpecialty(@ModelAttribute Specialty specialty, RedirectAttributes redirectAttributes) {
        specialtyRepository.save(specialty);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm chuyên khoa thành công!");
        return "redirect:/admin/specialties";
    }

    @PostMapping("/specialties/edit/{id}")
    public String updateSpecialty(@PathVariable Long id, @ModelAttribute Specialty specialty,
                                  RedirectAttributes redirectAttributes) {
        Specialty existingSpecialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid specialty Id:" + id));
        existingSpecialty.setName(specialty.getName());
        existingSpecialty.setDescription(specialty.getDescription());
        specialtyRepository.save(existingSpecialty);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật chuyên khoa thành công!");
        return "redirect:/admin/specialties";
    }

    @GetMapping("/specialties/delete/{id}")
    public String deleteSpecialty(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        specialtyRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa chuyên khoa thành công!");
        return "redirect:/admin/specialties";
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
