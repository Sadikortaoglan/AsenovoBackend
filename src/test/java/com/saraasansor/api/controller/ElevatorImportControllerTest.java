package com.saraasansor.api.controller;

import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.ElevatorImportResultItemResponse;
import com.saraasansor.api.dto.ElevatorImportResultResponse;
import com.saraasansor.api.service.ElevatorService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ElevatorImportControllerTest {

    @Test
    void templateDownloadEndpointShouldReturnXlsxFile() throws Exception {
        ElevatorService elevatorService = mock(ElevatorService.class);
        when(elevatorService.generateImportTemplateExcel()).thenReturn(new byte[]{1, 2, 3});

        ElevatorController controller = new ElevatorController();
        injectService(controller, elevatorService);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/elevators/import-template"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"elevator-import-template.xlsx\""))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        mockMvc.perform(get("/elevators/sample-excel"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"elevator-import-template.xlsx\""))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void importEndpointShouldReturnRowBasedResult() throws Exception {
        ElevatorService elevatorService = mock(ElevatorService.class);

        ElevatorImportResultResponse result = new ElevatorImportResultResponse();
        result.setTotalRows(1);
        result.setSuccessCount(1);
        result.setFailureCount(0);
        result.addItem(new ElevatorImportResultItemResponse(
                2,
                ElevatorImportResultItemResponse.Status.SUCCESS.name(),
                "Elevator imported successfully",
                "Asansör A",
                "Facility A"
        ));
        when(elevatorService.importFromExcel(any())).thenReturn(result);

        ElevatorController controller = new ElevatorController();
        injectService(controller, elevatorService);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "elevators.xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").toString(),
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/elevators/import-excel").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRows").value(1))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(0))
                .andExpect(jsonPath("$.data.items[0].status").value("SUCCESS"));

        mockMvc.perform(multipart("/elevators/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRows").value(1))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(0))
                .andExpect(jsonPath("$.data.items[0].status").value("SUCCESS"));
    }

    private void injectService(ElevatorController controller, ElevatorService elevatorService) throws Exception {
        java.lang.reflect.Field field = ElevatorController.class.getDeclaredField("elevatorService");
        field.setAccessible(true);
        field.set(controller, elevatorService);
    }
}
