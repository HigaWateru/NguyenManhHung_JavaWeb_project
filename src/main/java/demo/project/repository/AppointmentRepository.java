package demo.project.repository;

import demo.project.model.Appointment;
import demo.project.model.Doctor;
import demo.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatient(User patient);
    List<Appointment> findByDoctor(Doctor doctor);
    List<Appointment> findByDoctorAndStatus(Doctor doctor, demo.project.model._enum.Status status);
    long countByDoctorAndStatus(Doctor doctor, demo.project.model._enum.Status status);
    long countByDoctor(Doctor doctor);
    boolean existsByDoctorAndAppointmentDateAndAppointmentTime(Doctor doctor, LocalDate date, LocalTime time);
    List<Appointment> findByPatientAndStatus(User patient, demo.project.model._enum.Status status);
}
