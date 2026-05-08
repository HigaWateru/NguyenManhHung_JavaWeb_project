package demo.project.controller;

import demo.project.dto.AppointmentDto;
import demo.project.dto.DoctorResponse;
import demo.project.model.*;
import demo.project.model._enum.Status;
import demo.project.repository.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/patient")
public class PatientController {
    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionDetailRepository prescriptionDetailRepository;

    @GetMapping("/medical-history")
    public String medicalHistory(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            String username = (String) session.getAttribute("username");
            if (username != null) {
                user = userRepository.findByUsername(username);
                session.setAttribute("loginUser", user);
            }
        }
        
        if (user == null) return "redirect:/login";
        
        model.addAttribute("appointments", appointmentRepository.findByPatientAndStatus(user, Status.COMPLETED));
        return "patient/medical-history";
    }

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        if (username != null) {
            User user = userRepository.findByUsername(username);
            session.setAttribute("loginUser", user);
            model.addAttribute("appointments", appointmentRepository.findByPatient(user));
        }
        return "patient/home";
    }

    @GetMapping("/medical-record/{appointmentId}")
    public String viewMedicalRecord(@PathVariable Long appointmentId, Model model) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        
        MedicalRecord record = medicalRecordRepository.findByAppointment(appointment)
                .orElseThrow(() -> new RuntimeException("Medical record not found"));
        
        Prescription prescription = prescriptionRepository.findByMedicalRecord(record).orElse(null);
        if (prescription != null) {
            List<PrescriptionDetail> details = prescriptionDetailRepository.findByPrescription(prescription);
            model.addAttribute("prescription", prescription);
            model.addAttribute("prescriptionDetails", details);
        }

        model.addAttribute("appointment", appointment);
        model.addAttribute("record", record);
        return "patient/medical-record-detail";
    }

    @GetMapping("/appointments/book")
    public String bookAppointmentForm(Model model) {
        model.addAttribute("appointmentForm", new AppointmentDto());
        model.addAttribute("specialties", specialtyRepository.findAll());
        return "patient/book-appointment";
    }

    @PostMapping("/appointments/book")
    public String bookAppointment(@Valid @ModelAttribute("appointmentForm") AppointmentDto dto,
                                  BindingResult result, HttpSession session, Model model,
                                  RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("username");
        User patient = userRepository.findByUsername(username);

        if (result.hasErrors()) {
            model.addAttribute("specialties", specialtyRepository.findAll());
            return "patient/book-appointment";
        }

        if (dto.getAppointmentDate().equals(LocalDate.now()) && dto.getAppointmentTime().isBefore(LocalTime.now())) {
            result.rejectValue("appointmentTime", "error.pastTime", "Không thể đặt lịch ở khung giờ đã qua");
            model.addAttribute("specialties", specialtyRepository.findAll());
            return "patient/book-appointment";
        }

        Doctor doctor = doctorRepository.findById(dto.getDoctorId()).orElse(null);
        if (doctor == null) {
            result.rejectValue("doctorId", "error.doctor", "Bác sĩ không tồn tại");
            model.addAttribute("specialties", specialtyRepository.findAll());
            return "patient/book-appointment";
        }

        if (appointmentRepository.existsByDoctorAndAppointmentDateAndAppointmentTime(doctor, dto.getAppointmentDate(), dto.getAppointmentTime())) {
            result.rejectValue("appointmentTime", "error.duplicate", "Bác sĩ đã có lịch hẹn vào khung giờ này. Vui lòng chọn khung giờ khác.");
            model.addAttribute("specialties", specialtyRepository.findAll());
            return "patient/book-appointment";
        }

        Appointment appointment = Appointment.builder().patient(patient).doctor(doctor)
                .appointmentDate(dto.getAppointmentDate()).appointmentTime(dto.getAppointmentTime())
                .status(Status.PENDING).build();

        appointmentRepository.save(appointment);
        redirectAttributes.addFlashAttribute("successMessage", "Đặt lịch khám thành công!");
        return "redirect:/patient/home";
    }

    @PostMapping("/appointments/{appointmentId}/cancel")
    public String cancelAppointment(@PathVariable Long appointmentId, HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            String username = (String) session.getAttribute("username");
            user = username != null ? userRepository.findByUsername(username) : null;
        }

        if (user == null) return "redirect:/login";

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getPatient().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền hủy lịch hẹn này.");
            return "redirect:/patient/home";
        }

        if (appointment.getStatus() == Status.PENDING || appointment.getStatus() == Status.CONFIRMED) {
            appointment.setStatus(Status.CANCELLED);
            appointmentRepository.save(appointment);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy lịch hẹn thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Lịch hẹn này không thể hủy.");
        }

        return "redirect:/patient/home";
    }

    @GetMapping("/api/doctors-by-specialty")
    @ResponseBody
    public List<DoctorResponse> getDoctorsBySpecialty(@RequestParam Long specialtyId) {
        return specialtyRepository.findById(specialtyId).map(s -> s.getDoctors().stream()
                        .map(d -> DoctorResponse.builder().id(d.getId())
.fullName(d.getUser() != null && d.getUser().getProfile() != null ? d.getUser().getProfile().getFullName() : "Bác sĩ #" + d.getId())
                                .build()).toList()).orElse(List.of());
    }
}
