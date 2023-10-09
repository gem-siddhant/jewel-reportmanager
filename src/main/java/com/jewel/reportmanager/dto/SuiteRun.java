package com.jewel.reportmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SuiteRun {

    @Indexed(unique = true)
    private String s_run_id;

    private List<SuiteRunValues> values;
}
