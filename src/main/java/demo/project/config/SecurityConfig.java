package demo.project.config;

import demo.project.model.Profile;
import demo.project.model.User;
import demo.project.repository.UserRepository;
import demo.project.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/", "/home", "/login", "/register", "/process-login", "/access-denied").permitAll()
                .requestMatchers("/patient/**").hasAuthority("PATIENT")
                .requestMatchers("/admin/**").hasAuthority("ADMIN")
                .requestMatchers("/doctor/**").hasAnyAuthority("ADMIN", "DOCTOR")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/process-login")
                .failureUrl("/login?error")
                .successHandler((request, response, authentication) -> {
                    String username = authentication.getName();
                    User user = userRepository.findByUsername(username);
                    Profile profile = profileRepository.findByUser(user);
                    
                    request.getSession().setAttribute("username", username);
                    request.getSession().setAttribute("loginUser", user);
                    request.getSession().setAttribute("profile", profile);
                    
                    var authorities = authentication.getAuthorities();
                    
                    if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                        response.sendRedirect("/admin/dashboard");
                    } else if (authorities.stream().anyMatch(a -> a.getAuthority().equals("DOCTOR"))) {
                        response.sendRedirect("/doctor/dashboard");
                    } else {
                        response.sendRedirect("/patient/home");
                    }
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .exceptionHandling(exception -> exception
                .accessDeniedPage("/access-denied")
            );

        return http.build();
    }
}
