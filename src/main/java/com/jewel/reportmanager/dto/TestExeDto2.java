package com.jewel.reportmanager.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class TestExeDto2 {

    private String tc_run_id;

    private long start_time;

    private long end_time;

    @NotNull
    @NotBlank
    private String name;

    private Object category;

    private String log_file;

    @NotNull
    @Pattern(regexp = "^[A-Za-z]+$", message = "Invalid value")
    @Schema(example = "ACTIVE")
    private String status;

    @NotNull
    @NotBlank
    private String machine;
    private String result_file;

    @Pattern(regexp = "^(?=.*[a-zA-Z])[a-zA-Z0-9_. -]*$", message = "Invalid value")
    @Schema(example = "Gemjar")
    private String product_type;

    private boolean ignore;

    @NotEmpty
    private List<Object> steps;

    private List<Map<String, Object>> meta_data;

    private Map<String, Object> user_defined_data;

    @NotNull
    @NotBlank
    private String s_run_id;

    @NotNull
    @Pattern(regexp = "^(?=.*[a-zA-Z])[a-zA-Z0-9_. -]*$", message = "Invalid value")
    @Schema(example = "On Demand")
    private String run_type;

    @NotNull
    @Pattern(regexp = "^(?=.*[a-zA-Z])[a-zA-Z0-9_. -]*$", message = "Invalid value")
    @Schema(example = "Linux")
    private String run_mode;

    @Pattern(regexp = "^(?=.*[a-zA-Z])[a-zA-Z0-9_. -]*$", message = "Invalid value")
    @Schema(example = "Test.user_12")
    private String base_user;

    @Pattern(regexp = "^(?=.*[a-zA-Z])[a-zA-Z0-9_. -]*$",message = "Invalid value")
    @Schema(example = "Test.user_12")
    private String invoke_user;

    private Set<String> token_user;

    private String job_name;

    public Map<String, Object> getUser_defined_data() {
        return user_defined_data;
    }

    @JsonIgnore
    private ClassificationDetails classificationDetails;

    @JsonIgnore
    private Long varianceId;

    @JsonIgnore
    private List<Long> stepVarianceIds;

    private Long testcase_id;

    public TestExeDto2(TestExeDto testExeDto) {
        this.tc_run_id = testExeDto.getTc_run_id();
        this.start_time = testExeDto.getStart_time();
        this.end_time = testExeDto.getEnd_time();
        this.name = testExeDto.getName();
        this.category = testExeDto.getCategory();
        this.log_file = testExeDto.getLog_file();
        this.status = testExeDto.getStatus();
        this.machine = testExeDto.getMachine();
        this.result_file = testExeDto.getResult_file();
        this.product_type = testExeDto.getProduct_type();
        this.ignore = testExeDto.isIgnore();
        this.steps = testExeDto.getSteps();
        this.meta_data = testExeDto.getMeta_data();
        this.user_defined_data = testExeDto.getUser_defined_data();
        this.s_run_id = testExeDto.getS_run_id();
        this.run_type = testExeDto.getRun_type();
        this.run_mode = testExeDto.getRun_mode();
        this.base_user = testExeDto.getBase_user();
        this.invoke_user = testExeDto.getInvoke_user();
        this.token_user = testExeDto.getToken_user();
        this.job_name = testExeDto.getJob_name();
        this.classificationDetails = testExeDto.getClassificationDetails();
        this.varianceId = testExeDto.getVarianceId();
        this.stepVarianceIds = testExeDto.getStepVarianceIds();
        this.testcase_id = testExeDto.getTestcase_id();
    }

}
