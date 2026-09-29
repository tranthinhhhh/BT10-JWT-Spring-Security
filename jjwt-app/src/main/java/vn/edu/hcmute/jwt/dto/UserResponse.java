package vn.edu.hcmute.jwt.dto;

import java.time.Instant;
import vn.edu.hcmute.jwt.entity.AppUser;
import vn.edu.hcmute.jwt.entity.Role;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
