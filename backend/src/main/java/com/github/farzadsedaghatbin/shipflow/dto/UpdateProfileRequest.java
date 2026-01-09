package com.github.farzadsedaghatbin.shipflow.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Email(message = "Invalid email format")
    private String email;
    
    private String avatarUrl;
    private String bio;
    private String skills;
    private String department;
}
