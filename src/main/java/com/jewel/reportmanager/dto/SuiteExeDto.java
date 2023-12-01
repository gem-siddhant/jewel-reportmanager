package com.jewel.reportmanager.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@ToString
public class SuiteExeDto {

    private String s_run_id;

    private String s_id;

    private long p_id;

    private long s_start_time;

    private long s_end_time;

    @NotNull
    @Pattern(regexp = "^[A-Za-z]+$", message = "Invalid value")
    @Schema(example = "ACTIVE")
    private String status;
    @NotNull
    @Pattern(regexp = "^(?=.*[a-zA-Z])[a-zA-Z0-9_. -]*$", message = "Invalid value")
    @Schema(example = "Sample Project_1")
    private String project_name;

    @NotNull
    @Pattern(regexp = "^(?=.*[a-zA-Z])[a-zA-Z0-9_. -]*$", message = "Invalid value")
    @Schema(example = "Windows 11")
    private String os;

    @NotNull
    @Pattern(regexp = "^(?=.*[a-zA-Z])[a-zA-Z0-9_. -]*$", message = "Invalid value")
    @Schema(example = "test_User.123")
    private String user;

    @NotNull
    @Pattern(regexp = "^[A-Za-z]+$", message = "Invalid value")
    @Schema(example = "beta")
    private String env;

    @NotNull
    @NotBlank
    private String machine;

    private List<Map<String, Object>> meta_data;

    private List<String> testcase_details;

    private Object testcase_info;

    @NotNull
    @Pattern(regexp = "^[A-Za-z]+$", message = "Invalid value")
    @Schema(example = "Gemjar")
    private String framework_name;

    private String framework_version;

    @NotNull
    @Pattern(regexp = "^(?=.*[a-zA-Z])[a-zA-Z0-9_. -]*$", message = "Invalid value")
    @Schema(example = "Test Report_1")
    private String report_name;
    private Long expected_testcases;
    private String mode;
    private Set<Long> varianceIds;
    private ClassificationDetails classificationDetails;
    private SuiteExeMail suiteExeMail;
    private String build_id;
    private String sprint_name;

}

