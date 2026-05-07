package demo.project.running;

import demo.project.model.Doctor;
import demo.project.model.Medicine;
import demo.project.model.Specialty;
import demo.project.model.User;
import demo.project.model._enum.Role;
import demo.project.repository.DoctorRepository;
import demo.project.repository.MedicineRepository;
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
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeSpecialties();
        initializeMedicines();
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
        Medicine m = new Medicine();
        m.setName(name);
        m.setStock(stock);
        m.setPrice(price);
        m.setUnit(unit);
        return m;
    }

    private void initializeUsers() {
        // Tạo Admin
        if (userRepository.findByUsername("admin") == null) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("123456"))
                    .role(Role.ADMIN)
                    .isActive(true)
                    .build();
            userRepository.save(admin);
            System.out.println("Đã tạo tài khoản admin mẫu: admin/123456");
        }

        // Tạo Doctor
        if (userRepository.findByUsername("doctor") == null) {
            User doctorUser = User.builder()
                    .username("doctor")
                    .password(passwordEncoder.encode("123456"))
                    .role(Role.DOCTOR)
                    .isActive(true)
                    .build();
            userRepository.save(doctorUser);

            // Tạo thông tin Doctor chi tiết
            Specialty specialty = specialtyRepository.findAll().get(0); // Lấy chuyên khoa đầu tiên
            Doctor doctor = new Doctor();
            doctor.setUser(doctorUser);
            doctor.setSpecialty(specialty);
            doctor.setDescription("Bác sĩ chuyên khoa đầu ngành");
            doctorRepository.save(doctor);
            System.out.println("Đã tạo tài khoản bác sĩ mẫu: doctor/123456");
        }

        // Tạo Patient
        if (userRepository.findByUsername("patient") == null) {
            User patient = User.builder()
                    .username("patient")
                    .password(passwordEncoder.encode("123456"))
                    .role(Role.PATIENT)
                    .isActive(true)
                    .build();
            userRepository.save(patient);
            System.out.println("Đã tạo tài khoản bệnh nhân mẫu: patient/123456");
        }
    }
}
