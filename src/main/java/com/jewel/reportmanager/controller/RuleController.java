package com.jewel.reportmanager.controller;

import com.jewel.reportmanager.dto.Response;
import com.jewel.reportmanager.dto.RuleApiDto;
import com.jewel.reportmanager.entity.RuleApi;
import com.jewel.reportmanager.exception.CustomDataException;
import com.jewel.reportmanager.service.RuleService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.text.ParseException;
import java.util.Map;

@Validated
@RestController
public class RuleController {

    @Autowired
    private RuleService ruleService;
    @Autowired
    private ModelMapper modelMapper;

    @PostMapping(path = "/v1/rule", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response> getRuleReport(@RequestBody @Valid final RuleApiDto payload, HttpServletRequest request,
                                                  @RequestParam(value = "pageNo", required = false) final Integer pageNo,
                                                  @RequestParam(value = "sort", required = false) final Integer sort,
                                                  @RequestParam(value = "sortedColumn", required = false) final String sortedColumn) throws ParseException {
        try {
            RuleApi ruleApi = modelMapper.map(payload, RuleApi.class);
            return ResponseEntity.ok(ruleService.getRuleReport(ruleApi, request, pageNo, sort, sortedColumn));
        } catch (CustomDataException ex) {
            return ResponseEntity.status(ex.getHttpStatus()).body(new Response(ex.getData(), ex.getMessage(), ex.getOperationType()));
        }
    }

    @GetMapping(path = "/v3/rule/action", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response> getRuleActionReportV3(@RequestParam(required = false) final String s_run_id,
                                                        @RequestParam(required = false) final String tc_run_id, HttpServletRequest request,
                                                        @RequestParam(value = "pageNo", required = false) final Integer pageNo,
                                                        @RequestParam(value = "sort", required = false) final Integer sort,
                                                        @RequestParam(value = "sortedColumn", required = false) final String sortedColumn) {
        try {
            return ResponseEntity.ok(ruleService.getRuleActionReportV3(s_run_id, tc_run_id, request, pageNo, sort, sortedColumn));
        } catch (CustomDataException ex) {
            return ResponseEntity.status(ex.getHttpStatus()).body(new Response(ex.getData(), ex.getMessage(), ex.getOperationType(), ex.getSubOperationType()));
        }
    }

    @PutMapping(path = "/v1/buildDetails", produces = "application/json")
    public ResponseEntity<Response> updateBuildDetails(@RequestParam(value = "s_run_id") @NotBlank final String s_run_id,
                                                     @RequestParam(value = "build_id", required = false) final String buildId,
                                                     @RequestParam(value = "sprint_name", required = false) final String sprint_name,
                                                     HttpServletRequest request) {
        try {
            return ResponseEntity.ok(ruleService.updateBuildDetails(s_run_id, buildId, sprint_name, request));
        } catch (CustomDataException ex) {
            return ResponseEntity.status(ex.getHttpStatus()).body(new Response(ex.getData(), ex.getMessage(), ex.getOperationType()));
        }
    }

    @GetMapping(path = "/v1/buildDetails/json", produces = "application/json")
    public ResponseEntity<Response> getBuildDetails(@RequestParam(value = "s_run_id") @NotBlank final String s_run_id,
                                                  HttpServletRequest request) {
        try {
            return ResponseEntity.ok(ruleService.getBuildDetails(s_run_id, request));
        } catch (CustomDataException ex) {
            return ResponseEntity.status(ex.getHttpStatus()).body(new Response(ex.getData(), ex.getMessage(), ex.getOperationType()));
        }
    }

    @GetMapping(path = "/v1/tickets")
    public ResponseEntity<Response> suiteTickets(@RequestParam(value = "s_run_id") @NotBlank final String s_run_id, HttpServletRequest request) {
        try {
            return ResponseEntity.ok(ruleService.getTickets(s_run_id, request));
        } catch (CustomDataException ex) {
            return ResponseEntity.status(ex.getHttpStatus()).body(new Response(ex.getData(), ex.getMessage(), ex.getOperationType(), ex.getSubOperationType()));
        }
    }

    @PostMapping(path = "/v1/timeline")
    public ResponseEntity<Response> suiteTimeline(@RequestBody @NotEmpty final Map<String, Object> payload, HttpServletRequest request,
                                                  @RequestParam(value = "category", required = false) final String category,
                                                  @RequestParam(value = "search", required = false) final String search,
                                                  @RequestParam(value = "pageNo", required = false) final Integer pageNo,
                                                  @RequestParam(value = "sort", required = false) final Integer sort,
                                                  @RequestParam(value = "sortedColumn", required = false) final String sortedColumn) throws ParseException {
        try {
            return ResponseEntity.ok(ruleService.getSuiteTimeline(payload, request, category, search, pageNo, sort, sortedColumn));
        } catch (CustomDataException ex) {
            return ResponseEntity.status(ex.getHttpStatus()).body(new Response(ex.getData(), ex.getMessage(), ex.getOperationType(), ex.getSubOperationType()));
        }
    }

}
