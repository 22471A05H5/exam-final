package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.entity.User.UserRole;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
    
    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }
    
    public List<User> findByRole(UserRole role) {
        return userRepository.findByRole(role);
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    public User updateUser(User user) {
        User existingUser = getUserById(user.getId());
        if (existingUser != null) {
            existingUser.setFirstName(user.getFirstName());
            existingUser.setLastName(user.getLastName());
            existingUser.setEmail(user.getEmail());
            existingUser.setPhone(user.getPhone());
            existingUser.setAddress(user.getAddress());
            existingUser.setActive(user.isActive());
            // Don't update password unless it's provided
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            return userRepository.save(existingUser);
        }
        return null;
    }
    
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    @Transactional
    public void createHodAndAssistant(User hodUser, User assistantUser) {
        // Create both users in a single transaction
        // If either fails, both will be rolled back
        createUser(hodUser);
        createUser(assistantUser);
    }
    
    public void createDefaultSuperAdmin() {
        if (!userRepository.existsByUsername("superadmin")) {
            User superAdmin = new User();
            superAdmin.setUsername("superadmin");
            superAdmin.setPassword("admin123");
            superAdmin.setFirstName("Super");
            superAdmin.setLastName("Admin");
            superAdmin.setEmail("superadmin@exam.com");
            superAdmin.setRole(UserRole.SUPERADMIN);
            superAdmin.setActive(true);
            superAdmin.setEmployeeId("SA001");
            
            createUser(superAdmin);
        }
    }
}
