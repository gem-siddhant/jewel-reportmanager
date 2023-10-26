package com.jewel.reportmanager.service;

import com.jewel.reportmanager.dto.ProjectDto;
import com.jewel.reportmanager.dto.Response;
import com.jewel.reportmanager.dto.UserDto;
import com.jewel.reportmanager.entity.ColumnMapping;
import com.jewel.reportmanager.enums.ColumnLevel;
import com.jewel.reportmanager.enums.UserRole;
import com.jewel.reportmanager.exception.CustomDataException;
import com.jewel.reportmanager.repository.ColumnMappingRepository;
import com.jewel.reportmanager.utils.ColumnsUtils;
import com.jewel.reportmanager.utils.ReportUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import static com.jewel.reportmanager.enums.OperationType.*;
import static com.jewel.reportmanager.utils.ColumnsResponseConstants.*;
import static com.jewel.reportmanager.utils.ReportConstants.REQUEST_ACCESS;

@Slf4j
@Service
public class ColumnMappingService {

    @Autowired
    private ColumnMappingRepository columnMappingRepository;
    @Autowired
    private SequenceGenerator sequenceGenerator;


    /**
     * Add a new column mapping.
     *
     * @param columnMapping The column mapping to be added.
     * @param request The HTTP servlet request.
     * @return A map containing the result of the operation.
     * @throws CustomDataException if the operation fails.
     */
    public Map<String, Object> addColumnMapping(ColumnMapping columnMapping, HttpServletRequest request) {
        String username = ColumnsUtils.getUserNameFromToken(request);
        UserDto user = ReportUtils.getUserDtoFromServetRequest();

        // Check for the presence of a project ID
        checkForMissingProjectId(columnMapping);

        // Check access and permissions
        checkAccessAndPermissions(columnMapping, user);

        // Check for an existing column mapping
        ColumnMapping existingMapping = isFrameworkLevel(columnMapping)
                ? this.columnMappingRepository.findByLevelAndNameIgnoreCaseAndIsDeleted(columnMapping.getLevel(), columnMapping.getName(), false)
                : this.columnMappingRepository.findByLevelAndPidAndNameIgnoreCaseAndIsDeleted(columnMapping.getLevel(), columnMapping.getPid(), columnMapping.getName(), false);

        if (existingMapping != null) {
            log.error("Column Mapping already present. User: {}, Column Mapping: {}", username, columnMapping);
            throw new CustomDataException(COLUMN_MAPPING_PRESENT, null, FAILURE, HttpStatus.CONFLICT);
        }

        // Set common column properties
        columnMapping.setId(this.sequenceGenerator.generateSequence(ColumnMapping.SEQUENCE_NAME));
        columnMapping.setAddedBy(username);
        columnMapping.setAddedAt(new Date().getTime());
        columnMapping.setDeleted(false);

        Map<String, Object> result = processColumnMapping(columnMapping);

        log.info("Column Mapping added successfully. User: {}, Column Mapping: {}", username, columnMapping);
        return result;
    }

    /**
     * Update an existing column mapping.
     *
     * @param columnMapping The updated column mapping.
     * @param request The HTTP servlet request.
     * @return A map containing the result of the update operation.
     * @throws CustomDataException if the operation fails.
     */
    public Map<String, Object> updateColumnMapping(ColumnMapping columnMapping, HttpServletRequest request) {
        String username = ColumnsUtils.getUserNameFromToken(request);
        UserDto user = ReportUtils.getUserDtoFromServetRequest();

        // Check for the presence of a project ID
        checkForMissingProjectId(columnMapping);

        // Check if the column mapping with the given ID exists
        ColumnMapping existingMapping = this.columnMappingRepository.findByIdAndIsDeleted(columnMapping.getId(), false);

        if (existingMapping == null) {
            log.error("Column Mapping details not found. User: {}, Column Mapping ID: {}", username, columnMapping.getId());
            throw new CustomDataException(COLUMN_MAPPING_DETAILS_NOT_FOUND, null, FAILURE, HttpStatus.NOT_FOUND);
        }

        if (!columnMapping.getLevel().equals(existingMapping.getLevel())) {
            log.error("Level is not the same as the previous level. User: {}, Column Mapping ID: {}", username, columnMapping.getId());
            throw new CustomDataException(LEVEL_NOT_SAME_AS_PREVIOUS_LEVEL, null, FAILURE, HttpStatus.NOT_FOUND);
        }

        // Check access and permissions
        checkAccessAndPermissions(columnMapping, user);

        // Update common properties and process the column mapping
        columnMapping.setUpdatedBy(username);
        columnMapping.setUpdatedAt(new Date().getTime());

        Map<String, Object> result =  processColumnMapping(columnMapping);

        log.info("Column Mapping updated successfully. User: {}, Column Mapping ID: {}", username, columnMapping.getId());

        return result;
    }


