package com.jewel.reportmanager.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StepsDto {

    private String tc_run_id;

    private String s_run_id;

    private List<Object> steps;

}
