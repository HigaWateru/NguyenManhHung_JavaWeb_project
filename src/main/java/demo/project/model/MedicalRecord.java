package demo.project.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String diagnosis;
    private String notes;

    @OneToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;
}