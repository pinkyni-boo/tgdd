package com.hutech.demo.service;

import com.hutech.demo.AppRole;
import com.hutech.demo.model.Customer;
import com.hutech.demo.model.Role;
import com.hutech.demo.model.User;
import com.hutech.demo.repository.CustomerRepository;
import com.hutech.demo.repository.IRoleRepository;
import com.hutech.demo.repository.IUserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserService implements UserDetailsService {

    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public void save(@NotNull User user) {
        validateUniqueness(user);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    public void register(@NotNull User user) {
        save(user);
        setDefaultRole(user.getUsername());
        ensureCustomerProfile(user);
    }

    public void setDefaultRole(String username) {
        userRepository.findByUsername(username).ifPresentOrElse(
                user -> {
                    Role defaultRole = roleRepository.findByName(AppRole.USER.name())
                            .orElseGet(() -> roleRepository.findRoleById(AppRole.USER.value));
                    if (defaultRole == null) {
                        throw new IllegalStateException("Không tìm thấy quyền USER mặc định");
                    }
                    user.getRoles().add(defaultRole);
                    userRepository.save(user);
                },
                () -> {
                    throw new UsernameNotFoundException("User not found");
                }
        );
    }

    public User getDomainUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public void enableMfa(String username) {
        User user = getDomainUserByUsername(username);
        user.setMfaEnabled(true);
        userRepository.save(user);
    }

    public void setMfaSecret(String username, String secret) {
        User user = getDomainUserByUsername(username);
        user.setMfaSecret(secret);
        user.setMfaEnabled(false);
        userRepository.save(user);
    }

    // Tải thông tin chi tiết người dùng để xác thực.
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getAuthorities())
                .accountExpired(!user.isAccountNonExpired())
                .accountLocked(!user.isAccountNonLocked())
                .credentialsExpired(!user.isCredentialsNonExpired())
                .disabled(!user.isEnabled())
                .build();
    }

    // Tìm kiếm người dùng dựa trên tên đăng nhập.
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username);
    }

    private void validateUniqueness(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }
        if (userRepository.existsByPhone(user.getPhone())) {
            throw new IllegalArgumentException("Số điện thoại đã tồn tại");
        }
    }

    private void ensureCustomerProfile(User user) {
        customerRepository.findByPhone(user.getPhone())
                .orElseGet(() -> {
                    Customer customer = new Customer();
                    customer.setPhone(user.getPhone());
                    customer.setCustomerName(user.getUsername());
                    customer.setLoyaltyPoints(0);
                    return customerRepository.save(customer);
                });
    }
}
