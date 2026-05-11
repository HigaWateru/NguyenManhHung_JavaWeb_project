package demo.project.repository;

import demo.project.model.Appointment;
import demo.project.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByAppointment(Appointment appointment);
    boolean existsByAppointmentAndStatus(Appointment appointment, demo.project.model._enum.PaymentStatus status);
}
