package com.jewel.reportmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SuiteRunValues {

    private String framework;

    private String status;

    private long expected_count;

    private List<List<DependencyTree>> expected_testcases;
}
