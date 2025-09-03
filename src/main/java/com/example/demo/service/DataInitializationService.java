package com.example.demo.service;

import com.example.demo.entity.Department;
import com.example.demo.entity.User;
import com.example.demo.entity.User.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@Service
public class DataInitializationService implements CommandLineRunner {

    @Autowired
    private DepartmentService departmentService;
    
    @Autowired
    private UserService userService;

    @Override
    public void run(String... args) throws Exception {
        initializeSampleData();
    }

    private void initializeSampleData() {
        // Only create essential departments, no sample users
        createEssentialDepartments();
        
        System.out.println("✅ Essential data initialized successfully!");
    }

    private void createEssentialDepartments() {
        // Only create basic departments if none exist
        if (departmentService.findAll().isEmpty()) {
            try {
                departmentService.createDepartment("Computer Science", "CS", "Department of Computer Science and Engineering");
                departmentService.createDepartment("Electronics", "ECE", "Department of Electronics and Communication Engineering");
                departmentService.createDepartment("Mechanical", "ME", "Department of Mechanical Engineering");
                departmentService.createDepartment("Civil", "CE", "Department of Civil Engineering");
                departmentService.createDepartment("Mathematics", "MATH", "Department of Mathematics and Statistics");
                
                System.out.println("✅ Essential departments created");
            } catch (Exception e) {
                System.out.println("⚠️ Departments may already exist: " + e.getMessage());
            }
        }
    }
}
