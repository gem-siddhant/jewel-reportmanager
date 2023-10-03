package com.jewel.reportmanager.controller;

import com.codahale.metrics.annotation.Timed;
import com.jewel.reportmanager.dto.RuleApiDto;
import com.jewel.reportmanager.service.RuleService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.Map;

@RestController
public class RuleController {

    @Autowired
    private RuleService ruleService;

    @Timed
    @PostMapping(path = "/rule", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getRuleReport(@RequestBody RuleApiDto payload, HttpServletRequest request,
                                                @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                @RequestParam(value = "sort", required = false) Integer sort,
                                                @RequestParam(value = "sortedColumn", required = false) String sortedColumn) throws ParseException {
        return ruleService.getRuleReport(payload, request, pageNo, sort, sortedColumn);
    }

//    @Timed
//    @Hidden
//    @GetMapping(path = "/rule/action", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<Object> getRuleActionReport(@RequestParam(required = false) String s_run_id,
//                                                      @RequestParam(required = false) String tc_run_id, HttpServletRequest request,
//                                                      @RequestParam(value = "pageNo", required = false) Integer pageNo,
//                                                      @RequestParam(value = "sort", required = false) Integer sort,
//                                                      @RequestParam(value = "sortedColumn", required = false) String sortedColumn) {
//        return ruleService.getRuleActionReport(s_run_id, tc_run_id, request, pageNo, sort, sortedColumn);
//    }
//
//    @Timed
//    @GetMapping(path = "/v2/rule/overview", produces = MediaType.APPLICATION_JSON_VALUE)
//    @Hidden
//    public ResponseEntity<Object> getRuleActionReportVersionV2(@RequestParam(required = false) String s_run_id,
//                                                               @RequestParam(required = false) String tc_run_id, HttpServletRequest request,
//                                                               @RequestParam(value = "pageNo", required = false) Integer pageNo,
//                                                               @RequestParam(value = "sort", required = false) Integer sort,
//                                                               @RequestParam(value = "sortedColumn", required = false) String sortedColumn) {
//        return ruleService.getRuleActionReportWithoutTestCaseDetails(s_run_id, tc_run_id, request, pageNo, sort,
//                sortedColumn);
//    }
//
//    @Timed
//    @GetMapping(path = "/v2/rule/testcases", produces = MediaType.APPLICATION_JSON_VALUE)
//    @Hidden
//    public ResponseEntity<Object> getTestCasesByS_run_id(@RequestParam(required = false) String s_run_id,
//                                                         @RequestParam(required = false) String tc_run_id, HttpServletRequest request,
//                                                         @RequestParam(value = "pageNo", required = false) Integer pageNo,
//                                                         @RequestParam(value = "sort", required = false) Integer sort,
//                                                         @RequestParam(value = "sortedColumn", required = false) String sortedColumn) {
//        return ruleService.getTestCaseDetailsByS_run_id(s_run_id, pageNo, sort, sortedColumn);
//    }
//
//    @Timed
//    @GetMapping(path = "/rule/action/steps", produces = MediaType.APPLICATION_JSON_VALUE)
//    @Hidden
//    public ResponseEntity<Object> getRuleActionTestStepReport(@RequestParam String tc_run_id, HttpServletRequest request) {
//        return ruleService.getRuleActionTestStepReport(tc_run_id, request);
//    }
//
//    @Timed
//    @GetMapping(value = "/rule/action/chart", produces = MediaType.APPLICATION_JSON_VALUE)
//    @Hidden
//    public ResponseEntity getRuleActionChart(@RequestParam(required = false) String s_run_id,
//                                             @RequestParam(value = "pageNo", required = false) Integer pageNo,
//                                             @RequestParam(value = "sort", required = false) Integer sort,
//                                             @RequestParam(value = "sortedColumn", required = false) String sortedColumn) {
//        return ruleService.getRuleActionChart(s_run_id, pageNo, sort, sortedColumn);
//    }
//
//    @Timed
//    @Hidden
//    @GetMapping(path = "/v2/rule/action", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<Object> getRuleActionReportV2(@RequestParam(required = false) String s_run_id,
//                                                        @RequestParam(required = false) String tc_run_id, HttpServletRequest request,
//                                                        @RequestParam(value = "pageNo", required = false) Integer pageNo,
//                                                        @RequestParam(value = "sort", required = false) Integer sort,
//                                                        @RequestParam(value = "sortedColumn", required = false) String sortedColumn) {
//        return ruleService.getRuleActionReportV2(s_run_id, tc_run_id, request, pageNo, sort, sortedColumn);
//    }

    @Timed
    @GetMapping(path = "/v3/rule/action", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getRuleActionReportV3(@RequestParam(required = false) String s_run_id,
                                                        @RequestParam(required = false) String tc_run_id, HttpServletRequest request,
                                                        @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                        @RequestParam(value = "sort", required = false) Integer sort,
                                                        @RequestParam(value = "sortedColumn", required = false) String sortedColumn) {
        return ruleService.getRuleActionReportV3(s_run_id, tc_run_id, request, pageNo, sort, sortedColumn);
    }

    @Timed
    @PutMapping(path = "/buildDetails", produces = "application/json")
    public ResponseEntity<Object> updateBuildDetails(@RequestParam(value = "s_run_id") String s_run_id,
                                                     @RequestParam(value = "build_id", required = false) String buildId,
                                                     @RequestParam(value = "sprint_name", required = false) String sprint_name,
                                                     HttpServletRequest request) {

        return ruleService.updateBuildDetails(s_run_id, buildId, sprint_name, request);
    }

    @Timed
    @GetMapping(path = "/buildDetails/json", produces = "application/json")
    public ResponseEntity<Object> getBuildDetails(@RequestParam(value = "s_run_id") String s_run_id,
                                                  HttpServletRequest request) {
        return ruleService.getBuildDetails(s_run_id, request);
    }

//    @Timed
//    @GetMapping(path = "/v2/steps")
//    @Hidden
//    public ResponseEntity<Object> getSteps(@RequestParam("tc_run_id") String tc_run_id, HttpServletRequest request) {
//        return this.ruleService.getStepsDataByTcRunId(tc_run_id, request);
//    }
//
//    @Timed
//    @GetMapping("/v2/last5RunStackedBarChartOfReport")
//    @Hidden
//    public ResponseEntity<Object> last5RunStackedBarChartOfReportBySRunId(@RequestParam(required = false) String s_run_id,
//                                                                          HttpServletRequest request) {
//        return this.ruleService.last5RunStackedBarChartOfReportBySRunId(s_run_id, request);
//    }

    @Timed
    @GetMapping(path = "/tickets")
    public ResponseEntity<Object> suiteTickets(@RequestParam(value = "s_run_id") String s_run_id, HttpServletRequest request) {
        return this.ruleService.getTickets(s_run_id, request);
    }

    @Timed
    @PostMapping(path = "/timeline")
    public ResponseEntity<Object> suiteTimeline(@RequestBody Map<String, Object> payload, HttpServletRequest request,
                                                @RequestParam(value = "category", required = false) String category,
                                                @RequestParam(value = "search", required = false) String search,
                                                @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                @RequestParam(value = "sort", required = false) Integer sort,
                                                @RequestParam(value = "sortedColumn", required = false) String sortedColumn) throws ParseException {
        return this.ruleService.getSuiteTimeline(payload, request, category, search, pageNo, sort, sortedColumn);
    }

}
