package com.jewel.reportmanager.dto;

import com.jewel.reportmanager.enums.ColumnLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class ColumnMappingDto {

    private Long id;
    private Long pid;

    @NotNull
    @Field(targetType = FieldType.STRING)
    private ColumnLevel level;

    @NotNull
    @NotEmpty
    private String name;

    @NotNull
    private List<String> columns=new ArrayList<>();

}
