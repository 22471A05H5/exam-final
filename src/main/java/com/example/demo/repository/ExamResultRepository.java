package com.example.demo.repository;

import com.example.demo.entity.ExamResult;
import com.example.demo.entity.Exam;
import com.example.demo.entity.User;
import com.example.demo.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    List<ExamResult> findByExam(Exam exam);
    List<ExamResult> findByStudent(User student);
    Optional<ExamResult> findByExamAndStudent(Exam exam, User student);
    List<ExamResult> findByExamOrderByScoreDesc(Exam exam);
    List<ExamResult> findByExam_Department(Department department);
}
