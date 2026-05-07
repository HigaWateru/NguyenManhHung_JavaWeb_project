package demo.project.repository;

import demo.project.model.Prescription;
import demo.project.model.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    Optional<Prescription> findByMedicalRecord(MedicalRecord medicalRecord);
}
