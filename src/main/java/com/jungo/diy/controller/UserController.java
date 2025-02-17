package com.jungo.diy.controller;

import com.jungo.diy.entity.User;
import com.jungo.diy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping
    public List<User> listUsers() {
        return userService.getAllUsers(); 
    }

    @PostMapping
    public String createUser(@RequestBody User user) {
        userService.addUser(user); 
        return "User created: " + user.getName(); 
    }
}