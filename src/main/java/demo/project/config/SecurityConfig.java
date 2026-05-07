package demo.project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .failureUrl("/login?error")
                .successHandler((request, response, authentication) -> {
                    String username = authentication.getName();
                    request.getSession().setAttribute("username", username);
                    for (var auth : authentication.getAuthorities()) {
                        String role = auth.getAuthority();
                        if (role.equals("ROLE_ADMIN") || role.equals("ADMIN")) {
                            response.sendRedirect("/admin/dashboard");
                            return;
                        } else if (role.equals("ROLE_DOCTOR") || role.equals("DOCTOR")) {
                            response.sendRedirect("/doctor/dashboard");
                            return;
                        }
                    }
                    response.sendRedirect("/home");
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }
}
