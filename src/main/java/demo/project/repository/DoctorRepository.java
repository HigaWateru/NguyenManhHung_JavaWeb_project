package demo.project.repository;

import demo.project.model.Doctor;
import demo.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Doctor findByUser(User user);
}
