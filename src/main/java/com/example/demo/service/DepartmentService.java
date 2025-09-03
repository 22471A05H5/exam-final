package com.example.demo.service;

import com.example.demo.entity.Department;
import com.example.demo.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DepartmentService {
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    public List<Department> findAll() {
        return departmentRepository.findAll();
    }
    
    public List<Department> findAllActive() {
        return departmentRepository.findAllActiveDepartments();
    }
    
    public Optional<Department> findById(Long id) {
        return departmentRepository.findById(id);
    }
    
    public Optional<Department> findByName(String name) {
        return departmentRepository.findByName(name);
    }
    
    public Optional<Department> findByCode(String code) {
        return departmentRepository.findByCode(code);
    }
    
    public Department save(Department department) {
        return departmentRepository.save(department);
    }
    
    public Department createDepartment(String name, String code, String description) {
        if (departmentRepository.existsByName(name)) {
            throw new RuntimeException("Department with name '" + name + "' already exists");
        }
        if (departmentRepository.existsByCode(code)) {
            throw new RuntimeException("Department with code '" + code + "' already exists");
        }
        
        Department department = new Department(name, code, description);
        return departmentRepository.save(department);
    }
    
    public Department updateDepartment(Department department) {
        return departmentRepository.save(department);
    }
    
    public Department updateDepartment(Long id, String name, String code, String description) {
        Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Department not found with id: " + id));
        
        // Check for duplicates only if name/code is changing
        if (!department.getName().equals(name) && departmentRepository.existsByName(name)) {
            throw new RuntimeException("Department with name '" + name + "' already exists");
        }
        if (!department.getCode().equals(code) && departmentRepository.existsByCode(code)) {
            throw new RuntimeException("Department with code '" + code + "' already exists");
        }
        
        department.setName(name);
        department.setCode(code);
        department.setDescription(description);
        
        return departmentRepository.save(department);
    }
    
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Department not found with id: " + id));
        
        // Soft delete - mark as inactive instead of actual deletion
        department.setActive(false);
        departmentRepository.save(department);
    }
    
    public boolean existsByName(String name) {
        return departmentRepository.existsByName(name);
    }
    
    public boolean existsByCode(String code) {
        return departmentRepository.existsByCode(code);
    }
}
