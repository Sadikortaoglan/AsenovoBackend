package com.saraasansor.api.util;

import com.saraasansor.api.model.LabelType;

import java.time.LocalDate;

/**
 * Utility class for calculating label durations and end dates
 */
public class LabelDurationCalculator {

    /**
     * Get label duration in months for a given label type
     * Business rule: BLUE = 12 months
     */
    public static int getLabelDurationMonths(LabelType labelType) {
        if (labelType == null) {
            return 12; // Default to BLUE duration
        }
        
        switch (labelType) {
            case BLUE:
                return 12; // 12 months
            case GREEN:
                return 24; // 24 months
            case YELLOW:
                return 6;  // 6 months
            case RED:
                return 1;  // 1 month
            case ORANGE:
                return 9;  // 9 months
            default:
                return 12; // Default to BLUE duration
        }
    }

    /**
     * Calculate end date from label date and label type
     * endDate = labelDate + labelDuration
     */
    public static LocalDate calculateEndDate(LocalDate labelDate, LabelType labelType) {
        if (labelDate == null) {
            throw new IllegalArgumentException("Label date cannot be null");
        }
        
        int months = getLabelDurationMonths(labelType);
        return labelDate.plusMonths(months);
    }

    /**
     * Calculate status based on end date
     * if (today > endDate) → EXPIRED
     * else → ACTIVE
     */
    public static com.saraasansor.api.model.Elevator.Status calculateStatus(LocalDate endDate) {
        if (endDate == null) {
            return com.saraasansor.api.model.Elevator.Status.ACTIVE;
        }
        
        LocalDate today = LocalDate.now();
        if (today.isAfter(endDate)) {
            return com.saraasansor.api.model.Elevator.Status.EXPIRED;
        } else {
            return com.saraasansor.api.model.Elevator.Status.ACTIVE;
        }
    }

    /**
     * Calculate status and end date from label date and type
     */
    public static StatusResult calculateStatusAndEndDate(LocalDate labelDate, LabelType labelType) {
        LocalDate endDate = calculateEndDate(labelDate, labelType);
        com.saraasansor.api.model.Elevator.Status status = calculateStatus(endDate);
        return new StatusResult(endDate, status);
    }

    public static class StatusResult {
        private final LocalDate endDate;
        private final com.saraasansor.api.model.Elevator.Status status;

        public StatusResult(LocalDate endDate, com.saraasansor.api.model.Elevator.Status status) {
            this.endDate = endDate;
            this.status = status;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public com.saraasansor.api.model.Elevator.Status getStatus() {
            return status;
        }
    }
}
