package demo.project.repository;

import demo.project.model.Prescription;
import demo.project.model.MedicalRecord;
import demo.project.model._enum.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    Optional<Prescription> findByMedicalRecord(MedicalRecord medicalRecord);
    long countByStatus(Status status);
    List<Prescription> findTop5ByOrderByIdDesc();
}
