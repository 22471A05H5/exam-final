package com.example.demo.controller;

import com.example.demo.entity.*;
import com.example.demo.service.ExamService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private ExamService examService;
    
    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User student = getCurrentStudent();
        if (student != null && student.getDepartment() != null) {
            Department department = student.getDepartment();
            
            // Get available exams for the student's department
            List<Exam> availableExams = examService.getActiveExamsForDepartment(department);
            
            // Get student's exam results
            List<ExamResult> results = examService.getResultsByStudent(student);
            
            model.addAttribute("student", student);
            model.addAttribute("department", department);
            model.addAttribute("availableExams", availableExams);
            model.addAttribute("results", results);
            model.addAttribute("totalExams", availableExams.size());
            model.addAttribute("completedExams", results.size());
        }
        
        return "student/dashboard";
    }
    
    @GetMapping("/exams")
    public String viewAvailableExams(Model model) {
        User student = getCurrentStudent();
        if (student != null && student.getDepartment() != null) {
            List<Exam> availableExams = examService.getActiveExamsForDepartment(student.getDepartment());
            
            // Filter out exams already taken by the student
            availableExams.removeIf(exam -> examService.hasStudentTakenExam(exam, student));
            
            model.addAttribute("exams", availableExams);
            model.addAttribute("student", student);
        }
        return "student/exams";
    }
    
    @GetMapping("/exams/{id}/take")
    public String takeExam(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User student = getCurrentStudent();
        Exam exam = examService.getExamById(id).orElse(null);
        
        if (exam == null) {
            redirectAttributes.addFlashAttribute("error", "Exam not found!");
            return "redirect:/student/exams";
        }
        
        if (student == null || student.getDepartment() == null || 
            !exam.getDepartment().getId().equals(student.getDepartment().getId())) {
            redirectAttributes.addFlashAttribute("error", "Unauthorized access to this exam!");
            return "redirect:/student/exams";
        }
        
        // Check if student has already taken this exam
        if (examService.hasStudentTakenExam(exam, student)) {
            redirectAttributes.addFlashAttribute("error", "You have already taken this exam!");
            return "redirect:/student/results";
        }
        
        // Check if exam is currently active
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime())) {
            redirectAttributes.addFlashAttribute("error", "Exam has not started yet!");
            return "redirect:/student/exams";
        }
        
        if (now.isAfter(exam.getEndTime())) {
            redirectAttributes.addFlashAttribute("error", "Exam has ended!");
            return "redirect:/student/exams";
        }
        
        List<Question> questions = examService.getQuestionsByExam(exam);
        if (questions.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No questions available for this exam!");
            return "redirect:/student/exams";
        }
        
        model.addAttribute("exam", exam);
        model.addAttribute("questions", questions);
        model.addAttribute("student", student);
        model.addAttribute("timeRemaining", exam.getTimeLimit() * 60); // Convert minutes to seconds
        
        return "student/take-exam";
    }
    
    @PostMapping("/exams/{id}/submit")
    public String submitExam(@PathVariable Long id,
                            @RequestParam Map<String, String> answers,
                            @RequestParam Long startTime,
                            RedirectAttributes redirectAttributes) {
        try {
            User student = getCurrentStudent();
            Exam exam = examService.getExamById(id).orElse(null);
            
            if (exam == null || student == null) {
                redirectAttributes.addFlashAttribute("error", "Invalid exam or student!");
                return "redirect:/student/exams";
            }
            
            // Check if student has already taken this exam
            if (examService.hasStudentTakenExam(exam, student)) {
                redirectAttributes.addFlashAttribute("error", "You have already taken this exam!");
                return "redirect:/student/results";
            }
            
            List<Question> questions = examService.getQuestionsByExam(exam);
            
            // Calculate score
            int score = 0;
            Map<String, String> studentAnswers = new HashMap<>();
            
            for (Question question : questions) {
                String questionKey = "question_" + question.getId();
                String studentAnswer = answers.get(questionKey);
                
                if (studentAnswer != null) {
                    // Store with question ID as key (not "question_" prefix)
                    studentAnswers.put(String.valueOf(question.getId()), studentAnswer);
                    if (studentAnswer.equals(question.getCorrectAnswer())) {
                        score++;
                    }
                }
            }
            
            // Calculate time taken
            long endTime = System.currentTimeMillis();
            int timeTaken = (int) ((endTime - startTime) / (1000 * 60)); // in minutes
            
            // Create exam result
            ExamResult result = new ExamResult();
            result.setExam(exam);
            result.setStudent(student);
            result.setScore(score);
            result.setTotalQuestions(questions.size());
            result.setPercentage((double) score / questions.size() * 100);
            result.setSubmittedAt(LocalDateTime.now());
            result.setTimeTaken(timeTaken);
            
            // Convert answers to JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            result.setAnswers(objectMapper.writeValueAsString(studentAnswers));
            
            examService.submitExam(result);
            
            redirectAttributes.addFlashAttribute("success", 
                String.format("Exam submitted successfully! Score: %d/%d (%.1f%%)", 
                    score, questions.size(), result.getPercentage()));
            
            return "redirect:/student/results";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error submitting exam: " + e.getMessage());
            return "redirect:/student/exams/" + id + "/take";
        }
    }
    
    @GetMapping("/results")
    public String viewResults(Model model) {
        User student = getCurrentStudent();
        if (student != null) {
            List<ExamResult> results = examService.getResultsByStudent(student);
            model.addAttribute("results", results);
            model.addAttribute("student", student);
        }
        return "student/results";
    }
    
    @GetMapping("/results/{id}")
    public String viewDetailedResult(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User student = getCurrentStudent();
        ExamResult result = examService.getResultsByStudent(student).stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);
        
        if (result == null) {
            redirectAttributes.addFlashAttribute("error", "Result not found!");
            return "redirect:/student/results";
        }
        
        // Get exam questions
        List<Question> questions = examService.getQuestionsByExam(result.getExam());
        
        // Parse student answers from JSON
        Map<Long, String> studentAnswers = new HashMap<>();
        try {
            if (result.getAnswers() != null && !result.getAnswers().isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper();
                TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {};
                Map<String, String> answersMap = objectMapper.readValue(result.getAnswers(), typeRef);
                
                // Convert string keys to Long keys
                for (Map.Entry<String, String> entry : answersMap.entrySet()) {
                    try {
                        Long questionId = Long.parseLong(entry.getKey());
                        studentAnswers.put(questionId, entry.getValue());
                    } catch (NumberFormatException e) {
                        // Skip invalid question IDs
                    }
                }
            }
        } catch (Exception e) {
            // If parsing fails, continue with empty answers map
            System.err.println("Error parsing student answers: " + e.getMessage());
        }
        
        model.addAttribute("result", result);
        model.addAttribute("student", student);
        model.addAttribute("questions", questions);
        model.addAttribute("studentAnswers", studentAnswers);
        return "student/result-detail";
    }
    
    @GetMapping("/profile")
    public String viewProfile(Model model) {
        User student = getCurrentStudent();
        if (student != null) {
            model.addAttribute("student", student);
            return "student/profile";
        }
        return "redirect:/login";
    }
    
    private User getCurrentStudent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userService.findByUsername(username).orElse(null);
    }
}
