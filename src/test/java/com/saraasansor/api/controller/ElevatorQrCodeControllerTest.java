package com.saraasansor.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.dto.QrCodeResponseDTO;
import com.saraasansor.api.service.ElevatorQrCodeService;
import com.saraasansor.api.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ElevatorQrCodeControllerTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void elevatorQrcodesRouteShouldDefaultOnlyWithQrToTrue() throws Exception {
        ElevatorQrCodeService qrCodeService = mock(ElevatorQrCodeService.class);
        when(qrCodeService.list(any(Pageable.class), eq("abc"), eq(1L), eq(true)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ElevatorQrCodeController(qrCodeService, new ObjectMapper()))
                .build();

        mockMvc.perform(get("/elevator-qrcodes")
                        .param("search", "abc")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());

        verify(qrCodeService).list(any(Pageable.class), eq("abc"), eq(1L), eq(true));
    }

    @Test
    void printEndpointShouldReturnPng() throws Exception {
        ElevatorQrCodeService qrCodeService = mock(ElevatorQrCodeService.class);
        when(qrCodeService.generateQrImage(11L, 1L)).thenReturn(new byte[]{1, 2, 3});

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ElevatorQrCodeController(qrCodeService, new ObjectMapper()))
                .build();

        mockMvc.perform(get("/elevator-qrcodes/11/print"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));

        verify(qrCodeService).generateQrImage(11L, 1L);
    }

    @Test
    void qrCodesRouteShouldKeepLegacyBehaviorWithoutOnlyWithQrFilter() throws Exception {
        ElevatorQrCodeService qrCodeService = mock(ElevatorQrCodeService.class);
        QrCodeResponseDTO dto = new QrCodeResponseDTO();
        dto.setId(5L);
        dto.setElevatorId(10L);
        dto.setHasQr(false);

        when(qrCodeService.list(any(Pageable.class), eq(""), eq(1L), eq(false)))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ElevatorQrCodeController(qrCodeService, new ObjectMapper()))
                .build();

        mockMvc.perform(get("/qr-codes")
                        .param("search", "")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(5));

        verify(qrCodeService).list(any(Pageable.class), eq(""), eq(1L), eq(false));
    }
}
