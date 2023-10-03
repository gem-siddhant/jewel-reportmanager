package com.jewel.reportmanager.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectRoleDto {

    private long pid;

    private String username;

    private String role;

    private String status;

    private String createdBy;

    private long createdAt;

    private String updatedBy;

    private long updatedAt;

}
