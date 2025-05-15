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
import rw.bnr.user_service.dto.UpdateUsernameDto;
import rw.bnr.user_service.dto.UserDto;
import rw.bnr.user_service.model.User;
import rw.bnr.user_service.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

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
        User user = (User) authentication.getPrincipal();

        if(authentication.isAuthenticated() && !user.isDeleted())
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
        User user = userRepository.findByEmail(email);

        if (user == null)
            return false;

        // Should return true if the email exists and the account is deleted
        return userRepository.existsByEmail(email) && !user.isDeleted();
    }

    public List<UserDto> getAllUsers()
    {
        log.info("getAllUsers called in user service.");
        List<User> users = userRepository.findAll();
        List<UserDto> userDtos = new ArrayList<>();

        if (users.isEmpty())
        {
            log.info("No Users found by the user service.");
            return new ArrayList<>();
        }

        users.forEach(u ->
                {   if (!u.isDeleted())
                        userDtos.add(mapToDto(u));
                });
        return userDtos;
    }

    public UserDto getUserById(Long id)
    {
        log.info("getUserById called in user service.");
        User user = userRepository.findById(id).orElse(null);

        if (user == null || !user.isDeleted())
        {
            log.info("No User found by the user service.");
            return null;
        }

        log.info("User found.");
        return mapToDto(user);
    }

    private UserDto mapToDto(User u)
    {
        return UserDto.builder()
                .id(u.getId())
                .email(u.getEmail())
                .username(u.getUsername())
                .password(u.getPassword())
                .build();
    }

    public UserDto getUserByUsername(String username)
    {
        log.info("getUserByUsername called in user service.");
        User user = userRepository.findByUsername(username);

        if (user == null || user.isDeleted())
        {
            log.info("User with username {} not found by the user service.", username);
            return null;
        }

        log.info("User with username {} found.", username);
        return mapToDto(user);
    }

    public UserDto updateUsername(UpdateUsernameDto userDto, Long id)
    {
        log.info("updateUsername called in user service. Should return a user DTO");
        User user = userRepository.findById(id).orElse(null);

        if (user == null || user.isDeleted())
        {
            log.info("Could not update username. User with id {} not found by the user service.", id);
            return null;
        }

        user.setUsername(userDto.getUsername());
        log.info("User updated.");
        return mapToDto(userRepository.save(user));
    }

    public boolean deleteUser(long id)
    {
        log.info("deleteUser called in user service. Should return a boolean.");
        User user = userRepository.findById(id).orElse(null);

        if (user == null || user.isDeleted())
        {
            log.info("Could not delete user. User with id {} not found by the user service.", id);
            return false;
        }

        user.setDeleted(true);
        userRepository.save(user);
        log.info("User deleted.");
        return true;
    }
}
