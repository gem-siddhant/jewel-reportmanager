package com.jewel.reportmanager.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Getter
@Setter
@Document
public class Steps {

    @Field
    @Indexed(unique = true)
    private String tc_run_id;

    @Field
    private String s_run_id;

    @Field
    private List<Object> steps;

}
