package demo.project.service;

import demo.project.dto.ExaminationDto;
import demo.project.model.*;
import demo.project.model._enum.Status;
import demo.project.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExaminationService {
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionDetailRepository prescriptionDetailRepository;
    private final MedicineRepository medicineRepository;

    @Transactional
    public void saveExaminationResult(Long appointmentId, ExaminationDto dto) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        appointment.setStatus(Status.COMPLETED);
        appointmentRepository.save(appointment);

        MedicalRecord record = MedicalRecord.builder().appointment(appointment).diagnosis(dto.getDiagnosis())
                .notes(dto.getNotes()).build();
        medicalRecordRepository.save(record);

        if (dto.getMedicines() != null && !dto.getMedicines().isEmpty()) {
            Prescription prescription = Prescription.builder().medicalRecord(record).status(Status.PENDING).build();
            prescriptionRepository.save(prescription);

            for (ExaminationDto.MedicineItem item : dto.getMedicines()) {
                if (item.getMedicineId() != null) {
                    Medicine medicine = medicineRepository.findById(item.getMedicineId())
                            .orElseThrow(() -> new RuntimeException("Medicine not found"));

                    PrescriptionDetail detail = PrescriptionDetail.builder().prescription(prescription)
                            .medicine(medicine).quantity(item.getQuantity()).dosage(item.getDosage()).build();
                    prescriptionDetailRepository.save(detail);
                }
            }
        }
    }
}
