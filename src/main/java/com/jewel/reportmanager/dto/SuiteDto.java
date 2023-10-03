package com.jewel.reportmanager.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

public class SuiteDto {

    private Long s_id;

    @NotNull
    private Long p_id;


    @NotBlank
    private String project_name;


    @NotBlank
    private String report_name;

    private long created_on;

    private long updated_on;

    private String created_by;

    private String updated_by;

    private String status;


    private long testcaseCount;


    private Map<String, Object> configuration;



    private String source = "JEWEL";


    private String accessToken;


    public SuiteDto() {
    }

    public SuiteDto(Long s_id, Long p_id, String project_name, String report_name, long created_on, long updated_on, String created_by, String updated_by, String status, long testcaseCount, Map<String, Object> configuration, String framework, String source, String accessToken) {
        this.s_id = s_id;
        this.p_id = p_id;
        this.project_name = project_name;
        this.report_name = report_name;
        this.created_on = created_on;
        this.updated_on = updated_on;
        this.created_by = created_by;
        this.updated_by = updated_by;
        this.status = status;
        this.testcaseCount = testcaseCount;
        this.configuration = configuration;
        this.source = source;
        this.accessToken = accessToken;
    }

    public Long getS_id() {
        return s_id;
    }

    public void setS_id(Long s_id) {
        this.s_id = s_id;
    }

    public String getProject_name() {
        return project_name;
    }

    public void setProject_name(String project_name) {
        this.project_name = project_name;
    }

    public Long getP_id() {
        return p_id;
    }

    public void setP_id(Long p_id) {
        this.p_id = p_id;
    }

    public String getReport_name() {
        return report_name;
    }

    public void setReport_name(String report_name) {
        this.report_name = report_name;
    }

    public long getCreated_on() {
        return created_on;
    }

    public void setCreated_on(long created_on) {
        this.created_on = created_on;
    }

    public long getUpdated_on() {
        return updated_on;
    }

    public void setUpdated_on(long updated_on) {
        this.updated_on = updated_on;
    }

    public String getCreated_by() {
        return created_by;
    }

    public void setCreated_by(String created_by) {
        this.created_by = created_by;
    }

    public String getUpdated_by() {
        return updated_by;
    }

    public void setUpdated_by(String updated_by) {
        this.updated_by = updated_by;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTestcaseCount() {
        return testcaseCount;
    }

    public void setTestcaseCount(long testcaseCount) {
        this.testcaseCount = testcaseCount;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
