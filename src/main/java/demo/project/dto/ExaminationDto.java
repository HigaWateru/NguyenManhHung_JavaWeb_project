package demo.project.dto;

import lombok.Data;
import java.util.List;

@Data
public class ExaminationDto {
    private String diagnosis;
    private String notes;
    private List<MedicineItem> medicines;
    private List<LabTestItem> labTests;

    @Data
    public static class MedicineItem {
        private Long medicineId;
        private Integer quantity;
        private String dosage;
    }

    @Data
    public static class LabTestItem {
        private Long labTestTypeId;
        private String result;
        private String note;
    }
}
