package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.ExamResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ExamService {
    
    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private ExamResultRepository examResultRepository;
    
    public Exam createExam(Exam exam) {
        return examRepository.save(exam);
    }
    
    public List<Exam> getExamsByFaculty(User faculty) {
        return examRepository.findByFaculty(faculty);
    }
    
    public List<Exam> getExamsByDepartment(Department department) {
        return examRepository.findByDepartment(department);
    }
    
    public List<Exam> getActiveExamsForDepartment(Department department) {
        return examRepository.findByDepartmentAndIsActiveTrue(department);
    }
    
    public Optional<Exam> getExamById(Long id) {
        return examRepository.findById(id);
    }
    
    public void deleteExam(Long id) {
        examRepository.deleteById(id);
    }
    
    public Exam updateExam(Exam exam) {
        return examRepository.save(exam);
    }
    
    public List<Question> getQuestionsByExam(Exam exam) {
        return questionRepository.findByExamOrderByQuestionNumber(exam);
    }
    
    public Question saveQuestion(Question question) {
        return questionRepository.save(question);
    }
    
    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }
    
    public ExamResult submitExam(ExamResult result) {
        return examResultRepository.save(result);
    }
    
    public List<ExamResult> getResultsByExam(Exam exam) {
        return examResultRepository.findByExamOrderByScoreDesc(exam);
    }
    
    public List<ExamResult> getResultsByStudent(User student) {
        return examResultRepository.findByStudent(student);
    }
    
    public Optional<ExamResult> getResultByExamAndStudent(Exam exam, User student) {
        return examResultRepository.findByExamAndStudent(exam, student);
    }
    
    public boolean hasStudentTakenExam(Exam exam, User student) {
        return examResultRepository.findByExamAndStudent(exam, student).isPresent();
    }
    
    public List<ExamResult> getResultsByDepartment(Department department) {
        return examResultRepository.findByExam_Department(department);
    }
}
