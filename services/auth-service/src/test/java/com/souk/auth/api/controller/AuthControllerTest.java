package com.souk.auth.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.souk.auth.api.dto.ApiResponse;
import com.souk.auth.api.dto.LoginRequest;
import com.souk.auth.api.dto.RegisterRequest;
import com.souk.auth.api.dto.TokenResponse;
import com.souk.auth.service.AuthenticationService;
import com.souk.common.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_ShouldReturnCreated() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@example.com");
        req.setPassword("password");
        req.setRole("CUSTOMER"); // Assuming enum or string is valid

        User user = new User();
        user.setUserId(1L);
        user.setEmail("test@example.com");
        user.setEmailVerified(false);
        user.setMfaEnabled(false);

        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    void login_ShouldReturnOk_WhenCredentialsValid() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("password");

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("access-token");
        tokenResponse.setRefreshToken("refresh-token");
        tokenResponse.setMfaRequired(false);

        when(authenticationService.login(any(LoginRequest.class), anyString(), anyString())).thenReturn(tokenResponse);

        // Header User-Agent is used in controller logic for device info
        mockMvc.perform(post("/api/v1/auth/login")
                .header("User-Agent", "TestDevice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }
}
