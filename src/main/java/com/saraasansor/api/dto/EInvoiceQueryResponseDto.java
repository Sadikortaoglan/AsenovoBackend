package com.saraasansor.api.dto;

public class EInvoiceQueryResponseDto {

    private boolean eInvoiceUser;
    private String message;

    public EInvoiceQueryResponseDto() {
    }

    public EInvoiceQueryResponseDto(boolean eInvoiceUser, String message) {
        this.eInvoiceUser = eInvoiceUser;
        this.message = message;
    }

    public boolean isEInvoiceUser() {
        return eInvoiceUser;
    }

    public void setEInvoiceUser(boolean eInvoiceUser) {
        this.eInvoiceUser = eInvoiceUser;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
