package com.jewel.reportmanager.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

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
