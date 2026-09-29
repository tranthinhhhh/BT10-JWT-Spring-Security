package vn.edu.hcmute.jwt.controller;

import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hcmute.jwt.dto.UserResponse;
import vn.edu.hcmute.jwt.service.UserService;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(Principal principal) {
        return userService.getByEmail(principal.getName());
    }

    @GetMapping
    public List<UserResponse> allUsers() {
        return userService.findAll();
    }
}
