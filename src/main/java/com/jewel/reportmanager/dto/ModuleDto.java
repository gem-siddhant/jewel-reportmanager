package com.jewel.reportmanager.dto;


import com.jewel.reportmanager.enums.ModuleType;
import lombok.*;

import javax.validation.constraints.NotBlank;

import static com.jewel.reportmanager.utils.ReportResponseConstants.MODULE_NOT_VALID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ModuleDto {

    private Long id;

    @NotBlank(message = MODULE_NOT_VALID)
    private String moduleName;

    private String route;

    private String icon;

    private ModuleType type;

    private Long insertTime;

    private Long updatedAt;

}
