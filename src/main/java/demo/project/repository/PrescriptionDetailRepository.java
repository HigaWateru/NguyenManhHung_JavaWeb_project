package demo.project.repository;

import demo.project.model.PrescriptionDetail;
import demo.project.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PrescriptionDetailRepository extends JpaRepository<PrescriptionDetail, Long> {
    List<PrescriptionDetail> findByPrescription(Prescription prescription);
}
