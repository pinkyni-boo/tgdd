package com.hutech.demo.runner;

import com.hutech.demo.model.Customer;
import com.hutech.demo.model.Role;
import com.hutech.demo.model.User;
import com.hutech.demo.repository.CustomerRepository;
import com.hutech.demo.repository.IRoleRepository;
import com.hutech.demo.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@Order(5)
@RequiredArgsConstructor
public class SecurityDataSeeder implements CommandLineRunner {

    private final IRoleRepository roleRepository;
    private final IUserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Role adminRole = ensureRole("ADMIN", "Vai tro quan tri vien");
        Role managerRole = ensureRole("MANAGER", "Vai tro quan ly san pham");
        Role userRole = ensureRole("USER", "Vai tro nguoi dung");

        ensureUser("admin", "admin@hutech.com", "0900000001", "admin123", Set.of(adminRole));
        ensureUser("manager", "manager@hutech.com", "0900000002", "manager123", Set.of(managerRole));
        ensureUser("user", "user@hutech.com", "0900000003", "user123", Set.of(userRole));

        Customer testCustomer = customerRepository.findByPhone("0900000003").orElseGet(() -> {
            Customer customer = new Customer();
            customer.setPhone("0900000003");
            customer.setCustomerName("user");
            customer.setLoyaltyPoints(0);
            return customerRepository.save(customer);
        });
        if (testCustomer.getLoyaltyPoints() < 2500) {
            testCustomer.setLoyaltyPoints(2500);
            customerRepository.save(testCustomer);
        }
    }

    private Role ensureRole(String name, String description) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            return roleRepository.save(role);
        });
    }

    private void ensureUser(String username, String email, String phone, String rawPassword, Set<Role> roles) {
        userRepository.findByUsername(username).orElseGet(() -> {
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPhone(phone);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setProvider("local");
            user.setRoles(new HashSet<>(roles));
            user.setMfaEnabled(false);
            return userRepository.save(user);
        });
    }
}
