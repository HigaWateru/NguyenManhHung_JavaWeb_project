package demo.project.repository;

import demo.project.model.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(@NotBlank(message = "Tên người dùng không được để trống") String username);
}
