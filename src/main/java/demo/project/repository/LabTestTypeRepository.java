package demo.project.repository;

import demo.project.model.LabTestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabTestTypeRepository extends JpaRepository<LabTestType, Long> {
    List<LabTestType> findByActiveTrue();
}
