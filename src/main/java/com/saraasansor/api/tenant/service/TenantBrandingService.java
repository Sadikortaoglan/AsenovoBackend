package com.saraasansor.api.tenant.service;

import com.saraasansor.api.tenant.dto.TenantBrandingResponseDTO;
import com.saraasansor.api.tenant.dto.TenantBrandingUpdateRequestDTO;
import org.springframework.web.multipart.MultipartFile;

public interface TenantBrandingService {

    TenantBrandingResponseDTO getCurrentTenantBranding();

    TenantBrandingResponseDTO updateBranding(TenantBrandingUpdateRequestDTO requestDTO);

    TenantBrandingResponseDTO updateLogo(MultipartFile file);
}
