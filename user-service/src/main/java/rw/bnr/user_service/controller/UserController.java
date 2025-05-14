package rw.bnr.user_service.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import rw.bnr.user_service.dto.CredentialDto;
import rw.bnr.user_service.dto.ErrorResponse;
import rw.bnr.user_service.dto.JwtToken;
import rw.bnr.user_service.dto.UserDto;
import rw.bnr.user_service.service.UserService;

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
        // TODO: Retrieve all existing users and return them
        return ResponseEntity.ok().body(new UserDto());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@NotNull(message = "id cannot be null.")
                                     @Positive(message = "id cannot be negative")
                                     @PathVariable long id,
                                     @RequestParam(required = false) String userId)
    {
        // TODO: Retrieve a particular user
        return ResponseEntity.ok().body(new UserDto());
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
    public ResponseEntity<?> updateUser(@RequestBody UserDto userDto, @NotNull(message = "id cannot be null.")
                                        @PathVariable long id)
    {
        // TODO: Simply update the user's username.
        return ResponseEntity.ok().body(userDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@NotNull(message = "id cannot be null.")
                                            @Positive(message = "id cannot be negative")
                                            @PathVariable long id)
    {
        // TODO: Perform a soft delete of the user
        return ResponseEntity.ok().body(new UserDto());
    }
}
