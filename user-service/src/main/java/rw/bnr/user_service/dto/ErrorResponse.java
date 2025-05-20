package rw.bnr.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse
{
    private String message;

    public String toString()
    {
        return "{\n message: " + message + " \n}";
    }
}
