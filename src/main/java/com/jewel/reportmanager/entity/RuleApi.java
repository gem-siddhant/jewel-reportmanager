package com.jewel.reportmanager.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RuleApi {

    private long reportid;

    private List<String> project;

    private List<String> env;

    private String startTime;

    private String endTime;

    private List<Long> projectid;

}
