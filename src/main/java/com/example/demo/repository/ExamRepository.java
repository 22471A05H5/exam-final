package com.example.demo.repository;

import com.example.demo.entity.Exam;
import com.example.demo.entity.Department;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByFaculty(User faculty);
    List<Exam> findByDepartment(Department department);
    List<Exam> findByDepartmentAndIsActiveTrue(Department department);
    List<Exam> findByDepartmentAndStartTimeLessThanEqualAndEndTimeGreaterThanEqualAndIsActiveTrue(
        Department department, LocalDateTime now1, LocalDateTime now2);
}
