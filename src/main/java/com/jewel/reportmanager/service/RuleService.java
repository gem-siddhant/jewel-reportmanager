package com.jewel.reportmanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jewel.reportmanager.dto.*;
import com.jewel.reportmanager.dto.StepsDto;
import com.jewel.reportmanager.dto.ClassificationDetails;
import com.jewel.reportmanager.dto.RuleApi;
import com.jewel.reportmanager.enums.OperationType;
import com.jewel.reportmanager.enums.StatusColor;
import com.jewel.reportmanager.exception.CustomDataException;
import com.jewel.reportmanager.utils.ReportUtils;
import com.jewel.reportmanager.utils.RestApiUtils;
import com.mongodb.BasicDBObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.jewel.reportmanager.enums.OperationType.*;
import static com.jewel.reportmanager.enums.StatusColor.*;
import static com.jewel.reportmanager.enums.UserRole.*;
import static com.jewel.reportmanager.utils.ReportResponseConstants.NEVER_FIXED;
import static com.jewel.reportmanager.utils.ReportResponseConstants.*;
import static javax.accessibility.AccessibleState.ACTIVE;

@Slf4j
@Service
public class RuleService {

    @Autowired
    private SimpMessageSendingOperations simpMessagingTemplate;

    /**
     * @param payload
     * @param pageNo
     * @param sort
     * @param sortedColumn
     * @return Response
     * @throws ParseException
     */
    public Response getRuleReport(RuleApi payload, Integer pageNo,
                                  Integer sort, String sortedColumn) throws ParseException {

        if ((sort != null && sortedColumn == null) || (sort == null && sortedColumn != null)) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(BOTH_PARAMETERS_REQUIRED, null, FAILURE, HttpStatus.OK);
        }
        if (sort != null && sort != -1 && sort != 0 && sort != 1) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(INVALID_SORT_VALUE, null, FAILURE, HttpStatus.OK);
        }

        UserDto user = ReportUtils.getUserDtoFromServetRequest();
        String username = user.getUsername();

        List<Long> allPids = new ArrayList<>(payload.getProjectid());
        List<Long> accessPids;
        if (user.getRole().equalsIgnoreCase(USER.toString())) {
            accessPids = RestApiUtils.getProjectRolePidList(payload.getProjectid(), ACTIVE_STATUS, username);
        } else if (user.getRole().equalsIgnoreCase(ADMIN.toString())) {
            accessPids = RestApiUtils.getProjectPidListForRealCompanyNameAndStatus(payload.getProjectid(), ACTIVE_STATUS, user.getRealCompany().toUpperCase());
        } else {
            accessPids = RestApiUtils.getProjectPidList(payload.getProjectid(), ACTIVE_STATUS);
        }
        payload.setProjectid(accessPids);
        allPids.removeAll(accessPids);

        List<String> errors = new ArrayList<>();
        if (!allPids.isEmpty()) {
            List<String> projectNames = RestApiUtils.getProjectNames(payload.getProjectid());
            for (String projectName : projectNames) {
                errors.add("You don't have access for " + projectName.toUpperCase());
            }
        }

        switch ((int) payload.getReportid()) {
            case 1:
                return createSuiteRunReport(payload, pageNo, sort, sortedColumn, errors);
            case 2:
                return createSuiteSummaryReport(payload, pageNo, errors);
            case 3:
                return createSuiteDiagnoseReport(payload, pageNo, errors);
            case 4:
                return createTestCaseRunReport(payload, pageNo, sort, sortedColumn, errors);
            case 5:
                return createTestCaseSummaryReport(payload, pageNo, sort, sortedColumn, errors);
            case 6:
                return createTestCaseDiagnoseReport(payload, pageNo, sort, sortedColumn, errors);
            default:
                log.error("Error occurred due to records not found");
                throw new CustomDataException(REPORT_ID_NOT_VALID, null, FAILURE, HttpStatus.OK);
        }

    }

    /**
     * Creates suite run report.
     *
     * @param payload
     * @param pageNo
     * @param sort
     * @param sortedColumn
     * @param errors
     * @return
     * @throws ParseException
     */

    private Response createSuiteRunReport(RuleApi payload, Integer pageNo,
                                          Integer sort, String sortedColumn, List<String> errors) throws ParseException {

        Map<String, Object> result = new HashMap<>();
        List<Object> headers = new ArrayList<>();
        Collections.addAll(headers, "Project Name", "Report Name", "Environment", "Status", "Executed By", "Action",
                "Duration", "Testcase Summary");
        result.put("headers", headers);
        List<Map<String, Object>> data = new ArrayList<>();
        long startTime = new SimpleDateFormat("MM/dd/yyyy").parse(payload.getStartTime()).getTime();
        long endTime = new SimpleDateFormat("MM/dd/yyyy").parse(payload.getEndTime()).getTime()
                + (1000 * 60 * 60 * 24);
        List<String> projects = payload.getProject();
        projects.replaceAll(String::toLowerCase);
        List<String> envs = payload.getEnv();
        List<Long> p_ids = payload.getProjectid();
        envs.replaceAll(String::toLowerCase);

        long count = RestApiUtils.getSuiteExeCount(p_ids, envs, startTime, endTime);
        if (count == 0) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(SUITE_DETAILS_NOT_FOUND, null, FAILURE, HttpStatus.NOT_FOUND);
        }
        if (pageNo != null && pageNo <= 0) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(PAGE_NO_CANNOT_BE_NEGATIVE_OR_ZERO, null, FAILURE, HttpStatus.OK);
        }

        List<SuiteExeDto> suiteReports = RestApiUtils.getSuiteExes(p_ids, envs, startTime, endTime, pageNo, sort, sortedColumn);
        if (suiteReports.isEmpty()) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(PAGE_NUMBER_IS_ABOVE_TOTAL_PAGES, null, FAILURE, HttpStatus.OK);
        }
        List<String> sRunIds = RestApiUtils.getS_Run_Ids(p_ids, envs, startTime, endTime, pageNo, sort, sortedColumn);

        List<TestExeDto> testExeDtoList = RestApiUtils.getTestExeListForS_run_ids(sRunIds);

        for (SuiteExeDto suiteExeDto : suiteReports) {
            data.add(createSuiteExeReport(testExeDtoList, suiteExeDto));
        }

        Collections.reverse(data);

        result.put("data", data);
        result.put("totalElements", count);
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }
        return new Response(result, count + " Records found", SUCCESS);
    }

    /**
     * creates suite exe report for list of suite exes.
     *
     * @param testExeDtoList
     * @param suiteExeDto
     * @return
     */
    private Map<String, Object> createSuiteExeReport(List<TestExeDto> testExeDtoList, SuiteExeDto suiteExeDto) {
        Map<String, Object> temp = new HashMap<>();
        Map<String, Long> statusMap = new HashMap<>();
        Set<String> users = new HashSet<>();

        for (StatusColor statusColor : StatusColor.values()) {
            statusMap.put(statusColor.toString(), 0L);
        }
        if (!testExeDtoList.isEmpty()) {
            temp = getTestExeStatusForSuiteExe(testExeDtoList, suiteExeDto, users, statusMap);
        }

        createActionReportForSuiteExe(temp, suiteExeDto, users);
        return temp;
    }

    /**
     * Return test exe status for suite exe.
     *
     * @param testExeDtoList
     * @param suiteExeDto
     * @param users
     * @param statusMap
     * @return
     */
    private Map<String, Object> getTestExeStatusForSuiteExe(List<TestExeDto> testExeDtoList, SuiteExeDto suiteExeDto, Set<String> users, Map<String, Long> statusMap) {
        Map<String, Object> temp = new HashMap<>();
        long totalCount = 0L;
        for (TestExeDto testExeDto : testExeDtoList) {
            if (!testExeDto.getS_run_id().equals(suiteExeDto.getS_run_id())) {
                continue;
            }
            if (testExeDto.getInvoke_user() != null) {
                users.add(testExeDto.getInvoke_user());
            }
            if (testExeDto.getS_run_id().equals(suiteExeDto.getS_run_id())) {
                String status = testExeDto.getStatus().toUpperCase();
                switch (status) {
                    case "PASS":
                    case "FAIL":
                    case "EXE":
                    case "ERR":
                    case "INFO":
                    case "WARN":
                        long value = statusMap.get(status) + 1;
                        statusMap.put(status, value);
                        totalCount++;
                }
            }
        }
        if (totalCount > 0) {
            Map<String, Object> doughnutSubType = new HashMap<>();
            doughnutSubType.put("subType", "doughnut_chart");
            doughnutSubType.put("heading", "Total Testcase(s)");
            temp.put("Testcase Summary",
                    ReportUtils.createCustomObject(ReportUtils.createDoughnutChart(statusMap),
                            "chart", totalCount, "center", doughnutSubType));
        }
        return temp;
    }

    /**
     * Creates action report for suite exe to generate rule report.
     *
     * @param temp
     * @param suiteExeDto
     * @param users
     */
    private void createActionReportForSuiteExe(Map<String, Object> temp, SuiteExeDto suiteExeDto, Set<String> users) {
        Map<String, Object> actionReport = new HashMap<>();
        actionReport.put("subType", "execution_report");
        temp.put("Action",
                ReportUtils.createCustomObject(suiteExeDto.getS_run_id(), "action", suiteExeDto.getS_run_id(), "center",
                        actionReport));
        temp.put("Report Name",
                ReportUtils.createCustomObject(suiteExeDto.getReport_name(), "text", suiteExeDto.getReport_name(),
                        "left"));
        temp.put("Project Name",
                ReportUtils.createCustomObject(StringUtils.capitalize(suiteExeDto.getProject_name()), "text",
                        suiteExeDto.getProject_name(),
                        "left"));
        temp.put("Environment",
                ReportUtils.createCustomObject(StringUtils.capitalize(suiteExeDto.getEnv()), "text", suiteExeDto.getEnv(),
                        "left"));
        if (!users.isEmpty()) {
            temp.put("Executed By", ReportUtils.createCustomObject(users,
                    "pills", users, "left"));
        } else {
            temp.put("Executed By", ReportUtils.createCustomObject("-",
                    "text", "-", "center"));
        }
        temp.put("Status",
                ReportUtils.createCustomObject(suiteExeDto.getStatus(), "status", suiteExeDto.getStatus(), "center"));

        if (suiteExeDto.getS_end_time() != 0) {
            Map<String, Object> subtype = new HashMap<>();
            subtype.put("subType", "duration");
            Map<String, Object> values = new HashMap<>();
            values.put("start_time", suiteExeDto.getS_start_time());
            values.put("end_time", suiteExeDto.getS_end_time());
            temp.put("Duration",
                    ReportUtils.createCustomObject(values, "date",
                            ((float) (suiteExeDto.getS_end_time() - suiteExeDto.getS_start_time())),
                            "center", subtype));
        } else {
            temp.put("Duration", ReportUtils.createCustomObject("-", "text", suiteExeDto.getS_end_time(), "center"));
        }
    }

    private Response createSuiteSummaryReport(RuleApi payload, Integer pageNo, List<String> errors) throws ParseException {

        Map<String, Object> result = new HashMap<>();
        List<Object> headers = new ArrayList<>();
        List<Map<String, Object>> data = new ArrayList<>();
        Collections.addAll(headers, "Project Name", "Report Name", "Environment", "Suite Summary", "Last 5 Runs",
                "Stability Index",
                "Average Fix Time", "App Stability Score", "Automation Stability Score", "Analysis");
        result.put("headers", headers);

        long startTime = new SimpleDateFormat("MM/dd/yyyy").parse(payload.getStartTime()).getTime();
        long endTime = new SimpleDateFormat("MM/dd/yyyy").parse(payload.getEndTime()).getTime()
                + (1000 * 60 * 60 * 24);
        List<String> projects = payload.getProject();
        projects.replaceAll(String::toLowerCase);
        List<String> envs = payload.getEnv();
        envs.replaceAll(String::toLowerCase);
        List<Long> p_ids = payload.getProjectid();


        if (pageNo != null && pageNo <= 0) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(PAGE_NO_CANNOT_BE_NEGATIVE_OR_ZERO, null, FAILURE, HttpStatus.OK);
        }

        List<String> reportNames = RestApiUtils.getReportNames(p_ids, envs, startTime, endTime, pageNo);
        if (reportNames.isEmpty()) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(SUITE_DETAILS_NOT_FOUND, null, FAILURE, HttpStatus.NOT_FOUND);
        }

        long count = getReportDetailsToCreateSuiteSummaryReport(reportNames, p_ids, projects, startTime,
                endTime, envs, data);

        result.put("data", data);
        result.put("totalElements", count);
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }

        return new Response(result, count + " Records found", SUCCESS);
    }

    private Long getReportDetailsToCreateSuiteSummaryReport(List<String> reportNames, List<Long> p_ids, List<String> projects, long startTime, long endTime, List<String> envs, List<Map<String, Object>> data) {
        long count = 0;
        for (String reportName : reportNames) {
            Map<String, List<SuiteExeDto>> suiteMap = ReportUtils.getSuiteNames(reportName, p_ids, projects, startTime,
                    endTime, envs);
            count = count + suiteMap.size();
            for (Map.Entry<String, List<SuiteExeDto>> entry : suiteMap.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    continue;
                }
                List<SuiteExeDto> getAllSuites = entry.getValue();
                Map<String, Long> statusMap = new HashMap<>();
                for (StatusColor statusColor : StatusColor.values()) {
                    statusMap.put(statusColor.toString(), 0L);
                }

                long totalCount = getStatusMapForAllSuites(getAllSuites, statusMap);

                data.add(getReportDataForSuiteSummaryReport(getAllSuites, reportName, totalCount, statusMap));
            }
        }
        return count;
    }

    private Long getStatusMapForAllSuites(List<SuiteExeDto> getAllSuites, Map<String, Long> statusMap) {
        long totalCount = 0L;
        for (SuiteExeDto suiteExeDto : getAllSuites) {
            String status = suiteExeDto.getStatus().toUpperCase();
            switch (status) {
                case "PASS":
                case "FAIL":
                case "EXE":
                case "ERR":
                case "INFO":
                case "WARN":
                    long value = statusMap.get(status) + 1;
                    statusMap.put(status, value);
                    totalCount++;
            }
        }
        return totalCount;
    }

    private Map<String, Object> getReportDataForSuiteSummaryReport(List<SuiteExeDto> getAllSuites, String reportName, Long totalCount, Map<String, Long> statusMap) {
        String env = getAllSuites.get(0).getEnv();
        List<SuiteExeDto> sortedList = ReportUtils.getSortedListForSuiteExe(getAllSuites);
        double brokenIndex = ReportUtils.brokenIndexForSuiteExe(getAllSuites);
        int stabilityIndex = ReportUtils.stabilityIndex(brokenIndex);
        long averageFixTime = ReportUtils.averageFixTimeForSuiteExe(getAllSuites);
        long downTime = ReportUtils.getDownTimeForSuiteExe(sortedList);
        Map<String, Object> last5SuiteRuns = ReportUtils.last5SuiteRuns(getAllSuites);
        Map<String, Long> culprit = ReportUtils.culprit(getAllSuites);

        double devScore = ReportUtils.getScore(brokenIndex, downTime, averageFixTime, env, getAllSuites);
        double qaScore = ReportUtils.getQAScore(getAllSuites);
        String averageFixTimeStr;
        if (brokenIndex == 1) {
            averageFixTimeStr = NEVER_FIXED;
        } else {
            averageFixTimeStr = ReportUtils.convertLongToTime(averageFixTime);
        }
        Map<String, Object> temp = new HashMap<>();
        if (last5SuiteRuns != null) {
            Map<String, Object> stackedBarChartType = new HashMap<>();
            stackedBarChartType.put("subType", "stacked_bar_chart");
            stackedBarChartType.put("heading", "Last 5 Runs");
            long size = (long) last5SuiteRuns.get("size");
            last5SuiteRuns.remove("size");
            temp.put("Last 5 Runs", ReportUtils.createCustomObject(last5SuiteRuns, "chart", size, "center",
                    stackedBarChartType));
        } else {
            temp.put("Last 5 Runs",
                    ReportUtils.createCustomObject("-", "text", "-", "left"));
        }
        temp.put("Report Name",
                ReportUtils.createCustomObject(StringUtils.capitalize(reportName), "text", reportName,
                        "left"));
        temp.put("Project Name",
                ReportUtils.createCustomObject(
                        StringUtils.capitalize(getAllSuites.get(0).getProject_name()), "text",
                        getAllSuites.get(0).getProject_name(), "left"));
        temp.put("Environment",
                ReportUtils.createCustomObject(StringUtils.capitalize(getAllSuites.get(0).getEnv()), "text",
                        getAllSuites.get(0).getEnv(), "left"));
        Map<String, Object> doughnutSubType = new HashMap<>();
        doughnutSubType.put("subType", "doughnut_chart");
        doughnutSubType.put("heading", "Total Suite(s)");
        temp.put("Suite Summary",
                ReportUtils.createCustomObject(ReportUtils.createDoughnutChart(statusMap), "chart",
                        totalCount, "center", doughnutSubType));
        temp.put("Stability Index",
                ReportUtils.createCustomObject(stabilityIndex + "%", "text", stabilityIndex, "center"));
        temp.put("Average Fix Time",
                ReportUtils.createCustomObject(averageFixTimeStr, "text", averageFixTimeStr, "center"));
        if (culprit != null) {
            String averagePercentage = culprit.get("average") + "%";
            culprit.remove("average");
            temp.put("Analysis",
                    ReportUtils.createCustomObject(culprit, "tabs", averagePercentage, "left"));
        } else {
            temp.put("Analysis",
                    ReportUtils.createCustomObject("-", "text", "-", "left"));
        }
        temp.put("App Stability Score",
                ReportUtils.createCustomObject(devScore, "score", devScore, "center"));
        temp.put("Automation Stability Score",
                ReportUtils.createCustomObject(qaScore, "score", qaScore, "center"));
        temp.put("P ID", ReportUtils.createCustomObject(getAllSuites.get(0).getP_id(), "text",
                getAllSuites.get(0).getP_id(), "center"));
        return temp;
    }

    private Response createSuiteDiagnoseReport(RuleApi payload, Integer pageNo, List<String> errors) throws ParseException {

        Map<String, Object> result = new HashMap<>();
        List<Object> headers = new ArrayList<>();
        List<Map<String, Object>> data = new ArrayList<>();
        Collections.addAll(headers, "Project Name", "Report Name", "Environment", "Last Run Status", "Failing Since",
                "Stability Index", "Downtime", "Average Fix Time", "Last Pass", "Last Status Details",
                "Analysis");
        result.put("headers", headers);

        long startTime = new SimpleDateFormat("MM/dd/yyyy").parse(payload.getStartTime()).getTime();
        long endTime = new SimpleDateFormat("MM/dd/yyyy").parse(payload.getEndTime()).getTime()
                + (1000 * 60 * 60 * 24);

        List<String> projects = payload.getProject();
        projects.replaceAll(String::toLowerCase);
        List<String> envs = payload.getEnv();
        List<Long> p_ids = payload.getProjectid();
        envs.replaceAll(String::toLowerCase);

        if (pageNo != null && pageNo <= 0) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(PAGE_NO_CANNOT_BE_NEGATIVE_OR_ZERO, null, FAILURE, HttpStatus.OK);
        }

        List<String> reportNames = RestApiUtils.getReportNames(p_ids, envs, startTime, endTime, pageNo);
        if (reportNames.isEmpty()) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(SUITE_DETAILS_NOT_FOUND, null, FAILURE, HttpStatus.NOT_FOUND);
        }

        long count = getReportDetailsToCreateSuiteDiagnoseReport(reportNames, p_ids, projects, startTime,
                endTime, envs, data);

        result.put("data", data);
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }
        result.put("totalElements", count);

        return new Response(result, count + " Records found", SUCCESS);
    }

    private Long getReportDetailsToCreateSuiteDiagnoseReport(List<String> reportNames, List<Long> pIds, List<String> projects, long startTime, long endTime, List<String> envs, List<Map<String, Object>> data) {
        long count = 0;
        for (String reportName : reportNames) {
            Map<String, List<SuiteExeDto>> suiteMap = ReportUtils.getSuiteNames(reportName, pIds, projects, startTime,
                    endTime, envs);
            count = count + suiteMap.size();
            for (Map.Entry<String, List<SuiteExeDto>> entry : suiteMap.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    continue;
                }
                List<SuiteExeDto> getAllSuites = entry.getValue();
                List<SuiteExeDto> sortedList = ReportUtils.getSortedListForSuiteExe(getAllSuites);
                double brokenIndex = ReportUtils.brokenIndexForSuiteExe(getAllSuites);
                int stabilityIndex = ReportUtils.stabilityIndex(brokenIndex);
                String failingSince = getFailingSinceForSuiteExe(sortedList, brokenIndex);
                String lastRunStatus = sortedList.get(0).getStatus();
                Long lastPass = getLastPassForSuiteExe(sortedList);
                long downTime = ReportUtils.getDownTimeForSuiteExe(sortedList);
                Map<String, Long> culprit = ReportUtils.culprit(getAllSuites);

                Map<String, Long> statusMap = lastStatusDetails(sortedList);
                long totalCount = 0;
                for (Map.Entry<String, Long> entry1 : statusMap.entrySet()) {
                    totalCount = totalCount + entry1.getValue();
                }
                long averageFixTime = ReportUtils.averageFixTimeForSuiteExe(getAllSuites);
                String downTimeStr;
                String averageFixTimeStr;
                if (brokenIndex == 1) {
                    averageFixTimeStr = NEVER_FIXED;
                } else {
                    averageFixTimeStr = ReportUtils.convertLongToTime(averageFixTime);
                }

                if (downTime == 0) {
                    downTimeStr = "No Issues";
                } else {
                    downTimeStr = ReportUtils.convertLongToTime(downTime);
                }

                data.add(getDataForSuiteExeToCreateSuiteDiagnoseReport(reportName, getAllSuites, stabilityIndex, failingSince, lastRunStatus, lastPass,
                        culprit, downTimeStr, averageFixTimeStr, statusMap, totalCount));
            }
        }
        return count;
    }

    private Map<String, Object> getDataForSuiteExeToCreateSuiteDiagnoseReport(String reportName, List<SuiteExeDto> getAllSuites, int stabilityIndex, String failingSince, String lastRunStatus, Long lastPass,
                                                                              Map<String, Long> culprit, String downTimeStr, String averageFixTimeStr, Map<String, Long> statusMap, long totalCount) {
        Map<String, Object> temp = new HashMap<>();
        temp.put("Report Name",
                ReportUtils.createCustomObject(StringUtils.capitalize(reportName), "text", reportName,
                        "left"));

        temp.put("Project Name",
                ReportUtils.createCustomObject(
                        StringUtils.capitalize(getAllSuites.get(0).getProject_name()), "text",
                        getAllSuites.get(0).getProject_name(), "left"));
        if (culprit != null) {
            String averagePercentage = culprit.get("average") + "%";
            culprit.remove("average");
            temp.put("Analysis",
                    ReportUtils.createCustomObject(culprit, "tabs", averagePercentage, "left"));
        } else {
            temp.put("Analysis",
                    ReportUtils.createCustomObject("-", "text", "-", "left"));
        }
        temp.put("Environment",
                ReportUtils.createCustomObject(StringUtils.capitalize(getAllSuites.get(0).getEnv()), "text",
                        getAllSuites.get(0).getEnv(), "left"));
        Map<String, Object> doughnutSubType = new HashMap<>();
        doughnutSubType.put("heading", "Total Testcase(s)");
        doughnutSubType.put("subType", "doughnut_chart");
        temp.put("Last Status Details",
                ReportUtils.createCustomObject(ReportUtils.createDoughnutChart(statusMap), "chart",
                        totalCount, "center", doughnutSubType));
        temp.put("Stability Index",
                ReportUtils.createCustomObject(stabilityIndex + "%", "text", stabilityIndex, "center"));
        temp.put("Average Fix Time",
                ReportUtils.createCustomObject(averageFixTimeStr, "text", averageFixTimeStr, "center"));
        temp.put("Last Run Status",
                ReportUtils.createCustomObject(lastRunStatus, "status", lastRunStatus, "center"));
        if (lastPass > 0) {
            Map<String, Object> timereport = new HashMap<>();
            timereport.put("subType", "datetime");
            temp.put("Last Pass",
                    ReportUtils.createCustomObject(lastPass, "date", lastPass, "center", timereport));
        } else {
            temp.put("Last Pass", ReportUtils.createCustomObject("-", "text", "-", "center"));
        }
        temp.put("Failing Since",
                ReportUtils.createCustomObject(failingSince, "text", failingSince, "center"));
        temp.put("Downtime", ReportUtils.createCustomObject(downTimeStr, "text", downTimeStr, "center"));
        temp.put("P ID", ReportUtils.createCustomObject(getAllSuites.get(0).getP_id(), "text",
                getAllSuites.get(0).getP_id(), "center"));
        return temp;
    }

    private Map<String, Long> lastStatusDetails(List<SuiteExeDto> suites) {
        String sRunId = suites.get(0).getS_run_id();
        List<TestExeDto> testcaseDetails = RestApiUtils.getTestExeList(sRunId);
        Map<String, Long> statusMap = new HashMap<>();
        for (StatusColor statusColor : StatusColor.values()) {
            statusMap.put(statusColor.toString(), 0L);
        }
        long totalCount = 0;
        for (TestExeDto testExe : testcaseDetails) {
            String status = testExe.getStatus().toUpperCase();
            switch (status) {
                case "PASS":
                case "FAIL":
                case "EXE":
                case "ERR":
                case "INFO":
                case "WARN":
                    long value = statusMap.get(status) + 1;
                    statusMap.put(status, value);
                    totalCount++;
            }
        }
        return statusMap;
    }

    private Long getLastPassForSuiteExe(List<SuiteExeDto> suites) {
        for (SuiteExeDto suite : suites) {
            if (suite.getStatus().equalsIgnoreCase("PASS")) {
                return suite.getS_start_time();
            }
        }
        return 0L;
    }

    private String getFailingSinceForSuiteExe(List<SuiteExeDto> suites, double brokenIndex) {
        if (brokenIndex == 0) {
            return NO_ISSUES;
        } else if (brokenIndex == 1) {
            return NEVER_FIXED;
        } else {
            int count = 0;
            for (SuiteExeDto suite : suites) {
                if (!suite.getStatus().equalsIgnoreCase("FAIL")) {
                    break;
                }
                count++;
            }
            if (count == 0) {
                return NO_ISSUES;
            }
            return new StringBuilder().append("Last ").append(count).append(" Runs").toString();
        }
    }

    private Response createTestCaseRunReport(RuleApi payload, Integer pageNo, Integer sort,
                                             String sortedColumn, List<String> errors) {
        Map<String, Object> result = new HashMap<>();
        List<Object> headers = new ArrayList<>();
        Collections.addAll(headers, "Project Name", "TestCase Name", "Environment", "Status", "Action",
                "Product Type",
                "Duration");
        result.put("headers", headers);
        if (pageNo != null && pageNo <= 0) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(PAGE_NO_CANNOT_BE_NEGATIVE_OR_ZERO, null, FAILURE, HttpStatus.OK);
        }
        Map<String, Object> resultMap = RestApiUtils.getAllTestExesForTcRunId(payload, pageNo, sort,
                sortedColumn);
        long count = (long) resultMap.get("count");
        List<BasicDBObject> results = (List<BasicDBObject>) resultMap.get("results");
        if (count == 0) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(SUITE_DETAILS_NOT_FOUND, null, FAILURE, HttpStatus.NOT_FOUND);
        }
        List<Map<String, Object>> data = getDataForTestCaseRunReport(results);
        Collections.reverse(data);
        result.put("data", data);
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }
        result.put("totalElements", count);
        return new Response(result, count + " Records found", SUCCESS);
    }

    private List<Map<String, Object>> getDataForTestCaseRunReport(List<BasicDBObject> results) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (BasicDBObject testExe : results) {
            List<Document> suiteExeList = (List<Document>) testExe.get("result");
            Document suiteExe = suiteExeList.get(0);
            Map<String, Object> temp = new HashMap<>();
            Map<String, Object> actionReport = new HashMap<>();
            actionReport.put("subType", "teststep_report");
            temp.put("Action",
                    ReportUtils.createCustomObject(testExe.get("tc_run_id"), "action", testExe.get("tc_run_id"),
                            "center", actionReport));
            temp.put("TestCase Name",
                    ReportUtils.createCustomObject(testExe.get("name"), "text", testExe.get("name"), "left"));
            temp.put("Project Name",
                    ReportUtils.createCustomObject(
                            StringUtils.capitalize(String.valueOf(suiteExe.get("project_name"))), "text",
                            suiteExe.get("project_name"), "left"));
            temp.put("Environment",
                    ReportUtils.createCustomObject(StringUtils.capitalize(String.valueOf(suiteExe.get("env"))),
                            "text", suiteExe.get("env"), "left"));
            temp.put("Status",
                    ReportUtils.createCustomObject(testExe.get("status"), "status", testExe.get("status"),
                            "center"));
            temp.put("Product Type", ReportUtils.createCustomObject(testExe.get("product_type"), "text",
                    testExe.get("product_type"), "left"));
            temp.put("P ID",
                    ReportUtils.createCustomObject(suiteExe.get("p_id"), "text",
                            suiteExe.get("p_id"), "left"));

            if (((long) testExe.get("end_time")) != 0) {
                Map<String, Object> subtype = new HashMap<>();
                subtype.put("subType", "duration");
                Map<String, Object> values = new HashMap<>();
                values.put("start_time", (testExe.get("start_time")));
                values.put("end_time", (testExe.get("end_time")));
                temp.put("Duration",
                        ReportUtils.createCustomObject(values, "date",
                                ((float) (((long) testExe.get("end_time")) - ((long) testExe.get("start_time")))),
                                "center", subtype));
                temp.put("End Time",
                        ReportUtils.createCustomObject(((long) testExe.get("end_time")), "date",
                                (testExe.get("end_time")), "center"));
            } else {
                temp.put("Duration",
                        ReportUtils.createCustomObject("-", "text", (testExe.get("end_time")), "center"));
            }
            data.add(temp);
        }
        return data;
    }

    private Response createTestCaseSummaryReport(RuleApi payload, Integer pageNo, Integer sort,
                                                 String sortedColumn, Object errors) {
        Map<String, Object> result = new HashMap<>();

        List<Object> headers = new ArrayList<>();
        List<Map<String, Object>> data = new ArrayList<>();
        Collections.addAll(headers, "Project Name", "TestCase Name", "TestCase Summary",
                "Broken Index", "Average Fix Time");
        result.put("headers", headers);
        if (pageNo != null && pageNo <= 0) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(PAGE_NO_CANNOT_BE_NEGATIVE_OR_ZERO, null, FAILURE, HttpStatus.OK);
        }
        Map<String, Object> resultMap = RestApiUtils.getAllTestExesForTcRunId(payload, pageNo, sort,
                sortedColumn);
        long count = (long) resultMap.get("count");
        List<BasicDBObject> results = (List<BasicDBObject>) resultMap.get("results");
        if (count == 0) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(SUITE_DETAILS_NOT_FOUND, null, FAILURE, HttpStatus.NOT_FOUND);
        }
        Map<String, List<TestExeCommonDto>> listMap = new HashMap<>();
        for (BasicDBObject testExeDto : results) {
            List<Document> suiteExeList = (List<Document>) testExeDto.get("result");
            Document suiteExeDto = suiteExeList.get(0);
            TestExeCommonDto testExeDtoSummery = ReportUtils.getTestExeCommonByBasicObjectAndDocument(testExeDto,
                    suiteExeDto);
            StringBuilder key = new StringBuilder();
            key.append(testExeDtoSummery.getName()).append(":").append(testExeDtoSummery.getReport_name())
                    .append(":").append(testExeDtoSummery.getEnv()).append(":").append(testExeDtoSummery.getProject_name());
            List<TestExeCommonDto> list = listMap.getOrDefault(key.toString(), null);
            if (list == null) {
                List<TestExeCommonDto> testExeDtoSummeryList = new ArrayList<>();
                testExeDtoSummeryList.add(testExeDtoSummery);
                listMap.put(String.valueOf(key), testExeDtoSummeryList);
            } else {
                list.add(testExeDtoSummery);
                listMap.put(String.valueOf(key), list);
            }
        }

        for (Map.Entry<String, List<TestExeCommonDto>> entry : listMap.entrySet()) {

            List<TestExeCommonDto> testExeCommonDtoSummeryList = entry.getValue();

            double brokenIndex = ReportUtils.brokenIndexForTestExe(testExeCommonDtoSummeryList);
            String averageFixTime;
            if (brokenIndex == 1) {
                averageFixTime = NEVER_FIXED;
            } else {
                averageFixTime = ReportUtils.averageFixTimeForTestExeCommon(testExeCommonDtoSummeryList);
            }
            long totalCount = 0;
            long passCount = 0, failCount = 0, errCount = 0, warnCount = 0, infoCount = 0,
                    exeCount = 0;

            Map<String, Long> statusCount = new HashMap<>();
            for (TestExeCommonDto testExeDtovar : testExeCommonDtoSummeryList) {

                if (testExeDtovar.getStatus().equalsIgnoreCase("PASS")) {
                    passCount = passCount + 1;

                } else if (testExeDtovar.getStatus().equalsIgnoreCase("FAIL")) {
                    failCount = failCount + 1;

                } else if (testExeDtovar.getStatus().equalsIgnoreCase("ERR")) {
                    errCount = errCount + 1;

                } else if (testExeDtovar.getStatus().equalsIgnoreCase("WARN")) {
                    warnCount = warnCount + 1;

                } else if (testExeDtovar.getStatus().equalsIgnoreCase("INFO")) {
                    infoCount = infoCount + 1;

                } else if (testExeDtovar.getStatus().equalsIgnoreCase("EXE")) {
                    exeCount = exeCount + 1;

                }
                totalCount = totalCount + 1;
            }

            statusCount.put("PASS", passCount);
            statusCount.put("FAIL", failCount);
            statusCount.put("ERR", errCount);
            statusCount.put("WARN", warnCount);
            statusCount.put("INFO", infoCount);
            statusCount.put("EXE", exeCount);
            Map<String, Object> temp = new HashMap<>();
            temp.put("Project Name",
                    ReportUtils.createCustomObject(
                            StringUtils.capitalize(testExeCommonDtoSummeryList.get(0).getProject_name()), "text",
                            testExeCommonDtoSummeryList.get(0).getProject_name(), "left"));
            temp.put("TestCase Name",
                    ReportUtils.createCustomObject(testExeCommonDtoSummeryList.get(0).getName(), "text",
                            testExeCommonDtoSummeryList.get(0).getName(), "left"));

            Map<String, Object> doughnutSubType = new HashMap<>();
            doughnutSubType.put("subType", "doughnut_chart");
            doughnutSubType.put("heading", "Total Testcase(s)");
            temp.put("TestCase Summary",
                    ReportUtils.createCustomObject(ReportUtils.createDoughnutChart(statusCount),
                            "chart", totalCount, "center", doughnutSubType));

            temp.put("Broken Index", ReportUtils.createCustomObject(brokenIndex, "text", brokenIndex, "center"));
            temp.put("Average Fix Time",
                    ReportUtils.createCustomObject(averageFixTime, "text", averageFixTime, "center"));
            temp.put("P ID",
                    ReportUtils.createCustomObject(testExeCommonDtoSummeryList.get(0).getP_id(), "text",
                            testExeCommonDtoSummeryList.get(0).getP_id(), "left"));
            data.add(temp);
        }

        result.put("data", data);
        if (errors != null) {
            result.put("errors", errors);
        }
        result.put("totalElements", listMap.size());

        return new Response(result, listMap.size() + " Records found", SUCCESS);
    }

    private Response createTestCaseDiagnoseReport(RuleApi payload, Integer pageNo, Integer sort,
                                                  String sortedColumn, Object errors) {

        Map<String, Object> result = new HashMap<>();
        List<Object> headers = new ArrayList<>();
        List<Map<String, Object>> data = new ArrayList<>();
        Collections.addAll(headers, "Project Name", "TestCase Name", "Environment", "Report Name", "Last Run Status",
                "Failing Since", "Broken Index", "Downtime", "Average Fix Time", "Last Pass");
        result.put("headers", headers);
        if (pageNo != null && pageNo <= 0) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(PAGE_NO_CANNOT_BE_NEGATIVE_OR_ZERO, null, FAILURE, HttpStatus.OK);
        }
        Map<String, Object> resultMap = RestApiUtils.getAllTestExesForTcRunId(payload, pageNo, sort,
                sortedColumn);
        long count = (long) resultMap.get("count");
        List<BasicDBObject> results = (List<BasicDBObject>) resultMap.get("results");
        if (count == 0) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(SUITE_DETAILS_NOT_FOUND, null, FAILURE, HttpStatus.NOT_FOUND);
        }
        Map<String, List<TestExeCommonDto>> listMap = new HashMap<>();
        for (BasicDBObject testExeDto : results) {
            List<Document> ob = (List<Document>) testExeDto.get("result");
            Document suiteExeDto = ob.get(0);
            TestExeCommonDto testExeDtoDiagnose = ReportUtils.getTestExeCommonByBasicObjectAndDocument(testExeDto,
                    suiteExeDto);
            String key = testExeDtoDiagnose.getName() + ":" + testExeDtoDiagnose.getReport_name() + ":"
                    + testExeDtoDiagnose.getEnv() + ":" + testExeDtoDiagnose.getProject_name();
            List<TestExeCommonDto> list = listMap.getOrDefault(key, null);
            if (list == null) {
                List<TestExeCommonDto> testExeDtoDiagnoseList = new ArrayList<>();
                testExeDtoDiagnoseList.add(testExeDtoDiagnose);
                listMap.put(key, testExeDtoDiagnoseList);
            } else {
                list.add(testExeDtoDiagnose);
                listMap.put(key, list);
            }
        }

        for (Map.Entry<String, List<TestExeCommonDto>> entry : listMap.entrySet()) {
            List<TestExeCommonDto> testExeCommonDtoDiagnoseList = entry.getValue();
            List<TestExeCommonDto> sortedList = ReportUtils.getSortedListForTestExeCommon(testExeCommonDtoDiagnoseList);
            double brokenIndex = ReportUtils.brokenIndexForTestExe(testExeCommonDtoDiagnoseList);
            String failingSince = ReportUtils.getFailingSinceForTestExeCommon(sortedList, brokenIndex);
            String lastRunStatus = ReportUtils.lastRunStatusForTestExeCommon(sortedList);
            Long lastPass = ReportUtils.getLastPassForTestExeCommon(sortedList);
            String downTime = ReportUtils.getDownTimeForTestExeCommon(sortedList);
            String averageFixTime;
            if (brokenIndex == 1) {
                averageFixTime = NEVER_FIXED;
            } else if (brokenIndex == 0) {
                averageFixTime = "Never Broken";
            } else {
                averageFixTime = ReportUtils.averageFixTimeForTestExeCommon(testExeCommonDtoDiagnoseList);
            }
            Map<String, Object> temp = new HashMap<>();
            temp.put("TestCase Name",
                    ReportUtils.createCustomObject(testExeCommonDtoDiagnoseList.get(0).getName(), "text",
                            testExeCommonDtoDiagnoseList.get(0).getName(), "left"));
            temp.put("Report Name",
                    ReportUtils.createCustomObject(
                            StringUtils.capitalize(testExeCommonDtoDiagnoseList.get(0).getReport_name()), "text",
                            testExeCommonDtoDiagnoseList.get(0).getReport_name(), "left"));
            temp.put("Project Name",
                    ReportUtils.createCustomObject(
                            StringUtils.capitalize(testExeCommonDtoDiagnoseList.get(0).getProject_name()), "text",
                            testExeCommonDtoDiagnoseList.get(0).getProject_name(), "left"));
            temp.put("Environment",
                    ReportUtils.createCustomObject(StringUtils.capitalize(testExeCommonDtoDiagnoseList.get(0).getEnv()),
                            "text",
                            testExeCommonDtoDiagnoseList.get(0).getEnv(), "left"));
            temp.put("Broken Index", ReportUtils.createCustomObject(brokenIndex, "text", brokenIndex, "center"));
            temp.put("Average Fix Time",
                    ReportUtils.createCustomObject(averageFixTime, "text", averageFixTime, "center"));
            temp.put("Last Run Status",
                    ReportUtils.createCustomObject(lastRunStatus, "status", lastRunStatus, "center"));
            temp.put("P ID",
                    ReportUtils.createCustomObject(testExeCommonDtoDiagnoseList.get(0).getP_id(), "text",
                            testExeCommonDtoDiagnoseList.get(0).getP_id(), "left"));
            if (lastPass > 0) {
                Map<String, Object> timereport = new HashMap<>();
                timereport.put("subType", "datetime");
                temp.put("Last Pass",
                        ReportUtils.createCustomObject(lastPass, "date", lastPass, "center", timereport));
            } else {
                temp.put("Last Pass", ReportUtils.createCustomObject("-", "text", "-", "center"));
            }
            temp.put("Failing Since", ReportUtils.createCustomObject(failingSince, "text", failingSince, "center"));
            temp.put("Downtime", ReportUtils.createCustomObject(downTime, "text", downTime, "center"));
            data.add(temp);
        }

        result.put("data", data);
        if (errors != null) {
            result.put("errors", errors);
        }
        result.put("totalElements", listMap.size());

        return new Response(result, listMap.size() + " Records found", SUCCESS);
    }

    public Response getRuleActionReportV3(String s_run_id, String tc_run_id, HttpServletRequest request, Integer pageNo, Integer sort, String sortedColumn) {
        if (tc_run_id == null) {

            if ((sort != null && sortedColumn == null) || (sort == null && sortedColumn != null)) {
                log.error("Error occurred due to records not found");
                throw new CustomDataException(BOTH_PARAMETERS_REQUIRED, null, FAILURE, HttpStatus.OK);
            }
            if (sort != null && sort != -1 && sort != 0 && sort != 1) {
                log.error("Error occurred due to records not found");
                throw new CustomDataException(INVALID_SORT_VALUE, null, FAILURE, HttpStatus.OK);
            }

            if (pageNo != null && pageNo <= 0) {
                log.error("Error occurred due to records not found");
                throw new CustomDataException(PAGE_NO_CANNOT_BE_NEGATIVE_OR_ZERO, null, FAILURE, HttpStatus.OK);
            }

            Map<String, Object> result = new HashMap<>();
            SuiteExeDto getSuite = RestApiUtils.getSuiteExe(s_run_id);

            if (getSuite == null) {
                log.error("Error occurred due to records not found");
                throw new CustomDataException(SUITE_DETAILS_NOT_FOUND, null, FAILURE, HttpStatus.NOT_FOUND);
            }

            List<VarianceClassificationDto> varianceClassificationList = RestApiUtils.getVarianceClassificationList(getSuite.getVarianceIds(), ACTIVE_STATUS);
            Map<Long, VarianceClassificationDto> varianceList = new HashMap<>();
            List<Long> varinaceIds = new ArrayList<>();
            for (VarianceClassificationDto varianceClassification : varianceClassificationList) {
                varinaceIds.add(varianceClassification.getVarianceId());
                varianceList.put(varianceClassification.getVarianceId(), varianceClassification);
            }

            UserDto user1 = ReportUtils.getUserDtoFromServetRequest();

            ProjectDto project = RestApiUtils.getProjectByPidAndStatus(getSuite.getP_id(), ACTIVE_STATUS);
            if (project == null) {
                log.error("Error occurred due to records not found");
                throw new CustomDataException(PROJECT_NOT_EXISTS, null, FAILURE, HttpStatus.NOT_ACCEPTABLE);
            }
            if (!ReportUtils.validateRoleWithViewerAccess(user1, project)) {
                log.error("Error occurred due to records not found");
                throw new CustomDataException(USER_NOT_ACCESS_TO_PROJECT, null, FAILURE, HttpStatus.NOT_ACCEPTABLE);
            }
            if (getSuite.getStatus().equalsIgnoreCase("EXE")) {
                String expectedStatus = "PASS";
                int current_priority = Integer.MAX_VALUE;
                result.put("status", getSuite.getStatus());

                Map<String, Object> testcaseDetails = new HashMap<>();
                Map<String, List<Map<String, Object>>> statusFilterMap = new HashMap<>();

                List<Map<String, Object>> testcaseDetailsdata = new ArrayList<>();
                Set<String> testcaseDetailsHeaders = new LinkedHashSet<>();

                if (pageNo != null && pageNo <= 0) {
                    log.error("Error occurred due to records not found");
                    throw new CustomDataException(PAGE_NO_CANNOT_BE_NEGATIVE_OR_ZERO, null, FAILURE, HttpStatus.OK);
                }


                Map<String, Object> statusSubType = new HashMap<>();
                statusSubType.put("subType", "falseVariance");
                List<TestExeDto> tempTest = RestApiUtils.getTestExes(s_run_id, pageNo, sort, sortedColumn);
                if (!tempTest.isEmpty()) {
                    Set<String> frameworks = new HashSet<>();
                    List<String> statuses = new ArrayList<>();
                    long testcaseCountWithoutExe = 0L;
                    boolean suiteVarianceIsActive = false;
                    boolean suiteVarianceIsThere = false;
                    boolean suiteFalsePositiveIsActive = false;
                    boolean suiteFalsePositiveIsThere = false;
                    for (TestExeDto testExe : tempTest) {
                        boolean clickable = false;
                        boolean varianceIsActive = false;
                        boolean varianceIsThere = false;
                        boolean falsePositiveIsActive = false;
                        boolean falsePositiveIsThere = false;
                        if (testExe.getVarianceId() != null || (testExe.getStepVarianceIds() != null && !testExe.getStepVarianceIds().isEmpty()) || testExe.getClassificationDetails() != null) {
                            if (testExe.getVarianceId() != null || (testExe.getStepVarianceIds() != null && !testExe.getStepVarianceIds().isEmpty())) {
                                varianceIsThere = true;
                                suiteVarianceIsThere = true;
                                VarianceClassificationDto varianceClassification = varianceList.getOrDefault(testExe.getVarianceId(), null);
                                if (varianceClassification != null) {
                                    varianceIsActive = true;
                                    suiteVarianceIsActive = true;
                                    clickable = true;
                                    testExe.setStatus("PASS");
                                }
                                if (ReportUtils.checkoneListContainsElementOfAnotherList(varinaceIds, testExe.getStepVarianceIds())) {
                                    varianceIsActive = true;
                                    suiteVarianceIsActive = true;
                                    testExe.setStatus(ReportUtils.checkStatusOfTestCaseByStepsIfVarianceIsThere(testExe.getTc_run_id(), varianceList));
                                }
                            }
                            if (testExe.getClassificationDetails() != null) {
                                suiteFalsePositiveIsThere = true;
                                falsePositiveIsThere = true;
                                if (testExe.getClassificationDetails().isFalsePositiveStatus()) {
                                    suiteFalsePositiveIsActive = true;
                                    falsePositiveIsActive = true;
                                    clickable = true;
                                }
                                if (testExe.getClassificationDetails().isChildFalsePostiveStatus()) {
                                    suiteFalsePositiveIsActive = true;
                                    falsePositiveIsActive = true;
                                }
                            }
                        }
                        ObjectMapper oMapper = new ObjectMapper();
                        LinkedHashMap<String, Object> map = oMapper.convertValue(testExe, LinkedHashMap.class);
                        if (testExe.getUser_defined_data() != null) {
                            map.putAll(testExe.getUser_defined_data());
                        }
                        map.remove("user_defined_data");
                        map.remove("steps");
                        map.remove("meta_data");
                        map.remove("ignore");
                        map.remove("log_file");
                        map.remove("result_file");
                        map.remove("s_run_id");
                        map.remove("tc_run_id");
                        testcaseDetailsHeaders.remove("classificationDetails");
                        testcaseDetailsHeaders.remove("stepVarianceIds");
                        testcaseDetailsHeaders.addAll(map.keySet());


                        testcaseDetails.put("headers", testcaseDetailsHeaders);
                        Map<String, Object> temp = new HashMap<>();

                        for (String key : map.keySet()) {

                            if (key.equalsIgnoreCase("start_time") || key.equalsIgnoreCase("end_time")) {
                                Map<String, Object> timereport = new HashMap<>();
                                timereport.put("subType", "datetime");
                                temp.put(ReportUtils.changeKeyValue(key),
                                        ReportUtils.createCustomObject(map.get(key), "date", map.get(key), "center",
                                                timereport));
                            } else if (key.equalsIgnoreCase("status")) {
                                temp.put(ReportUtils.changeKeyValue(key),
                                        ReportUtils.createCustomObject(map.get(key), "crud", map.get(key),
                                                "center", statusSubType));
                            } else {
                                temp.put(ReportUtils.changeKeyValue(key),
                                        ReportUtils.createCustomObject(map.get(key), "text", map.get(key), "left"));

                            }

                        }
                        if (testExe.getStatus().equalsIgnoreCase("FAIL") || testExe.getStatus().equalsIgnoreCase("ERR")) {
                            temp.put("EDIT_ICON", ReportUtils.createCustomObject(ACTIVE_STATUS, "text", ACTIVE_STATUS, "left"));
                        } else {
                            temp.put("EDIT_ICON", ReportUtils.createCustomObject("INACTIVE", "text", "INACTIVE", "left"));
                        }
                        if (varianceIsActive) {
                            temp.put("ISCLICKABLE", ReportUtils.createCustomObject(clickable, "text", clickable, "left"));
                            temp.put("ICON", ReportUtils.createCustomObject("VARIANCE_ACTIVE", "text", "VARIANCE_ACTIVE", "left"));
                        } else if (falsePositiveIsActive) {
                            temp.put("ISCLICKABLE", ReportUtils.createCustomObject(clickable, "text", clickable, "left"));
                            temp.put("ICON", ReportUtils.createCustomObject("FALSE_POSITIVE_ACTIVE", "text", "FALSE_POSITIVE_ACTIVE", "left"));
                            if (testExe.getClassificationDetails().getReason() != null && !testExe.getClassificationDetails().getReason().isEmpty())
                                temp.put("REASON", ReportUtils.createCustomObject(testExe.getClassificationDetails().getReason(), "text", testExe.getClassificationDetails().getReason(), "left"));
                        } else if (varianceIsThere) {
                            temp.put("ICON", ReportUtils.createCustomObject("VARIANCE_INACTIVE", "text", "VARIANCE_INACTIVE", "left"));
                        } else if (falsePositiveIsThere) {
                            temp.put("ICON", ReportUtils.createCustomObject("FALSE_POSITIVE_INACTIVE", "text", "FALSE_POSITIVE_INACTIVE", "left"));
                            if (testExe.getClassificationDetails().getReason() != null && !testExe.getClassificationDetails().getReason().isEmpty())
                                temp.put("REASON", ReportUtils.createCustomObject(testExe.getClassificationDetails().getReason(), "text", testExe.getClassificationDetails().getReason(), "left"));
                        }
                        statuses.add(testExe.getStatus());
                        frameworks.add(testExe.getProduct_type());
                        if (!testExe.getStatus().equalsIgnoreCase("EXE")) {
                            testcaseCountWithoutExe += 1;
                        }

                        temp.put("VARIANCEID", ReportUtils.createCustomObject(testExe.getVarianceId(), "text", testExe.getVarianceId(), "left"));
                        temp.put("TC_RUN_ID", ReportUtils.createCustomObject(testExe.getTc_run_id(), "text", testExe.getTc_run_id(), "left"));
                        temp.remove("STEPVARIANCEIDS");
                        temp.remove("CLASSIFICATIONDETAILS");
                        List<Map<String, Object>> statusMap = statusFilterMap.getOrDefault(testExe.getStatus().toUpperCase(), null);
                        if (statusMap == null) {
                            statusMap = new ArrayList<>();
                        }
                        statusMap.add(temp);
                        statusFilterMap.put(testExe.getStatus().toUpperCase(), statusMap);
                    }
                    if (statusFilterMap.getOrDefault("EXE", null) != null) {
                        testcaseDetailsdata.addAll(statusFilterMap.getOrDefault("EXE", null));
                        statusFilterMap.put("EXE", null);
                    }
                    for (StatusColor statusColor : ReportUtils.getStatusColorInSorted()) {
                        if (statusFilterMap.getOrDefault(statusColor.toString().toUpperCase(), null) != null) {
                            testcaseDetailsdata.addAll(statusFilterMap.getOrDefault(statusColor.toString().toUpperCase(), null));
                        }
                    }
                    testcaseDetailsHeaders.remove("varianceId");
                    testcaseDetails.put("data", testcaseDetailsdata);
                    testcaseDetails.put("headers", testcaseDetailsHeaders);
                    Map<String, Object> iconMap = new HashMap<>();
                    if (suiteVarianceIsActive) {
                        iconMap = ReportUtils.createCustomObject("VARIANCE_ACTIVE", "text", "VARIANCE_ACTIVE", "left");
                    } else if (suiteFalsePositiveIsActive) {
                        iconMap = ReportUtils.createCustomObject("FALSE_POSITIVE_ACTIVE", "text", "FALSE_POSITIVE_ACTIVE", "left");
                    } else if (suiteVarianceIsThere) {
                        iconMap = ReportUtils.createCustomObject("VARIANCE_INACTIVE", "text", "VARIANCE_INACTIVE", "left");
                    } else if (suiteFalsePositiveIsThere) {
                        iconMap = ReportUtils.createCustomObject("FALSE_POSITIVE_INACTIVE", "text", "FALSE_POSITIVE_INACTIVE", "left");
                    }
                    Map<String, Long> testcaseInfo = new TreeMap<String, Long>(Collections.reverseOrder());
                    if (statuses != null) {
                        for (String status : statuses) {
                            testcaseInfo.put(status, ReportUtils.getStatuswiseCount(getSuite.getS_run_id(), status));
                            if (StatusColor.valueOf(status.toUpperCase()).priority < current_priority) {
                                System.out.println(
                                        StatusColor.valueOf(status.toUpperCase()).priority + "-----" + current_priority);
                                expectedStatus = status.toUpperCase();
                                current_priority = StatusColor.valueOf(status.toUpperCase()).priority;
                                System.out.println("------" + StatusColor.valueOf(status.toUpperCase()).priority);
                            }
                        }
                        Long sumOthers = 0L;

                        Iterator<Map.Entry<String, Long>> iterator = testcaseInfo.entrySet().iterator();

                        while (iterator.hasNext()) {

                            Map.Entry<String, Long> entry = iterator.next();

                            if (!(entry.getKey().equalsIgnoreCase("PASS") || entry.getKey().equalsIgnoreCase("FAIL"))) {
                                sumOthers += entry.getValue();
                                iterator.remove();
                            }
                        }

                        testcaseInfo.put("OTHERS", sumOthers);

                    }
                    if (expectedStatus.equalsIgnoreCase("EXE")) {
                        expectedStatus = "PASS";
                    }
                    Map<String, Object> exe_data = new HashMap<>();
                    Map<String, Object> testcase_progress = new HashMap<>();
                    Map<String, Object> expected_status = new HashMap<>();
                    expected_status.put("expected", expectedStatus);
                    expected_status.put("current", getSuite.getStatus());
                    testcase_progress.put("expected",
                            getSuite.getExpected_testcases() != null ? getSuite.getExpected_testcases() : 0);
                    testcase_progress.put("executed", testcaseCountWithoutExe);

                    SuiteRun suiteRunData = RestApiUtils.getSuiteRun(getSuite.getS_run_id());
                    List<List<DependencyTree>> ans = new ArrayList<>();
                    assert suiteRunData != null;
                    for (SuiteRunValues suiteRunValues : suiteRunData.getValues()) {
                        if (suiteRunValues.getExpected_testcases() != null) {
                            ans.addAll(suiteRunValues.getExpected_testcases());
                        }
                    }
                    exe_data.put("testcase_progress", testcase_progress);
                    exe_data.put("expected_status", expected_status);

                    exe_data.put("expected_completion",
                            Math.round(ReportUtils.getTimeRemainingNew(getSuite, ans)));
                    result.put("Infra Headers", ReportUtils.createInfraHeadersData(getSuite));

                    if (statuses.isEmpty()) {
                        testcaseInfo = null;
                    }
                    result.put("Execution Headers", ReportUtils.createExecutionHeadersDataWithVarinceAndFalsePositive(getSuite, iconMap));
                    exe_data.put("testcase_info", testcaseInfo);
                    List<String> columns = RestApiUtils.findColumnMapping(project.getPid(), getSuite.getReport_name(), frameworks.stream().collect(Collectors.toList()));
                    if (columns != null && !columns.isEmpty()) {
                        List<String> headers = new ArrayList<>();
                        for (String header : testcaseDetailsHeaders) {
                            headers.add(header.replace(" ", "_").toLowerCase());
                        }
                        List<String> finalHeaders = new ArrayList<>();
                        for (String column : columns) {
                            String value = column.toLowerCase().replace(" ", "_");
                            if (headers.contains(value)) {
                                finalHeaders.add(value);
                                headers.remove(value);
                            }
                        }
                        testcaseDetails.put("headers", finalHeaders);
                        testcaseDetails.put("filterHeaders", headers);
                        testcaseDetails.replace("headers", ReportUtils.headersDataRefactor((List<String>) testcaseDetails.get("headers")));
                        testcaseDetails.replace("filterHeaders", ReportUtils.headersDataRefactor((List<String>) testcaseDetails.get("filterHeaders")));
                        result.put("exe_data", exe_data);
                        result.put("TestCase_Details", testcaseDetails);
                    } else {
                        List<String> data = new ArrayList<>();
                        data.addAll((Set<String>) testcaseDetails.get("headers"));
                        testcaseDetails.replace("headers", ReportUtils.headersDataRefactor(data));
                        result.put("exe_data", exe_data);
                        result.put("TestCase_Details", testcaseDetails);
                    }
                } else {
                    Map<String, Object> exe_data = new HashMap<>();
                    Map<String, Object> testcase_progress = new HashMap<>();
                    Map<String, Object> expected_status = new HashMap<>();
                    expected_status.put("expected", expectedStatus);
                    expected_status.put("current", getSuite.getStatus());
                    testcase_progress.put("expected",
                            getSuite.getExpected_testcases() != null ? getSuite.getExpected_testcases() : 0);
                    testcase_progress.put("executed",
                            getSuite.getTestcase_details() != null ? getSuite.getTestcase_details().size() : 0);

                    SuiteRun suiteRunData = RestApiUtils.getSuiteRun(getSuite.getS_run_id());
                    List<List<DependencyTree>> ans = new ArrayList<>();
                    for (SuiteRunValues suiteRunValues : suiteRunData.getValues()) {
                        if (suiteRunValues.getExpected_testcases() != null) {
                            ans.addAll(suiteRunValues.getExpected_testcases());
                        }
                    }
                    exe_data.put("testcase_info", null);
                    result.put("Execution Headers", ReportUtils.createExecutionHeadersDataWithVarinceAndFalsePositive(getSuite, null));
                    exe_data.put("testcase_progress", testcase_progress);
                    exe_data.put("expected_status", expected_status);
                    Date unixTime = new Date();
                    exe_data.put("expected_completion",
                            Math.round(ReportUtils.getTimeRemainingNew(getSuite, ans)));
                    result.put("Infra Headers", ReportUtils.createInfraHeadersData(getSuite));
                    testcase_progress.put("executed",
                            testcaseDetailsdata != null ? testcaseDetailsdata.size() : 0);
                    result.put("exe_data", exe_data);
                    result.put("TestCase_Details", null);
                }

                return new Response(result, EXE_REPORT_SUCCESSFULLY_FETCHED, SUCCESS);
            } else {
                Map<String, Object> last5RunsBarGraph = ReportUtils.Last5RunsStackedBarChartBySuiteExe(getSuite);
                if (last5RunsBarGraph != null) {
                    result.put("Last_5_Runs_Bar_Chart", last5RunsBarGraph);
                }
                Map<String, Object> testcaseDetails = new HashMap<>();
                List<Map<String, Object>> testcaseDetailsdata = new ArrayList<>();
                Map<String, List<Map<String, Object>>> statusFilterMap = new HashMap<>();
                Set<String> testcaseDetailsHeaders = new LinkedHashSet<>();

                if (getSuite.getTestcase_details() == null) {
                    log.error("Error occurred due to records not found");
                    throw new CustomDataException(TESTCASE_DETAILS_NOT_FOUND, null, FAILURE, HttpStatus.OK);
                }
                long count = getSuite.getTestcase_details().size();
                if (count == 0) {
                    log.error("Error occurred due to records not found");
                    throw new CustomDataException(TESTCASE_DETAILS_NOT_FOUND_FOR_INTERVAL, null, FAILURE, HttpStatus.OK);
                }
                if (pageNo != null && pageNo <= 0) {
                    log.error("Error occurred due to records not found");
                    throw new CustomDataException(PAGE_NO_CANNOT_BE_NEGATIVE_OR_ZERO, null, FAILURE, HttpStatus.OK);
                }

                List<TestExeDto> tempTest = RestApiUtils.getTestExes(s_run_id, pageNo, sort, sortedColumn);
                if (tempTest.isEmpty()) {
                    log.error("Error occurred due to records not found");
                    throw new CustomDataException(TESTCASE_DETAILS_NOT_FOUND, null, FAILURE, HttpStatus.OK);
                }

                Set<String> frameworks = new HashSet<>();
                Set<String> category = new HashSet<>();
                Map<String, Long> categoryMap = new HashMap<>();
                List<String> statues = new ArrayList<>();
                boolean suiteVarianceIsActive = false;
                boolean suiteVarianceIsThere = false;
                boolean suiteFalsePositiveIsActive = false;
                boolean suiteFalsePositiveIsThere = false;
                HashSet<String> statusesSet = new HashSet<>();
                Map<String, Object> statusSubType = new HashMap<>();
                statusSubType.put("subType", "falseVariance");
                for (TestExeDto testExe : tempTest) {
                    boolean clickable = false;
                    boolean varianceIsActive = false;
                    boolean varianceIsThere = false;
                    boolean falsePositiveIsActive = false;
                    boolean falsePositiveIsThere = false;
                    if (testExe.getVarianceId() != null || (testExe.getStepVarianceIds() != null && testExe.getStepVarianceIds().size() > 0) || testExe.getClassificationDetails() != null) {
                        if (testExe.getVarianceId() != null || (testExe.getStepVarianceIds() != null && testExe.getStepVarianceIds().size() > 0)) {
                            varianceIsThere = true;
                            suiteVarianceIsThere = true;
                            VarianceClassificationDto varianceClassification = varianceList.getOrDefault(testExe.getVarianceId(), null);
                            if (varianceClassification != null) {
                                varianceIsActive = true;
                                suiteVarianceIsActive = true;
                                clickable = true;
                                testExe.setStatus("PASS");
                            }
                            if (ReportUtils.checkoneListContainsElementOfAnotherList(varinaceIds, testExe.getStepVarianceIds())) {
                                varianceIsActive = true;
                                suiteVarianceIsActive = true;
                                testExe.setStatus(ReportUtils.checkStatusOfTestCaseByStepsIfVarianceIsThere(testExe.getTc_run_id(), varianceList));
                            }
                        }
                        if (testExe.getClassificationDetails() != null) {
                            suiteFalsePositiveIsThere = true;
                            falsePositiveIsThere = true;
                            if (testExe.getClassificationDetails().isFalsePositiveStatus()) {
                                suiteFalsePositiveIsActive = true;
                                falsePositiveIsActive = true;
                                clickable = true;
                            }
                            if (testExe.getClassificationDetails().isChildFalsePostiveStatus()) {
                                suiteFalsePositiveIsActive = true;
                                falsePositiveIsActive = true;
                            }
                        }
                    }
                    ObjectMapper oMapper = new ObjectMapper();
                    LinkedHashMap<String, Object> map = oMapper.convertValue(testExe, LinkedHashMap.class);
                    if (testExe.getUser_defined_data() != null) {
                        map.putAll(testExe.getUser_defined_data());
                    }
                    map.remove("user_defined_data");
                    map.remove("steps");
                    map.remove("meta_data");
                    map.remove("ignore");
                    map.remove("log_file");
                    map.remove("result_file");

                    map.remove("tc_run_id");
                    map.remove("s_run_id");

                    map.remove("classificationDetails");
                    map.remove("stepVarianceIds");
                    testcaseDetailsHeaders.addAll(map.keySet());
                    Map<String, Object> temp = new HashMap<String, Object>();
                    for (String key : map.keySet()) {

                        if (key.equalsIgnoreCase("start_time") || key.equalsIgnoreCase("end_time")) {
                            Map<String, Object> timereport = new HashMap<>();
                            timereport.put("subType", "datetime");
                            temp.put(ReportUtils.changeKeyValue(key),
                                    ReportUtils.createCustomObject(map.get(key), "date", map.get(key), "center",
                                            timereport));
                        } else if (key.equalsIgnoreCase("status")) {
                            temp.put(ReportUtils.changeKeyValue(key),
                                    ReportUtils.createCustomObject(map.get(key), "crud", map.get(key), "center", statusSubType));
                        } else {
                            temp.put(ReportUtils.changeKeyValue(key),
                                    ReportUtils.createCustomObject(map.get(key), "text", map.get(key), "left"));

                        }

                    }
                    if (testExe.getStatus().equalsIgnoreCase("FAIL") || testExe.getStatus().equalsIgnoreCase("ERR")) {
                        temp.put("EDIT_ICON", ReportUtils.createCustomObject(ACTIVE_STATUS, "text", ACTIVE_STATUS, "left"));
                    } else {
                        temp.put("EDIT_ICON", ReportUtils.createCustomObject("INACTIVE", "text", "INACTIVE", "left"));
                    }
                    if (varianceIsActive) {
                        temp.put("ISCLICKABLE", ReportUtils.createCustomObject(clickable, "text", clickable, "left"));
                        temp.put("ICON", ReportUtils.createCustomObject("VARIANCE_ACTIVE", "text", "VARIANCE_ACTIVE", "left"));
                    } else if (falsePositiveIsActive) {
                        temp.put("ISCLICKABLE", ReportUtils.createCustomObject(clickable, "text", clickable, "left"));
                        temp.put("ICON", ReportUtils.createCustomObject("FALSE_POSITIVE_ACTIVE", "text", "FALSE_POSITIVE_ACTIVE", "left"));
                        if (testExe.getClassificationDetails().getReason() != null && !testExe.getClassificationDetails().getReason().isEmpty())
                            temp.put("REASON", ReportUtils.createCustomObject(testExe.getClassificationDetails().getReason(), "text", testExe.getClassificationDetails().getReason(), "left"));
                    } else if (varianceIsThere) {
                        temp.put("ICON", ReportUtils.createCustomObject("VARIANCE_INACTIVE", "text", "VARIANCE_INACTIVE", "left"));
                    } else if (falsePositiveIsThere) {
                        temp.put("ICON", ReportUtils.createCustomObject("FALSE_POSITIVE_INACTIVE", "text", "FALSE_POSITIVE_INACTIVE", "left"));
                        if (testExe.getClassificationDetails().getReason() != null && !testExe.getClassificationDetails().getReason().isEmpty())
                            temp.put("REASON", ReportUtils.createCustomObject(testExe.getClassificationDetails().getReason(), "text", testExe.getClassificationDetails().getReason(), "left"));
                    }
                    if (testExe.getCategory() != null && testExe.getCategory().getClass().isArray()) {
                        List<String> categories = Arrays.asList((String[]) testExe.getCategory());
                        for (String data : categories) {
                            String category1 = data;
                            category.add(category1);
                            categoryMap.put(category1.toUpperCase() + "_" + testExe.getStatus(),
                                    categoryMap.getOrDefault(category1.toUpperCase() + "_" + testExe.getStatus(), 0L) + 1);
                        }
                    } else if (testExe.getCategory() != null) {
                        String category1 = (String) testExe.getCategory();
                        category.add(category1);
                        categoryMap.put(category1.toUpperCase() + "_" + testExe.getStatus(),
                                categoryMap.getOrDefault(category1.toUpperCase() + "_" + testExe.getStatus(), 0L) + 1);
                    }
                    frameworks.add(testExe.getProduct_type());
                    statusesSet.add(testExe.getStatus());
                    statues.add(testExe.getStatus());
                    temp.put("TC_RUN_ID", ReportUtils.createCustomObject(testExe.getTc_run_id(), "text", testExe.getTc_run_id(), "left"));
                    temp.put("VARIANCEID", ReportUtils.createCustomObject(testExe.getVarianceId(), "text", testExe.getVarianceId(), "left"));
                    temp.remove("STEPVARIANCEIDS");
                    temp.remove("CLASSIFICATIONDETAILS");

                    List<Map<String, Object>> statusMap = statusFilterMap.getOrDefault(testExe.getStatus().toUpperCase(), null);
                    if (statusMap == null) {
                        statusMap = new ArrayList<>();
                    }
                    statusMap.add(temp);
                    statusFilterMap.put(testExe.getStatus().toUpperCase(), statusMap);

                }

                for (StatusColor statusColor : ReportUtils.getStatusColorInSorted()) {
                    if (statusFilterMap.getOrDefault(statusColor.toString().toUpperCase(), null) != null) {
                        testcaseDetailsdata.addAll(statusFilterMap.getOrDefault(statusColor.toString().toUpperCase(), null));
                    }
                }
                testcaseDetails.put("data", testcaseDetailsdata);
                Map<String, Object> iconMap = new HashMap<>();
                if (suiteVarianceIsActive) {
                    iconMap = ReportUtils.createCustomObject("VARIANCE_ACTIVE", "text", "VARIANCE_ACTIVE", "left");
                } else if (suiteFalsePositiveIsActive) {
                    iconMap = ReportUtils.createCustomObject("FALSE_POSITIVE_ACTIVE", "text", "FALSE_POSITIVE_ACTIVE", "left");
                } else if (suiteVarianceIsThere) {
                    iconMap = ReportUtils.createCustomObject("VARIANCE_INACTIVE", "text", "VARIANCE_INACTIVE", "left");
                } else if (suiteFalsePositiveIsThere) {
                    iconMap = ReportUtils.createCustomObject("FALSE_POSITIVE_INACTIVE", "text", "FALSE_POSITIVE_INACTIVE", "left");
                }
                String finalStatus = "";
                int priority = Integer.MAX_VALUE;
                for (String status : statusesSet) {
                    if (StatusColor.valueOf(status.toUpperCase()).priority < priority) {
                        priority = StatusColor.valueOf(status.toUpperCase()).priority;
                    }
                }
                for (StatusColor val : StatusColor.values()) {
                    if (val.priority == priority) {
                        finalStatus = val.name();
                    }
                }

                testcaseDetailsHeaders.remove("varianceId");
                testcaseDetails.put("headers", testcaseDetailsHeaders);
                getSuite.setStatus(finalStatus);
                Map<String, Object> testCaseInfo = ReportUtils.testCaseInfoDoughnutChart(statues);
                if (testCaseInfo != null) {
                    result.put("Testcase Info", testCaseInfo);
                }
                Map<String, Object> CategoryBarChart = ReportUtils.categoryStackedBarChartByS_run_id(categoryMap, category);
                if (CategoryBarChart != null) {
                    result.put("Category_Bar_Chart", CategoryBarChart);
                }
                result.put("Execution Headers", ReportUtils.createExecutionHeadersDataWithVarinceAndFalsePositive(getSuite, iconMap));
                result.put("Infra Headers", ReportUtils.createInfraHeadersData(getSuite));
                result.put("status", getSuite.getStatus());
                List<String> columns = RestApiUtils.findColumnMapping(project.getPid(), getSuite.getReport_name(), frameworks.stream().collect(Collectors.toList()));
                if (columns != null && columns.size() > 0) {
                    List<String> headers = new ArrayList<>();
                    for (String header : testcaseDetailsHeaders) {
                        headers.add(header.replace(" ", "_").toLowerCase());
                    }
                    List<String> finalHeaders = new ArrayList<>();
                    for (String column : columns) {
                        String value = column.toLowerCase().replace(" ", "_");
                        if (headers.contains(value)) {
                            finalHeaders.add(value);
                            headers.remove(value);
                        }
                    }
                    testcaseDetails.put("headers", finalHeaders);
                    testcaseDetails.put("filterHeaders", headers);
                    testcaseDetails.replace("headers", ReportUtils.headersDataRefactor((List<String>) testcaseDetails.get("headers")));
                    testcaseDetails.replace("filterHeaders", ReportUtils.headersDataRefactor((List<String>) testcaseDetails.get("filterHeaders")));
                    result.put("TestCase_Details", testcaseDetails);
                    result.put("totalElements", getSuite.getTestcase_details().size());

                    return new Response(result, DATA_FETCHED_SUCCESSFULLY, SUCCESS);
                }
                List<String> data = new ArrayList<String>();
                data.addAll((Set<String>) testcaseDetails.get("headers"));
                testcaseDetails.replace("headers", ReportUtils.headersDataRefactor(data));
                result.put("TestCase_Details", testcaseDetails);
                result.put("totalElements", getSuite.getTestcase_details().size());

                return new Response(result, DATA_FETCHED_SUCCESSFULLY, SUCCESS);
            }

        } else {
            ObjectMapper oMapper = new ObjectMapper();
            Map<String, Object> stepData = new HashMap<>();
            Set<String> stepsListHeaders = new HashSet<>();
            List<Map<String, Object>> stepsVariableValue = new ArrayList<>();

            TestExeDto tempTest = RestApiUtils.getTestExe(tc_run_id);
            if (tempTest == null) {
                log.error("Error occurred due to records not found");
                throw new CustomDataException(TESTCASE_DETAILS_NOT_FOUND, null, FAILURE, HttpStatus.OK);
            }

            String username = ReportUtils.getUserDtoFromServetRequest().getUsername();

            UserDto user = ReportUtils.getUserDtoFromServetRequest();

            SuiteExeDto getSuite = RestApiUtils.getSuiteExe(tempTest.getS_run_id());
            if (getSuite == null) {
                log.error("Error occurred due to records not found");
                throw new CustomDataException(SUITE_DETAILS_NOT_FOUND, null, FAILURE, HttpStatus.NOT_FOUND);
            }
            ProjectDto project = RestApiUtils.getProjectByPidAndStatus(getSuite.getP_id(), ACTIVE_STATUS);

            if (project == null) {
                log.error("Error occurred due to records not found");
                throw new CustomDataException(PROJECT_NOT_EXISTS, null, FAILURE, HttpStatus.NOT_ACCEPTABLE);
            }

            ProjectRoleDto currentProject = RestApiUtils.getProjectRoleByPidAndUsername(getSuite.getP_id(), username);

            if (currentProject == null && !((user.getRole().equalsIgnoreCase(ADMIN.toString()) && project.getRealcompanyname().equalsIgnoreCase(user.getRealCompany())) || user.getRole().equalsIgnoreCase(SUPER_ADMIN.toString()))) {
                log.error("Error occurred due to records not found");
                throw new CustomDataException(USER_NOT_ACCESS_TO_PROJECT, null, OperationType.INFO, HttpStatus.NOT_ACCEPTABLE, REQUEST_ACCESS);
            }

            List<VarianceClassificationDto> varianceClassificationList = RestApiUtils.getVarianceClassificationList(getSuite.getVarianceIds(), ACTIVE_STATUS);
            Map<Long, VarianceClassificationDto> variannceList = new HashMap<>();
            List<Long> varinaceIds = new ArrayList<>();
            for (VarianceClassificationDto varianceClassification : varianceClassificationList) {
                varinaceIds.add(varianceClassification.getVarianceId());
                variannceList.put(varianceClassification.getVarianceId(), varianceClassification);
            }
            boolean varianceIsActiveAtTestLevel = false;
            boolean falsePositiveIsActiveAtTestLevel = false;
            boolean varianceIsThereAtTestLevel = false;
            boolean falsePositiveIsThereAtTestLevel = false;
            String statusTestLevel = null;
            if (tempTest.getVarianceId() != null || tempTest.getClassificationDetails() != null) {
                if (tempTest.getVarianceId() != null) {
                    varianceIsThereAtTestLevel = true;
                }
                VarianceClassificationDto varianceClassification = variannceList.getOrDefault(tempTest.getVarianceId(), null);
                if (varianceClassification != null) {
                    varianceIsActiveAtTestLevel = true;
                    statusTestLevel = "PASS";
                }
                if (tempTest.getClassificationDetails() != null) {
                    falsePositiveIsThereAtTestLevel = true;
                    if (tempTest.getClassificationDetails().isFalsePositiveStatus()) {
                        falsePositiveIsActiveAtTestLevel = true;
                        statusTestLevel = "PASS";
                    }
                }
            }
            Map<String, Object> statusSubType = new HashMap<>();
            statusSubType.put("subType", "falseVariance");
            List<Map<String, Object>> gallery = new ArrayList<>();
            StepsDto steps = RestApiUtils.getSteps(tc_run_id);
            if (steps != null) {
                List<String> statuesList = new ArrayList<>();
                for (Object step : steps.getSteps()) {
                    boolean clickable = false;
                    boolean varianceIsActive = varianceIsActiveAtTestLevel;
                    boolean varianceIsThere = varianceIsThereAtTestLevel;
                    boolean falsePositiveIsActive = falsePositiveIsActiveAtTestLevel;
                    boolean falsePositiveIsThere = falsePositiveIsThereAtTestLevel;
                    Map<String, Object> stepMap = oMapper.convertValue(step, Map.class);
                    stepsListHeaders.addAll(stepMap.keySet());
                    stepsListHeaders.remove("tc_run_id");
                    stepsListHeaders.remove("s_run_id");
                    Map<String, Object> temp = new HashMap<String, Object>();
                    for (String key : stepsListHeaders) {
                        String status = statusTestLevel;
                        String subStepStatus = statusTestLevel;
                        ClassificationDetails classificationDetails = null;
                        if (stepsListHeaders.contains("VARIANCEID")) {
                            Long varianceId = (Long) stepMap.get("VARIANCEID");
                            varianceIsThere = true;
                            VarianceClassificationDto varianceClassification = variannceList.getOrDefault(varianceId, null);
                            if (varianceClassification != null) {
                                clickable = true;
                                varianceIsActive = true;
                                status = "PASS";
                                subStepStatus = "PASS";
                            }
                        }
                        if (stepsListHeaders.contains("CLASSIFICATIONDETAILS")) {
                            falsePositiveIsThere = true;
                            classificationDetails = oMapper.convertValue(stepMap.get("CLASSIFICATIONDETAILS"), ClassificationDetails.class);
                            if (classificationDetails != null && classificationDetails.isFalsePositiveStatus()) {
                                clickable = true;
                                falsePositiveIsActive = true;
                                subStepStatus = "Pass";
                            }
                        }
                        if (key.equalsIgnoreCase("sub_step")) {
                            if (stepMap.get("sub_step") != null) {
                                List<Map<String, Object>> subStepsVariableValue = new ArrayList<Map<String, Object>>();
                                List<Map<String, Object>> maps = (List<Map<String, Object>>) stepMap.get(key);
                                Set<String> subStepsHeaders = new HashSet<String>();
                                for (Map map1 : maps) {
                                    Map<String, Object> subStepsTemp = new HashMap<String, Object>();
                                    subStepsHeaders.addAll(map1.keySet());
                                    for (String key2 : subStepsHeaders) {
                                        if (key2.equalsIgnoreCase("start_time") || key2.equalsIgnoreCase("end_time")) {
                                            Map<String, Object> timereport = new HashMap<>();
                                            timereport.put("subType", "datetime");
                                            subStepsTemp.put(ReportUtils.changeKeyValue(key2),
                                                    ReportUtils.createCustomObject(map1.get(key2), "date", map1.get(key2),
                                                            "center", timereport));
                                        } else if (key2.equalsIgnoreCase("status")) {
                                            if (subStepStatus != null) {
                                                subStepsTemp.put(ReportUtils.changeKeyValue(key2),
                                                        ReportUtils.createCustomObject(subStepStatus, "crud", subStepStatus,
                                                                "center", statusSubType));
                                            } else {
                                                subStepsTemp.put(ReportUtils.changeKeyValue(key2),
                                                        ReportUtils.createCustomObject(map1.get(key2), "crud", map1.get(key2),
                                                                "center", statusSubType));
                                            }
                                            subStepsTemp.put("EDIT_ICON", ReportUtils.createCustomObject("INACTIVE", "text", "INACTIVE", "left"));
                                            if (map1.get(key2) != null && (map1.get(key2).toString().equalsIgnoreCase("ERR") || map1.get(key2).toString().equalsIgnoreCase("FAIL")) || (classificationDetails != null && classificationDetails.isFalsePositiveStatus())) {
                                                if (varianceIsActive) {
                                                    subStepsTemp.put("ISCLICKABLE", ReportUtils.createCustomObject(false, "text", false, "left"));
                                                    subStepsTemp.put("ICON", ReportUtils.createCustomObject("VARIANCE_ACTIVE", "text", "VARIANCE_ACTIVE", "left"));
                                                } else if (falsePositiveIsActive && classificationDetails != null) {
                                                    subStepsTemp.put("ISCLICKABLE", ReportUtils.createCustomObject(false, "text", false, "left"));
                                                    subStepsTemp.put("ICON", ReportUtils.createCustomObject("FALSE_POSITIVE_ACTIVE", "text", "FALSE_POSITIVE_ACTIVE", "left"));
                                                    if (classificationDetails != null && classificationDetails.getReason() != null && !classificationDetails.getReason().isEmpty())
                                                        subStepsTemp.put("REASON", ReportUtils.createCustomObject(classificationDetails.getReason(), "text", classificationDetails.getReason(), "left"));
                                                } else if (varianceIsThere) {
                                                    subStepsTemp.put("ICON", ReportUtils.createCustomObject("VARIANCE_INACTIVE", "text", "VARIANCE_INACTIVE", "left"));
                                                } else if (falsePositiveIsThere && classificationDetails != null) {
                                                    subStepsTemp.put("ICON", ReportUtils.createCustomObject("FALSE_POSITIVE_INACTIVE", "text", "FALSE_POSITIVE_INACTIVE", "left"));
                                                    if (classificationDetails != null && classificationDetails.getReason() != null && !classificationDetails.getReason().isEmpty())
                                                        subStepsTemp.put("REASON", ReportUtils.createCustomObject(classificationDetails.getReason(), "text", classificationDetails.getReason(), "left"));
                                                }
                                            }

                                        } else if (key2.equalsIgnoreCase("screenshot")) {
                                            subStepsTemp.put(ReportUtils.changeKeyValue(key2),
                                                    ReportUtils.createCustomObject(map1.get(key2), "image",
                                                            map1.get(key2),
                                                            "center"));
                                            Map<String, Object> screenshot = new HashMap<>();
                                            if (map1.get(key2) != null) {
                                                if (map1.get("step name") != null) {
                                                    screenshot.put(map1.get("step name").toString(), (map1.get(key2)));
                                                } else {
                                                    screenshot.put(map1.get("title").toString(), (map1.get(key2)));
                                                }
                                                gallery.add(screenshot);
                                            }

                                        } else {
                                            subStepsTemp.put(ReportUtils.changeKeyValue(key2),
                                                    ReportUtils.createCustomObject(map1.get(key2), "text", map1.get(key2),
                                                            "left"));
                                        }

                                    }
                                    subStepsVariableValue.add(subStepsTemp);
                                }
                                Map<String, Object> subStepsData = new HashMap<>();
                                subStepsData.put("data", subStepsVariableValue);
                                subStepsData.put("headers", ReportUtils.headersDataStepRefactor(subStepsHeaders));
                                temp.put("SUB_STEPS", subStepsData);
                            }
                            continue;
                        }
                        if (key.equalsIgnoreCase("start_time") || key.equalsIgnoreCase("end_time")) {
                            Map<String, Object> timereport = new HashMap<>();
                            timereport.put("subType", "datetime");
                            temp.put(ReportUtils.changeKeyValue(key),
                                    ReportUtils.createCustomObject(stepMap.get(key), "date", stepMap.get(key),
                                            "center", timereport));
                        } else if (key.equalsIgnoreCase("status")) {
                            if (status != null) {
                                statuesList.add(status);
                                temp.put(ReportUtils.changeKeyValue(key),
                                        ReportUtils.createCustomObject(status, "status", status,
                                                "center"));

                                if (status.equalsIgnoreCase("FAIL") || status.equalsIgnoreCase("ERR") && !varianceIsActive && !falsePositiveIsActive) {
                                    temp.put("EDIT_ICON", ReportUtils.createCustomObject(ACTIVE_STATUS, "text", ACTIVE_STATUS, "left"));
                                } else {
                                    temp.put("EDIT_ICON", ReportUtils.createCustomObject("INACTIVE", "text", "INACTIVE", "left"));
                                }
                            } else {
                                String stepStatus = (String) stepMap.get(key);
                                statuesList.add(stepStatus);
                                temp.put(ReportUtils.changeKeyValue(key),
                                        ReportUtils.createCustomObject(stepStatus, "status", stepStatus,
                                                "center"));

                                if (stepStatus.equalsIgnoreCase("FAIL") || stepStatus.equalsIgnoreCase("ERR") && !varianceIsActive && !falsePositiveIsActive) {
                                    temp.put("EDIT_ICON", ReportUtils.createCustomObject(ACTIVE_STATUS, "text", ACTIVE_STATUS, "left"));
                                } else {
                                    temp.put("EDIT_ICON", ReportUtils.createCustomObject("INACTIVE", "text", "INACTIVE", "left"));
                                }
                            }

                            if ((stepMap.get(key).toString() != null && (stepMap.get(key).toString().equalsIgnoreCase("ERR") || stepMap.get(key).toString().equalsIgnoreCase("FAIL"))) || (classificationDetails != null && classificationDetails.isFalsePositiveStatus())) {
                                if (varianceIsActive) {
                                    temp.put("ISCLICKABLE", ReportUtils.createCustomObject(clickable, "text", clickable, "left"));
                                    temp.put("ICON", ReportUtils.createCustomObject("VARIANCE_ACTIVE", "text", "VARIANCE_ACTIVE", "left"));
                                } else if (falsePositiveIsActive && classificationDetails != null && classificationDetails.isFalsePositiveStatus()) {
                                    temp.put("ISCLICKABLE", ReportUtils.createCustomObject(clickable, "text", clickable, "left"));
                                    temp.put("ICON", ReportUtils.createCustomObject("FALSE_POSITIVE_ACTIVE", "text", "FALSE_POSITIVE_ACTIVE", "left"));
                                    if (classificationDetails != null && classificationDetails.getReason() != null && !classificationDetails.getReason().isEmpty())
                                        temp.put("REASON", ReportUtils.createCustomObject(classificationDetails.getReason(), "text", classificationDetails.getReason(), "left"));
                                } else if (varianceIsThere) {
                                    temp.put("ICON", ReportUtils.createCustomObject("VARIANCE_INACTIVE", "text", "VARIANCE_INACTIVE", "left"));
                                } else if (falsePositiveIsThere && classificationDetails != null) {
                                    temp.put("ICON", ReportUtils.createCustomObject("FALSE_POSITIVE_INACTIVE", "text", "FALSE_POSITIVE_INACTIVE", "left"));
                                    if (classificationDetails != null && classificationDetails.getReason() != null && !classificationDetails.getReason().isEmpty())
                                        temp.put("REASON", ReportUtils.createCustomObject(classificationDetails.getReason(), "text", classificationDetails.getReason(), "left"));
                                }
                            }
                        } else if (key.equalsIgnoreCase("screenshot")) {
                            temp.put(ReportUtils.changeKeyValue(key),
                                    ReportUtils.createCustomObject(stepMap.get(key), "image",
                                            stepMap.get(key),
                                            "center"));
                            Map<String, Object> screenshot = new HashMap<>();
                            if (stepMap.get(key) != null) {
                                if (stepMap.get("step name") != null) {
                                    screenshot.put(stepMap.get("step name").toString(), (stepMap.get(key)));
                                } else {
                                    screenshot.put(stepMap.get("title").toString(), (stepMap.get(key)));
                                }
                                gallery.add(screenshot);
                            }
                        } else {
                            temp.put(ReportUtils.changeKeyValue(key),
                                    ReportUtils.createCustomObject(stepMap.get(key), "text", stepMap.get(key),
                                            "left"));
                        }
                    }
                    temp.put("PRODUCT TYPE", ReportUtils.createCustomObject(tempTest.getProduct_type(), "text", tempTest.getProduct_type(),
                            "left"));
                    temp.remove("CLASSIFICATIONDETAILS");
                    stepsVariableValue.add(temp);
                }
                Map<String, Object> testcase_info = new HashMap<>();
                for (String status : statuesList) {

                    testcase_info.put(status.toUpperCase(),
                            Long.valueOf(Long.valueOf(testcase_info.getOrDefault(status.toUpperCase(), 0L).toString()) + 1));

                }
                if (testcase_info.size() != 0) {
                    testcase_info.put("TOTAL", Long.valueOf(statuesList.size()));
                }
                if (tempTest.getMeta_data() != null && tempTest.getMeta_data().size() >= 3) {
                    tempTest.getMeta_data().set(2, testcase_info);
                }
            }

            stepsListHeaders.remove("sub_step");
            stepsListHeaders.remove("tc_run_id");
            stepsListHeaders.remove("s_run_id");
            stepsListHeaders.remove("CLASSIFICATIONDETAILS");
            stepsListHeaders.remove("VARIANCEID");
            stepData.put("headers", ReportUtils.headersDataStepRefactor(stepsListHeaders));
            stepData.put("metaData", tempTest.getMeta_data());
            stepData.put("gallery", gallery);
            stepData.put("data", stepsVariableValue);
            stepData.put("tc_run_id", tc_run_id);

            return new Response(stepData, "", SUCCESS);

        }
    }

    public Response updateBuildDetails(String s_run_id, String buildId, String sprint_name) {

        UserDto user = ReportUtils.getUserDtoFromServetRequest();
        ProjectDto project;
        SuiteExeDto suiteExeDto = RestApiUtils.getSuiteExe(s_run_id);

        if (suiteExeDto != null && user.getRole().equalsIgnoreCase(SUPER_ADMIN.toString())) {
            project = RestApiUtils.getProjectByPidAndStatus(suiteExeDto.getP_id(), ACTIVE_STATUS);
        } else {
            project = RestApiUtils.getProjectByRealCompanyNameAndProjectAndStatus(
                    user.getRealCompany(), suiteExeDto.getProject_name(), ACTIVE_STATUS);
        }
        if (project == null && !user.getRole().equalsIgnoreCase(SUPER_ADMIN.toString())) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(PROJECT_NOT_EXISTS, null, FAILURE, HttpStatus.NOT_ACCEPTABLE);
        }

        ProjectRoleDto projectRole = RestApiUtils.getProjectRoleEntity(project.getPid(), user.getUsername(), ACTIVE_STATUS);

        if (projectRole == null &&
                !((user.getRole().equalsIgnoreCase(ADMIN.toString())
                        && project.getRealcompanyname().equalsIgnoreCase(user.getRealCompany()))
                        || user.getRole().equalsIgnoreCase(SUPER_ADMIN.toString()))) {

            log.error("Error occurred due to records not found");
            throw new CustomDataException(USER_NOT_ACCESS_TO_INSERT_DATA, null, FAILURE, HttpStatus.NOT_ACCEPTABLE);
        } else if (projectRole != null && !projectRole.getRole().equalsIgnoreCase("ADMIN")) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(USER_NOT_ADMIN_ACCESS_TO_PROJECT, null, FAILURE, HttpStatus.NOT_ACCEPTABLE);
        } else {

            if (buildId != null || sprint_name != null) {

                if (buildId != null && !buildId.trim().isEmpty()) {
                    suiteExeDto.setBuild_id(buildId);

                }
                if (sprint_name != null && !sprint_name.trim().isEmpty()) {
                    suiteExeDto.setSprint_name(sprint_name);
                }
            } else {
                log.error("Error occurred due to records not found");
                throw new CustomDataException(REQUIRED_FIELDS_CANNOT_BE_NULL, null, FAILURE, HttpStatus.BAD_REQUEST);
            }
            RestApiUtils.updateSuiteExe(s_run_id, suiteExeDto);
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put(s_run_id, "Updated");
            simpMessagingTemplate.convertAndSendToUser(String.valueOf(project.getPid()), "/private", messageMap);

            return new Response(null, REPORT_UPDATED_SUCCESSFULLY, SUCCESS);
        }
    }

    public Response getBuildDetails(String s_run_id, HttpServletRequest request) {

        UserDto user = ReportUtils.getUserDtoFromServetRequest();
        ProjectDto project;
        SuiteExeDto suiteExeDto = RestApiUtils.getSuiteExe(s_run_id);

        if (suiteExeDto != null && user.getRole().equalsIgnoreCase(SUPER_ADMIN.toString())) {
            project = RestApiUtils.getProjectByPidAndStatus(suiteExeDto.getP_id(), ACTIVE_STATUS);
        } else {
            project = RestApiUtils.getProjectByRealCompanyNameAndProjectAndStatus(
                    user.getRealCompany(), suiteExeDto.getProject_name(), ACTIVE_STATUS);
        }
        if (project == null && !user.getRole().equalsIgnoreCase(SUPER_ADMIN.toString())) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(PROJECT_NOT_EXISTS, null, FAILURE, HttpStatus.NOT_ACCEPTABLE);
        }

        ProjectRoleDto projectRole = RestApiUtils.getProjectRoleEntity(project.getPid(), user.getUsername(), ACTIVE_STATUS);

        if (projectRole == null &&
                !((user.getRole().equalsIgnoreCase(ADMIN.toString())
                        && project.getRealcompanyname().equalsIgnoreCase(user.getRealCompany()))
                        || user.getRole().equalsIgnoreCase(SUPER_ADMIN.toString()))) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(USER_NOT_ACCESS_TO_INSERT_DATA, null, FAILURE, HttpStatus.NOT_ACCEPTABLE);
        } else if (projectRole != null && !projectRole.getRole().equalsIgnoreCase("ADMIN")) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(USER_NOT_ADMIN_ACCESS_TO_PROJECT, null, FAILURE, HttpStatus.NOT_ACCEPTABLE);
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("Build ID", suiteExeDto.getBuild_id());
            data.put("Sprint Name", suiteExeDto.getSprint_name());

            return new Response(data, DETAILS_FETCHED_SUCCESSFULLY, SUCCESS);
        }

    }

    public Response getSuiteTimeline(Map<String, Object> payload, HttpServletRequest request, String category, String search, Integer pageNo, Integer sort, String sortedColumn) throws ParseException {

        UserDto user1 = ReportUtils.getUserDtoFromServetRequest();

        SuiteExeDto getSuite = RestApiUtils.getSuiteExe((String) payload.get("s_run_id"));

        if (getSuite == null) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(SUITE_DETAILS_NOT_FOUND, null, FAILURE, HttpStatus.NOT_FOUND);
        }

        ProjectDto project = RestApiUtils.getProjectByPidAndStatus(getSuite.getP_id(), ACTIVE_STATUS);
        if (project == null) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(PROJECT_NOT_EXISTS, null, FAILURE, HttpStatus.NOT_ACCEPTABLE);

        }
        if (!ReportUtils.validateRoleWithViewerAccess(user1, project)) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(USER_NOT_ACCESS_TO_PROJECT, null, OperationType.INFO, HttpStatus.NOT_ACCEPTABLE, REQUEST_ACCESS);
        }

        Map<String, Object> result = new HashMap<>();
        List<Object> headers = new ArrayList<>();
        Collections.addAll(headers, "Start Time", "Status", "Action", "Testcases", "Run Type", "Run Mode", "Token User", "Base User");
        result.put("headers", headers);
        List<Map<String, Object>> data = new ArrayList<>();
        long starttime = new SimpleDateFormat("MM/dd/yyyy").parse(payload.get("start_time").toString()).getTime();
        long endtime = new SimpleDateFormat("MM/dd/yyyy").parse((String) payload.get("end_time")).getTime() + (1000 * 60 * 60 * 24);


        if (pageNo != null && pageNo <= 0) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(PAGE_NO_CANNOT_BE_NEGATIVE_OR_ZERO, null, FAILURE, HttpStatus.OK);
        }

        List<SuiteExeDto> suiteReports = RestApiUtils.getSuiteExesForSuiteTimeline(getSuite.getP_id(), category, getSuite.getEnv(), getSuite.getReport_name(), starttime, endtime, pageNo, sort, sortedColumn);
        if (suiteReports.isEmpty()) {
            result.put("data", data);
            return new Response(result, NO_RECORDS_FOUND, SUCCESS);
        }
        List<String> sRunIds = RestApiUtils.getS_Run_IdsForSuiteTimeline(getSuite.getP_id(), category, getSuite.getEnv(), getSuite.getReport_name(), starttime, endtime, pageNo, sort, sortedColumn);

        List<TestExeDto> testcaseDetails = RestApiUtils.getTestExeListForS_run_ids(sRunIds);
        SuiteDto suiteData = RestApiUtils.getSuiteByReportNameAndStatus(getSuite.getReport_name(), ACTIVE_STATUS);
        if (suiteData != null) {
            result.put("s_id", suiteData.getS_id());
        }
        for (SuiteExeDto suiteExeDto : suiteReports) {
            HashMap<String, Object> temp = new HashMap<>();
            Map<String, Object> timeMap = new HashMap<>();
            timeMap.put("subType", "datetime");
            temp.put("Start Time", ReportUtils.createCustomObject(suiteExeDto.getS_start_time(), "date", suiteExeDto.getS_start_time(), "left", timeMap));
            temp.put("Status", ReportUtils.createCustomObject(suiteExeDto.getStatus(), "status", suiteExeDto.getStatus(), "left"));
            HashMap<String, Object> actionMap = new HashMap<>();
            actionMap.put("subType", "execution_report");
            temp.put("Action", ReportUtils.createCustomObject(suiteExeDto.getS_run_id(), "action", suiteExeDto.getS_run_id(), "center", actionMap));

            Set<String> baseUserSet = new HashSet<>();
            Set<String> tokenUserSet = new HashSet<>();
            Set<String> runTypeSet = new HashSet<>();
            Set<String> runModeSet = new HashSet<>();
            Map<String, Object> statusMap = new HashMap<>();

            if (!testcaseDetails.isEmpty()) {
                long totalCount = 0L;
                for (TestExeDto testExeDto : testcaseDetails) {
                    if (!testExeDto.getS_run_id().equals(suiteExeDto.getS_run_id())) {
                        continue;
                    }
                    switch (testExeDto.getStatus().toUpperCase()) {
                        case "PASS":
                            if (testExeDto.getS_run_id().equals(suiteExeDto.getS_run_id())) {
                                long value = Long.parseLong(statusMap.get(PASS.toString()).toString()) + 1;
                                statusMap.put(PASS.toString(), value);
                                totalCount++;
                            }
                            break;
                        case "FAIL":
                            if (testExeDto.getS_run_id().equals(suiteExeDto.getS_run_id())) {
                                long value = Long.parseLong(statusMap.get(FAIL.toString()).toString()) + 1;
                                statusMap.put(FAIL.toString(), value);
                                totalCount++;
                            }
                            break;
                        case "EXE":
                            if (testExeDto.getS_run_id().equals(suiteExeDto.getS_run_id())) {
                                long value = Long.parseLong(statusMap.get(EXE.toString()).toString()) + 1;
                                statusMap.put(EXE.toString(), value);
                                totalCount++;
                            }
                            break;
                        case "ERR":
                            if (testExeDto.getS_run_id().equals(suiteExeDto.getS_run_id())) {
                                long value = Long.parseLong(statusMap.get(ERR.toString()).toString()) + 1;
                                statusMap.put(ERR.toString(), value);
                                totalCount++;
                            }
                            break;
                        case "INFO":
                            if (testExeDto.getS_run_id().equals(suiteExeDto.getS_run_id())) {
                                long value = Long.parseLong(statusMap.get(StatusColor.INFO.toString()).toString()) + 1;
                                statusMap.put(StatusColor.INFO.toString(), value);
                                totalCount++;
                            }
                            break;
                        case "WARN":
                            if (testExeDto.getS_run_id().equals(suiteExeDto.getS_run_id())) {
                                long value = Long.parseLong(statusMap.get(WARN.toString()).toString()) + 1;
                                statusMap.put(WARN.toString(), value);
                                totalCount++;
                            }
                            break;
                    }

                    if (testExeDto.getBase_user() != null) baseUserSet.add(testExeDto.getBase_user());
                    if (testExeDto.getToken_user() != null) tokenUserSet.addAll(testExeDto.getToken_user());
                    runTypeSet.add(testExeDto.getRun_type());
                    runModeSet.add(testExeDto.getRun_mode());

                }
                if ((search != null && !(search.equals("") || search.equalsIgnoreCase("null")) && !verifySearch(search, tokenUserSet))) {
                    continue;
                }
                HashMap<String, Object> statusSubType = new HashMap<>();
                statusSubType.put("subType", "timeline_tc");
                statusMap.put("TOTAL", totalCount);
                if (suiteExeDto.getStatus().equalsIgnoreCase("EXE") && totalCount != suiteExeDto.getExpected_testcases()) {
                    statusMap.put("EXE", suiteExeDto.getExpected_testcases() - totalCount);
                }
                if (suiteExeDto.getStatus().equalsIgnoreCase("ERR") && totalCount != suiteExeDto.getExpected_testcases()) {
                    statusMap.put("ERR", Long.parseLong(statusMap.get(ERR.toString()).toString()) + Math.abs(suiteExeDto.getExpected_testcases() - totalCount));
                }
                temp.put("Testcases", ReportUtils.createCustomObject(statusMap, "crud", statusMap, "left", statusSubType));


            }
            temp.put("Token User", ReportUtils.createCustomObject(tokenUserSet, "text", tokenUserSet, "left"));
            temp.put("Base User", ReportUtils.createCustomObject(baseUserSet, "text", baseUserSet, "left"));
            temp.put("Run Type", ReportUtils.createCustomObject(runTypeSet, "text", runTypeSet, "left"));
            temp.put("Run Mode", ReportUtils.createCustomObject(runModeSet, "text", runModeSet, "left"));
            data.add(temp);
        }
        Collections.reverse(data);
        result.put("data", data);

        return new Response(result, data.size() + " record(s) fetched successfully", SUCCESS);
    }

    private boolean verifySearch(String search, Set<String> tokenUserSet) {
        for (String name : tokenUserSet) {
            if (name.contains(search)) {
                return true;
            }
        }
        return false;
    }

    public Response getTickets(String s_run_id) {

        UserDto user = ReportUtils.getUserDtoFromServetRequest();

        SuiteExeDto getSuite = RestApiUtils.getSuiteExe(s_run_id);
        if (getSuite == null) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(SUITE_DETAILS_NOT_FOUND, null, FAILURE, HttpStatus.NOT_FOUND);
        }

        ProjectDto project = RestApiUtils.getProjectByPidAndStatus(getSuite.getP_id(), ACTIVE.toString());
        if (project == null) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(PROJECT_NOT_EXISTS, null, FAILURE, HttpStatus.NOT_ACCEPTABLE);
        }
        if (!ReportUtils.validateRoleWithViewerAccess(user, project)) {
            log.error("Error occurred due to records not found");
            throw new CustomDataException(USER_NOT_ACCESS_TO_PROJECT, null, OperationType.INFO, HttpStatus.NOT_ACCEPTABLE, REQUEST_ACCESS);
        }

        String azureUrl = "https://dev.azure.com/GEM-QualityEngineering/_workitems/edit/";
        String jiraUrl = "https://gemecosystem.atlassian.net/browse/";

        HashMap<String, Object> type = new HashMap<>();
        List<HashMap<String, Object>> jiraList = new ArrayList<>();
        List<HashMap<String, Object>> azureList = new ArrayList<>();
        if (getSuite.getMeta_data() != null) {
            List<Map<String, Object>> data = new ArrayList<>(getSuite.getMeta_data());
            if (!data.isEmpty()) {
                for (Map<String, Object> map : data) {
                    HashMap<String, Object> jiraTickets = new HashMap<>();
                    HashMap<String, Object> azureTickets = new HashMap<>();
                    if (map.containsKey("Jira_id")) {
                        jiraTickets.put("ID", map.get("Jira_id"));
                        jiraTickets.put("Type", "Suite");
                        jiraTickets.put("Link", jiraUrl + map.get("Jira_id"));
                        jiraList.add(jiraTickets);
                    }
                    if (map.containsKey("Azure_id")) {
                        azureTickets.put("ID", map.get("Azure_id"));
                        azureTickets.put("Type", "Suite");
                        azureTickets.put("Link", azureUrl + map.get("Azure_id"));
                        azureList.add(azureTickets);
                    }
                }
            }
        }
        List<TestExeDto> testList = RestApiUtils.getTestExeList(s_run_id);
        if (!testList.isEmpty()) {

            for (TestExeDto testExeDto : testList) {
                HashMap<String, Object> jiraTickets = new HashMap<>();
                HashMap<String, Object> azureTickets = new HashMap<>();
                for (Map<String, Object> map : testExeDto.getMeta_data()) {
                    if (map.containsKey("Jira_id")) {
                        jiraTickets.put("ID", map.get("Jira_id"));
                        jiraTickets.put("Type", "Testcase (" + testExeDto.getName() + ")");
                        jiraTickets.put("Link", jiraUrl + map.get("Jira_id"));
                        jiraList.add(jiraTickets);
                    }
                    if (map.containsKey("Azure_id")) {
                        azureTickets.put("ID", map.get("Azure_id"));
                        azureTickets.put("Type", "Testcase (" + testExeDto.getName() + ")");
                        azureTickets.put("Link", azureUrl + map.get("Azure_id"));
                        azureList.add(azureTickets);
                    }
                }
            }
        }
        if (jiraList.isEmpty() && azureList.isEmpty()) {
            return new Response(null, NO_TICKETS_FOUND, SUCCESS);
        }
        type.put("Jira", jiraList);
        type.put("Azure", azureList);

        return new Response(null, TICKETS_FETCHED_SUCCESSFULLY, SUCCESS);
    }
}