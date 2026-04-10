package com.saraasansor.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

public class ElevatorContractCreateRequest {

    @NotNull
    @JsonAlias({"elevator_id", "selectedElevatorId", "selected_elevator_id"})
    private Long elevatorId;

    @JsonAlias({"date", "startAt", "startDate", "contract_date"})
    private String contractDate;

    @JsonAlias({"contractContent", "content", "html", "contract_html"})
    private String contractHtml;

    @JsonAlias({"contractStatus"})
    private String status;

    public Long getElevatorId() {
        return elevatorId;
    }

    public void setElevatorId(Long elevatorId) {
        this.elevatorId = elevatorId;
    }

    public String getContractDate() {
        return contractDate;
    }

    public void setContractDate(String contractDate) {
        this.contractDate = contractDate;
    }

    public String getContractHtml() {
        return contractHtml;
    }

    public void setContractHtml(String contractHtml) {
        this.contractHtml = contractHtml;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
