package com.jewel.reportmanager.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.Map;

@Getter
@Setter
public class ApisEnterPointDto {

    @NotBlank
    private String companyName;

    @NotEmpty
    private Map<String,Object> urls;

    private long created_on;

    private long updated_on;

    private String created_by;

    private String updated_by;

}
