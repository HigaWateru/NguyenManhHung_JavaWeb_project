package demo.project.running;

import demo.project.model.*;
import demo.project.model._enum.Role;
import demo.project.repository.DoctorRepository;
import demo.project.repository.LabTestTypeRepository;
import demo.project.repository.MedicineRepository;
import demo.project.repository.ProfileRepository;
import demo.project.repository.SpecialtyRepository;
import demo.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;
    private final MedicineRepository medicineRepository;
    private final LabTestTypeRepository labTestTypeRepository;
    private final DoctorRepository doctorRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeSpecialties();
        initializeMedicines();
        initializeLabTestTypes();
        initializeUsers();
    }

    private void initializeSpecialties() {
        if (specialtyRepository.count() == 0) {
            List<String> names = Arrays.asList("Nội tổng quát", "Nhi khoa", "Sản phụ khoa", "Tai mũi họng", "Da liễu", "Răng hàm mặt", "Nhãn khoa");
            for (String name : names) {
                Specialty s = new Specialty();
                s.setName(name);
                s.setDescription("Chuyên khoa " + name);
                specialtyRepository.save(s);
            }
            System.out.println("Đã khởi tạo danh mục chuyên khoa.");
        }
    }

    private void initializeMedicines() {
        if (medicineRepository.count() == 0) {
            List<Medicine> medicines = Arrays.asList(
                createMedicine("Paracetamol 500mg", 1000, 1500.0, "Viên"),
                createMedicine("Amoxicillin 500mg", 500, 3000.0, "Viên"),
                createMedicine("Vitamin C 1000mg", 2000, 1000.0, "Viên"),
                createMedicine("Panadol Extra", 800, 2000.0, "Viên"),
                createMedicine("Efferalgan 500mg", 600, 2500.0, "Viên")
            );
            medicineRepository.saveAll(medicines);
            System.out.println("Đã khởi tạo danh mục thuốc mẫu.");
        }
    }

    private Medicine createMedicine(String name, Integer stock, Double price, String unit) {
        Medicine m = Medicine.builder().name(name).stock(stock).price(price).unit(unit).build();
        return m;
    }

    private void initializeLabTestTypes() {
        if (labTestTypeRepository.count() == 0) {
            List<LabTestType> labTestTypes = Arrays.asList(
                    createLabTestType("Xét nghiệm công thức máu", "Đánh giá hồng cầu, bạch cầu, tiểu cầu và các chỉ số huyết học cơ bản.", 120000.0),
                    createLabTestType("Xét nghiệm đường huyết", "Đo nồng độ glucose trong máu để hỗ trợ tầm soát và theo dõi đái tháo đường.", 60000.0),
                    createLabTestType("Xét nghiệm chức năng gan", "Kiểm tra các chỉ số men gan và bilirubin.", 180000.0),
                    createLabTestType("Xét nghiệm chức năng thận", "Đánh giá creatinine, ure và các chỉ số liên quan đến chức năng thận.", 150000.0),
                    createLabTestType("Xét nghiệm nước tiểu", "Phân tích các chỉ số cơ bản trong nước tiểu.", 80000.0),
                    createLabTestType("Xét nghiệm CRP", "Định lượng CRP để hỗ trợ đánh giá tình trạng viêm nhiễm.", 140000.0),
                    createLabTestType("Xét nghiệm PCR", "Phát hiện vật liệu di truyền của tác nhân gây bệnh.", 450000.0)
            );
            labTestTypeRepository.saveAll(labTestTypes);
            System.out.println("Đã khởi tạo danh mục loại xét nghiệm mẫu.");
        }
    }

    private LabTestType createLabTestType(String name, String description, Double price) {
        return LabTestType.builder()
                .name(name)
                .description(description)
                .price(price)
                .active(true)
                .build();
    }

    private void initializeUsers() {
        // Tạo Admin
        if (userRepository.findByUsername("admin") == null) {
            User admin = User.builder().username("admin").password(passwordEncoder.encode("123456"))
                    .role(Role.ADMIN).isActive(true).build();
            userRepository.save(admin);

            Profile adminProfile = Profile.builder().user(admin).fullName("Pengunistrator").phone("0123456789")
                    .build();
            profileRepository.save(adminProfile);
            System.out.println("Đã tạo tài khoản admin mẫu: admin/123456");
        }

        // Tạo Doctor
        if (userRepository.findByUsername("doctor") == null) {
            User doctorUser = User.builder().username("doctor").password(passwordEncoder.encode("123456"))
                    .role(Role.DOCTOR).isActive(true).build();
            userRepository.save(doctorUser);

            Profile doctorProfile = Profile.builder().user(doctorUser).fullName("Nguyễn Văn A")
                    .phone("0987654321").build();
            profileRepository.save(doctorProfile);

            Specialty specialty = specialtyRepository.findAll().get(0);
            Doctor doctor = Doctor.builder().user(doctorUser).specialty(specialty).description("Bác sĩ chuyên khoa đầu ngành").build();
            doctorRepository.save(doctor);
            System.out.println("Đã tạo tài khoản bác sĩ mẫu: doctor/123456");
        }

        // Tạo Patient
        if (userRepository.findByUsername("patient") == null) {
            User patient = User.builder().username("patient").password(passwordEncoder.encode("123456"))
                    .role(Role.PATIENT).isActive(true).build();
            userRepository.save(patient);

            Profile patientProfile = Profile.builder().user(patient).fullName("Nguyễn Văn B").phone("0333444555")
                    .build();
            profileRepository.save(patientProfile);
            System.out.println("Đã tạo tài khoản bệnh nhân mẫu: patient/123456");
        }
    }
}
