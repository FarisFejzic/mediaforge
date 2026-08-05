package com.mediaforge.api.auth;

import com.mediaforge.api.auth.dto.LoginRequest;
import com.mediaforge.api.auth.dto.LoginResponse;
import com.mediaforge.api.auth.dto.RegisterRequest;
import com.mediaforge.api.auth.dto.UserResponse;
import com.mediaforge.common.domain.User;
import com.mediaforge.common.domain.enums.Role;
import com.mediaforge.common.repository.UserRepository;
import com.mediaforge.common.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;


@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;

    }

    public UserResponse register(RegisterRequest request){
        if (userRepository.existsByEmail(request.email())){
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = User.create(
        request.email(),
        passwordEncoder.encode(request.password()),
        Role.USER,
        OffsetDateTime.now()
        );

        User saved = userRepository.save(user);

        return new UserResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getRole(),
                saved.getCreatedAt()
        );
    }

    public LoginResponse login(LoginRequest request){
        User user = userRepository.findByEmail(request.email()).orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())){
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().toString()
        );

        return new LoginResponse(token, "Bearer", 3600L);
    }
}
