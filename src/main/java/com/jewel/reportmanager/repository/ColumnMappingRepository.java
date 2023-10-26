package com.jewel.reportmanager.repository;


import com.jewel.reportmanager.entity.ColumnMapping;
import com.jewel.reportmanager.enums.ColumnLevel;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ColumnMappingRepository extends MongoRepository<ColumnMapping,Long> {

  ColumnMapping findByLevelAndPidAndNameIgnoreCaseAndIsDeleted(ColumnLevel columnLevel, Long pid, String name, boolean isDeleted);

  ColumnMapping findByLevelAndIsDeleted(ColumnLevel level, boolean b);

  ColumnMapping findByIdAndIsDeleted(Long id, boolean b);

  ColumnMapping findByLevelAndPidAndIsDeleted(ColumnLevel project, Long pid, boolean b);

  ColumnMapping findByLevelAndNameIgnoreCaseAndIsDeleted(ColumnLevel framework, String framework1, boolean b);

  ColumnMapping findByLevelAndNameContainingIgnoreCaseAndIsDeleted(ColumnLevel framework, String framework1, boolean b);
}
