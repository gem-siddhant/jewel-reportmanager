package com.jewel.reportmanager.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Document
public class DatabaseSequence {

   @Id
   private String id;

   @Field
   private long seq;

}
