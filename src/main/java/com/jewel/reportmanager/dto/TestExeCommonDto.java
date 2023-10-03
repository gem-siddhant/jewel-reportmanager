package com.jewel.reportmanager.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class TestExeCommonDto {

    private String env;

    private String project_name;

    private String report_name;

    private String tc_run_id;

    private long start_time;

    private long end_time;

    private String name;

    private String category;

    private String log_file;

    private String status;

    private String user;

    private String machine;

    private String result_file;

    private String product_type;

    private boolean ignore;

    private List<Object> steps;

    private List<Map<String, Object>> miscData;

    private Map<String, Object> userDefinedData;

    private String s_run_id;

    private long p_id;

}