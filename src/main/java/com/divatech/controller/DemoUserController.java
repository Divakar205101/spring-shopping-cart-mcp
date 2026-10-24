package com.divatech.controller;

import com.divatech.entity.DemoUser;
import com.divatech.respository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class DemoUserController {

    private final UserRepository userRepository;

    public DemoUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<DemoUser> createUser(@RequestBody UserRequest request) {
        DemoUser user = new DemoUser();
        user.setName(request.name());
        user.setLocation(request.location());
        user.setCreatedBy("shopping-cart-mcp-rest-api");
        user.setCreatedDate(LocalDateTime.now());

        DemoUser savedUser = userRepository.save(user);
        return ResponseEntity.created(URI.create("/api/users/" + savedUser.getId()))
                .body(savedUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DemoUser> updateUser(
            @PathVariable Long id,
            @RequestBody UserRequest request) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setName(request.name());
                    user.setLocation(request.location());
                    return ResponseEntity.ok(userRepository.save(user));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<DemoUser> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/search")
    public List<DemoUser> getUsersByName(@RequestParam String name) {
        return userRepository.findByNameContainingIgnoreCase(name);
    }

    public record UserRequest(String name, String location) {
    }
}
