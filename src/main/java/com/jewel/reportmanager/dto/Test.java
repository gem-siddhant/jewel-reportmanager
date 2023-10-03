package com.jewel.reportmanager.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jewel.reportmanager.enums.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import javax.validation.constraints.NotBlank;
import java.util.*;

@Getter
@Setter
@Document
@JsonInclude(value=JsonInclude.Include.NON_EMPTY,content = JsonInclude.Include.NON_EMPTY)
public class Test {

    @Transient
    public static final String SEQUENCE_NAME = "tc_id";

    @Transient
    public static final String UNIQUE_ID="tc_unique_id";

    @Indexed(unique = true)
    @Field
    private Long tc_id;

    private Long isolatedVersionId;

    private Set<Long> s_id=new HashSet<>();
    @NotBlank
    private String name;


    @Field
    private String category;

    @Field
    private String type;

    @Field
    private Map<String, Object> frameworkDefinedData;

    @Field
    private List<String> scenario_steps;

    @Field
    private List<String> examples;


    @Field
    private String run_flag;


    @Field
    private List<String> tags;


    @JsonIgnore
    private long created_on;

    @JsonIgnore
    private long updated_on;


    private String created_by;

    @JsonIgnore
    private String updated_by;

    @Field
    private String source = "JEWEL";

    @Field
    private String framework;

    private List<String> description=new ArrayList<>();

    @Field(targetType = FieldType.STRING)
    private PriorityType priority=PriorityType.LOW;

    @Field(targetType = FieldType.STRING)
    private SeverityType severity=SeverityType.LOW;

    private Map<String,Object> variables;


    private Long folderId;

    @JsonIgnore
    private Set<Long> requirementId=new HashSet<>();

    @JsonIgnore
    private Set<Long> manualTestCaseId=new HashSet<>();

    private String approvedBy;

    private Long pid;

    private Long version;

    private String assignedTo;

    @Field(targetType = FieldType.STRING)
    private ManualTestCaseStatusType status=ManualTestCaseStatusType.NEW;

    @Field(targetType = FieldType.STRING)
    private ManualTestCaseType manualTestcaseType=ManualTestCaseType.FUNCTIONAL;


    @Field(targetType = FieldType.STRING)
    private TestCaseType testcaseType=TestCaseType.AUTOMATION;

    @Field
    private List<TestSteps> testSteps=new ArrayList<>();

    @JsonIgnore
    private boolean isDeleted=false;

    @Transient
    private String projectName;


}
