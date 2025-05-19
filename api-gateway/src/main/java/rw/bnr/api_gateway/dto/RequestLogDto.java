package rw.bnr.api_gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestLogDto
{
    private String service;
    private String ip;
    private String method;
    private String path;
    private int status;
    private String userId;
    private LocalDateTime timestamp;
}
