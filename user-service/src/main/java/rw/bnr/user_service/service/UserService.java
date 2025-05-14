package rw.bnr.user_service.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rw.bnr.user_service.dto.CredentialDto;
import rw.bnr.user_service.dto.JwtToken;
import rw.bnr.user_service.dto.UserDto;
import rw.bnr.user_service.model.User;
import rw.bnr.user_service.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService
{
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public JwtToken authenticateUser(CredentialDto credentialDto)
    {
        log.info("authenticateUser called. Should return a JWT token");
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(credentialDto.getEmail(), credentialDto.getPassword()));

        if(authentication.isAuthenticated())
        {
            log.info("User authenticated. Creating JWT to return to user.");
            return JwtToken.builder()
                    .token(jwtService.generateToken(credentialDto.getEmail()))
                    .build();
        }

        log.info("User NOT authenticated.");
        return null;
    }

    public UserDto registerUser(@Valid UserDto userDto)
    {
        log.info("registerUser called. Should return a user DTO");
        User newUser = User.builder()
                .username(userDto.getUsername())
                .password(passwordEncoder.encode(userDto.getPassword()))
                .email(userDto.getEmail())
                .isDeleted(false)
                .build();

        log.info("New user created.");
        return new UserDto(userRepository.save(newUser));
    }

    public boolean emailExists(String email)
    {
        log.info("emailExists called. Should return a boolean value");
        return userRepository.existsByEmail(email);
    }
}
