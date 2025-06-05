package kopo.jeonnam.dto.user;

import lombok.AllArgsConstructor;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class PasswordChangeRequest {
    private String email;
    private String name;
    private String currentPassword;
    private String newPassword;
}