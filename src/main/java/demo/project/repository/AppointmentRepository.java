package demo.project.repository;

import demo.project.model.Appointment;
import demo.project.model.Doctor;
import demo.project.model.User;
import demo.project.model._enum.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatient(User patient);
    List<Appointment> findByPatientAndStatusNot(User patient, Status status);
    List<Appointment> findByDoctor(Doctor doctor);
    List<Appointment> findByDoctorAndStatusNot(Doctor doctor, Status status);
    List<Appointment> findByDoctorAndStatus(Doctor doctor, demo.project.model._enum.Status status);
    long countByDoctorAndStatus(Doctor doctor, demo.project.model._enum.Status status);
    long countByDoctor(Doctor doctor);
    long countByDoctorAndStatusNot(Doctor doctor, Status status);
    boolean existsByDoctorAndAppointmentDateAndAppointmentTime(Doctor doctor, LocalDate date, LocalTime time);
    boolean existsByDoctorAndAppointmentDateAndAppointmentTimeAndStatusIn(Doctor doctor, LocalDate date, LocalTime time, List<Status> statuses);
    List<Appointment> findByDoctorAndAppointmentDateAndStatusIn(Doctor doctor, LocalDate date, List<Status> statuses);
    List<Appointment> findByPatientAndStatus(User patient, demo.project.model._enum.Status status);
}
