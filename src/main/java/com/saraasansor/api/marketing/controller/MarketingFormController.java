package com.saraasansor.api.marketing.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.marketing.dto.ContactRequestDto;
import com.saraasansor.api.marketing.dto.DemoRequestDto;
import com.saraasansor.api.marketing.dto.PlanRequestDto;
import com.saraasansor.api.marketing.dto.TrialProvisionResponseDto;
import com.saraasansor.api.marketing.dto.TrialRequestDto;
import com.saraasansor.api.marketing.dto.TrialSubmissionResultDto;
import com.saraasansor.api.marketing.service.MarketingLeadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class MarketingFormController {

    private final MarketingLeadService marketingLeadService;

    public MarketingFormController(MarketingLeadService marketingLeadService) {
        this.marketingLeadService = marketingLeadService;
    }

    @PostMapping("/demo-request")
    public ResponseEntity<ApiResponse<Object>> submitDemoRequest(@Valid @RequestBody DemoRequestDto request) {
        String message = marketingLeadService.submitDemoRequest(request);
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    @PostMapping("/trial-request")
    public ResponseEntity<ApiResponse<TrialProvisionResponseDto>> submitTrialRequest(@Valid @RequestBody TrialRequestDto request) {
        try {
            TrialSubmissionResultDto result = marketingLeadService.submitTrialRequest(request);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success(result.getMessage(), result.getResponse()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(ex.getMessage()));
        }
    }

    @GetMapping("/trial-request/{requestToken}")
    public ResponseEntity<ApiResponse<TrialProvisionResponseDto>> getTrialRequestStatus(@PathVariable String requestToken) {
        try {
            TrialProvisionResponseDto response = marketingLeadService.getTrialRequestStatus(requestToken);
            return ResponseEntity.ok(ApiResponse.success("Trial durumu getirildi.", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping("/plan-request")
    public ResponseEntity<ApiResponse<Object>> submitPlanRequest(@Valid @RequestBody PlanRequestDto request) {
        String message = marketingLeadService.submitPlanRequest(request);
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    @PostMapping("/contact")
    public ResponseEntity<ApiResponse<Object>> submitContactRequest(@Valid @RequestBody ContactRequestDto request) {
        String message = marketingLeadService.submitContactRequest(request);
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }
}
