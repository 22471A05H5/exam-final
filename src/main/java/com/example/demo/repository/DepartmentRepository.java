package com.example.demo.repository;

import com.example.demo.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
    Optional<Department> findByName(String name);
    
    Optional<Department> findByCode(String code);
    
    boolean existsByName(String name);
    
    boolean existsByCode(String code);
    
    List<Department> findByActiveTrue();
    
    @Query("SELECT d FROM Department d WHERE d.active = true ORDER BY d.name")
    List<Department> findAllActiveDepartments();
}
