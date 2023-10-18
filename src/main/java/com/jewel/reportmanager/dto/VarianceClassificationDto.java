package com.jewel.reportmanager.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class VarianceClassificationDto {

    @Transient
    public static final String SEQUENCE_NAME = "variance_id";

    @Indexed(unique = true)
    private Long varianceId;

    @NotNull
    private String reason;

    @NotNull
    private Long startTime;

    @NotNull
    private Long endDate;

    @NotNull
    private String tc_run_id;

    private String name;

    @NotNull
    private String category;

    private Long pid;

    private String reportName;

    private String env;

    private String markedBy;

    @NotNull
    private String match;

    private String varianceStatus;

    private String updatedBy;

}
