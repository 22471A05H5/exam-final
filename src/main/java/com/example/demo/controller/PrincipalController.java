package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.entity.Department;
import com.example.demo.entity.ExamResult;
import com.example.demo.service.UserService;
import com.example.demo.service.DepartmentService;
import com.example.demo.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/principal")
public class PrincipalController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private DepartmentService departmentService;
    
    @Autowired
    private ExamService examService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        String username = authentication.getName();
        User principal = userService.findByUsername(username).orElse(null);
        
        // Get counts for dashboard statistics
        List<User> hods = userService.findByRole(User.UserRole.HOD);
        List<User> assistants = userService.findByRole(User.UserRole.ASSISTANT);
        List<User> students = userService.findByRole(User.UserRole.STUDENT);
        List<Department> departments = departmentService.findAll();
        
        // Enhance departments with additional information
        List<Map<String, Object>> departmentInfo = departments.stream()
            .map(dept -> {
                Map<String, Object> info = new HashMap<>();
                info.put("name", dept.getName());
                info.put("code", dept.getCode());
                info.put("description", dept.getDescription());
                
                // Find HOD for this department
                User hodForDept = hods.stream()
                    .filter(hod -> hod.getDepartment() != null && hod.getDepartment().getId().equals(dept.getId()))
                    .findFirst()
                    .orElse(null);
                
                info.put("hodName", hodForDept != null ? 
                    hodForDept.getFirstName() + " " + hodForDept.getLastName() : "Not Assigned");
                
                // Count faculty and students for this department
                long facultyCount = userService.findByRole(User.UserRole.FACULTY).stream()
                    .filter(faculty -> faculty.getDepartment() != null && 
                           faculty.getDepartment().getId().equals(dept.getId()))
                    .count();
                
                long studentCount = students.stream()
                    .filter(student -> student.getDepartment() != null && 
                           student.getDepartment().getId().equals(dept.getId()))
                    .count();
                
                info.put("facultyCount", facultyCount);
                info.put("studentCount", studentCount);
                
                return info;
            })
            .limit(5) // Show only first 5 departments on dashboard
            .toList();
        
        model.addAttribute("user", principal);
        model.addAttribute("hodCount", hods.size());
        model.addAttribute("assistantCount", assistants.size());
        model.addAttribute("departmentCount", departments.size());
        model.addAttribute("studentCount", students.size());
        model.addAttribute("departments", departmentInfo);
        
        // TODO: Add recent messages when communication system is implemented
        // model.addAttribute("recentMessages", messageService.getRecentMessagesForPrincipal(principal.getId()));
        
        return "principal/dashboard";
    }

    @GetMapping("/hods")
    public String viewHods(Model model) {
        List<User> hods = userService.findByRole(User.UserRole.HOD);
        model.addAttribute("hods", hods);
        return "principal/hods";
    }

    @GetMapping("/assistants")
    public String viewAssistants(Model model) {
        List<User> assistants = userService.findByRole(User.UserRole.ASSISTANT);
        model.addAttribute("assistants", assistants);
        return "principal/assistants";
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        return "principal/edit-user";
    }

    @PostMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, @ModelAttribute User user, RedirectAttributes redirectAttributes) {
        try {
            user.setId(id);
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("success", "User updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating user: " + e.getMessage());
        }
        
        // Redirect based on user role
        if (user.getRole() == User.UserRole.HOD) {
            return "redirect:/principal/hods";
        } else {
            return "redirect:/principal/assistants";
        }
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found!");
                return "redirect:/principal/hods";
            }
            
            User.UserRole userRole = user.getRole();
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully!");
            
            // Redirect based on user role
            if (userRole == User.UserRole.HOD) {
                return "redirect:/principal/hods";
            } else {
                return "redirect:/principal/assistants";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting user: " + e.getMessage());
            return "redirect:/principal/hods";
        }
    }

    @GetMapping("/departments")
    public String viewDepartments(Model model) {
        List<Department> departments = departmentService.findAll();
        List<User> hods = userService.findByRole(User.UserRole.HOD);
        
        // Enhance departments with HOD and statistics information
        List<Map<String, Object>> departmentInfo = departments.stream()
            .map(dept -> {
                Map<String, Object> info = new HashMap<>();
                info.put("id", dept.getId());
                info.put("name", dept.getName());
                info.put("code", dept.getCode());
                info.put("description", dept.getDescription());
                
                // Find HOD for this department
                User hodForDept = hods.stream()
                    .filter(hod -> hod.getDepartment() != null && hod.getDepartment().getId().equals(dept.getId()))
                    .findFirst()
                    .orElse(null);
                
                info.put("hodName", hodForDept != null ? 
                    hodForDept.getFirstName() + " " + hodForDept.getLastName() : "Not Assigned");
                info.put("hodEmail", hodForDept != null ? hodForDept.getEmail() : "");
                
                // Count faculty and students for this department
                long facultyCount = userService.findByRole(User.UserRole.FACULTY).stream()
                    .filter(faculty -> faculty.getDepartment() != null && 
                           faculty.getDepartment().getId().equals(dept.getId()))
                    .count();
                
                long studentCount = userService.findByRole(User.UserRole.STUDENT).stream()
                    .filter(student -> student.getDepartment() != null && 
                           student.getDepartment().getId().equals(dept.getId()))
                    .count();
                
                info.put("facultyCount", facultyCount);
                info.put("studentCount", studentCount);
                
                return info;
            })
            .toList();
        
        model.addAttribute("departments", departmentInfo);
        return "principal/departments";
    }

    @GetMapping("/students")
    public String viewStudents(Model model) {
        List<User> students = userService.findByRole(User.UserRole.STUDENT);
        List<Department> departments = departmentService.findAll();
        
        model.addAttribute("students", students);
        model.addAttribute("departments", departments);
        return "principal/students";
    }

    @GetMapping("/performance")
    public String viewPerformance(Model model) {
        // Get performance statistics across departments
        List<Department> departments = departmentService.findAll();
        List<User> students = userService.findByRole(User.UserRole.STUDENT);
        
        // Calculate performance metrics per department
        List<Map<String, Object>> departmentPerformance = departments.stream()
            .map(dept -> {
                Map<String, Object> perf = new HashMap<>();
                perf.put("departmentName", dept.getName());
                perf.put("departmentCode", dept.getCode());
                
                long deptStudentCount = students.stream()
                    .filter(student -> student.getDepartment() != null && 
                           student.getDepartment().getId().equals(dept.getId()))
                    .count();
                
                perf.put("studentCount", deptStudentCount);
                
                // Calculate actual performance metrics
                List<ExamResult> deptResults = examService.getResultsByDepartment(dept);
                if (!deptResults.isEmpty()) {
                    double avgScore = deptResults.stream()
                        .mapToDouble(ExamResult::getPercentage)
                        .average()
                        .orElse(0.0);
                    
                    long passCount = deptResults.stream()
                        .filter(result -> result.getPercentage() >= 50.0)
                        .count();
                    
                    double passRate = deptResults.size() > 0 ? 
                        (double) passCount / deptResults.size() * 100 : 0.0;
                    
                    perf.put("averageScore", Math.round(avgScore * 100.0) / 100.0);
                    perf.put("passRate", Math.round(passRate * 100.0) / 100.0);
                    perf.put("totalExams", deptResults.size());
                } else {
                    perf.put("averageScore", 0.0);
                    perf.put("passRate", 0.0);
                    perf.put("totalExams", 0);
                }
                
                return perf;
            })
            .toList();
        
        // Prepare chart data for visualization
        StringBuilder chartLabels = new StringBuilder();
        StringBuilder chartData = new StringBuilder();
        StringBuilder chartColors = new StringBuilder();
        
        String[] colors = {"#FF6384", "#36A2EB", "#FFCE56", "#4BC0C0", "#9966FF", "#FF9F40"};
        
        for (int i = 0; i < departmentPerformance.size(); i++) {
            Map<String, Object> dept = departmentPerformance.get(i);
            if (i > 0) {
                chartLabels.append(",");
                chartData.append(",");
                chartColors.append(",");
            }
            chartLabels.append("'").append(dept.get("departmentName")).append("'");
            chartData.append(dept.get("averageScore"));
            chartColors.append("'").append(colors[i % colors.length]).append("'");
        }
        
        model.addAttribute("departmentPerformance", departmentPerformance);
        model.addAttribute("totalStudents", students.size());
        model.addAttribute("chartLabels", chartLabels.toString());
        model.addAttribute("chartData", chartData.toString());
        model.addAttribute("chartColors", chartColors.toString());
        
        return "principal/performance";
    }

    @GetMapping("/communication")
    public String viewCommunication(Model model) {
        // Get all HODs for communication
        List<User> hods = userService.findByRole(User.UserRole.HOD);
        
        model.addAttribute("hods", hods);
        // TODO: Add message history when communication system is implemented
        // model.addAttribute("messages", messageService.getMessagesForPrincipal());
        
        return "principal/communication";
    }
}
