package com.loganalyzer.service;

import com.loganalyzer.dto.JwtResponse;
import com.loganalyzer.dto.LoginRequest;
import com.loganalyzer.entity.Role;
import com.loganalyzer.entity.User;
import com.loganalyzer.repository.UserRepository;
import com.loganalyzer.security.JwtUtils;
import com.loganalyzer.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        String refreshToken = jwtUtils.generateRefreshToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Set<String> roles = userPrincipal.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toSet());

        // Update last login time
        User user = userRepository.findByUsername(userPrincipal.getUsername()).orElse(null);
        if (user != null) {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        }

        return new JwtResponse(jwt, refreshToken, userPrincipal.getId(), userPrincipal.getUsername(), roles);
    }

    public JwtResponse refreshToken(String refreshToken) {
        if (jwtUtils.validateJwtToken(refreshToken) && !jwtUtils.isTokenExpired(refreshToken)) {
            String username = jwtUtils.getUserNameFromJwtToken(refreshToken);
            User user = userRepository.findByUsername(username).orElse(null);
            
            if (user != null && user.getIsActive()) {
                String newAccessToken = jwtUtils.generateTokenFromUsername(username, 86400000); // 24 hours
                String newRefreshToken = jwtUtils.generateTokenFromUsername(username, 604800000); // 7 days
                
                Set<String> roles = user.getRoles().stream()
                        .map(role -> "ROLE_" + role.getAuthority())
                        .collect(Collectors.toSet());
                
                return new JwtResponse(newAccessToken, newRefreshToken, user.getId(), user.getUsername(), roles);
            }
        }
        return null;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return userRepository.findByUsername(userPrincipal.getUsername()).orElse(null);
        }
        return null;
    }

    public void initializeDefaultUsers() {
        if (userRepository.count() == 0) {
            // Create admin user
            Set<Role> adminRoles = new HashSet<>();
            adminRoles.add(Role.ADMIN);
            adminRoles.add(Role.ANALYST);
            adminRoles.add(Role.VIEWER);
            
            User admin = new User("admin", encoder.encode("admin123"), adminRoles);
            userRepository.save(admin);

            // Create analyst user
            Set<Role> analystRoles = new HashSet<>();
            analystRoles.add(Role.ANALYST);
            analystRoles.add(Role.VIEWER);
            
            User analyst = new User("analyst", encoder.encode("analyst123"), analystRoles);
            userRepository.save(analyst);

            // Create viewer user
            Set<Role> viewerRoles = new HashSet<>();
            viewerRoles.add(Role.VIEWER);
            
            User viewer = new User("viewer", encoder.encode("viewer123"), viewerRoles);
            userRepository.save(viewer);
        }
    }
}