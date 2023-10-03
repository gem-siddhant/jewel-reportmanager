package com.jewel.reportmanager.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Set;

@Getter
@Setter
public class ProjectDto {

    private long pid;

    @NotNull
    @Pattern(regexp = "^(?=.*[a-zA-Z])[a-zA-Z0-9_. -]*$", message = "Invalid value")
    @Schema(example = "Sample Project_1")
    private String projectName;

    private String realcompanyname;

    @NotNull
    private String description;

    private Set<String> managers;

    private Set<String> qaLead;

    private Set<String> devTeam;

    private Set<String> qaTeam;

    @NotEmpty
    @Schema(example = "[\"beta\", \"prod\"]")
    private Set<String> env;

    @JsonIgnore
    private String createdBy;

    @JsonIgnore
    private long createdAt;

    @JsonIgnore
    private String updatedBy;

    @JsonIgnore
    private long updatedAt;

    @JsonIgnore
    @Schema(example = "ACTIVE")
    private String status;

}