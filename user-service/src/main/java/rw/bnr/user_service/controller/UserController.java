package rw.bnr.user_service.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.apache.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import rw.bnr.user_service.dto.*;
import rw.bnr.user_service.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController
{
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserDto userDto)
    {
        log.info("Registering user: {}", userDto);

        if (userService.emailExists(userDto.getEmail()))
        {
            log.info("Email already exists: {}", userDto.getEmail());
            return ResponseEntity
                    .status(HttpStatus.SC_UNPROCESSABLE_ENTITY)
                    .body(new ErrorResponse("Email already exists"));
        }

        UserDto savedUser = userService.registerUser(userDto);
        log.info("Registered user: {}", savedUser);

        return ResponseEntity.created(
                ServletUriComponentsBuilder
                        .fromCurrentContextPath()
                        .path("/user/{id}")
                        .buildAndExpand(savedUser.getId())
                .toUri()
        ).body(savedUser);
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers()
    {
        log.info("Retrieving all users.");
        List<UserDto> users = userService.getAllUsers();

        if (users.isEmpty())
        {
            log.info("No users found.");
            return ResponseEntity.noContent().build();
        }

        log.info("Retrieved {} users.", users.size());
        return ResponseEntity.ok().body(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@NotNull(message = "id cannot be null.")
                                     @Positive(message = "id cannot be negative")
                                     @Valid @PathVariable long id)
    {
        log.info("Retrieving user with id {}.", id);
        UserDto user = userService.getUserById(id);

        if (user == null)
        {
            log.info("User with id {} not found.", id);
            return ResponseEntity
                    .status(HttpStatus.SC_NOT_FOUND)
                    .body(new ErrorResponse("User with id " + id + " not found."));
        }

        log.info("Retrieved {} user.", user);
        return ResponseEntity.ok().body(user);
    }

    @GetMapping
    public ResponseEntity<?> getUser(@NotBlank(message = "username cannot be blank.")
                                    @RequestParam
                                    @Valid String username)
    {
        log.info("Retrieving user with username {}.", username);
        UserDto user = userService.getUserByUsername(username);

        if (user == null)
        {
            log.info("User with username {} not found.", username);
            return ResponseEntity
                    .status(HttpStatus.SC_NOT_FOUND)
                    .body(new ErrorResponse("User with username " + username + " not found."));
        }

        log.info("Retrieved {} with username {}.", user, username);
        return ResponseEntity.ok().body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody CredentialDto credentialDto)
    {
        log.info("Login credentials: {}", credentialDto);
        JwtToken token = userService.authenticateUser(credentialDto);

        if (token == null)
        {
            log.info("Login failed. Token is null");
            return ResponseEntity
                    .status(HttpStatus.SC_UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid credentials"));
        }

        log.info("Login successful. Token: {}", token);
        return ResponseEntity.ok().body(token);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateUser(@Valid @RequestBody UpdateUsernameDto userDto,
                                        @NotNull(message = "id cannot be null.")
                                        @Valid @PathVariable long id)
    {
        log.info("Updating username for user with id {}.", id);
        UserDto user = userService.updateUsername(userDto, id);

        if (user == null)
        {
            log.info("Could not update username. User with id {} not found.", id);
            return ResponseEntity
                    .status(HttpStatus.SC_NOT_FOUND)
                    .body(new ErrorResponse("Could not update username. User with id " + id + " not found."));
        }

        log.info("Updating username for user with id {} successfully.", id);
        return ResponseEntity.ok().body(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@NotNull(message = "id cannot be null.")
                                            @Positive(message = "id cannot be negative")
                                            @PathVariable long id)
    {
        log.info("Deleting user with id {}.", id);
        boolean isDeleted = userService.deleteUser(id);

        if (isDeleted)
        {
            log.info("User with id {} deleted.", id);
            return ResponseEntity
                    .status(HttpStatus.SC_OK)
                    .body("User deleted successfully.");
        }

        log.info("Delete failed. User with id {} not found.", id);
        return ResponseEntity
                .status(HttpStatus.SC_NOT_FOUND)
                .body(new ErrorResponse("Delete failed. User with id " + id + " not found."));
    }
}
