package com.divatech.tools;

import com.divatech.entity.DemoUser;
import com.divatech.respository.UserRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserMcpService {

    private final UserRepository userRepository;

    public UserMcpService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Tool(name = "addUser", description = "Create a user with a name and location.")
    public DemoUser addUser(
            @ToolParam(description = "The user's name") String name,
            @ToolParam(description = "The user's location") String location) {
        DemoUser user = new DemoUser();
        user.setName(name);
        user.setLocation(location);
        user.setCreatedBy("shopping-cart-mcp");
        user.setCreatedDate(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Tool(name = "updateUser", description = "Update a user's name and location using their user ID.")
    public DemoUser updateUser(
            @ToolParam(description = "The ID of the user to update") Long id,
            @ToolParam(description = "The user's new name") String name,
            @ToolParam(description = "The user's new location") String location) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setName(name);
                    user.setLocation(location);
                    return userRepository.save(user);
                })
                .orElse(null);
    }

    @Tool(name = "getUsersByName", description = "Find users whose names contain the provided text, ignoring case.")
    public List<DemoUser> getUsersByName(@ToolParam(description = "Full or partial user name") String name) {
        return userRepository.findByNameContainingIgnoreCase(name);
    }

    @Tool(name = "getAllUsers", description = "Retrieve all users.")
    public List<DemoUser> getAllUsers() {
        return userRepository.findAll();
    }

    @Tool(name = "getUsersByLocation", description = "Find users whose locations contain the provided text, ignoring case.")
    public List<DemoUser> getUsersByLocation(@ToolParam(description = "Full or partial location") String location) {
        return userRepository.findByLocationContainingIgnoreCase(location);
    }
}
