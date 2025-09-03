package com.example.demo.controller;

import com.example.demo.entity.Department;
import com.example.demo.entity.User;
import com.example.demo.entity.User.UserRole;
import com.example.demo.service.DepartmentService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/assistant")
public class AssistantController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private DepartmentService departmentService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User assistant = userService.findByUsername(username).orElse(null);
        
        if (assistant != null && assistant.getDepartment() != null) {
            Department department = assistant.getDepartment();
            
            // Get faculty and students count for this department
            List<User> allUsers = userService.findByRole(UserRole.FACULTY);
            allUsers.addAll(userService.findByRole(UserRole.STUDENT));
            
            List<User> departmentFaculty = allUsers.stream()
                .filter(user -> user.getRole() == UserRole.FACULTY && 
                               user.getDepartment() != null && 
                               user.getDepartment().getId().equals(department.getId()))
                .collect(Collectors.toList());
                
            List<User> departmentStudents = allUsers.stream()
                .filter(user -> user.getRole() == UserRole.STUDENT && 
                               user.getDepartment() != null && 
                               user.getDepartment().getId().equals(department.getId()))
                .collect(Collectors.toList());
            
            model.addAttribute("assistant", assistant);
            model.addAttribute("department", department);
            model.addAttribute("facultyCount", departmentFaculty.size());
            model.addAttribute("studentCount", departmentStudents.size());
        }
        
        return "assistant/dashboard";
    }
    
    @GetMapping("/faculty")
    public String viewFaculty(Model model) {
        User assistant = getCurrentAssistant();
        if (assistant != null && assistant.getDepartment() != null) {
            List<User> faculty = userService.findByRole(UserRole.FACULTY).stream()
                .filter(user -> user.getDepartment() != null && 
                               user.getDepartment().getId().equals(assistant.getDepartment().getId()))
                .collect(Collectors.toList());
            
            model.addAttribute("faculty", faculty);
            model.addAttribute("department", assistant.getDepartment());
        }
        return "assistant/faculty";
    }
    
    @GetMapping("/students")
    public String viewStudents(Model model) {
        User assistant = getCurrentAssistant();
        if (assistant != null && assistant.getDepartment() != null) {
            List<User> students = userService.findByRole(UserRole.STUDENT).stream()
                .filter(user -> user.getDepartment() != null && 
                               user.getDepartment().getId().equals(assistant.getDepartment().getId()))
                .collect(Collectors.toList());
            
            model.addAttribute("students", students);
            model.addAttribute("department", assistant.getDepartment());
        }
        return "assistant/students";
    }
    
    @GetMapping("/faculty/create")
    public String createFacultyForm(Model model) {
        User assistant = getCurrentAssistant();
        if (assistant != null) {
            model.addAttribute("department", assistant.getDepartment());
        }
        return "assistant/create-faculty";
    }
    
    @PostMapping("/faculty/create")
    public String createFaculty(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam String firstName,
                               @RequestParam String lastName,
                               @RequestParam String email,
                               @RequestParam String phone,
                               @RequestParam String address,
                               RedirectAttributes redirectAttributes) {
        try {
            User assistant = getCurrentAssistant();
            if (assistant == null || assistant.getDepartment() == null) {
                redirectAttributes.addFlashAttribute("error", "Assistant department not found!");
                return "redirect:/assistant/faculty";
            }
            
            User faculty = new User();
            faculty.setUsername(username);
            faculty.setPassword(password);
            faculty.setFirstName(firstName);
            faculty.setLastName(lastName);
            faculty.setEmail(email);
            faculty.setPhone(phone);
            faculty.setAddress(address);
            faculty.setRole(UserRole.FACULTY);
            faculty.setDepartment(assistant.getDepartment());
            faculty.setEmployeeId("F" + System.currentTimeMillis() % 10000);
            faculty.setActive(true);
            
            userService.createUser(faculty);
            redirectAttributes.addFlashAttribute("success", "Faculty created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating faculty: " + e.getMessage());
        }
        
        return "redirect:/assistant/faculty";
    }
    
    @GetMapping("/students/create")
    public String createStudentForm(Model model) {
        User assistant = getCurrentAssistant();
        if (assistant != null) {
            model.addAttribute("department", assistant.getDepartment());
        }
        return "assistant/create-student";
    }
    
    @PostMapping("/students/create")
    public String createStudent(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam String firstName,
                               @RequestParam String lastName,
                               @RequestParam String email,
                               @RequestParam String phone,
                               @RequestParam String address,
                               RedirectAttributes redirectAttributes) {
        try {
            User assistant = getCurrentAssistant();
            if (assistant == null || assistant.getDepartment() == null) {
                redirectAttributes.addFlashAttribute("error", "Assistant department not found!");
                return "redirect:/assistant/students";
            }
            
            User student = new User();
            student.setUsername(username);
            student.setPassword(password);
            student.setFirstName(firstName);
            student.setLastName(lastName);
            student.setEmail(email);
            student.setPhone(phone);
            student.setAddress(address);
            student.setRole(UserRole.STUDENT);
            student.setDepartment(assistant.getDepartment());
            student.setEmployeeId("S" + System.currentTimeMillis() % 10000);
            student.setActive(true);
            
            userService.createUser(student);
            redirectAttributes.addFlashAttribute("success", "Student created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating student: " + e.getMessage());
        }
        
        return "redirect:/assistant/students";
    }
    
    @GetMapping("/faculty/{id}/edit")
    public String editFacultyForm(@PathVariable Long id, Model model) {
        User faculty = userService.getUserById(id);
        User assistant = getCurrentAssistant();
        
        // Verify faculty belongs to assistant's department
        if (faculty != null && assistant != null && 
            faculty.getDepartment() != null && assistant.getDepartment() != null &&
            faculty.getDepartment().getId().equals(assistant.getDepartment().getId())) {
            model.addAttribute("user", faculty);
            model.addAttribute("department", assistant.getDepartment());
            return "assistant/edit-faculty";
        }
        
        return "redirect:/assistant/faculty";
    }
    
    @PostMapping("/faculty/{id}/edit")
    public String editFaculty(@PathVariable Long id,
                             @RequestParam String firstName,
                             @RequestParam String lastName,
                             @RequestParam String email,
                             @RequestParam String phone,
                             @RequestParam String address,
                             RedirectAttributes redirectAttributes) {
        try {
            User faculty = userService.getUserById(id);
            User assistant = getCurrentAssistant();
            
            // Verify faculty belongs to assistant's department
            if (faculty != null && assistant != null && 
                faculty.getDepartment() != null && assistant.getDepartment() != null &&
                faculty.getDepartment().getId().equals(assistant.getDepartment().getId())) {
                
                faculty.setFirstName(firstName);
                faculty.setLastName(lastName);
                faculty.setEmail(email);
                faculty.setPhone(phone);
                faculty.setAddress(address);
                
                userService.updateUser(faculty);
                redirectAttributes.addFlashAttribute("success", "Faculty updated successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Unauthorized access!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating faculty: " + e.getMessage());
        }
        
        return "redirect:/assistant/faculty";
    }
    
    @GetMapping("/students/{id}/edit")
    public String editStudentForm(@PathVariable Long id, Model model) {
        User student = userService.getUserById(id);
        User assistant = getCurrentAssistant();
        
        // Verify student belongs to assistant's department
        if (student != null && assistant != null && 
            student.getDepartment() != null && assistant.getDepartment() != null &&
            student.getDepartment().getId().equals(assistant.getDepartment().getId())) {
            model.addAttribute("user", student);
            model.addAttribute("department", assistant.getDepartment());
            return "assistant/edit-student";
        }
        
        return "redirect:/assistant/students";
    }
    
    @PostMapping("/students/{id}/edit")
    public String editStudent(@PathVariable Long id,
                             @RequestParam String firstName,
                             @RequestParam String lastName,
                             @RequestParam String email,
                             @RequestParam String phone,
                             @RequestParam String address,
                             RedirectAttributes redirectAttributes) {
        try {
            User student = userService.getUserById(id);
            User assistant = getCurrentAssistant();
            
            // Verify student belongs to assistant's department
            if (student != null && assistant != null && 
                student.getDepartment() != null && assistant.getDepartment() != null &&
                student.getDepartment().getId().equals(assistant.getDepartment().getId())) {
                
                student.setFirstName(firstName);
                student.setLastName(lastName);
                student.setEmail(email);
                student.setPhone(phone);
                student.setAddress(address);
                
                userService.updateUser(student);
                redirectAttributes.addFlashAttribute("success", "Student updated successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Unauthorized access!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating student: " + e.getMessage());
        }
        
        return "redirect:/assistant/students";
    }
    
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id);
            User assistant = getCurrentAssistant();
            
            // Verify user belongs to assistant's department
            if (user != null && assistant != null && 
                user.getDepartment() != null && assistant.getDepartment() != null &&
                user.getDepartment().getId().equals(assistant.getDepartment().getId())) {
                
                UserRole role = user.getRole();
                userService.deleteUser(id);
                redirectAttributes.addFlashAttribute("success", "User deleted successfully!");
                
                // Redirect based on user role
                if (role == UserRole.FACULTY) {
                    return "redirect:/assistant/faculty";
                } else {
                    return "redirect:/assistant/students";
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Unauthorized access!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting user: " + e.getMessage());
        }
        
        return "redirect:/assistant/dashboard";
    }
    
    private User getCurrentAssistant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userService.findByUsername(username).orElse(null);
    }
}
