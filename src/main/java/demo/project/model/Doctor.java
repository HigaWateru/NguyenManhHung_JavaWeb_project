package demo.project.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String description;

    @ManyToOne
    @JoinColumn(name = "specialty_id")
    private Specialty specialty;
}
