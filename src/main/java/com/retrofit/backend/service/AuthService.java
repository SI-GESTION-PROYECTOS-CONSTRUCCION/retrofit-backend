package com.retrofit.backend.service;

import com.retrofit.backend.auth.AuthRequest;

import org.springframework.web.bind.annotation.RequestBody;

public interface AuthService {

    String login(@RequestBody AuthRequest request);

}
