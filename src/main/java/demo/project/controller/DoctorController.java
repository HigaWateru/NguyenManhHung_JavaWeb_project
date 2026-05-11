package demo.project.controller;

import demo.project.model._enum.Status;
import demo.project.dto.ExaminationDto;
import demo.project.model.Appointment;
import demo.project.model.Doctor;
import demo.project.model.User;
import demo.project.repository.AppointmentRepository;
import demo.project.repository.DoctorRepository;
import demo.project.repository.LabTestTypeRepository;
import demo.project.repository.MedicineRepository;
import demo.project.repository.UserRepository;
import demo.project.service.ExaminationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/doctor")
@RequiredArgsConstructor
public class DoctorController {
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final MedicineRepository medicineRepository;
    private final LabTestTypeRepository labTestTypeRepository;
    private final ExaminationService examinationService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        User user = userRepository.findByUsername(username);
        if (user != null) {
            session.setAttribute("loginUser", user);
            Doctor doctor = doctorRepository.findByUser(user);
            if (doctor != null) {
                List<Appointment> appointments = appointmentRepository.findByDoctorAndStatusIn(doctor,
                        List.of(Status.CONFIRMED, Status.PENDING_PAYMENT, Status.PENDING, Status.COMPLETED));
                model.addAttribute("appointments", appointments);
                
                long pendingCount = appointmentRepository.countByDoctorAndStatus(doctor, Status.CONFIRMED)
                        + appointmentRepository.countByDoctorAndStatus(doctor, Status.PENDING_PAYMENT)
                        + appointmentRepository.countByDoctorAndStatus(doctor, Status.PENDING);
                long completedCount = appointmentRepository.countByDoctorAndStatus(doctor, Status.COMPLETED);
                long totalCount = pendingCount + completedCount;
                
                model.addAttribute("pendingCount", pendingCount);
                model.addAttribute("completedCount", completedCount);
                model.addAttribute("totalCount", totalCount);
            }
        }
        return "doctor/dashboard";
    }

    @GetMapping("/history")
    public String examinationHistory(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        User user = userRepository.findByUsername(username);
        if (user != null) {
            session.setAttribute("loginUser", user);
            Doctor doctor = doctorRepository.findByUser(user);
            if (doctor != null) {
                model.addAttribute("appointments", appointmentRepository.findByDoctorAndStatus(doctor, Status.COMPLETED));
            }
        }
        return "doctor/history";
    }

    @GetMapping("/examine/{appointmentId}")
    public String examineForm(@PathVariable Long appointmentId, Model model) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        if (appointment.getStatus() != Status.CONFIRMED && appointment.getStatus() != Status.PENDING) {
            throw new RuntimeException("Appointment is not ready for examination");
        }
        
        model.addAttribute("appointment", appointment);
        model.addAttribute("examinationDto", new ExaminationDto());
        model.addAttribute("medicines", medicineRepository.findAll());
        model.addAttribute("labTestTypes", labTestTypeRepository.findByActiveTrue());
        return "doctor/examine";
    }

    @PostMapping("/examine/{appointmentId}")
    public String submitExamination(@PathVariable Long appointmentId, @ModelAttribute ExaminationDto dto,
                                    RedirectAttributes redirectAttributes, Model model) {
        try {
            examinationService.saveExaminationResult(appointmentId, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu kết quả khám bệnh thành công!");
            return "redirect:/doctor/dashboard";
        } catch (Exception e) {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new RuntimeException("Appointment not found"));
            model.addAttribute("appointment", appointment);
            model.addAttribute("examinationDto", dto);
            model.addAttribute("medicines", medicineRepository.findAll());
            model.addAttribute("labTestTypes", labTestTypeRepository.findByActiveTrue());
            model.addAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            return "doctor/examine";
        }
    }
}
