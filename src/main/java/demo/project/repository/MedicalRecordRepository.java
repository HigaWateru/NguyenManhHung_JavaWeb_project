package demo.project.repository;

import demo.project.model.MedicalRecord;
import demo.project.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    Optional<MedicalRecord> findByAppointment(Appointment appointment);
}
