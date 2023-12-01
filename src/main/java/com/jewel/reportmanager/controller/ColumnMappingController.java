package com.jewel.reportmanager.controller;

import com.jewel.reportmanager.dto.ColumnMappingDto;
import com.jewel.reportmanager.dto.Response;
import com.jewel.reportmanager.entity.ColumnMapping;
import com.jewel.reportmanager.exception.CustomDataException;
import com.jewel.reportmanager.service.ColumnMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

import static com.jewel.reportmanager.enums.OperationType.*;
import static com.jewel.reportmanager.utils.ReportResponseConstants.*;

@Validated
@RestController
public class ColumnMappingController {
    @Autowired
    private ColumnMappingService columnMappingService;
    @Autowired
    private ModelMapper modelMapper;

    @Operation(summary = "Add a new column mapping", description = "Adds a new column mapping based on the provided data.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Column mapping created successfully.", content = {
                    @Content(mediaType = "application/json")})
    })
    @PostMapping("/v2/column")
    public ResponseEntity addColumn(@RequestBody @Valid final ColumnMappingDto columnMappingDto, HttpServletRequest request) {
        try {
            ColumnMapping columnMapping = this.modelMapper.map(columnMappingDto, ColumnMapping.class);
            Map<String, Object> result = columnMappingService.addColumnMapping(columnMapping, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(new Response(result, COLUMN_MAPPING_CREATE_SUCCESSFULLY, Success));
        } catch (CustomDataException ex) {
            return ResponseEntity.status(ex.getHttpStatus()).body(new Response(ex.getData(), ex.getMessage(), ex.getOperationType()));
        }
    }

    @Operation(summary = "Update an existing column mapping", description = "Updates an existing column mapping based on the provided data.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Column mapping updated successfully.", content = {
                    @Content(mediaType = "application/json")})
    })
    @PutMapping("/v2/column")
    public ResponseEntity updateColumn(@RequestBody @Valid final ColumnMappingDto columnMappingDto, HttpServletRequest request) {
        try {
            ColumnMapping columnMapping = this.modelMapper.map(columnMappingDto, ColumnMapping.class);
            Map<String, Object> result = columnMappingService.updateColumnMapping(columnMapping, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(new Response(result, COLUMN_MAPPING_UPDATE_SUCCESSFULLY, Success));
        } catch (CustomDataException ex) {
            return ResponseEntity.status(ex.getHttpStatus()).body(new Response(ex.getData(), ex.getMessage(), ex.getOperationType()));
        }
    }

    @Operation(summary = "Delete a column mapping", description = "Deletes a column mapping based on its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Column mapping deleted successfully.", content = {
                    @Content(mediaType = "application/json")})
    })
    @DeleteMapping("/v2/column")
    public ResponseEntity deleteColumnMapping(@RequestParam("id") Long id, HttpServletRequest request) {
        try {
            Response resp = columnMappingService.deleteColumnMapping(id, request);
            return ResponseEntity.ok().body(resp);
        } catch (CustomDataException ex) {
            return ResponseEntity.status(ex.getHttpStatus()).body(new Response(ex.getData(), ex.getMessage(), ex.getOperationType()));
        }
    }

    @Operation(summary = "Find column mapping details", description = "Finds column mapping details based on PID, name, and frameworks.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data fetched successfully !!", content = {
                    @Content(mediaType = "application/json")})
    })
    @GetMapping("/v2/column/pid/name/frameworks")
    public ResponseEntity<Object> findColumnMapping(@RequestParam @NotNull final Long pid,
                                                    @RequestParam @NotBlank final String name,
                                                    @RequestParam @NotEmpty final List<String> frameworks) {
        List<String> response = columnMappingService.findColumnMapping(pid, name, frameworks);
        if (response.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Response(null, COLUMN_DETAILS_NOT_FOUND, Failure));
        }
        return ResponseEntity.status(HttpStatus.OK).body(new Response(response, DATA_FETCHED_SUCCESSFULLY, Success));
    }
}
