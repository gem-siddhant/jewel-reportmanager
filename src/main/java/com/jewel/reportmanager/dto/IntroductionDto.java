package com.jewel.reportmanager.dto;


import lombok.*;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class IntroductionDto {

    @NotBlank
    private String gemName;

    private Object data;

}
