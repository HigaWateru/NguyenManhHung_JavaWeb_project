package demo.project.repository;

import demo.project.model.LabTestResult;
import demo.project.model.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabTestResultRepository extends JpaRepository<LabTestResult, Long> {
    List<LabTestResult> findByMedicalRecord(MedicalRecord medicalRecord);
}
