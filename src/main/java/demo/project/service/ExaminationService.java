package demo.project.service;

import demo.project.dto.ExaminationDto;
import demo.project.model.*;
import demo.project.model._enum.PaymentStatus;
import demo.project.model._enum.Status;
import demo.project.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExaminationService {
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionDetailRepository prescriptionDetailRepository;
    private final MedicineRepository medicineRepository;
    private final LabTestTypeRepository labTestTypeRepository;
    private final LabTestResultRepository labTestResultRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public void saveExaminationResult(Long appointmentId, ExaminationDto dto) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        MedicalRecord record = MedicalRecord.builder().appointment(appointment).diagnosis(dto.getDiagnosis())
                .notes(dto.getNotes()).build();
        medicalRecordRepository.save(record);

        double invoiceAmount = 0.0;
        boolean hasMedicine = dto.getMedicines() != null && !dto.getMedicines().isEmpty();
        boolean hasLabTest = dto.getLabTests() != null && !dto.getLabTests().isEmpty();

        if (hasMedicine) {
            Set<Long> medicineIds = new HashSet<>();
            for (ExaminationDto.MedicineItem item : dto.getMedicines()) {
                if (item.getMedicineId() != null && !medicineIds.add(item.getMedicineId())) {
                    throw new RuntimeException("Thuốc bị trùng trong đơn. Vui lòng chỉ chọn mỗi thuốc một lần.");
                }
            }

            Prescription prescription = Prescription.builder().medicalRecord(record).status(Status.PENDING).build();
            prescriptionRepository.save(prescription);

            for (ExaminationDto.MedicineItem item : dto.getMedicines()) {
                if (item.getMedicineId() != null) {
                    Medicine medicine = medicineRepository.findById(item.getMedicineId())
                            .orElseThrow(() -> new RuntimeException("Medicine not found"));

                    PrescriptionDetail detail = PrescriptionDetail.builder().prescription(prescription)
                            .medicine(medicine).quantity(item.getQuantity()).dosage(item.getDosage()).build();
                    prescriptionDetailRepository.save(detail);

                    int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
                    invoiceAmount += medicine.getPrice() != null ? medicine.getPrice() * quantity : 0.0;
                }
            }
        }

        if (hasLabTest) {
            Set<Long> labTestTypeIds = new HashSet<>();
            for (ExaminationDto.LabTestItem item : dto.getLabTests()) {
                if (item.getLabTestTypeId() != null && !labTestTypeIds.add(item.getLabTestTypeId())) {
                    throw new RuntimeException("Xét nghiệm bị trùng trong đơn. Vui lòng chỉ chọn mỗi xét nghiệm một lần.");
                }
            }

            for (ExaminationDto.LabTestItem item : dto.getLabTests()) {
                if (item.getLabTestTypeId() != null) {
                    LabTestType labTestType = labTestTypeRepository.findById(item.getLabTestTypeId())
                            .orElseThrow(() -> new RuntimeException("Lab test type not found"));

                    LabTestResult labTestResult = LabTestResult.builder().medicalRecord(record).labTestType(labTestType)
                            .result(item.getResult()).note(item.getNote()).build();
                    labTestResultRepository.save(labTestResult);

                    invoiceAmount += labTestType.getPrice() != null ? labTestType.getPrice() : 0.0;
                }
            }
        }

        if (invoiceAmount > 0) {
            Payment payment = Payment.builder().appointment(appointment).amount(invoiceAmount).provider("DEMO_INVOICE")
                    .transactionCode("INVOICE-" + appointment.getId() + "-" + System.currentTimeMillis())
                    .status(PaymentStatus.UNPAID).build();
            paymentRepository.save(payment);
            appointment.setStatus(Status.PENDING_PAYMENT);
        } else {
            appointment.setStatus(Status.COMPLETED);
        }
        appointmentRepository.save(appointment);
    }
}