    /**
     * Delete a column mapping.
     *
     * @param id The ID of the column mapping to be deleted.
     * @param request The HTTP servlet request.
     * @return A response indicating the result of the delete operation.
     * @throws CustomDataException if the operation fails.
     */
    public Response deleteColumnMapping(Long id, HttpServletRequest request) {
        String username = ColumnsUtils.getUserNameFromToken(request);
        UserDto user = ReportUtils.getUserDtoFromServetRequest();

        ColumnMapping columnMapping = this.columnMappingRepository.findByIdAndIsDeleted(id, false);
        if (columnMapping == null) {
            log.error("Column Mapping details not found. User: {}, Column Mapping ID: {}", username, id);
            throw new CustomDataException(COLUMN_MAPPING_DETAILS_NOT_FOUND, null, FAILURE, HttpStatus.CONFLICT);
        }
        // Check access and permissions
        checkAccessAndPermissions(columnMapping, user);

        // Set common properties and process the column mapping
        columnMapping.setDeleted(true);
        columnMapping.setUpdatedAt(new Date().getTime());
        columnMapping.setUpdatedBy(username);
        this.columnMappingRepository.save(columnMapping);

        log.info("Column Mapping deleted successfully. User: {}, Column Mapping ID: {}", username, id);

        return new Response(null, COLUMN_MAPPING_DELETE_SUCCESSFULLY, SUCCESS);
    }


    /**
     * Find column mappings based on the specified parameters.
     *
     * @param pid The project ID.
     * @param name The name of the column mapping.
     * @param frameworks The list of frameworks to search for.
     * @return A list of column mappings that match the criteria.
     */
    public List<String> findColumnMapping(Long pid, String name, List<String> frameworks) {
        ColumnMapping columnMapping = null;
        List<ColumnLevel> levelsToCheck = Arrays.asList(ColumnLevel.JOB_NAME, ColumnLevel.PROJECT_REPORT);
        for (ColumnLevel level : levelsToCheck) {
            columnMapping = this.columnMappingRepository.findByLevelAndPidAndNameIgnoreCaseAndIsDeleted(level, pid, name, false);
            if (columnMapping != null && !columnMapping.getColumns().isEmpty()) {
                log.info("Found matching column mapping. PID: {}, Name: {}, Level: {}", pid, name, level);
                return columnMapping.getColumns();
            }
        }

        columnMapping = this.columnMappingRepository.findByLevelAndPidAndIsDeleted(ColumnLevel.PROJECT, pid, false);
        if (columnMapping != null) {
            log.info("Found matching column mapping at the project level. PID: {}", pid);
            return columnMapping.getColumns().size() > 0 ? columnMapping.getColumns() : null;
        }

        List<String> columns = new ArrayList<>();
        for (String framework : frameworks) {
            String frameworkName = framework.toUpperCase().contains(GEM_PYP) ? GEM_PYP : framework;
            ColumnMapping columnMapping1 = frameworkName.equals(GEM_PYP)
                    ? this.columnMappingRepository.findByLevelAndNameContainingIgnoreCaseAndIsDeleted(ColumnLevel.FRAMEWORK, GEM_PYP, false)
                    : this.columnMappingRepository.findByLevelAndNameContainingIgnoreCaseAndIsDeleted(ColumnLevel.FRAMEWORK, framework, false);
            if (columnMapping1 != null && columnMapping1.getColumns().size() > 0) {
                log.info("Found matching column mapping for framework: {}. Columns: {}", frameworkName, columnMapping1.getColumns());
                columns.addAll(columnMapping1.getColumns());
            }
        }
        log.info("Returning the list of matching columns: {}", columns);
        return columns;
    }


