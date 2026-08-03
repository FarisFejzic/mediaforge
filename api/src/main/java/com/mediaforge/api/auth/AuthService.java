package com.mediaforge.api.auth;

import com.mediaforge.api.auth.dto.RegisterRequest;
import com.mediaforge.api.auth.dto.UserResponse;
import com.mediaforge.common.domain.User;
import com.mediaforge.common.domain.enums.Role;
import com.mediaforge.common.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;


@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;

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
}
