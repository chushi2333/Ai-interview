package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.entities.User;

import java.util.Optional;

public interface UserService {
    Optional<User> getUserById(Long id);
}
