package com.skycarwash.service;

import com.skycarwash.dto.LoginRequest;
import com.skycarwash.dto.LoginResponse;
import com.skycarwash.entity.User;
import com.skycarwash.repository.UserRepository;
import com.skycarwash.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.phone(), request.password())
            );
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid phone or password");
        }

        User user = userRepository.findByPhone(request.phone())
                .orElseThrow(() -> new BadCredentialsException("Invalid phone or password"));

        String token = jwtUtil.generateToken(user);

        return new LoginResponse(token, user.getRole().name(), user.getId(), user.getName());
    }
}
