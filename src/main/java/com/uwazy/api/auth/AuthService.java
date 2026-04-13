package com.uwazy.api.auth;

import com.uwazy.api.auth.dto.ForgotPasswordRequest;
import com.uwazy.api.auth.dto.ResetPasswordRequest;
import com.uwazy.api.user.Role;
import com.uwazy.api.user.User;
import com.uwazy.api.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService, AuthenticationManager authenticationManager,
            PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        var user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Default role is student if not provided
        user.setRole(request.getRole() != null ? request.getRole() : Role.ETUDIANT);

        userRepository.save(user);

        var userDetails = new CustomUserDetails(user);
        var jwtToken = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(jwtToken)
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }

    public AuthResponse authenticate(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        var userDetails = new CustomUserDetails(user);
        var jwtToken = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(jwtToken)
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public String generatePasswordResetToken(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User with email " + request.getEmail() + " not found"));
        
        // Create new password reset token
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        
        PasswordResetToken savedToken = passwordResetTokenRepository.save(token);
        
        // In a real implementation, send this token via email
        // For now, just return the token for testing purposes
        return savedToken.getToken();
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid password reset token"));

        if (!resetToken.isValid()) {
            throw new RuntimeException("Password reset token is expired or has been used");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }
}