    /**
     * Process a column mapping by ensuring valid values.
     *
     * @param columnMapping The column mapping to process.
     * @return A map containing the ID of the processed column mapping.
     * @throws CustomDataException if the operation fails.
     */
    public Map<String, Object> processColumnMapping(ColumnMapping columnMapping) {
        int i = 0;
        for (String value : columnMapping.getColumns()) {
            value = value.toUpperCase();
            if (value == null || value.isEmpty()) {
                log.error("Invalid column value in the column mapping. Value: {}", value);
                throw new CustomDataException(COLUMN_MAPPING_VALUE_NOT_CORRECT, null, FAILURE, HttpStatus.BAD_REQUEST);
            }
            columnMapping.getColumns().set(i, value.toUpperCase());
            i++;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("ID", columnMapping.getId());
        this.columnMappingRepository.save(columnMapping);
        log.info("Column mapping processed. ID: {}", columnMapping.getId());
        return map;
    }


    /**
     * Check if the column mapping is at the framework level.
     *
     * @param columnMapping The column mapping to check.
     * @return True if it's at the framework level, false otherwise.
     */
    private boolean isFrameworkLevel(ColumnMapping columnMapping) {
        return columnMapping.getLevel().toString().equalsIgnoreCase(ColumnLevel.FRAMEWORK.toString());
    }


    /**
     * Check for the presence of a project ID in the column mapping.
     *
     * @param columnMapping The column mapping to check.
     * @throws CustomDataException if the project ID is missing.
     */
    private void checkForMissingProjectId(ColumnMapping columnMapping) {
        if (!isFrameworkLevel(columnMapping) && columnMapping.getPid() == null) {
            log.error("Project ID is missing in the column mapping. Column Mapping ID: {}", columnMapping.getId());
            throw new CustomDataException(PROJECT_ID_IS_NOT_FOUND, null, FAILURE, HttpStatus.NOT_FOUND);
        }
    }


    /**
     * Check access and permissions for the column mapping.
     *
     * @param columnMapping The column mapping to check.
     * @param user The user performing the action.
     * @throws CustomDataException if access and permissions are not valid.
     */
    private void checkAccessAndPermissions(ColumnMapping columnMapping, UserDto user) {
        if (!isFrameworkLevel(columnMapping)) {
            ProjectDto project = ColumnsUtils.getProjectByPidAndStatus(columnMapping.getPid(), "ACTIVE");
            if (project == null) {
                log.error("Project does not exist. Project ID: {}", columnMapping.getPid());
                throw new CustomDataException(PROJECT_DOES_NOT_EXIST, null, FAILURE, HttpStatus.NOT_ACCEPTABLE);
            }
            if (!ColumnsUtils.validateRoleWithViewerAccess(user, project)) {
                log.error("User doesn't have access to view this report. User: {}", user.getUsername());
                throw new CustomDataException(USER_DOES_NOT_HAVE_ACCESS_TO_VIEW_REPORT, null, INFO, HttpStatus.NOT_ACCEPTABLE, REQUEST_ACCESS);
            }
        } else if (!user.getRole().equalsIgnoreCase(UserRole.SUPER_ADMIN.toString())) {
            log.error("User does not have super-admin access. User: {}", user.getUsername());
            throw new CustomDataException(USER_DOES_NOT_HAVE_SUPER_ADMIN_ACCESS, null, FAILURE, HttpStatus.UNAUTHORIZED);
        }
    }

}
