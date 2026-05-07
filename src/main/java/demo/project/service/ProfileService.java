package demo.project.service;

import demo.project.model.Doctor;
import demo.project.model.Profile;
import demo.project.model.Specialty;
import demo.project.model.User;
import demo.project.repository.DoctorRepository;
import demo.project.repository.ProfileRepository;
import demo.project.repository.SpecialtyRepository;
import demo.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;

    public Profile getProfileByUser(User user) {
        Profile profile = profileRepository.findByUser(user);
        if (profile == null) {
            profile = new Profile();
            profile.setUser(user);
            profile = profileRepository.save(profile);
        }
        return profile;
    }

    public Doctor getDoctorByUser(User user) {
        return doctorRepository.findByUser(user);
    }

    @Transactional
    public void updateProfile(User user, Profile updatedProfile, String description, Long specialtyId) {
        Profile profile = getProfileByUser(user);
        profile.setFullName(updatedProfile.getFullName());
        profile.setAddress(updatedProfile.getAddress());
        profile.setPhone(updatedProfile.getPhone());
        profile.setGender(updatedProfile.getGender());
        profile.setDob(updatedProfile.getDob());
        if (updatedProfile.getPhoto() != null && !updatedProfile.getPhoto().isEmpty()) {
            profile.setPhoto(updatedProfile.getPhoto());
        }
        profileRepository.save(profile);

        if (user.getRole().name().equals("DOCTOR")) {
            Doctor doctor = doctorRepository.findByUser(user);
            if (doctor != null) {
                doctor.setDescription(description);
                if (specialtyId != null) {
                    Specialty specialty = specialtyRepository.findById(specialtyId).orElse(null);
                    if (specialty != null) doctor.setSpecialty(specialty);
                }
                doctorRepository.save(doctor);
            }
        }
    }
}
