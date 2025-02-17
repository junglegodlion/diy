package com.jungo.diy.service;

import com.jungo.diy.entity.User;
import com.jungo.diy.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    public List<User> getAllUsers() {
        return userMapper.findAll(); 
    }

    public int addUser(User user) {
        return userMapper.insert(user); 
    }
}