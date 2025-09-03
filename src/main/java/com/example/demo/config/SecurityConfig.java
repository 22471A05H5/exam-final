package com.example.demo.config;

import com.example.demo.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/login", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/superadmin/**").hasRole("SUPERADMIN")
                .requestMatchers("/principal/**").hasAnyRole("SUPERADMIN", "PRINCIPAL")
                .requestMatchers("/hod/**").hasAnyRole("SUPERADMIN", "PRINCIPAL", "HOD")
                .requestMatchers("/assistant/**").hasAnyRole("SUPERADMIN", "PRINCIPAL", "HOD", "ASSISTANT")
                .requestMatchers("/faculty/**").hasAnyRole("SUPERADMIN", "PRINCIPAL", "HOD", "ASSISTANT", "FACULTY")
                .requestMatchers("/student/**").hasAnyRole("SUPERADMIN", "PRINCIPAL", "HOD", "ASSISTANT", "FACULTY", "STUDENT")
                .requestMatchers("/admin/**").hasAnyRole("SUPERADMIN", "ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
