package com.jewel.reportmanager.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.List;

@Getter
@Setter
public class StepsDto {

    @Indexed(unique = true)
    private String tc_run_id;

    private String s_run_id;

    private List<Object> steps;

}
