package com.skycarwash.service;

import com.skycarwash.dto.*;
import com.skycarwash.entity.User;
import com.skycarwash.exception.BusinessException;
import com.skycarwash.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        if (request.role() == User.Role.MANAGER) {
            throw new BusinessException("Cannot create another MANAGER account", HttpStatus.BAD_REQUEST);
        }
        if (userRepository.existsByPhone(request.phone().trim())) {
            throw new BusinessException("Phone number already in use", HttpStatus.CONFLICT);
        }

        String tempPassword = generateTempPassword();

        User user = User.builder()
                .name(request.name().trim())
                .phone(request.phone().trim())
                .role(request.role())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .build();

        return new CreateUserResponse(UserResponse.from(userRepository.save(user)), tempPassword);
    }

    @Transactional
    public UserResponse updateStatus(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
        if (user.getRole() == User.Role.MANAGER) {
            throw new BusinessException("Cannot disable the MANAGER account");
        }
        user.setActive(active);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public ResetPasswordResponse resetPassword(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        String tempPassword = generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(user);
        return new ResetPasswordResponse(tempPassword);
    }

    private String generateTempPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(random.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
