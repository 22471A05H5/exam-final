package com.example.demo.controller;

import com.example.demo.entity.Department;
import com.example.demo.entity.User;
import com.example.demo.entity.User.UserRole;
import com.example.demo.service.DepartmentService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/superadmin")
public class SuperAdminController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private DepartmentService departmentService;

    @GetMapping("/principals/create")
    public String createPrincipalForm(Model model) {
        model.addAttribute("newUser", new User());
        return "superadmin/create-principal";
    }

    @PostMapping("/principals/create")
    public String createPrincipal(@ModelAttribute User newUser, RedirectAttributes redirectAttributes) {
        try {
            if (newUser.getPassword() == null || newUser.getPassword().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Password is required!");
                return "redirect:/superadmin/principals/create";
            }
            
            newUser.setRole(UserRole.PRINCIPAL);
            newUser.setActive(true);
            newUser.setEmployeeId("PRI" + System.currentTimeMillis());
            userService.createUser(newUser);
            redirectAttributes.addFlashAttribute("success", "Principal created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating principal: " + e.getMessage());
        }
        return "redirect:/superadmin/principals/create";
    }

    @GetMapping("/hods/create")
    public String createHodForm(Model model) {
        model.addAttribute("hodUser", new User());
        model.addAttribute("assistantUser", new User());
        List<Department> departments = departmentService.findAll();
        model.addAttribute("departments", departments);
        return "superadmin/create-hod";
    }

    @PostMapping("/hods/create")
    public String createHod(@RequestParam String hodFirstName,
                           @RequestParam String hodLastName,
                           @RequestParam String hodEmail,
                           @RequestParam String hodUsername,
                           @RequestParam String hodPassword,
                           @RequestParam(required = false) String hodPhone,
                           @RequestParam String assistantFirstName,
                           @RequestParam String assistantLastName,
                           @RequestParam String assistantEmail,
                           @RequestParam String assistantUsername,
                           @RequestParam String assistantPassword,
                           @RequestParam(required = false) String assistantPhone,
                           @RequestParam(required = false) Long departmentId,
                           RedirectAttributes redirectAttributes) {
        try {
            // Validate department selection
            if (departmentId == null) {
                redirectAttributes.addFlashAttribute("error", "Please select a department!");
                return "redirect:/superadmin/hods/create";
            }
            
            // Validate passwords
            if (hodPassword == null || hodPassword.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "HOD password is required!");
                return "redirect:/superadmin/hods/create";
            }
            if (assistantPassword == null || assistantPassword.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Assistant password is required!");
                return "redirect:/superadmin/hods/create";
            }
            
            // Validate all required fields before creating any users
            if (hodFirstName == null || hodFirstName.trim().isEmpty() ||
                hodLastName == null || hodLastName.trim().isEmpty() ||
                hodEmail == null || hodEmail.trim().isEmpty() ||
                hodUsername == null || hodUsername.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "All HOD fields are required!");
                return "redirect:/superadmin/hods/create";
            }
            
            if (assistantFirstName == null || assistantFirstName.trim().isEmpty() ||
                assistantLastName == null || assistantLastName.trim().isEmpty() ||
                assistantEmail == null || assistantEmail.trim().isEmpty() ||
                assistantUsername == null || assistantUsername.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "All Assistant fields are required!");
                return "redirect:/superadmin/hods/create";
            }
            
            // Create HOD first
            User hodUser = new User();
            hodUser.setFirstName(hodFirstName);
            hodUser.setLastName(hodLastName);
            hodUser.setEmail(hodEmail);
            hodUser.setUsername(hodUsername);
            hodUser.setPassword(hodPassword);
            hodUser.setPhone(hodPhone);
            hodUser.setRole(UserRole.HOD);
            hodUser.setActive(true);
            hodUser.setEmployeeId("HOD" + System.currentTimeMillis());
            
            // Set department for HOD
            Department department = departmentService.findById(departmentId).orElse(null);
            if (department == null) {
                redirectAttributes.addFlashAttribute("error", "Selected department not found!");
                return "redirect:/superadmin/hods/create";
            }
            hodUser.setDepartment(department);
            
            // Create Assistant
            User assistantUser = new User();
            assistantUser.setFirstName(assistantFirstName);
            assistantUser.setLastName(assistantLastName);
            assistantUser.setEmail(assistantEmail);
            assistantUser.setUsername(assistantUsername);
            assistantUser.setPassword(assistantPassword);
            assistantUser.setPhone(assistantPhone);
            assistantUser.setRole(UserRole.ASSISTANT);
            assistantUser.setActive(true);
            assistantUser.setEmployeeId("AST" + System.currentTimeMillis());
            assistantUser.setDepartment(department);
            
            // Save both users in a single transaction - if either fails, both will be rolled back
            userService.createHodAndAssistant(hodUser, assistantUser);

            redirectAttributes.addFlashAttribute("success", "HOD and Assistant created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating users: " + e.getMessage());
        }
        return "redirect:/superadmin/hods/create";
    }

    @GetMapping("/principals")
    public String viewPrincipals(Model model) {
        List<User> principals = userService.getUsersByRole(UserRole.PRINCIPAL);
        model.addAttribute("principals", principals);
        return "superadmin/principals";
    }

    @GetMapping("/hods")
    public String viewHods(Model model) {
        List<User> hods = userService.getUsersByRole(UserRole.HOD);
        List<User> assistants = userService.getUsersByRole(UserRole.ASSISTANT);
        model.addAttribute("hods", hods);
        model.addAttribute("assistants", assistants);
        return "superadmin/hods";
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        return "superadmin/edit-user";
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
        if (user.getRole() == UserRole.PRINCIPAL) {
            return "redirect:/superadmin/principals";
        } else {
            return "redirect:/superadmin/hods";
        }
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found!");
                return "redirect:/superadmin/hods";
            }
            
            UserRole role = user.getRole();
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully!");
            
            // Redirect based on user role
            if (role == UserRole.PRINCIPAL) {
                return "redirect:/superadmin/principals";
            } else if (role == UserRole.HOD) {
                return "redirect:/superadmin/hods";
            } else {
                return "redirect:/superadmin/assistants";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting user: " + e.getMessage());
            return "redirect:/superadmin/hods";
        }
    }

    @PostMapping("/principals/{id}/delete")
    public String deletePrincipal(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id);
            if (user == null || user.getRole() != UserRole.PRINCIPAL) {
                redirectAttributes.addFlashAttribute("error", "Principal not found!");
                return "redirect:/superadmin/principals";
            }
            
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "Principal deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting principal: " + e.getMessage());
        }
        return "redirect:/superadmin/principals";
    }

    @GetMapping("/assistants")
    public String viewAssistants(Model model) {
        List<User> assistants = userService.getUsersByRole(UserRole.ASSISTANT);
        model.addAttribute("assistants", assistants);
        return "superadmin/assistants";
    }
    
    @GetMapping("/departments")
    public String viewDepartments(Model model) {
        List<Department> departments = departmentService.findAll();
        model.addAttribute("departments", departments);
        return "superadmin/departments";
    }
    
    @PostMapping("/departments/{id}/delete")
    public String deleteDepartment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            departmentService.deleteDepartment(id);
            redirectAttributes.addFlashAttribute("success", "Department deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting department: " + e.getMessage());
        }
        return "redirect:/superadmin/departments";
    }
    
    @GetMapping("/departments/{id}/edit")
    public String editDepartmentForm(@PathVariable Long id, Model model) {
        Department department = departmentService.findById(id).orElse(null);
        if (department == null) {
            return "redirect:/superadmin/departments";
        }
        model.addAttribute("department", department);
        return "superadmin/edit-department";
    }
    
    @PostMapping("/departments/{id}/edit")
    public String editDepartment(@PathVariable Long id, 
                               @RequestParam String name,
                               @RequestParam String code,
                               @RequestParam String description,
                               RedirectAttributes redirectAttributes) {
        try {
            Department department = departmentService.findById(id).orElse(null);
            if (department != null) {
                department.setName(name);
                department.setCode(code);
                department.setDescription(description);
                departmentService.updateDepartment(department);
                redirectAttributes.addFlashAttribute("success", "Department updated successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Department not found!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating department: " + e.getMessage());
        }
        return "redirect:/superadmin/departments";
    }

    @GetMapping("/assistants/{id}/edit")
    public String editAssistantForm(@PathVariable Long id, Model model) {
        User assistant = userService.getUserById(id);
        List<Department> departments = departmentService.findAll();
        model.addAttribute("user", assistant);
        model.addAttribute("departments", departments);
        return "superadmin/edit-assistant";
    }

    @PostMapping("/assistants/{id}/edit")
    public String editAssistant(@PathVariable Long id,
                               @RequestParam String firstName,
                               @RequestParam String lastName,
                               @RequestParam String email,
                               @RequestParam String phone,
                               @RequestParam String address,
                               @RequestParam(required = false) Long departmentId,
                               RedirectAttributes redirectAttributes) {
        try {
            User assistant = userService.getUserById(id);
            assistant.setFirstName(firstName);
            assistant.setLastName(lastName);
            assistant.setEmail(email);
            assistant.setPhone(phone);
            assistant.setAddress(address);
            
            if (departmentId != null) {
                Department department = departmentService.findById(departmentId).orElse(null);
                assistant.setDepartment(department);
            }
            
            userService.updateUser(assistant);
            redirectAttributes.addFlashAttribute("success", "Assistant updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating assistant: " + e.getMessage());
        }
        
        return "redirect:/superadmin/assistants";
    }

    @GetMapping("/departments/create")
    public String createDepartmentForm(Model model) {
        model.addAttribute("department", new Department());
        return "superadmin/create-department";
    }

    @PostMapping("/departments/create")
    public String createDepartment(@RequestParam String name,
                                 @RequestParam String code,
                                 @RequestParam String description,
                                 RedirectAttributes redirectAttributes) {
        try {
            departmentService.createDepartment(name, code, description);
            redirectAttributes.addFlashAttribute("success", "Department created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating department: " + e.getMessage());
        }
        
        return "redirect:/superadmin/departments/create";
    }
    
    @GetMapping("/dashboard")
    public String superAdminDashboard() {
        return "dashboard";
    }
}
