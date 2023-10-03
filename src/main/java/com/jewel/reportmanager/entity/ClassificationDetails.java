package com.jewel.reportmanager.entity;

import com.jewel.reportmanager.enums.ClassificationType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Getter
@Setter
public class ClassificationDetails {

    @Field(targetType = FieldType.STRING)
    private ClassificationType classification;

    private String reason ;

    private String oldStatus;

    private boolean falsePositiveStatus;

    private long created_on;

    private long updated_on;

    private String created_by;

    private String updated_by;

    private boolean childFalsePostiveStatus;

}
