package com.example.demo.controller;

import com.example.demo.entity.*;
import com.example.demo.service.ExamService;
import com.example.demo.service.UserService;
import com.example.demo.service.AIQuestionService;
import com.example.demo.dto.StudentScoreInfo;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@Controller
@RequestMapping("/faculty")
public class FacultyController {

    @Autowired
    private ExamService examService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AIQuestionService aiQuestionService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User faculty = getCurrentFaculty();
        if (faculty != null && faculty.getDepartment() != null) {
            Department department = faculty.getDepartment();
            
            // Get faculty's exams
            List<Exam> exams = examService.getExamsByFaculty(faculty);
            
            // Get students in the same department
            List<User> students = userService.findByRole(User.UserRole.STUDENT).stream()
                .filter(user -> user.getDepartment() != null && 
                               user.getDepartment().getId().equals(department.getId()))
                .collect(Collectors.toList());
            
            model.addAttribute("faculty", faculty);
            model.addAttribute("department", department);
            model.addAttribute("exams", exams);
            model.addAttribute("students", students);
            model.addAttribute("totalExams", exams.size());
            model.addAttribute("totalStudents", students.size());
        }
        
        return "faculty/dashboard";
    }
    
    @GetMapping("/exams")
    public String viewExams(Model model) {
        User faculty = getCurrentFaculty();
        if (faculty != null) {
            List<Exam> exams = examService.getExamsByFaculty(faculty);
            model.addAttribute("exams", exams);
            model.addAttribute("faculty", faculty);
        }
        return "faculty/exams";
    }
    
    @GetMapping("/exams/create")
    public String createExamForm(Model model) {
        User faculty = getCurrentFaculty();
        if (faculty != null) {
            model.addAttribute("faculty", faculty);
            model.addAttribute("department", faculty.getDepartment());
        }
        return "faculty/create-exam";
    }
    
    @GetMapping("/exams/create/manual")
    public String createManualExamForm(Model model) {
        User faculty = getCurrentFaculty();
        if (faculty != null) {
            model.addAttribute("faculty", faculty);
            model.addAttribute("department", faculty.getDepartment());
        }
        return "faculty/create-manual-exam";
    }
    
    @GetMapping("/exams/create/ai")
    public String createAIExamForm(Model model) {
        User faculty = getCurrentFaculty();
        if (faculty != null) {
            model.addAttribute("faculty", faculty);
            model.addAttribute("department", faculty.getDepartment());
        }
        return "faculty/create-ai-exam";
    }
    
    @GetMapping("/exams/create/csv")
    public String createCSVExamForm(Model model) {
        User faculty = getCurrentFaculty();
        if (faculty != null) {
            model.addAttribute("faculty", faculty);
            model.addAttribute("department", faculty.getDepartment());
        }
        return "faculty/create-csv-exam";
    }
    
    @PostMapping("/exams/create")
    public String createExam(@RequestParam String title,
                            @RequestParam String description,
                            @RequestParam Integer totalQuestions,
                            @RequestParam Integer timeLimit,
                            @RequestParam String startTime,
                            @RequestParam String endTime,
                            @RequestParam String examType,
                            RedirectAttributes redirectAttributes) {
        try {
            User faculty = getCurrentFaculty();
            if (faculty == null || faculty.getDepartment() == null) {
                redirectAttributes.addFlashAttribute("error", "Faculty department not found!");
                return "redirect:/faculty/exams";
            }
            
            Exam exam = new Exam();
            exam.setTitle(title);
            exam.setDescription(description);
            exam.setTotalQuestions(totalQuestions);
            exam.setTimeLimit(timeLimit);
            exam.setFaculty(faculty);
            exam.setDepartment(faculty.getDepartment());
            exam.setExamType(Exam.ExamType.valueOf(examType));
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            exam.setStartTime(LocalDateTime.parse(startTime, formatter));
            exam.setEndTime(LocalDateTime.parse(endTime, formatter));
            
            Exam savedExam = examService.createExam(exam);
            redirectAttributes.addFlashAttribute("success", "Exam created successfully!");
            
            // Redirect to add questions based on exam type
            if (examType.equals("MANUAL")) {
                return "redirect:/faculty/exams/" + savedExam.getId() + "/questions/add";
            } else if (examType.equals("CSV_UPLOAD")) {
                return "redirect:/faculty/exams/" + savedExam.getId() + "/questions/upload";
            } else {
                return "redirect:/faculty/exams/" + savedExam.getId() + "/questions/generate";
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating exam: " + e.getMessage());
            return "redirect:/faculty/exams/create";
        }
    }
    
    @PostMapping("/exams/create/manual")
    public String createManualExam(@RequestParam String title,
                                  @RequestParam String description,
                                  @RequestParam Integer totalQuestions,
                                  @RequestParam Integer timeLimit,
                                  @RequestParam String startTime,
                                  @RequestParam String endTime,
                                  RedirectAttributes redirectAttributes) {
        try {
            User faculty = getCurrentFaculty();
            if (faculty == null || faculty.getDepartment() == null) {
                redirectAttributes.addFlashAttribute("error", "Faculty department not found!");
                return "redirect:/faculty/dashboard";
            }
            
            Exam exam = new Exam();
            exam.setTitle(title);
            exam.setDescription(description);
            exam.setTotalQuestions(totalQuestions);
            exam.setTimeLimit(timeLimit);
            exam.setFaculty(faculty);
            exam.setDepartment(faculty.getDepartment());
            exam.setExamType(Exam.ExamType.MANUAL);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            exam.setStartTime(LocalDateTime.parse(startTime, formatter));
            exam.setEndTime(LocalDateTime.parse(endTime, formatter));
            
            Exam savedExam = examService.createExam(exam);
            redirectAttributes.addFlashAttribute("success", "Exam created! Now add questions manually.");
            
            return "redirect:/faculty/exams/" + savedExam.getId() + "/questions/add";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating exam: " + e.getMessage());
            return "redirect:/faculty/exams/create/manual";
        }
    }
    
    @PostMapping("/exams/create/ai")
    public String createAIExam(@RequestParam(required = false) String title,
                              @RequestParam(required = false) String topic,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) String subject,
                              @RequestParam(required = false) String difficulty,
                              @RequestParam(required = false) Integer totalQuestions,
                              @RequestParam(required = false) Integer timeLimit,
                              @RequestParam(required = false) String startTime,
                              @RequestParam(required = false) String endTime,
                              @RequestParam(required = false) String confirmed,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        
        // If this is a GET-like request (no parameters), show the form
        if (title == null && topic == null) {
            User faculty = getCurrentFaculty();
            if (faculty != null) {
                model.addAttribute("faculty", faculty);
                model.addAttribute("department", faculty.getDepartment());
            }
            return "faculty/create-ai-exam";
        }
        // Validate required fields - use subject if topic is null
        String finalTopic = (topic != null && !topic.trim().isEmpty()) ? topic : subject;
        if (title == null || title.trim().isEmpty() || 
            finalTopic == null || finalTopic.trim().isEmpty() ||
            totalQuestions == null || timeLimit == null ||
            startTime == null || endTime == null) {
            redirectAttributes.addFlashAttribute("error", "All fields are required!");
            return "redirect:/faculty/exams/create/ai";
        }

        try {
            User faculty = getCurrentFaculty();
            if (faculty == null || faculty.getDepartment() == null) {
                redirectAttributes.addFlashAttribute("error", "Faculty department not found!");
                return "redirect:/faculty/dashboard";
            }
            
            Exam exam = new Exam();
            exam.setTitle(title);
            exam.setDescription(description != null ? description : "");
            exam.setTotalQuestions(totalQuestions);
            exam.setTimeLimit(timeLimit);
            exam.setFaculty(faculty);
            exam.setDepartment(faculty.getDepartment());
            exam.setExamType(Exam.ExamType.AI_GENERATED);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            exam.setStartTime(LocalDateTime.parse(startTime, formatter));
            exam.setEndTime(LocalDateTime.parse(endTime, formatter));
            
            Exam savedExam = examService.createExam(exam);
            
            // Automatically generate AI questions using Google Gemini API
            try {
                String finalDifficulty = difficulty != null && !difficulty.trim().isEmpty() ? difficulty : "MEDIUM";
                
                List<Map<String, String>> aiQuestions = aiQuestionService.generateQuestions(finalTopic, finalDifficulty, totalQuestions);
                
                int questionNumber = 1;
                int savedQuestions = 0;
                
                for (Map<String, String> aiQuestion : aiQuestions) {
                    // Validate question data before saving
                    String questionText = aiQuestion.get("questionText");
                    String optionA = aiQuestion.get("optionA");
                    String optionB = aiQuestion.get("optionB");
                    String optionC = aiQuestion.get("optionC");
                    String optionD = aiQuestion.get("optionD");
                    String correctAnswer = aiQuestion.get("correctAnswer");
                    
                    // Skip questions with missing data
                    if (questionText == null || questionText.trim().isEmpty() ||
                        optionA == null || optionA.trim().isEmpty() ||
                        optionB == null || optionB.trim().isEmpty() ||
                        optionC == null || optionC.trim().isEmpty() ||
                        optionD == null || optionD.trim().isEmpty() ||
                        correctAnswer == null || correctAnswer.trim().isEmpty()) {
                        
                        System.out.println("DEBUG: Skipping invalid question in controller: " + questionText);
                        continue;
                    }
                    
                    Question question = new Question();
                    question.setExam(savedExam);
                    question.setQuestionText(questionText.trim());
                    question.setOptionA(optionA.trim());
                    question.setOptionB(optionB.trim());
                    question.setOptionC(optionC.trim());
                    question.setOptionD(optionD.trim());
                    question.setCorrectAnswer(correctAnswer.trim());
                    question.setExplanation(aiQuestion.get("explanation") != null ? aiQuestion.get("explanation").trim() : "No explanation provided");
                    question.setQuestionNumber(questionNumber++);
                    
                    examService.saveQuestion(question);
                    savedQuestions++;
                }
                
                redirectAttributes.addFlashAttribute("success", 
                    String.format("AI Exam '%s' created successfully with %d valid questions generated by Google Gemini AI!", title, savedQuestions));
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("warning", 
                    "Exam created but AI question generation failed: " + e.getMessage() + ". You can manually add questions later.");
            }
            
            return "redirect:/faculty/exams";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating AI exam: " + e.getMessage());
            return "redirect:/faculty/exams/create/ai";
        }
    }
    
    @PostMapping("/exams/create/csv")
    public String createCSVExam(@RequestParam String title,
                               @RequestParam String description,
                               @RequestParam Integer timeLimit,
                               @RequestParam String startTime,
                               @RequestParam String endTime,
                               @RequestParam("csvFile") MultipartFile file,
                               RedirectAttributes redirectAttributes) {
        try {
            User faculty = getCurrentFaculty();
            if (faculty == null || faculty.getDepartment() == null) {
                redirectAttributes.addFlashAttribute("error", "Faculty department not found!");
                return "redirect:/faculty/dashboard";
            }
            
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a CSV file!");
                return "redirect:/faculty/exams/create/csv";
            }
            
            // Parse CSV to count questions
            String content = new String(file.getBytes());
            String[] lines = content.split("\n");
            int questionCount = Math.max(0, lines.length - 1); // Subtract header
            
            Exam exam = new Exam();
            exam.setTitle(title);
            exam.setDescription(description);
            exam.setTotalQuestions(questionCount);
            exam.setTimeLimit(timeLimit);
            exam.setFaculty(faculty);
            exam.setDepartment(faculty.getDepartment());
            exam.setExamType(Exam.ExamType.CSV_UPLOAD);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            exam.setStartTime(LocalDateTime.parse(startTime, formatter));
            exam.setEndTime(LocalDateTime.parse(endTime, formatter));
            
            Exam savedExam = examService.createExam(exam);
            
            // Parse and create questions from CSV
            int questionNumber = 1;
            int addedQuestions = 0;
            
            for (int i = 1; i < lines.length; i++) { // Skip header
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                
                String[] parts = line.split(",");
                if (parts.length >= 6) {
                    Question question = new Question();
                    question.setExam(savedExam);
                    question.setQuestionText(parts[0].trim().replaceAll("\"", ""));
                    question.setOptionA(parts[1].trim().replaceAll("\"", ""));
                    question.setOptionB(parts[2].trim().replaceAll("\"", ""));
                    question.setOptionC(parts[3].trim().replaceAll("\"", ""));
                    question.setOptionD(parts[4].trim().replaceAll("\"", ""));
                    question.setCorrectAnswer(parts[5].trim().replaceAll("\"", ""));
                    question.setQuestionNumber(questionNumber++);
                    
                    if (parts.length > 6) {
                        question.setExplanation(parts[6].trim().replaceAll("\"", ""));
                    }
                    
                    examService.saveQuestion(question);
                    addedQuestions++;
                }
            }
            
            redirectAttributes.addFlashAttribute("success", 
                String.format("CSV Exam created successfully with %d questions!", addedQuestions));
            
            return "redirect:/faculty/exams";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating CSV exam: " + e.getMessage());
            return "redirect:/faculty/exams/create/csv";
        }
    }
    
    @GetMapping("/exams/{id}/questions/add")
    public String addQuestionsForm(@PathVariable Long id, Model model) {
        Exam exam = examService.getExamById(id).orElse(null);
        User faculty = getCurrentFaculty();
        
        if (exam != null && faculty != null && exam.getFaculty().getId().equals(faculty.getId())) {
            List<Question> existingQuestions = examService.getQuestionsByExam(exam);
            if (existingQuestions == null) {
                existingQuestions = new ArrayList<>();
            }
            model.addAttribute("exam", exam);
            model.addAttribute("existingQuestions", existingQuestions);
            model.addAttribute("nextQuestionNumber", existingQuestions.size() + 1);
        } else {
            model.addAttribute("existingQuestions", new ArrayList<>());
        }
        
        return "faculty/add-questions";
    }
    
    @PostMapping("/exams/{id}/questions/add")
    public String addQuestion(@PathVariable Long id,
                             @RequestParam String questionText,
                             @RequestParam String optionA,
                             @RequestParam String optionB,
                             @RequestParam String optionC,
                             @RequestParam String optionD,
                             @RequestParam String correctAnswer,
                             @RequestParam(required = false) String explanation,
                             RedirectAttributes redirectAttributes) {
        try {
            Exam exam = examService.getExamById(id).orElse(null);
            User faculty = getCurrentFaculty();
            
            if (exam != null && faculty != null && exam.getFaculty().getId().equals(faculty.getId())) {
                Question question = new Question();
                question.setExam(exam);
                question.setQuestionText(questionText);
                question.setOptionA(optionA);
                question.setOptionB(optionB);
                question.setOptionC(optionC);
                question.setOptionD(optionD);
                question.setCorrectAnswer(correctAnswer);
                question.setExplanation(explanation);
                
                List<Question> existingQuestions = examService.getQuestionsByExam(exam);
                question.setQuestionNumber(existingQuestions.size() + 1);
                
                examService.saveQuestion(question);
                redirectAttributes.addFlashAttribute("success", "Question added successfully!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding question: " + e.getMessage());
        }
        
        return "redirect:/faculty/exams/" + id + "/questions/add";
    }
    
    @GetMapping("/exams/{id}/questions/upload")
    public String uploadQuestionsForm(@PathVariable Long id, Model model) {
        Exam exam = examService.getExamById(id).orElse(null);
        User faculty = getCurrentFaculty();
        
        if (exam != null && faculty != null && exam.getFaculty().getId().equals(faculty.getId())) {
            model.addAttribute("exam", exam);
        }
        
        return "faculty/upload-questions";
    }
    
    @PostMapping("/exams/{id}/questions/upload")
    public String uploadQuestions(@PathVariable Long id,
                                 @RequestParam("csvFile") MultipartFile file,
                                 RedirectAttributes redirectAttributes) {
        try {
            Exam exam = examService.getExamById(id).orElse(null);
            User faculty = getCurrentFaculty();
            
            if (exam == null || faculty == null || !exam.getFaculty().getId().equals(faculty.getId())) {
                redirectAttributes.addFlashAttribute("error", "Unauthorized access!");
                return "redirect:/faculty/exams";
            }
            
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a CSV file!");
                return "redirect:/faculty/exams/" + id + "/questions/upload";
            }
            
            // Parse CSV and create questions
            String content = new String(file.getBytes());
            String[] lines = content.split("\n");
            
            int questionNumber = examService.getQuestionsByExam(exam).size() + 1;
            int addedQuestions = 0;
            
            for (int i = 1; i < lines.length; i++) { // Skip header
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                
                String[] parts = line.split(",");
                if (parts.length >= 6) {
                    Question question = new Question();
                    question.setExam(exam);
                    question.setQuestionText(parts[0].trim().replaceAll("\"", ""));
                    question.setOptionA(parts[1].trim().replaceAll("\"", ""));
                    question.setOptionB(parts[2].trim().replaceAll("\"", ""));
                    question.setOptionC(parts[3].trim().replaceAll("\"", ""));
                    question.setOptionD(parts[4].trim().replaceAll("\"", ""));
                    question.setCorrectAnswer(parts[5].trim().replaceAll("\"", ""));
                    question.setQuestionNumber(questionNumber++);
                    
                    if (parts.length > 6) {
                        question.setExplanation(parts[6].trim().replaceAll("\"", ""));
                    }
                    
                    examService.saveQuestion(question);
                    addedQuestions++;
                }
            }
            
            redirectAttributes.addFlashAttribute("success", 
                String.format("Successfully uploaded %d questions from CSV!", addedQuestions));
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error uploading CSV: " + e.getMessage());
        }
        
        return "redirect:/faculty/exams/" + id + "/questions/add";
    }
    
    @GetMapping("/exams/{id}/questions/generate")
    public String generateQuestionsForm(@PathVariable Long id, Model model) {
        Exam exam = examService.getExamById(id).orElse(null);
        User faculty = getCurrentFaculty();
        
        if (exam != null && faculty != null && exam.getFaculty().getId().equals(faculty.getId())) {
            model.addAttribute("exam", exam);
        }
        
        return "faculty/generate-questions";
    }
    
    @PostMapping("/exams/{id}/questions/generate")
    public String generateQuestions(@PathVariable Long id,
                                   @RequestParam String topic,
                                   @RequestParam String difficulty,
                                   @RequestParam Integer numberOfQuestions,
                                   RedirectAttributes redirectAttributes) {
        try {
            Exam exam = examService.getExamById(id).orElse(null);
            User faculty = getCurrentFaculty();
            
            if (exam == null || faculty == null || !exam.getFaculty().getId().equals(faculty.getId())) {
                redirectAttributes.addFlashAttribute("error", "Unauthorized access!");
                return "redirect:/faculty/exams";
            }
            
            // Clear existing questions first
            List<Question> existingQuestions = examService.getQuestionsByExam(exam);
            for (Question q : existingQuestions) {
                examService.deleteQuestion(q.getId());
            }
            
            // Generate questions using real AI API (Gemini)
            List<Map<String, String>> aiQuestions = aiQuestionService.generateQuestions(topic, difficulty, numberOfQuestions);
            
            int questionNumber = 1;
            for (Map<String, String> aiQuestion : aiQuestions) {
                Question question = new Question();
                question.setExam(exam);
                question.setQuestionText(aiQuestion.get("questionText"));
                question.setOptionA(aiQuestion.get("optionA"));
                question.setOptionB(aiQuestion.get("optionB"));
                question.setOptionC(aiQuestion.get("optionC"));
                question.setOptionD(aiQuestion.get("optionD"));
                question.setCorrectAnswer(aiQuestion.get("correctAnswer"));
                question.setExplanation(aiQuestion.get("explanation"));
                question.setQuestionNumber(questionNumber++);
                
                examService.saveQuestion(question);
            }
            
            redirectAttributes.addFlashAttribute("success", 
                String.format("Successfully generated %d AI questions for topic: %s", numberOfQuestions, topic));
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error generating questions: " + e.getMessage());
        }
        
        return "redirect:/faculty/exams/" + id + "/questions/add";
    }
    
    @GetMapping("/exams/{id}/results")
    public String viewExamResults(@PathVariable Long id, Model model) {
        Exam exam = examService.getExamById(id).orElse(null);
        User faculty = getCurrentFaculty();
        
        if (exam != null && faculty != null && exam.getFaculty().getId().equals(faculty.getId())) {
            List<ExamResult> results = examService.getResultsByExam(exam);
            model.addAttribute("exam", exam);
            model.addAttribute("results", results);
        }
        
        return "faculty/exam-results";
    }
    
    @GetMapping("/students")
    public String redirectToStudentResults() {
        return "redirect:/faculty/student-results";
    }
    
    @GetMapping("/student-results")
    public String viewStudentResults(Model model) {
        User faculty = getCurrentFaculty();
        if (faculty != null && faculty.getDepartment() != null) {
            List<User> students = userService.findByRole(User.UserRole.STUDENT).stream()
                .filter(user -> user.getDepartment() != null && 
                               user.getDepartment().getId().equals(faculty.getDepartment().getId()))
                .collect(Collectors.toList());
            
            // Calculate student scores for faculty's exams
            java.util.Map<Long, StudentScoreInfo> studentScores = new java.util.HashMap<>();
            java.util.Map<String, ExamResult> studentExamResults = new java.util.HashMap<>();
            List<Exam> facultyExams = examService.getExamsByFaculty(faculty);
            
            for (User student : students) {
                List<ExamResult> studentResults = examService.getResultsByStudent(student);
                // Filter results for this faculty's exams only
                List<ExamResult> facultyExamResults = studentResults.stream()
                    .filter(result -> facultyExams.stream()
                        .anyMatch(exam -> exam.getId().equals(result.getExam().getId())))
                    .collect(Collectors.toList());
                
                // Store individual exam results for display
                for (ExamResult result : facultyExamResults) {
                    String key = student.getId() + "_" + result.getExam().getId();
                    studentExamResults.put(key, result);
                }
                
                if (!facultyExamResults.isEmpty()) {
                    double averageScore = facultyExamResults.stream()
                        .mapToDouble(ExamResult::getPercentage)
                        .average()
                        .orElse(0.0);
                    
                    double bestScore = facultyExamResults.stream()
                        .mapToDouble(ExamResult::getPercentage)
                        .max()
                        .orElse(0.0);
                    
                    StudentScoreInfo scoreInfo = new StudentScoreInfo();
                    scoreInfo.setExamsTaken(facultyExamResults.size());
                    scoreInfo.setAverageScore(averageScore);
                    scoreInfo.setBestScore(bestScore);
                    
                    studentScores.put(student.getId(), scoreInfo);
                }
            }
            
            model.addAttribute("exams", facultyExams);
            model.addAttribute("faculty", faculty);
            model.addAttribute("department", faculty.getDepartment());
        }
        return "faculty/student-results";
    }
    
    @GetMapping("/exams/{examId}/students")
    public String viewExamStudents(@PathVariable Long examId, Model model, RedirectAttributes redirectAttributes) {
        User faculty = getCurrentFaculty();
        if (faculty == null) {
            return "redirect:/login";
        }
        
        // Get the exam and verify it belongs to this faculty
        Optional<Exam> examOpt = examService.getExamById(examId);
        if (examOpt.isEmpty() || !examOpt.get().getFaculty().getId().equals(faculty.getId())) {
            redirectAttributes.addFlashAttribute("error", "Exam not found or access denied!");
            return "redirect:/faculty/dashboard";
        }
        
        Exam exam = examOpt.get();
        
        // Get all students who took this specific exam
        List<ExamResult> examResults = examService.getResultsByExam(exam);
        List<User> studentsWhoTookExam = examResults.stream()
            .map(ExamResult::getStudent)
            .distinct()
            .collect(Collectors.toList());
        
        // Create a map of student results for this exam
        java.util.Map<Long, ExamResult> studentResults = examResults.stream()
            .collect(Collectors.toMap(
                result -> result.getStudent().getId(),
                result -> result
            ));
        
        model.addAttribute("exam", exam);
        model.addAttribute("students", studentsWhoTookExam);
        model.addAttribute("studentResults", studentResults);
        model.addAttribute("faculty", faculty);
        
        return "faculty/exam-students";
    }
    
    @GetMapping("/exams/{id}/edit")
    public String editExamForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Exam exam = examService.getExamById(id).orElse(null);
            User faculty = getCurrentFaculty();
            
            if (exam == null) {
                redirectAttributes.addFlashAttribute("error", "Exam not found!");
                return "redirect:/faculty/exams";
            }
            
            if (faculty == null || !exam.getFaculty().getId().equals(faculty.getId())) {
                redirectAttributes.addFlashAttribute("error", "Unauthorized access!");
                return "redirect:/faculty/exams";
            }
            
            model.addAttribute("exam", exam);
            model.addAttribute("faculty", faculty);
            return "faculty/edit-exam";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error loading exam: " + e.getMessage());
            return "redirect:/faculty/exams";
        }
    }
    
    @PostMapping("/exams/{id}/edit")
    public String updateExam(@PathVariable Long id,
                            @RequestParam String title,
                            @RequestParam String description,
                            @RequestParam Integer timeLimit,
                            @RequestParam String startTime,
                            @RequestParam String endTime,
                            RedirectAttributes redirectAttributes) {
        try {
            Exam exam = examService.getExamById(id).orElse(null);
            User faculty = getCurrentFaculty();
            
            if (exam == null) {
                redirectAttributes.addFlashAttribute("error", "Exam not found!");
                return "redirect:/faculty/exams";
            }
            
            if (faculty == null || !exam.getFaculty().getId().equals(faculty.getId())) {
                redirectAttributes.addFlashAttribute("error", "Unauthorized access!");
                return "redirect:/faculty/exams";
            }
            
            // Update exam details
            exam.setTitle(title);
            exam.setDescription(description);
            exam.setTimeLimit(timeLimit);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            exam.setStartTime(LocalDateTime.parse(startTime, formatter));
            exam.setEndTime(LocalDateTime.parse(endTime, formatter));
            
            examService.updateExam(exam);
            redirectAttributes.addFlashAttribute("success", "Exam updated successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating exam: " + e.getMessage());
        }
        
        return "redirect:/faculty/exams";
    }

    @PostMapping("/exams/{id}/delete")
    public String deleteExam(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Exam exam = examService.getExamById(id).orElse(null);
            User faculty = getCurrentFaculty();
            
            if (exam != null && faculty != null && exam.getFaculty().getId().equals(faculty.getId())) {
                examService.deleteExam(id);
                redirectAttributes.addFlashAttribute("success", "Exam deleted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Unauthorized access!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting exam: " + e.getMessage());
        }
        
        return "redirect:/faculty/exams";
    }
    
    @PostMapping("/exams/{id}/toggle-status")
    public String toggleExamStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Exam exam = examService.getExamById(id).orElse(null);
            User faculty = getCurrentFaculty();
            
            if (exam != null && faculty != null && exam.getFaculty().getId().equals(faculty.getId())) {
                exam.setIsActive(!exam.getIsActive());
                examService.updateExam(exam);
                String status = exam.getIsActive() ? "activated" : "deactivated";
                redirectAttributes.addFlashAttribute("success", "Exam " + status + " successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Unauthorized access!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating exam status: " + e.getMessage());
        }
        
        return "redirect:/faculty/exams";
    }
    
    @GetMapping("/students/{id}/results")
    public String viewStudentResults(@PathVariable Long id, Model model) {
        User faculty = getCurrentFaculty();
        User student = userService.getUserById(id);
        
        if (faculty != null && student != null && 
            student.getDepartment().getId().equals(faculty.getDepartment().getId())) {
            
            List<Exam> facultyExams = examService.getExamsByFaculty(faculty);
            List<ExamResult> studentResults = examService.getResultsByStudent(student).stream()
                .filter(result -> facultyExams.stream()
                    .anyMatch(exam -> exam.getId().equals(result.getExam().getId())))
                .collect(Collectors.toList());
            
            model.addAttribute("student", student);
            model.addAttribute("results", studentResults);
            model.addAttribute("faculty", faculty);
        }
        
        return "faculty/student-results";
    }

    @PostMapping("/exams/{id}/cleanup-questions")
    public String cleanupPlaceholderQuestions(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Exam exam = examService.getExamById(id).orElse(null);
            User faculty = getCurrentFaculty();
            
            if (exam != null && faculty != null && exam.getFaculty().getId().equals(faculty.getId())) {
                List<Question> questions = examService.getQuestionsByExam(exam);
                int deletedCount = 0;
                
                for (Question q : questions) {
                    if (isPlaceholderQuestion(q)) {
                        examService.deleteQuestion(q.getId());
                        deletedCount++;
                    }
                }
                
                redirectAttributes.addFlashAttribute("success", 
                    String.format("Cleaned up %d placeholder questions. Use AI generation to add realistic questions.", deletedCount));
            } else {
                redirectAttributes.addFlashAttribute("error", "Unauthorized access!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error cleaning questions: " + e.getMessage());
        }
        
        return "redirect:/faculty/exams/" + id + "/questions/add";
    }
    
    private boolean isPlaceholderQuestion(Question q) {
        return q.getQuestionText().contains("AI Generated") ||
               q.getQuestionText().contains("Basic Programming question") ||
               q.getQuestionText().contains("most important aspect") ||
               q.getQuestionText().contains("Generic question") ||
               q.getOptionA().contains("Correct answer for") ||
               q.getOptionA().contains("Simple concept") ||
               q.getOptionA().contains("Incorrect option") ||
               q.getOptionB().contains("Incorrect option") ||
               q.getOptionC().contains("Incorrect option") ||
               q.getOptionD().contains("Incorrect option");
    }

    @PostMapping("/api/generate-preview")
    @ResponseBody
    public ResponseEntity<List<Map<String, String>>> generatePreviewQuestions(@RequestBody Map<String, Object> request) {
        try {
            String topic = (String) request.get("topic");
            String difficulty = (String) request.get("difficulty");
            Integer numberOfQuestions = (Integer) request.get("numberOfQuestions");
            
            // Generate questions using real AI API (Gemini)
            List<Map<String, String>> aiQuestions = aiQuestionService.generateQuestions(topic, difficulty, numberOfQuestions);
            
            return ResponseEntity.ok(aiQuestions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private User getCurrentFaculty() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userService.findByUsername(username).orElse(null);
    }
}
