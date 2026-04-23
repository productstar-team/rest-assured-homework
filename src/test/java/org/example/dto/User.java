package org.example.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;


@Data
@Builder
public class User {
    private String username;
    private String password;
    private boolean enabled;
    private Set<Role> roles;
}
