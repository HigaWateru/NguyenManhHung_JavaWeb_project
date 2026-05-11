package demo.project.controller;

import demo.project.dto.DoctorDto;
import demo.project.model.Appointment;
import demo.project.model.Doctor;
import demo.project.model.LabTestType;
import demo.project.model.Medicine;
import demo.project.model.Prescription;
import demo.project.model.PrescriptionDetail;
import demo.project.model.Profile;
import demo.project.model.Specialty;
import demo.project.model.User;
import demo.project.model._enum.PaymentStatus;
import demo.project.model._enum.Role;
import demo.project.model._enum.Status;
import demo.project.repository.AppointmentRepository;
import demo.project.repository.DoctorRepository;
import demo.project.repository.LabTestTypeRepository;
import demo.project.repository.MedicineRepository;
import demo.project.repository.PaymentRepository;
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
    private final LabTestTypeRepository labTestTypeRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionDetailRepository prescriptionDetailRepository;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
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
        model.addAttribute("labTestTypeCount", labTestTypeRepository.count());
        model.addAttribute("recentPrescriptions", prescriptionRepository.findTop5ByOrderByIdDesc());
        return "admin/dashboard";
    }

    // lab test type
    @GetMapping("/lab-tests")
    public String listLabTestTypes(Model model) {
        model.addAttribute("labTestTypes", labTestTypeRepository.findAll());
        model.addAttribute("labTestType", LabTestType.builder().active(true).build());
        return "admin/lab-tests";
    }

    @PostMapping("/lab-tests/add")
    public String addLabTestType(@ModelAttribute LabTestType labTestType, RedirectAttributes redirectAttributes) {
        normalizeLabTestType(labTestType);
        labTestTypeRepository.save(labTestType);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm loại xét nghiệm thành công!");
        return "redirect:/admin/lab-tests";
    }

    @PostMapping("/lab-tests/edit/{id}")
    public String updateLabTestType(@PathVariable Long id, @ModelAttribute LabTestType labTestType,
                                    RedirectAttributes redirectAttributes) {
        LabTestType existingLabTestType = labTestTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid lab test type Id:" + id));
        existingLabTestType.setName(labTestType.getName());
        existingLabTestType.setDescription(labTestType.getDescription());
        existingLabTestType.setPrice(labTestType.getPrice());
        existingLabTestType.setActive(Boolean.TRUE.equals(labTestType.getActive()));
        normalizeLabTestType(existingLabTestType);
        labTestTypeRepository.save(existingLabTestType);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật loại xét nghiệm thành công!");
        return "redirect:/admin/lab-tests";
    }

    @GetMapping("/lab-tests/delete/{id}")
    public String deleteLabTestType(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        LabTestType labTestType = labTestTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid lab test type Id:" + id));
        labTestType.setActive(false);
        labTestTypeRepository.save(labTestType);
        redirectAttributes.addFlashAttribute("successMessage", "Đã tạm ngưng loại xét nghiệm!");
        return "redirect:/admin/lab-tests";
    }

    private void normalizeLabTestType(LabTestType labTestType) {
        if (labTestType.getName() != null) {
            labTestType.setName(labTestType.getName().trim());
        }
        if (labTestType.getActive() == null) {
            labTestType.setActive(false);
        }
    }

    // medicine
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

    //  prescription approval
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

        Appointment appointment = prescription.getMedicalRecord().getAppointment();
        if (appointment != null) {
            boolean hasUnpaidPayment = paymentRepository.existsByAppointmentAndStatus(appointment, PaymentStatus.UNPAID);
            appointment.setStatus(hasUnpaidPayment ? Status.PENDING_PAYMENT : Status.COMPLETED);
            appointmentRepository.save(appointment);
        }

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

    // specialty
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

    // doctor
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
