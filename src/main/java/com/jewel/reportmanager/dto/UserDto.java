package com.jewel.reportmanager.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.util.List;

@Getter
@Setter
@ToString
public class UserDto {

    @NotBlank
    @Pattern(regexp = "^(?=.*[a-zA-Z])[a-zA-Z0-9_.-]*$", message = "Invalid value")
    @Schema(example = "test_User.12-3")
    private String username;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z]+$", message = "Invalid value")
    @Schema(example = "Test")
    private String firstName;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z]+$", message = "Invalid value")
    @Schema(example = "User1")
    private String lastName;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$", message = "Invalid value")
    @Schema(example = "test@example.com")
    private String email;

    @NotEmpty(message = "Invalid value")
    private String password;

    @NotEmpty(message = "Invalid value")
    private String company;

    private long insertTime;

    private long updateTime;

    private String status;

    private String companyType;

    private String realCompanyType;

    private List<String> modules;

    private long verifiedTime;

    private long selfVerifiedTime;

    private String userType;

    private String realCompany;

    private String domain;

    private String role;

    @JsonIgnore
    private Boolean isDeleted=false;

    private String updatedBy;
}