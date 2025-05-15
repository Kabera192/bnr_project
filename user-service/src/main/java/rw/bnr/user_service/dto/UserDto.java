package rw.bnr.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rw.bnr.user_service.model.User;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto
{
    private long id;

    @NotBlank(message = "Username cannot be blank.")
    private String username;

    @NotBlank(message = "Password cannot be blank.")
    private String password;

    @NotBlank(message = "Email cannot be blank.")
    @Email(
            regexp = "^(\"?[a-zA-Z0-9._%+-]+(?:\\\\.[a-zA-Z0-9._%+-]+)*\"?@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})$",
            message = "Email provided is invalid")
    private String email;

    public UserDto(User user)
    {
        id = user.getId();
        email = user.getEmail();
        username = user.getUsername();
        password = user.getPassword();
    }
}
