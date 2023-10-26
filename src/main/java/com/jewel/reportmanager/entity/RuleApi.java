package com.jewel.reportmanager.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import java.util.List;

import static com.jewel.reportmanager.utils.ReportResponseConstants.NOT_ACCESS_TO_PROJECT;

@Getter
@Setter
@ToString
public class RuleApi {

    private long reportid;

    private List<String> project;

    private List<String> env;

    private String startTime;

    private String endTime;

    private List<Long> projectid;

}
