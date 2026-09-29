package com.retrofit.backend.service;

import com.retrofit.backend.dto.ForgotPasswordRequestDto;
import com.retrofit.backend.dto.ResetPasswordRequestDto;

public interface PasswordResetService {
    void processForgotPassword(ForgotPasswordRequestDto request);
    void resetPassword(ResetPasswordRequestDto request);
}
