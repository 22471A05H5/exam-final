package com.example.demo.controller;

import com.example.demo.entity.Department;
import com.example.demo.entity.User;
import com.example.demo.entity.User.UserRole;
import com.example.demo.entity.Exam;
import com.example.demo.entity.ExamResult;
import com.example.demo.service.UserService;
import com.example.demo.service.ExamService;
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
@RequestMapping("/hod")
public class HodController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private ExamService examService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User hod = userService.findByUsername(username).orElse(null);
        
        if (hod != null && hod.getDepartment() != null) {
            Department department = hod.getDepartment();
            
            // Get all users under this HOD's department
            List<User> allUsers = userService.findByRole(UserRole.ASSISTANT);
            allUsers.addAll(userService.findByRole(UserRole.FACULTY));
            allUsers.addAll(userService.findByRole(UserRole.STUDENT));
            
            List<User> departmentAssistants = allUsers.stream()
                .filter(user -> user.getRole() == UserRole.ASSISTANT && 
                               user.getDepartment() != null && 
                               user.getDepartment().getId().equals(department.getId()))
                .collect(Collectors.toList());
                
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
            
            model.addAttribute("hod", hod);
            model.addAttribute("department", department);
            model.addAttribute("assistants", departmentAssistants);
            model.addAttribute("faculty", departmentFaculty);
            model.addAttribute("students", departmentStudents);
            model.addAttribute("totalAssistants", departmentAssistants.size());
            model.addAttribute("totalFaculty", departmentFaculty.size());
            model.addAttribute("totalStudents", departmentStudents.size());
        }
        
        return "hod/dashboard";
    }

    @GetMapping("/assistants")
    public String viewAssistants(Model model) {
        User hod = getCurrentHod();
        if (hod != null && hod.getDepartment() != null) {
            List<User> assistants = userService.findByRole(UserRole.ASSISTANT).stream()
                .filter(user -> user.getDepartment() != null && 
                               user.getDepartment().getId().equals(hod.getDepartment().getId()))
                .collect(Collectors.toList());
            model.addAttribute("assistants", assistants);
            model.addAttribute("department", hod.getDepartment());
        }
        return "hod/assistants";
    }
    
    @GetMapping("/faculty")
    public String viewFaculty(Model model) {
        User hod = getCurrentHod();
        if (hod != null && hod.getDepartment() != null) {
            List<User> faculty = userService.findByRole(UserRole.FACULTY).stream()
                .filter(user -> user.getDepartment() != null && 
                               user.getDepartment().getId().equals(hod.getDepartment().getId()))
                .collect(Collectors.toList());
            model.addAttribute("faculty", faculty);
            model.addAttribute("department", hod.getDepartment());
        }
        return "hod/faculty";
    }
    
    @GetMapping("/students")
    public String viewStudents(Model model) {
        User hod = getCurrentHod();
        if (hod != null && hod.getDepartment() != null) {
            List<User> students = userService.findByRole(UserRole.STUDENT).stream()
                .filter(user -> user.getDepartment() != null && 
                               user.getDepartment().getId().equals(hod.getDepartment().getId()))
                .collect(Collectors.toList());
            // Get exam results for students in this department
            List<ExamResult> studentResults = students.stream()
                .flatMap(student -> examService.getResultsByStudent(student).stream())
                .collect(Collectors.toList());
            
            model.addAttribute("students", students);
            model.addAttribute("studentResults", studentResults);
            model.addAttribute("department", hod.getDepartment());
        }
        return "hod/students";
    }
    
    @GetMapping("/exams")
    public String viewExams(Model model) {
        User hod = getCurrentHod();
        if (hod != null && hod.getDepartment() != null) {
            Department department = hod.getDepartment();
            
            // Get all exams created by faculty in this department
            List<Exam> departmentExams = examService.getExamsByDepartment(department);
            
            model.addAttribute("exams", departmentExams);
            model.addAttribute("department", department);
            model.addAttribute("hod", hod);
        }
        return "hod/exams";
    }
    
    @GetMapping("/exams/{id}/results")
    public String viewExamResults(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User hod = getCurrentHod();
        Exam exam = examService.getExamById(id).orElse(null);
        
        if (exam == null) {
            redirectAttributes.addFlashAttribute("error", "Exam not found!");
            return "redirect:/hod/exams";
        }
        
        // Verify exam belongs to HOD's department
        if (hod == null || hod.getDepartment() == null || 
            !exam.getDepartment().getId().equals(hod.getDepartment().getId())) {
            redirectAttributes.addFlashAttribute("error", "Unauthorized access to this exam!");
            return "redirect:/hod/exams";
        }
        
        List<ExamResult> results = examService.getResultsByExam(exam);
        model.addAttribute("exam", exam);
        model.addAttribute("results", results);
        model.addAttribute("department", hod.getDepartment());
        
        return "hod/exam-results";
    }
    
    @GetMapping("/performance")
    public String viewPerformance(Model model) {
        User hod = getCurrentHod();
        if (hod != null && hod.getDepartment() != null) {
            Department department = hod.getDepartment();
            
            // Get all exam results for this department
            List<ExamResult> deptResults = examService.getResultsByDepartment(department);
            
            // Calculate department performance metrics
            double avgScore = deptResults.stream()
                .mapToDouble(ExamResult::getPercentage)
                .average()
                .orElse(0.0);
            
            long passCount = deptResults.stream()
                .filter(result -> result.getPercentage() >= 50.0)
                .count();
            
            double passRate = deptResults.size() > 0 ? 
                (double) passCount / deptResults.size() * 100 : 0.0;
            
            model.addAttribute("department", department);
            model.addAttribute("results", deptResults);
            model.addAttribute("averageScore", Math.round(avgScore * 100.0) / 100.0);
            model.addAttribute("passRate", Math.round(passRate * 100.0) / 100.0);
            model.addAttribute("totalExams", deptResults.size());
        }
        return "hod/performance";
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        return "hod/edit-user";
    }

    @PostMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, @ModelAttribute User user, RedirectAttributes redirectAttributes) {
        try {
            user.setId(id);
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("success", "Assistant updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating assistant: " + e.getMessage());
        }
        
        return "redirect:/hod/assistants";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Assistant not found!");
                return "redirect:/hod/assistants";
            }
            
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "Assistant deleted successfully!");
            return "redirect:/hod/assistants";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting assistant: " + e.getMessage());
            return "redirect:/hod/assistants";
        }
    }
    
    private User getCurrentHod() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userService.findByUsername(username).orElse(null);
    }
}
