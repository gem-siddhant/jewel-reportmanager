package com.jewel.reportmanager.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Getter
@Setter
public class ReportNamesDto {

    @NotNull
    @Pattern(regexp = "^(?=.*[a-zA-Z])[a-zA-Z0-9_. -]*$", message = "Invalid value")
    @Schema(example = "Test Report-1")
    private String reportName;

    @NotNull
    @Pattern(regexp = "^[A-Za-z]+$", message = "Invalid value")
    @Schema(example = "PRE")
    private String reportType;

    @NotNull
    @Pattern(regexp = "^[A-Za-z]+$", message = "Invalid value")
    @Schema(example = "ACTIVE")
    private String status;

}
