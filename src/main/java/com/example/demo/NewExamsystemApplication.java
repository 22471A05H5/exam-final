package com.example.demo;

import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NewExamsystemApplication implements CommandLineRunner {

	@Autowired
	private UserService userService;

	public static void main(String[] args) {
		SpringApplication.run(NewExamsystemApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// Create only SuperAdmin user - no other data
		userService.createDefaultSuperAdmin();
		System.out.println("✅ SuperAdmin created: username=superadmin, password=admin123");
		System.out.println("🔗 Login at: http://localhost:8081/login");
	}
}
