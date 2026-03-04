package com.saraasansor.api.controller;

import com.saraasansor.api.dto.auth.LoginResponse;
import com.saraasansor.api.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    @Test
    void loginShouldReturnSuccessForValidCredentials() throws Exception {
        AuthService authService = mock(AuthService.class);

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setAccessToken("access-token");
        loginResponse.setRefreshToken("refresh-token");
        loginResponse.setRole("STAFF_USER");
        loginResponse.setUserType("STAFF");

        when(authService.login(any())).thenReturn(loginResponse);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"staff\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    void loginShouldReturnUnauthorizedForInvalidCredentials() throws Exception {
        AuthService authService = mock(AuthService.class);
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"staff\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
