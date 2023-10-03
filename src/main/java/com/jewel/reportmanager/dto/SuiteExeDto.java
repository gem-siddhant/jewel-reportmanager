package com.jewel.reportmanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;

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

    public String getFramework_version() {
        return framework_version;
    }

    public void setFramework_version(String framework_version) {
        this.framework_version = framework_version;
    }

    public Long getExpected_testcases() {
        return expected_testcases;
    }

    public void setExpected_testcases(Long expected_testcases) {
        this.expected_testcases = expected_testcases;
    }

    public String getReport_name() {
        return report_name;
    }

    public void setReport_name(String report_name) {
        this.report_name = report_name;
    }

    public long getP_id() {
        return p_id;
    }

    public void setP_id(long p_id) {
        this.p_id = p_id;
    }

    public String getFramework_name() {
        return framework_name;
    }

    public void setFramework_name(String framework_name) {
        this.framework_name = framework_name;
    }

    public Object getTestcase_info() {
        return testcase_info;
    }

    public void setTestcase_info(Object testcase_info) {
        this.testcase_info = testcase_info;
    }

    public String getS_run_id() {
        return s_run_id;
    }

    public void setS_run_id(String s_run_id) {
        this.s_run_id = s_run_id;
    }

    public String getS_id() {
        return s_id;
    }

    public void setS_id(String s_id) {
        this.s_id = s_id;
    }

    public long getS_start_time() {
        return s_start_time;
    }

    public void setS_start_time(long s_start_time) {
        this.s_start_time = s_start_time;
    }

    public long getS_end_time() {
        return s_end_time;
    }

    public void setS_end_time(long s_end_time) {
        this.s_end_time = s_end_time;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProject_name() {
        return project_name;
    }

    public void setProject_name(String project_name) {
        this.project_name = project_name;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getMachine() {
        return machine;
    }

    public void setMachine(String machine) {
        this.machine = machine;
    }

    public List<Map<String, Object>> getMeta_data() {
        return meta_data;
    }

    public void setMeta_data(List<Map<String, Object>> meta_data) {
        this.meta_data = meta_data;
    }

    public List<String> getTestcase_details() {
        return testcase_details;
    }

    public void setTestcase_details(List<String> testcase_details) {
        this.testcase_details = testcase_details;
    }

}

