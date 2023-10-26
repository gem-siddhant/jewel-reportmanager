package com.jewel.reportmanager.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jewel.reportmanager.enums.ColumnLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Document(collection = "column_mapping")
@JsonInclude(value=JsonInclude.Include.NON_EMPTY,content = JsonInclude.Include.NON_EMPTY)
public class ColumnMapping {

    @Transient
    public static final String SEQUENCE_NAME = "col_id";

    @Id
    private Long id;

    private Long pid;

    @Field(targetType = FieldType.STRING)
    private ColumnLevel level;

    private String name;

    @NotNull
    private List<String> columns=new ArrayList<>();

    @JsonIgnore
    private String addedBy;

    @JsonIgnore
    private Long addedAt;

    @JsonIgnore
    private String updatedBy;

    @JsonIgnore
    private Long updatedAt;

    private boolean isDeleted;

}
