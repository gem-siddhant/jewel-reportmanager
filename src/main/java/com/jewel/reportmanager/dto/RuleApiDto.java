package com.jewel.reportmanager.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.util.List;

import static com.jewel.reportmanager.utils.ReportResponseConstants.NOT_ACCESS_TO_PROJECT;

@Getter
@Setter
public class RuleApiDto {

    private long reportid;

    private List<String> project;

    private List<String> env;

    private String startTime;

    private String endTime;

    @NotEmpty(message = NOT_ACCESS_TO_PROJECT)
    private List<Long> projectid;

}
