package demo.project.controller;

import demo.project.dto.AppointmentDto;
import demo.project.dto.DoctorResponse;
import demo.project.model.*;
import demo.project.model._enum.PaymentStatus;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final LabTestResultRepository labTestResultRepository;
    private final PaymentRepository paymentRepository;

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
            List<Appointment> appointments = appointmentRepository.findByPatientAndStatusNot(user, Status.CANCELLED)
                    .stream().filter(appointment -> appointment.getPatient().isActive()).toList();
            model.addAttribute("appointments", appointments);
            Map<Long, Boolean> paidPayments = new HashMap<>();
            Map<Long, Boolean> unpaidPayments = new HashMap<>();
            Map<Long, String> prescriptionStatuses = new HashMap<>();
            for (Appointment appointment : appointments) {
                paidPayments.put(appointment.getId(),
                        paymentRepository.existsByAppointmentAndStatus(appointment, PaymentStatus.PAID));
                unpaidPayments.put(appointment.getId(),
                        paymentRepository.existsByAppointmentAndStatus(appointment, PaymentStatus.UNPAID));

                MedicalRecord record = medicalRecordRepository.findByAppointment(appointment).orElse(null);
                if (record == null) {
                    prescriptionStatuses.put(appointment.getId(), "Chưa khám");
                } else {
                    Prescription prescription = prescriptionRepository.findByMedicalRecord(record).orElse(null);
                    if (prescription == null) {
                        prescriptionStatuses.put(appointment.getId(), "Không kê đơn");
                    } else if (prescription.getStatus() == Status.PENDING) {
                        prescriptionStatuses.put(appointment.getId(), "Chờ duyệt");
                    } else if (prescription.getStatus() == Status.CONFIRMED) {
                        prescriptionStatuses.put(appointment.getId(), "Đã duyệt");
                    } else if (prescription.getStatus() == Status.COMPLETED) {
                        prescriptionStatuses.put(appointment.getId(), "Hoàn thành");
                    } else if (prescription.getStatus() == Status.CANCELLED) {
                        prescriptionStatuses.put(appointment.getId(), "Đã hủy");
                    } else {
                        prescriptionStatuses.put(appointment.getId(), prescription.getStatus().name());
                    }
                }
            }
            model.addAttribute("paidPayments", paidPayments);
            model.addAttribute("unpaidPayments", unpaidPayments);
            model.addAttribute("prescriptionStatuses", prescriptionStatuses);
            model.addAttribute("pendingCount", appointments.stream()
                    .filter(appointment -> appointment.getStatus() == Status.PENDING).count());
            model.addAttribute("pendingPaymentCount", appointments.stream()
                    .filter(appointment -> appointment.getStatus() == Status.PENDING_PAYMENT
                            && paymentRepository.existsByAppointmentAndStatus(appointment, PaymentStatus.UNPAID)).count());
            model.addAttribute("confirmedCount", appointments.stream()
                    .filter(appointment -> appointment.getStatus() == Status.CONFIRMED).count());
            model.addAttribute("awaitingApprovalCount", appointments.stream()
                    .filter(appointment -> appointment.getStatus() == Status.AWAITING_APPROVAL).count());
            model.addAttribute("completedCount", appointments.stream()
                    .filter(appointment -> appointment.getStatus() == Status.COMPLETED).count());
            model.addAttribute("now", LocalDateTime.now());
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
        model.addAttribute("labTestResults", labTestResultRepository.findByMedicalRecord(record));
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

        if (appointmentRepository.existsByDoctorAndAppointmentDateAndAppointmentTimeAndStatusIn(doctor,
                dto.getAppointmentDate(), dto.getAppointmentTime(), List.of(Status.PENDING_PAYMENT, Status.PENDING,
                        Status.CONFIRMED, Status.AWAITING_APPROVAL))) {
            result.rejectValue("appointmentTime", "error.duplicate", "Bác sĩ đã có lịch hẹn vào khung giờ này. Vui lòng chọn khung giờ khác.");
            model.addAttribute("specialties", specialtyRepository.findAll());
            return "patient/book-appointment";
        }

        Appointment appointment = Appointment.builder().patient(patient).doctor(doctor)
                .appointmentDate(dto.getAppointmentDate()).appointmentTime(dto.getAppointmentTime())
                .status(Status.PENDING).build();

        appointmentRepository.save(appointment);
        redirectAttributes.addFlashAttribute("successMessage", "Đặt lịch khám thành công! Bác sĩ sẽ khám và kê đơn trước khi bạn thanh toán.");
        return "redirect:/patient/home";
    }

    @GetMapping("/appointments/{appointmentId}/payment")
    public String showPaymentForm(@PathVariable Long appointmentId, HttpSession session,
                                  Model model, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            String username = (String) session.getAttribute("username");
            user = username != null ? userRepository.findByUsername(username) : null;
        }

        if (user == null) return "redirect:/login";

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getPatient().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thanh toán lịch hẹn này.");
            return "redirect:/patient/home";
        }

        if (appointment.getStatus() != Status.PENDING_PAYMENT) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lịch hẹn này không ở trạng thái chờ thanh toán.");
            return "redirect:/patient/home";
        }

        Payment unpaidPayment = paymentRepository.findByAppointment(appointment).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.UNPAID)
                .findFirst().orElse(null);

        if (unpaidPayment == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hiện không có hóa đơn nào cần thanh toán.");
            return "redirect:/patient/home";
        }

        model.addAttribute("appointment", appointment);
        model.addAttribute("amount", unpaidPayment.getAmount());
        model.addAttribute("unpaidPayment", unpaidPayment);
        return "patient/payment";
    }

    @PostMapping("/appointments/{appointmentId}/payment")
    public String processPayment(@PathVariable Long appointmentId,
                                 @RequestParam String provider,
                                 HttpSession session,
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
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thanh toán lịch hẹn này.");
            return "redirect:/patient/home";
        }

        if (appointment.getStatus() != Status.PENDING_PAYMENT) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lịch hẹn này không ở trạng thái chờ thanh toán.");
            return "redirect:/patient/home";
        }

        Payment unpaidPayment = paymentRepository.findByAppointment(appointment).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.UNPAID)
                .findFirst().orElse(null);

        if (unpaidPayment == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hiện không có hóa đơn nào cần thanh toán.");
            return "redirect:/patient/home";
        }

        LocalDateTime paidAt = LocalDateTime.now();
        String transactionCode = provider + "-" + appointment.getId() + "-" + System.currentTimeMillis();
        paymentRepository.findByAppointment(appointment).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.UNPAID)
                .forEach(payment -> {
                    payment.setProvider(provider);
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setPaidAt(paidAt);
                    payment.setTransactionCode(transactionCode);
                    paymentRepository.save(payment);
                });

        appointment.setStatus(Status.COMPLETED);
        appointmentRepository.save(appointment);
        redirectAttributes.addFlashAttribute("successMessage", "Thanh toán demo thành công! Hóa đơn đã được thanh toán và lịch khám hoàn tất.");
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
            LocalDateTime appointmentDateTime = appointment.getAppointmentDate().atTime(appointment.getAppointmentTime());
            if (!LocalDateTime.now().isBefore(appointmentDateTime.minusHours(12))) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể hủy lịch hẹn trong vòng 12 tiếng trước giờ khám.");
                return "redirect:/patient/home";
            }

            appointment.setStatus(Status.CANCELLED);
            appointmentRepository.save(appointment);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy lịch hẹn thành công! Khung giờ của bác sĩ đã được mở lại để bệnh nhân khác có thể đặt lịch.");
        } else if (appointment.getStatus() == Status.PENDING_PAYMENT || appointment.getStatus() == Status.COMPLETED) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lịch hẹn này đã được bác sĩ khám, không thể hủy.");
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
