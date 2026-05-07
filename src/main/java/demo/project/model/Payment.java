package demo.project.model;

import demo.project.model._enum.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;
    private String provider;
    private String transactionCode;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime paidAt;

    @ManyToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;
}