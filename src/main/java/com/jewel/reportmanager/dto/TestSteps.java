package com.jewel.reportmanager.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class TestSteps {

    private Integer id;

    @NotNull
    @NotBlank
    private String name;

    @NotNull
    @NotBlank
    private String description;

    @NotNull
    @NotBlank
    private String expectedResult;

    private Boolean  screenshot;

    @JsonIgnore
    private Long tcIsolatedVersionId;

    @JsonIgnore
    private String linkTestCaseNameWithId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public Boolean getScreenshot() {
        return screenshot;
    }

    public void setScreenshot(Boolean screenshot) {
        this.screenshot = screenshot;
    }

    public Long getTcIsolatedVersionId() {
        return tcIsolatedVersionId;
    }

    public void setTcIsolatedVersionId(Long tcIsolatedVersionId) {
        this.tcIsolatedVersionId = tcIsolatedVersionId;
    }

    public String getLinkTestCaseNameWithId() {
        return linkTestCaseNameWithId;
    }

    public void setLinkTestCaseNameWithId(String linkTestCaseNameWithId) {
        this.linkTestCaseNameWithId = linkTestCaseNameWithId;
    }
}