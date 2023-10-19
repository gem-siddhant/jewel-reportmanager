package com.jewel.reportmanager.utils;

import com.jewel.reportmanager.dto.*;
import com.jewel.reportmanager.entity.RuleApi;
import com.jewel.reportmanager.enums.StatusColor;
import com.jewel.reportmanager.enums.UserRole;
import com.jewel.reportmanager.exception.CustomDataException;
import com.mongodb.BasicDBObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.jewel.reportmanager.enums.OperationType.Failure;
import static com.jewel.reportmanager.enums.ProjectAccessType.ADMIN;
import static com.jewel.reportmanager.enums.TestCaseType.MANUAL;
import static com.jewel.reportmanager.utils.ReportResponseConstants.USER_DETAILS_NOT_FOUND;

@Slf4j
public class ReportUtils {

    private static String userManagerUrl;
    private static String gemUrl;
    private static String projectManagerUrl;
    private static MongoOperations mongoOperations;

    @Value("${user.manager.url}")
    public void setUserManagerUrl(String userManagerUrl) {
        ReportUtils.userManagerUrl = userManagerUrl;
    }

    @Value("${project.manager.url}")
    public void setProjectManagerUrl(String projectManagerUrl) {
        ReportUtils.projectManagerUrl = projectManagerUrl;
    }

    @Value("${gem.url}")
    public void setGemUrl(String gemUrl) {
        ReportUtils.gemUrl = gemUrl;
    }

    @Autowired
    public void setMongoOperations(MongoOperations mongoOperations) {
        ReportUtils.mongoOperations = mongoOperations;
    }

    /**
     * @return Username from the httpServletRequest
     */
    public static String getUsernameFromServetRequest() {
        String username = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        log.info("Username from servlet request: {}", username);
        return username;
    }

    /**
     * Return user from servlet request.
     *
     * @return user
     */
    public static UserDto getUserDtoFromServetRequest() {
        String username = getUsernameFromServetRequest();
        UserDto userDto = getUsernameAndIsDeleted(username, false);
        if (userDto == null) {
            log.error("Error occurred while trying to fetch user for username: {}", username);
            throw new CustomDataException(USER_DETAILS_NOT_FOUND, null, Failure, HttpStatus.NOT_ACCEPTABLE);
        }
        log.info("User details from servlet request: {}", userDto);
        return userDto;
    }

    /**
     * Returns user details by username and IsDeleted
     *
     * @param username
     * @param deleted
     * @return user
     */
    public static UserDto getUsernameAndIsDeleted(String username, Boolean deleted) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("username", username);
        uriVariables.put("deleted", deleted);
        return (UserDto) RestClient.getApi(userManagerUrl + "/v1/username/deleted?username={username}&deleted={deleted}", httpEntity, UserDto.class, uriVariables).getBody();
    }

    /**
     * To create custom map object.
     *
     * @param value
     * @param type
     * @param sortValue
     * @param align
     * @return map
     */
    public static Map<String, Object> createCustomObject(Object value, String type, Object sortValue, String align) {
        Map<String, Object> result = new HashMap<>();
        result.put("value", value);
        result.put("type", type);
        result.put("sortValue", sortValue);
        result.put("align", align);
        return result;
    }

    /**
     * To create custom map object.
     *
     * @param value
     * @param type
     * @param sortValue
     * @param align
     * @param map
     * @return map
     */
    public static Map<String, Object> createCustomObject(Object value, String type, Object sortValue, String align, Map<String, Object> map) {
        Map<String, Object> result = new HashMap<>();
        result.put("value", value);
        result.put("type", type);
        result.put("sortValue", sortValue);
        result.put("align", align);
        result.putAll(map);
        return result;
    }

    public static Map<String, String> getSuiteColumnName() {
        Map<String, String> columns = new HashMap<>();
        columns.put("suite name", "report_name");
        columns.put("project name", "project_name");
        columns.put("environment", "env");
        columns.put("status", "status");
        columns.put("framework", "framework_name");
        columns.put("start time", "s_start_time");
        columns.put("end time", "s_end_time");
        return columns;
    }

    public static boolean validateRoleWithViewerAccess(UserDto user, ProjectDto project) {
        if (project == null) {
            return false;
        }
        Query query = new Query(Criteria.where("pid").is(project.getPid()).and("username").is(user.getUsername()).and("status").is("ACTIVE"));
        ProjectRoleDto projectRole = mongoOperations.findOne(query, ProjectRoleDto.class);
        return projectRole != null || ((user.getRole().equalsIgnoreCase(ADMIN.toString()) && project.getRealcompanyname().equalsIgnoreCase(user.getRealCompany())) || user.getRole().equalsIgnoreCase(UserRole.SUPER_ADMIN.toString()));
    }

    public static Map<String, Object> getTestCaseExesByQuery(RuleApi payload, Integer pageNo, Integer sort,
                                                             String sortedColumn) throws ParseException {
        Map<String, Object> data = new HashMap<>();
        Map<String, String> getTestcaseColumnName = getTestcaseColumnName();
        long startTime = new SimpleDateFormat("MM/dd/yyyy").parse(payload.getStartTime()).getTime();
        long endTime = new SimpleDateFormat("MM/dd/yyyy").parse(payload.getEndTime()).getTime()
                + (1000 * 60 * 60 * 24);
        List<String> projects = payload.getProject();
        projects.replaceAll(String::toLowerCase);
        List<String> envs = payload.getEnv();
        envs.replaceAll(String::toLowerCase);
        List<Criteria> criteria2 = new ArrayList<>();
        criteria2.add(Criteria.where("result.p_id").in(payload.getProjectid()));
        criteria2.add(Criteria.where("result.env").in(envs));
        criteria2.add(Criteria.where("start_time").gte(startTime));
        criteria2.add(Criteria.where("end_time").lte(endTime));

        LookupOperation lookupOperation = LookupOperation.newLookup()
                .from("suiteExe")
                .localField("s_run_id")
                .foreignField("s_run_id")
                .as("result");
        MatchOperation matchOperation = Aggregation
                .match(new Criteria().andOperator(criteria2.toArray(new Criteria[criteria2.size()])));
        LimitOperation limitOperation = null;
        SkipOperation skipOperation = null;
        SortOperation sortOperation = null;
        Aggregation aggregation = null;
        if (pageNo != null && pageNo > 0) {
            skipOperation = Aggregation.skip((long) ((pageNo - 1) * 8));
            limitOperation = Aggregation.limit(pageNo * 8);
        }

        if (sort != null && sort != 0 && sortedColumn != null) {
            sortOperation = Aggregation.sort(sort == 1 ? Sort.Direction.ASC : Sort.Direction.DESC,
                    getTestcaseColumnName.get(sortedColumn.toLowerCase()));
        }

        if (pageNo != null && (sort == null || sortedColumn == null)) {

            aggregation = Aggregation.newAggregation(lookupOperation, matchOperation, limitOperation, skipOperation);
        } else if (pageNo == null && sort != null && sortedColumn != null) {

            aggregation = Aggregation.newAggregation(lookupOperation, matchOperation, sortOperation);

        } else if (pageNo != null) {

            aggregation = Aggregation.newAggregation(lookupOperation, matchOperation, sortOperation, skipOperation,
                    limitOperation);
        } else {
            aggregation = Aggregation.newAggregation(lookupOperation, matchOperation);
        }

        CountOperation countOperation = new CountOperation("tc_run_id");
        Aggregation countAggregation = Aggregation.newAggregation(lookupOperation, matchOperation, countOperation);
        BasicDBObject countResult = mongoOperations.aggregate(countAggregation, "testExe", BasicDBObject.class)
                .getUniqueMappedResult();
        long count = 0;
        if (countResult != null) {
            count = ((Number) countResult.get("tc_run_id")).longValue();
        }
        List<BasicDBObject> results = mongoOperations.aggregate(aggregation, "testExe", BasicDBObject.class)
                .getMappedResults();
        data.put("count", count);
        data.put("results", results);
        return data;
    }

    public static Map<String, String> getTestcaseColumnName() {
        Map<String, String> columns = new HashMap<>();
        columns.put("name", "name");
        columns.put("category", "category");
        columns.put("status", "status");
        columns.put("user", "user");
        columns.put("product Type", "product_type");
        columns.put("start time", "start_time");
        columns.put("end time", "end_time");
        columns.put("machine", "machine");
        return columns;
}

    public static double brokenIndexForTestExe(List<TestExeCommonDto> testExes) {

        if (testExes.size() == 1) {
            if (testExes.get(0).getStatus().equalsIgnoreCase("FAIL")) {
                return 1;
            } else {
                return 0;
            }
        }
        double denom = Math.floor(testExes.size() / (double) 2);

        double numenator = 0;
        int count = 0;
        TestExeCommonDto curr = null;
        TestExeCommonDto prev = null;

        for (TestExeCommonDto testExe : testExes) {

            if (curr == null && prev == null) {
                if (testExe.getStatus().equalsIgnoreCase("FAIL")) {
                    count++;
                }
                curr = testExe;
            } else {
                if (testExe.getStatus().equalsIgnoreCase("FAIL")) {
                    count++;
                }
                prev = curr;
                curr = testExe;
                if ((prev.getStatus().equalsIgnoreCase("PASS") || prev.getStatus().equalsIgnoreCase("INFO")
                        || prev.getStatus().equalsIgnoreCase("EXE") || prev.getStatus().equalsIgnoreCase("WARN"))
                        && (curr.getStatus().equalsIgnoreCase("FAIL") || curr.getStatus().equalsIgnoreCase("ERR"))) {
                    numenator++;
                }

            }
        }

        if (count == testExes.size()) {
            return 1;
        }
        double res = Math.round((float) (numenator / denom) * 100.0) / 100.0;
        return res;
    }

    public static double brokenIndexForSuiteExe(List<SuiteExeDto> suites) {

        if (suites.size() == 1) {
            if (suites.get(0).getStatus().equalsIgnoreCase("FAIL")) {
                return 1;
            } else {
                return 0;
            }
        }
        double denom = Math.floor(suites.size() / (double) 2);

        double numenator = 0;
        int count = 0;
        SuiteExeDto curr = null;
        SuiteExeDto prev = null;

        for (SuiteExeDto suite : suites) {

            if (curr == null && prev == null) {
                if (suite.getStatus().equalsIgnoreCase("FAIL")) {
                    count++;
                }
                curr = suite;
            } else {
                if (suite.getStatus().equalsIgnoreCase("FAIL")) {
                    count++;
                }
                prev = curr;
                curr = suite;
                if ((prev.getStatus().equalsIgnoreCase("PASS") || prev.getStatus().equalsIgnoreCase("INFO")
                        || prev.getStatus().equalsIgnoreCase("EXE") || prev.getStatus().equalsIgnoreCase("WARN"))
                        && (curr.getStatus().equalsIgnoreCase("FAIL") || curr.getStatus().equalsIgnoreCase("ERR"))) {
                    numenator++;
                }

            }
        }
        if (count == suites.size()) {
            return 1;
        }
        double res = Math.round((float) (numenator / denom) * 100.0) / 100.0;
        return res;
    }

    public static String averageFixTimeForTestExeCommon(List<TestExeCommonDto> testExes) {

        TestExeCommonDto curr = null;
        TestExeCommonDto prev = null;
        List<Long> averageTimes = new ArrayList<>();

        for (TestExeCommonDto testExe : testExes) {

            if (curr == null && prev == null) {

                curr = testExe;
            } else {

                prev = curr;
                curr = testExe;
                if ((curr.getStatus().equalsIgnoreCase("PASS") || curr.getStatus().equalsIgnoreCase("INFO")
                        || curr.getStatus().equalsIgnoreCase("EXE") || curr.getStatus().equalsIgnoreCase("WARN"))
                        && (prev.getStatus().equalsIgnoreCase("FAIL") || prev.getStatus().equalsIgnoreCase("ERR"))) {
                    averageTimes.add(curr.getStart_time() - prev.getStart_time());
                }

            }
        }
        double sum = 0;
        for (double current : averageTimes) {
            sum = sum + current;

        }
        double average = sum / averageTimes.size();
        average = average / 1000;

        String res = "";
        DecimalFormat df = new DecimalFormat("#");
        if (average < 60) {
            res = average + " sec(s)";
        } else if (average >= 60 && average < 3600) {
            String ans = df.format(Math.floor(average / 60)) + " min ";
            if (Math.floor(average % 60) != 0) {
                ans = ans + df.format(Math.floor(average % 60)) + " sec(s)";
            }
            res = ans;
        } else if (average >= 60 && average < 86400) {
            String ans = df.format(Math.floor(average / 3600)) + " hour ";
            if (Math.floor((average % 3600) / 60) != 0) {
                ans = ans + df.format(Math.floor((average % 3600) / 60)) + " min ";
            }
            if (Math.floor((average % 3600) % 60) != 0) {
                ans = ans + df.format(Math.floor((average % 3600) % 60)) + " sec(s)";
            }

            res = ans;
        } else {
            String ans = df.format(Math.floor(average / 86400)) + " days ";
            if (Math.floor((average % 86400) / 3600) != 0) {
                ans = ans + df.format(Math.floor((average % 86400) / 3600)) + " hours ";
            }
            res = ans;
        }

        if (res.equals("") || sum == 0) {
            res = "Never Broken";
        }
        return res;
    }

    public static long averageFixTimeForSuiteExe(List<SuiteExeDto> suites) {

        SuiteExeDto curr = null;
        SuiteExeDto prev = null;
        List<Long> averageTimes = new ArrayList<>();

        for (SuiteExeDto suite : suites) {

            if (curr == null) {

                curr = suite;
            } else {

                prev = curr;
                curr = suite;
                if ((curr.getStatus().equalsIgnoreCase("PASS") || curr.getStatus().equalsIgnoreCase("INFO")
                        || curr.getStatus().equalsIgnoreCase("EXE") || curr.getStatus().equalsIgnoreCase("WARN"))
                        && (prev.getStatus().equalsIgnoreCase("FAIL") || prev.getStatus().equalsIgnoreCase("ERR"))) {
                    averageTimes.add(curr.getS_start_time() - prev.getS_start_time());
                }

            }
        }
        double sum = 0;
        for (double current : averageTimes) {
            sum = sum + current;

        }
        double average = sum / averageTimes.size();
        average = average / 1000;
        return (long) average;

    }

    public static long getDownTimeForSuiteExe(List<SuiteExeDto> suites) {
        if (suites.get(0).getStatus().equalsIgnoreCase("PASS")) {
            return 0;
        } else {
            SuiteExeDto prev = null;
            long firstFailTime = 0;
            for (SuiteExeDto suite : suites) {
                if (suite.getStatus().equalsIgnoreCase("PASS")) {
                    break;
                }
                prev = suite;
            }
            if (prev != null) {
                firstFailTime = prev.getS_start_time();
            }

            Date d = new Date();
            long downTime = (d.getTime() - firstFailTime) / 1000;
            if (downTime < 0) {
                return 0;
            }
            return downTime;
        }
    }

    public static int stabilityIndex(double brokenIndex) {
        return 100 - ((int) (100 * brokenIndex));
    }


    public static String lastRunStatusForTestExeCommon(List<TestExeCommonDto> testExes) {
        return testExes.get(0).getStatus();
    }

    public static String getFailingSinceForTestExeCommon(List<TestExeCommonDto> testExes, double brokenIndex) {
        if (brokenIndex == 0) {
            return "No Issues";
        } else if (brokenIndex == 1) {
            return "Never Fixed";
        } else {
            int count = 0;
            for (TestExeCommonDto testExe : testExes) {
                if (!testExe.getStatus().equalsIgnoreCase("FAIL")) {
                    break;
                }
                count++;

            }
            if (count == 0) {
                return "No Issues";
            }
            return "Last " + count + " Runs";
        }
    }

    public static Long getLastPassForTestExeCommon(List<TestExeCommonDto> testExes) {
        for (TestExeCommonDto testExe : testExes) {
            if (testExe.getStatus().equalsIgnoreCase("PASS")) {
                return testExe.getStart_time();
            }

        }
        return 0L;
    }

    public static String getDownTimeForTestExeCommon(List<TestExeCommonDto> testExes) {
        if (testExes.get(0).getStatus().equalsIgnoreCase("PASS")) {
            return "No Issues";
        } else {
            TestExeCommonDto prev = null;
            long firstFailTime = 0;
            for (TestExeCommonDto testExe : testExes) {
                if (testExe.getStatus().equalsIgnoreCase("PASS")) {
                    break;
                }
                prev = testExe;
            }
            if (prev != null) {
                firstFailTime = prev.getEnd_time();
            }

            Date d = new Date();
            long downTime = (d.getTime() - firstFailTime) / 1000;
            if (downTime < 0) {
                return "No Issues";
            }
            String res = "";
            DecimalFormat df = new DecimalFormat("#");
            if (downTime < 60) {
                res = downTime + " sec(s)";
            } else if (downTime >= 60 && downTime < 3600) {
                String ans = df.format(Math.floor(downTime / (double) 60)) + " min ";
                if (Math.floor(downTime % 60) != 0) {
                    ans = ans + df.format(Math.floor(downTime % 60)) + " sec(s)";
                }
                res = ans;
            } else if (downTime >= 60 && downTime < 86400) {
                String ans = df.format(Math.floor(downTime / (double) 3600)) + " hour ";
                if (Math.floor((downTime % 3600) / (double) 60) != 0) {
                    ans = ans + df.format(Math.floor((downTime % 3600) / (double) 60)) + " min ";
                }
                if (Math.floor((downTime % 3600) % 60) != 0) {
                    ans = ans + df.format(Math.floor((downTime % 3600) % 60)) + " sec(s)";
                }

                res = ans;
            } else {
                String ans = df.format(Math.floor(downTime / (double) 86400)) + " days ";
                if (Math.floor((downTime % 86400) / (double) 3600) != 0) {
                    ans = ans + df.format(Math.floor((downTime % 86400) / (double) 3600)) + " hours ";
                }
                res = ans;
            }

            return res;
        }
    }

    public static TestExeCommonDto getTestExeCommonByBasicObjectAndDocument(BasicDBObject testExe, Document suiteExe) {
        TestExeCommonDto testExeDiagnose = new TestExeCommonDto();
        testExeDiagnose.setEnv((String) suiteExe.get("env"));
        testExeDiagnose.setReport_name((String) suiteExe.get("report_name"));
        testExeDiagnose.setProject_name((String) suiteExe.get("project_name"));
        testExeDiagnose.setTc_run_id((String) testExe.get("tc_run_id"));
        testExeDiagnose.setStart_time((Long) testExe.get("start_time"));
        testExeDiagnose.setEnd_time((Long) testExe.get("end_time"));
        testExeDiagnose.setName((String) testExe.get("name"));
        testExeDiagnose.setCategory((String) testExe.get("category"));
        testExeDiagnose.setLog_file((String) testExe.get("log_file"));
        testExeDiagnose.setStatus((String) testExe.get("status"));
        testExeDiagnose.setUser((String) testExe.get("user"));
        testExeDiagnose.setMachine((String) testExe.get("machine"));
        testExeDiagnose.setResult_file((String) testExe.get("result_file"));
        testExeDiagnose.setProduct_type((String) testExe.get("product_type"));
        testExeDiagnose.setIgnore((Boolean) testExe.get("ignore"));
        testExeDiagnose.setSteps((List<Object>) testExe.get("steps"));
        testExeDiagnose.setMiscData((List<Map<String, Object>>) testExe.get("miscData"));
        testExeDiagnose.setUserDefinedData((Map<String, Object>) testExe.get("userDefinedData"));
        testExeDiagnose.setS_run_id((String) testExe.get("s_run_id"));
        testExeDiagnose.setP_id((Long) suiteExe.get("p_id"));
        return testExeDiagnose;
    }

    public static Object createDoughnutChart(Map<String, Long> res) {

        Map<String, Object> map = new HashMap<>();
        List<String> labels = new ArrayList<>();
        labels.addAll(res.keySet());

        List<Object> datasets = new ArrayList<>();
        Map<String, Object> datasetValue = new HashMap<>();
        List<Long> data = new ArrayList<>();
        List<String> backgroundColor = new ArrayList<>();
        List<String> borderColor = new ArrayList<>();
        for (String status : labels) {
            data.add(res.get(status));
            backgroundColor.add(StatusColor.valueOf(status).color);
            borderColor.add(StatusColor.valueOf(status).color);

        }
        datasetValue.put("data", data);
        datasetValue.put("borderWidth", 1);
        datasetValue.put("backgroundColor", backgroundColor);
        datasetValue.put("borderColor", borderColor);
        datasets.add(datasetValue);
        map.put("datasets", datasets);
        map.put("labels", labels);
        return map;

    }

    public static double getScore(double brokenIndex) {
        return Math.round((999 - (999 * (0.35 * brokenIndex))) * 100.0) / 100.0;
    }

    public static double getScore(double brokenIndex, long downTime, long averageFixTime, String env,
                           List<SuiteExeDto> suiteExeList) {
        double averageFixTimeScore = 25;
        double downTimeScore = 15;
        double averageFixTimeCount = 0;
        double downTimeCount = 0;
        double testCaseScore = 15;
        double suiteScore = 10;
        if (env.equalsIgnoreCase("prod")) {
            averageFixTimeCount = Math.floor((Math.floor(averageFixTime / (double) 60)) / 30);
            downTimeCount = Math.floor((Math.floor(downTime / (double) 60)) / 15);
        } else if (env.equalsIgnoreCase("beta") || env.equalsIgnoreCase("uat") || env.equalsIgnoreCase("pre-prod")) {
            averageFixTimeCount = Math.floor((Math.floor(averageFixTime / (double) 60)) / 180);
            downTimeCount = Math.floor((Math.floor(downTime / (double) 60)) / 90);
        } else {
            averageFixTimeCount = Math.floor((Math.floor(averageFixTime / (double) 60)) / 1440);
            downTimeCount = Math.floor((Math.floor(downTime / (double) 60)) / 1440);
        }
        averageFixTimeScore = averageFixTimeScore - averageFixTimeCount;
        downTimeScore = downTimeScore - downTimeCount;
        if (averageFixTimeScore < 0) {
            averageFixTimeScore = 0;
        }
        if (downTimeScore < 0) {
            downTimeScore = 0;
        }
        double count = 0;
        List<String> sRunIdsList = new ArrayList<>();
        for (SuiteExeDto suiteExe : suiteExeList) {
            sRunIdsList.add(suiteExe.getS_run_id());
            if (suiteExe.getStatus().equalsIgnoreCase("FAIL") || suiteExe.getStatus().equalsIgnoreCase("ERR")) {
                count++;
            }
        }
        Query failOrExeTestCasesCountQuery = new Query(
                Criteria.where("status").in("ERR", "FAIL").and("s_run_id").in(sRunIdsList));
        Query testCaseCountQuery = new Query(Criteria.where("s_run_id").in(sRunIdsList));
        double totalTestCaseCount = mongoOperations.count(testCaseCountQuery, TestExeDto.class);
        double failTestCaseCount = mongoOperations.count(failOrExeTestCasesCountQuery, TestExeDto.class);
        if (totalTestCaseCount > 0) {
            testCaseScore = testCaseScore - ((failTestCaseCount / totalTestCaseCount) * 15);
        } else {
            testCaseScore = 0;
        }
        if (testCaseScore < 0) {
            testCaseScore = 0;
        }

        if (suiteExeList.size() > 0) {
            suiteScore = suiteScore - ((count / suiteExeList.size()) * 10);
        } else {
            suiteScore = 0;
        }
        if (suiteScore < 0) {
            suiteScore = 0;
        }
        brokenIndex = 1 - brokenIndex;

        double brokenIndexWeight = Math.round(((999 * (0.35 * brokenIndex))) * 100) / 100;
        double downTimeWeight = Math.round(999 * (downTimeScore / 100));
        double averageFixTimeWeight = Math.round(999 * (averageFixTimeScore / 100));
        double suiteTestCaseWeight = Math.round(999 * (testCaseScore / 100));
        double suiteWeight = Math.round(999 * (suiteScore / 100));
        return (brokenIndexWeight + downTimeWeight + averageFixTimeWeight + suiteTestCaseWeight + suiteWeight);
    }

    public static long getStatuswiseCount(String s_run_id, String status) {
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<Criteria>();
        criteria.add(Criteria.where("s_run_id").is(s_run_id));
        if (status.equalsIgnoreCase("OTHER")) {
            List<String> statuses = new ArrayList<String>();
            Collections.addAll(statuses, "PASS", "FAIL");
            criteria.add(Criteria.where("status").not().in(statuses));
        } else if (!(status.equalsIgnoreCase("TOTAL"))) {
            criteria.add(Criteria.where("status").is(status));
        } else {

        }
        query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[criteria.size()])));

        long count = mongoOperations.count(query, TestExeDto.class);
        return count;
    }

    public Map<String, Long> getStatuswiseCountReportName(String report_name, long starttime, long endtime) {
        Map<String, Long> res = new HashMap<String, Long>();
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<Criteria>();
        criteria.add(Criteria.where("report_name").is(report_name));
        criteria.add(Criteria.where("start_time").gt(starttime));
        criteria.add(Criteria.where("end_time").lt(endtime));

        List<String> distinctStatus = mongoOperations.findDistinct(query, "status", TestExeCommonDto.class, String.class);
        for (String status : distinctStatus) {
            Query queryStatus = new Query();
            List<Criteria> criteriaStatus = new ArrayList<Criteria>();
            criteriaStatus.add(Criteria.where("report_name").is(report_name));
            criteriaStatus.add(Criteria.where("start_time").gt(starttime));
            criteriaStatus.add(Criteria.where("end_time").lt(endtime));
            criteriaStatus.add(Criteria.where("status").is(status));
            queryStatus.addCriteria(new Criteria().andOperator(criteriaStatus.toArray(new Criteria[criteria.size()])));
            res.put(status.toUpperCase(), mongoOperations.count(queryStatus, TestExeCommonDto.class));

        }

        return res;
    }

    public static List<TestExeCommonDto> getSortedListForTestExeCommon(List<TestExeCommonDto> list) {
        List<TestExeCommonDto> testExes = new ArrayList<>();
        testExes.addAll(list);
        Collections.sort(testExes, new TimeComparatorTestExeCommon());
        return testExes;
    }

    public static List<SuiteExeDto> getSortedListForSuiteExe(List<SuiteExeDto> list) {
        List<SuiteExeDto> suites = new ArrayList<>();
        suites.addAll(list);
        Collections.sort(suites, new TimeComparator());
        return suites;
    }

    static class TimeComparator implements Comparator<SuiteExeDto> {

        @Override
        public int compare(SuiteExeDto data, SuiteExeDto data2) {
            if (data.getS_start_time() == data2.getS_start_time())
                return 0;
            else if (data.getS_start_time() < data2.getS_start_time())
                return 1;
            else
                return -1;
        }
    }

    private List<Map<String, Object>> getLinkedSteps(List<Map<String, Object>> data, Long isolatedVersionId, int id,
                                                     boolean finalSteps) {
        if (data == null) {
            return null;
        }
        log.info("working");
        Query query = new Query(Criteria.where("isolatedVersionId").is(isolatedVersionId).and("isDeleted").is(false)
                .and("testcaseType").is(MANUAL.toString()));
        Test manualTestCase = mongoOperations.findOne(query, Test.class);
        if (manualTestCase == null) {
            return null;
        }

        for (int i = 0; i < manualTestCase.getTestSteps().size(); i++) {
            Map<String, Object> map = new HashMap<>();
            if (i == 0) {
                if (manualTestCase.getTestSteps().get(i).getTcIsolatedVersionId() != null) {
                    data = getLinkedSteps(data, manualTestCase.getTestSteps().get(i).getTcIsolatedVersionId(), id,
                            finalSteps);
                    if (data == null) {
                        return null;
                    }
                }
                if (manualTestCase.getTestSteps().get(i).getName() != null
                        && !manualTestCase.getTestSteps().get(i).getName().isEmpty()
                        && manualTestCase.getTestSteps().get(i).getDescription() != null
                        && !manualTestCase.getTestSteps().get(i).getDescription().isEmpty()) {
                    map.put("status", StatusColor.PENDING.toString());
                    map.put("step name", manualTestCase.getTestSteps().get(i).getName());
                    map.put("step description", manualTestCase.getTestSteps().get(i).getDescription());
                    map.put("expected result", manualTestCase.getTestSteps().get(i).getExpectedResult());
                    data.add(map);
                }
            } else {
                if (manualTestCase.getTestSteps().get(i).getTcIsolatedVersionId() != null) {
                    data = getLinkedSteps(data, manualTestCase.getTestSteps().get(i).getTcIsolatedVersionId(), id,
                            finalSteps);
                    if (data == null) {
                        return null;
                    }
                }
                if (manualTestCase.getTestSteps().get(i).getName() != null
                        && !manualTestCase.getTestSteps().get(i).getName().isEmpty()
                        && manualTestCase.getTestSteps().get(i).getDescription() != null
                        && !manualTestCase.getTestSteps().get(i).getDescription().isEmpty()) {
                    map.put("status", StatusColor.PENDING.toString());
                    map.put("step name", manualTestCase.getTestSteps().get(i).getName());
                    map.put("step description", manualTestCase.getTestSteps().get(i).getDescription());
                    map.put("expected result", manualTestCase.getTestSteps().get(i).getExpectedResult());
                    data.add(map);
                }
            }
        }
        return data;
    }

    public static String checkStatusOfTestCaseByStepsIfVarianceIsThere(String tc_run_id,
                                                                       Map<Long, VarianceClassificationDto> data) {
        Query query = new Query(Criteria.where("tc_run_id").is(tc_run_id));
        StepsDto steps = mongoOperations.findOne(query, StepsDto.class);
        if (steps.getSteps().size() == 0) {
            return "PASS";
        }
        Set<String> statues = new HashSet<>();
        for (Object step : steps.getSteps()) {
            String status = null;
            Map<String, Object> finalstep = (Map<String, Object>) step;
            if (finalstep.getOrDefault("VARIANCEID", null) != null) {
                Long varianceId = (Long) finalstep.getOrDefault("VARIANCEID", null);
                if (data.getOrDefault(varianceId, null) != null) {
                    status = "PASS";
                } else {
                    status = (String) finalstep.get("status");
                }
            } else {
                status = (String) finalstep.get("status");
            }
            statues.add(status);
        }
        String finalStatus = "";
        int priority = Integer.MAX_VALUE;
        for (String status : statues) {
            if (StatusColor.valueOf(status.toUpperCase()).priority < priority) {
                priority = StatusColor.valueOf(status.toUpperCase()).priority;
            }
        }
        for (StatusColor val : StatusColor.values()) {
            if (val.priority == priority) {
                finalStatus = val.name();
            }
        }
        return finalStatus;

    }

    public static boolean checkoneListContainsElementOfAnotherList(List<Long> mainList, List<Long> list) {
        for (Long value : list) {
            if (mainList.contains(value)) {
                return true;
            }
        }
        return false;
    }

    public static List<StatusColor> getStatusColorInSorted() {
        Set<StatusColor> set = new TreeSet<>(new Comparator<StatusColor>() {
            @Override
            public int compare(StatusColor o1, StatusColor o2) {
                int returnValue = 0;
                if (o1.getPriority() < o2.getPriority()) {
                    returnValue = -1;
                } else if (o1.getPriority() > o2.getPriority()) {
                    returnValue = 1;
                }
                return returnValue;
            }
        });
        Collections.addAll(set, StatusColor.values());
        return set.stream().collect(Collectors.toList());
    }

    public static String changeKeyValue(String key) {
        return key.replace('_', ' ').toUpperCase();
    }

    static class TimeComparatorTestExeCommon implements Comparator<TestExeCommonDto> {

        @Override
        public int compare(TestExeCommonDto data, TestExeCommonDto data2) {
            if (data.getStart_time() == data2.getStart_time())
                return 0;
            else if (data.getStart_time() < data2.getStart_time())
                return 1;
            else
                return -1;
        }

    }

    public static Double getTimeRemainingNew(SuiteExeDto suite, List<List<DependencyTree>> ans) {

        Double remaining_time = 0.0;

        List<Criteria> criteria = new ArrayList<Criteria>();
        Query query3 = new Query();
        int counter = 0, exe_count = 0;
        Long T3 = 0L;

        Map<String, Long> testcaseHistory = new HashMap<>();
        criteria.add(
                Criteria.where("project_name").in(suite.getProject_name()).and("p_id").in(suite.getP_id()));

        criteria.add(Criteria.where("env").in(suite.getEnv()));
        criteria.add(Criteria.where("report_name").is(suite.getReport_name()));
        criteria.add(Criteria.where("s_start_time").lt(suite.getS_start_time()));
        query3.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[criteria.size()])))
                .with(Sort.by(Sort.Direction.DESC, "s_start_time")).limit(5);

        List<SuiteExeDto> suites_T3 = mongoOperations.find(query3, SuiteExeDto.class);
        if (suites_T3.size() == 0) {

            return Double.valueOf(60);
        }
        List<TestExeDto> tests = new ArrayList<>();
        List<String> testcase_details2 = new ArrayList<>();
        for (SuiteExeDto suiteExe : suites_T3) {
            Query query2 = new Query();

            if (suiteExe.getStatus().equalsIgnoreCase("EXE")) {
                exe_count++;
                if (exe_count == suites_T3.size()) {

                    return Double.valueOf(60);
                }
                continue;
            }
            if ((suiteExe.getTestcase_details() == null || suiteExe.getTestcase_details().size() == 0)) {
                continue;
            }
            T3 += (suiteExe.getS_end_time() - suiteExe.getS_start_time());

            counter++;
            testcase_details2 = suiteExe.getTestcase_details();
            if (testcase_details2 != null && !testcase_details2.isEmpty()) {

                query2.addCriteria(Criteria.where("tc_run_id").in(testcase_details2));
                tests = mongoOperations.find(query2, TestExeDto.class);
                if (!tests.isEmpty()) {

                    for (TestExeDto testExe : tests) {
                        Long tempTime = 0L;

                        tempTime += testExe.getEnd_time() - testExe.getStart_time();

                        if (!testcaseHistory.isEmpty() && testcaseHistory.containsKey(testExe.getName())) {
                            testcaseHistory.put(testExe.getName(),
                                    (testcaseHistory.get(testExe.getName()) + tempTime));
                        } else {
                            testcaseHistory.put(testExe.getName(), tempTime);
                        }

                    }
                }

            }
        }
        for (String value : testcaseHistory.keySet()) {

            testcaseHistory.put(value, (testcaseHistory.get(value)) / testcase_details2.size());

        }
        if (counter == 0) {
            return 60.0;
        }

        remaining_time = (double) ((T3 / counter));
        List<String> testcase_details = suite.getTestcase_details();

        if (Objects.isNull(suite.getMode()) || suite.getMode().equalsIgnoreCase("sequence")) {

            if (testcase_details == null) {

                return (double) (remaining_time / 1000) + 60;
            }
            Query Q2 = new Query();

            Q2.addCriteria(Criteria.where("tc_run_id").in(testcase_details));
            tests = mongoOperations.find(Q2, TestExeDto.class);
            if (tests != null && !tests.isEmpty()) {

                Long temp = 0L;
                for (TestExeDto testExe : tests) {

                    if (testcaseHistory.containsKey(testExe.getName())) {
                        temp += testcaseHistory.get(testExe.getName());

                    }

                }

                remaining_time = (double) (remaining_time - temp) / 1000;
                if (remaining_time < 0) {
                    return 60.0;
                }

                return Math.abs(remaining_time + 60);
            } else {
                return 60.0;
            }

        } else {
            Long temp = 0L;

            Query query = new Query();

            tests = new ArrayList<>();

            if (ans == null || ans.size() == 0) {
                return 60.0;
            }
            int i = 0;

            temp = 0L;
            if (testcase_details.isEmpty()) {

                return (remaining_time / 1000) + 60;
            } else {
                query.addCriteria(Criteria.where("tc_run_id").in(testcase_details));
                tests = mongoOperations.find(query, TestExeDto.class);

                for (TestExeDto test : tests) {

                    i = 0;
                    for (List<DependencyTree> it : ans) {

                        if (i == 0) {
                            i++;
                            continue;
                        }
                        Long testTime = 0L;
                        for (DependencyTree et : it) {

                            if (test.getName().equalsIgnoreCase(et.data.getName())
                                    && testcaseHistory.containsKey(test.getName())
                                    && testTime < testcaseHistory.get(test.getName())) {

                                testTime = testcaseHistory.get(test.getName());

                            }

                        }
                        temp += testTime;

                    }

                }

                remaining_time = (remaining_time - temp) / 1000;

                if (remaining_time < 0) {
                    return 60.0;
                }
                return remaining_time + 60;

            }
        }

    }

    public static List<String> headersDataRefactor(List<String> list) {
        List<String> finalList = new ArrayList<>();
        finalList.addAll(list);

        for (int i = 0; i < finalList.size(); i++) {
            if (finalList.get(i).toLowerCase().equals("name")) {
                Collections.swap(finalList, 0, i);
            }
            if (finalList.get(i).toLowerCase().equals("status") && finalList.size() > 1) {
                Collections.swap(finalList, 1, i);
            }
            if (finalList.get(i).toLowerCase().equals("start_time") && finalList.size() > 2) {
                Collections.swap(finalList, 2, i);
            }
            if (finalList.get(i).toLowerCase().equals("end_time") && finalList.size() > 3) {
                Collections.swap(finalList, 3, i);
            }

        }
        for (int i = 0; i < finalList.size(); i++) {
            finalList.set(i, changeKeyValue(finalList.get(i)));
        }

        return finalList;
    }

    public static String getDuration(long s_start_time, long s_end_time) {
        DecimalFormat df = new DecimalFormat("#");
        float seconds = Float.parseFloat(df.format((float) (s_end_time - s_start_time) / 1000));
        String res = "";
        if (seconds < 60) {
            res = seconds + " sec(s)";
        } else if (seconds >= 60 && seconds < 3600) {
            String ans = df.format(Math.floor(seconds / 60)) + " min ";
            if (Math.floor(seconds % 60) != 0) {
                ans = ans + df.format(Math.floor(seconds % 60)) + " sec(s)";
            }
            res = ans;
        } else if (seconds >= 60 && seconds < 86400) {
            String ans = df.format(Math.floor(seconds / 3600)) + " hour ";
            if (Math.floor((seconds % 3600) / 60) != 0) {
                ans = ans + df.format(Math.floor((seconds % 3600) / 60)) + " min ";
            }
            if (Math.floor((seconds % 3600) % 60) != 0) {
                ans = ans + df.format(Math.floor((seconds % 3600) % 60)) + " sec(s)";
            }

            res = ans;
        } else {
            String ans = df.format(Math.floor(seconds / 86400)) + " days ";
            if (Math.floor((seconds % 86400) / 3600) != 0) {
                ans = ans + df.format(Math.floor((seconds % 86400) / 3600)) + " hours ";
            }

            res = ans;
        }

        return res;
    }

    public static List<String> headersDataStepRefactor(Set<String> list) {
        List<String> finalList = new ArrayList<>();
        finalList.addAll(list);

        for (int i = 0; i < finalList.size(); i++) {
            if (finalList.get(i).toLowerCase().equals("step name")) {
                Collections.swap(finalList, 0, i);
            }
            if (finalList.get(i).toLowerCase().equals("step description")) {
                Collections.swap(finalList, 1, i);
            }
            if (finalList.get(i).toLowerCase().equals("status")) {
                Collections.swap(finalList, finalList.size() - 1, i);
            }

        }
        finalList.replaceAll(ReportUtils::changeKeyValue);

        return finalList;
    }

    public static Map<String, List<SuiteExeDto>> getSuiteNames(String reportName, List<Long> pid, List<String> projects, long starttime,
                                                     long endtime,
                                                     List<String> envs) {
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<Criteria>();
        criteria.add(Criteria.where("report_name").is(reportName));
        criteria.add(Criteria.where("p_id").in(pid));
        criteria.add(Criteria.where("project_name").in(projects));
        criteria.add(Criteria.where("s_start_time").gte(starttime));
        criteria.add(Criteria.where("s_end_time").lte(endtime));
        criteria.add(Criteria.where("env").in(envs));
        query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[criteria.size()])));
        Map<String, List<SuiteExeDto>> map = new HashMap<>();
        List<SuiteExeDto> suiteExeList = mongoOperations.find(query, SuiteExeDto.class);
        for (SuiteExeDto suiteExe : suiteExeList) {
            String key = suiteExe.getProject_name() + ":" + suiteExe.getReport_name() + ":" + suiteExe.getEnv();
            List<SuiteExeDto> data = map.getOrDefault(key, null);
            if (data == null) {
                List<SuiteExeDto> newlist = new ArrayList<>();
                newlist.add(suiteExe);
                map.put(key, newlist);
            } else {
                data.add(suiteExe);
                map.put(key, data);
            }
        }
        return map;
    }

    public static Map<String, Object> last5SuiteRuns(List<SuiteExeDto> getAllSuites) {
        List<SuiteExeDto> suiteExes = new ArrayList<>();
        suiteExes.addAll(getAllSuites);
        suiteExes = suiteExes.subList(0, suiteExes.size() >= 5 ? 5 : suiteExes.size());
        if (suiteExes.size() > 0) {
            List<Long> passCount = new ArrayList<>();
            List<Long> failCount = new ArrayList<>();
            List<Long> warnCount = new ArrayList<>();
            List<Long> infoCount = new ArrayList<>();
            List<Long> errCount = new ArrayList<>();
            List<Long> exeCount = new ArrayList<>();
            List<Long> labels = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            long suiteFailCount = 0L;
            for (SuiteExeDto suiteExe : suiteExes) {
                if (suiteExe.getStatus().toUpperCase().equals("FAIL")) {
                    suiteFailCount++;
                }
                Query query1 = new Query(Criteria.where("s_run_id").is(suiteExe.getS_run_id()));
                List<TestExeDto> testExeList = mongoOperations.find(query1, TestExeDto.class);
                Map<String, Long> statusMap = new HashMap<>();
                for (StatusColor statusColor : StatusColor.values()) {
                    statusMap.put(statusColor.toString(), 0L);
                }
                long totalCount = 0L;
                for (TestExeDto testExe : testExeList) {
                    if (testExe.getStatus().toUpperCase().equals(StatusColor.PASS.toString())) {
                        long value = statusMap.get(StatusColor.PASS.toString()) + 1;
                        statusMap.put(StatusColor.PASS.toString(), value);
                        totalCount++;
                        continue;
                    }
                    if (testExe.getStatus().toUpperCase().equals(StatusColor.FAIL.toString())) {
                        long value = statusMap.get(StatusColor.FAIL.toString()) + 1;
                        statusMap.put(StatusColor.FAIL.toString(), value);
                        totalCount++;
                        continue;
                    }
                    if (testExe.getStatus().toUpperCase().equals(StatusColor.EXE.toString())) {
                        long value = statusMap.get(StatusColor.EXE.toString()) + 1;
                        statusMap.put(StatusColor.EXE.toString(), value);
                        totalCount++;
                        continue;
                    }
                    if (testExe.getStatus().toUpperCase().equals(StatusColor.ERR.toString())) {
                        long value = statusMap.get(StatusColor.ERR.toString()) + 1;
                        statusMap.put(StatusColor.ERR.toString(), value);
                        totalCount++;
                        continue;
                    }
                    if (testExe.getStatus().toUpperCase().equals(StatusColor.INFO.toString())) {
                        long value = statusMap.get(StatusColor.INFO.toString()) + 1;
                        statusMap.put(StatusColor.INFO.toString(), value);
                        totalCount++;
                        continue;
                    }
                    if (testExe.getStatus().toUpperCase().equals(StatusColor.WARN.toString())) {
                        long value = statusMap.get(StatusColor.WARN.toString()) + 1;
                        statusMap.put(StatusColor.WARN.toString(), value);
                        totalCount++;
                        continue;
                    }

                }

                for (StatusColor statusColor : StatusColor.values()) {
                    switch (statusColor.toString()) {
                        case "PASS":
                            passCount.add(statusMap.get(statusColor.toString()));
                            break;
                        case "FAIL":
                            failCount.add(statusMap.get(statusColor.toString()));
                            break;
                        case "WARN":
                            warnCount.add(statusMap.get(statusColor.toString()));
                            break;
                        case "INFO":
                            infoCount.add(statusMap.get(statusColor.toString()));
                            break;
                        case "ERR":
                            errCount.add(statusMap.get(statusColor.toString()));
                            break;
                        case "EXE":
                            exeCount.add(statusMap.get(statusColor.toString()));
                            break;
                    }

                }
                labels.add(suiteExe.getS_start_time());
                ids.add(suiteExe.getS_run_id());
            }

            Map<String, Object> suiteBarGraph = new HashMap<>();
            suiteBarGraph.put("labels", labels);
            List<Map<String, Object>> datasets = new ArrayList<>();

            for (StatusColor statusColor : StatusColor.values()) {
                Map<String, Object> datasetmap = new HashMap<>();
                datasetmap.put("label", statusColor.toString());
                switch (statusColor.toString().toUpperCase()) {
                    case "PASS":
                        datasetmap.put("data", passCount);
                        datasetmap.put("backgroundColor", statusColor.color);
                        break;
                    case "FAIL":
                        datasetmap.put("data", failCount);
                        datasetmap.put("backgroundColor", statusColor.color);
                        break;
                    case "WARN":
                        datasetmap.put("data", warnCount);
                        datasetmap.put("backgroundColor", statusColor.color);
                        break;
                    case "INFO":
                        datasetmap.put("data", infoCount);
                        datasetmap.put("backgroundColor", statusColor.color);
                        break;
                    case "ERR":
                        datasetmap.put("data", errCount);
                        datasetmap.put("backgroundColor", statusColor.color);
                        break;
                    case "EXE":
                        datasetmap.put("data", exeCount);
                        datasetmap.put("backgroundColor", statusColor.color);
                        break;
                }
                datasets.add(datasetmap);
            }
            suiteBarGraph.put("ids", ids);
            suiteBarGraph.put("click", true);
            suiteBarGraph.put("datasets", datasets);
            suiteBarGraph.put("size", suiteFailCount);
            return suiteBarGraph;
        } else {
            return null;
        }
    }

    public static Map<String, Long> culprit(List<SuiteExeDto> getAllSuites) {
        if (getAllSuites.size() == 0) {
            return null;
        }
        List<String> srunIds = getAllSuites.stream().map(x -> x.getS_run_id()).collect(Collectors.toList());

        Query query = new Query(Criteria.where("s_run_id").in(srunIds));
        List<TestExeDto> testExeList = mongoOperations.find(query, TestExeDto.class);
        Map<String, Long> totalCountMap = new HashMap<>();
        Map<String, Long> failCountMap = new HashMap<>();
        Map<String, Long> finalMap = new HashMap<>();
        String testCaseName = null;
        long percentage = 0L;
        long averagePercentage = 0L;

        for (TestExeDto testExe : testExeList) {
            if (testExe.getStatus().toUpperCase().equals("FAIL") || testExe.getStatus().toUpperCase().equals("EXE")) {
                failCountMap.put(testExe.getName(), failCountMap.getOrDefault(testExe.getName(), 0L) + 1);
            }
            totalCountMap.put(testExe.getName(), totalCountMap.getOrDefault(testExe.getName(), 0L) + 1);
        }
        if (failCountMap.size() == 0) {
            return null;
        }

        for (String name : totalCountMap.keySet()) {
            long failCount = failCountMap.getOrDefault(name, 0L);
            long totalCount = totalCountMap.getOrDefault(name, 0L);
            long failedPercentage = (failCount * 100) / totalCount;
            if (failedPercentage > percentage) {
                testCaseName = name;
                percentage = failedPercentage;
            }
            if (failedPercentage > 50) {
                finalMap.put(name, failedPercentage);
                averagePercentage = averagePercentage + percentage;
            }
        }

        if (finalMap.size() == 0 && percentage > 0) {
            finalMap.put(testCaseName, percentage);
            averagePercentage = averagePercentage + percentage;
        }
        if (finalMap.size() > 0) {
            averagePercentage = averagePercentage / finalMap.size();
            finalMap.put("average", Long.valueOf((String.valueOf(averagePercentage))));
            return finalMap;
        }
        return null;
    }

    public static double getQAScore(List<SuiteExeDto> getAllSuites) {
        List<String> sRunIdsList = new ArrayList<>();
        double suiteErrCount = 0;
        double testcaseErrCount = 0;
        double falsePositive = 0;

        for (SuiteExeDto suiteExe : getAllSuites) {

            if (suiteExe.getS_run_id().equalsIgnoreCase("TEST-PROJECT_Beta_ce3192d6-9634-424c-97bc-d08caf344401")) {
                System.out.println("truuuuuu");
            }
            sRunIdsList.add(suiteExe.getS_run_id());
            if (suiteExe.getStatus().equalsIgnoreCase("ERR")) {
                suiteErrCount++;
            }
            if (suiteExe.getClassificationDetails() != null
                    && suiteExe.getClassificationDetails().getClassification() != null
                    && suiteExe.getClassificationDetails().isChildFalsePostiveStatus()) {
                falsePositive++;
            }
        }

        Query errTestCasesCountQuery = new Query(
                Criteria.where("status").in("ERR").and("s_run_id").in(sRunIdsList));
        Query testCaseCountQuery = new Query(Criteria.where("s_run_id").in(sRunIdsList));

        Query falsePositiveCountQuery = new Query(
                Criteria.where("classificationDetails.childFalsePostiveStatus").is(true).and("s_run_id")
                        .in(sRunIdsList));

        double totalTestCaseCount = mongoOperations.count(testCaseCountQuery, TestExeDto.class);
        double errTestCaseCount = mongoOperations.count(errTestCasesCountQuery, TestExeDto.class);

        double falsePositiveTestCount = mongoOperations.count(falsePositiveCountQuery, TestExeDto.class);

        double finalSuiteScore = (((999 * 0.5) * (getAllSuites.size() - suiteErrCount)) / getAllSuites.size());
        double finalTestcaseScore = ((((999 * 0.5) * (totalTestCaseCount - errTestCaseCount)) / totalTestCaseCount));

        if (falsePositive != 0.0) {
            double falsePercentage = (getAllSuites.size() * falsePositive) / 100;
            finalSuiteScore = finalSuiteScore - (399.6 * falsePercentage) / 100;
        }
        if (falsePositiveTestCount != 0.0) {
            double falsePercentage = (totalTestCaseCount * falsePositiveTestCount) / 100;
            finalTestcaseScore = finalTestcaseScore - (399.6 * falsePercentage) / 100;
        }

        return Math.round(finalSuiteScore + finalTestcaseScore);
    }

    public static String convertLongToTime(double seconds) {
        String res = "";
        DecimalFormat df = new DecimalFormat("#");
        if (seconds < 60) {
            res = seconds + " sec(s)";
        } else if (seconds >= 60 && seconds < 3600) {
            String ans = df.format(Math.floor(seconds / 60)) + "min";

            res = ans;
        } else if (seconds >= 60 && seconds < 86400) {
            String ans = df.format(Math.floor(seconds / 3600)) + "hr ";
            if (Math.floor((seconds % 3600) / 60) != 0) {
                ans = ans + df.format(Math.floor((seconds % 3600) / 60)) + "m";
            }

            res = ans;
        } else {
            String ans = df.format(Math.floor(seconds / 86400)) + "d ";
            if (Math.floor((seconds % 86400) / 3600) != 0) {
                ans = ans + df.format(Math.floor((seconds % 86400) / 3600)) + "hr";
            }

            res = ans;
        }

        return res;
    }

    public static String getFailingSinceForSuiteExe(List<SuiteExeDto> suites, double brokenIndex) {
        if (brokenIndex == 0) {
            return "No Issues";
        } else if (brokenIndex == 1) {
            return "Never Fixed";
        } else {
            int count = 0;
            for (SuiteExeDto suite : suites) {
                if (!suite.getStatus().equalsIgnoreCase("FAIL")) {
                    break;
                }
                count++;

            }
            if (count == 0) {
                return "No Issues";
            }
            return "Last " + count + " Runs";
        }
    }

    public static Long getLastPassForSuiteExe(List<SuiteExeDto> suites) {
        for (SuiteExeDto suite : suites) {
            if (suite.getStatus().equalsIgnoreCase("PASS")) {
                return suite.getS_start_time();
            }

        }
        return 0L;
    }

    public static String lastRunStatusForSuiteExe(List<SuiteExeDto> suites) {
        return suites.get(0).getStatus();
    }

    public static Map<String, Long> lastStatusDetails(List<SuiteExeDto> suites) {
        String s_run_id = suites.get(0).getS_run_id();
        Query query = new Query(Criteria.where("s_run_id").is(s_run_id));
        List<TestExeDto> TestcaseDetails = mongoOperations.find(query, TestExeDto.class);
        Map<String, Long> statusMap = new HashMap<>();
        for (StatusColor statusColor : StatusColor.values()) {
            statusMap.put(statusColor.toString(), 0L);
        }
        long totalCount = 0;
        for (TestExeDto testExe : TestcaseDetails) {
            if (testExe.getStatus().toUpperCase().equals(StatusColor.PASS.toString())) {
                long value = statusMap.get(StatusColor.PASS.toString()) + 1;
                statusMap.put(StatusColor.PASS.toString(), value);
                totalCount++;
                continue;
            }
            if (testExe.getStatus().toUpperCase().equals(StatusColor.FAIL.toString())) {
                long value = statusMap.get(StatusColor.FAIL.toString()) + 1;
                statusMap.put(StatusColor.FAIL.toString(), value);
                totalCount++;
                continue;
            }
            if (testExe.getStatus().toUpperCase().equals(StatusColor.EXE.toString())) {
                long value = statusMap.get(StatusColor.EXE.toString()) + 1;
                statusMap.put(StatusColor.EXE.toString(), value);
                totalCount++;
                continue;
            }
            if (testExe.getStatus().toUpperCase().equals(StatusColor.ERR.toString())) {
                long value = statusMap.get(StatusColor.ERR.toString()) + 1;
                statusMap.put(StatusColor.ERR.toString(), value);
                totalCount++;
                continue;
            }
            if (testExe.getStatus().toUpperCase().equals(StatusColor.INFO.toString())) {
                long value = statusMap.get(StatusColor.INFO.toString()) + 1;
                statusMap.put(StatusColor.INFO.toString(), value);
                totalCount++;
                continue;
            }
            if (testExe.getStatus().toUpperCase().equals(StatusColor.WARN.toString())) {
                long value = statusMap.get(StatusColor.WARN.toString()) + 1;
                statusMap.put(StatusColor.WARN.toString(), value);
                totalCount++;
                continue;
            }
        }
        return statusMap;
    }

    public static Object createExecutionHeadersDataWithVarinceAndFalsePositive(SuiteExeDto getSuite, Map<String, Object> iconMap) {

        Map<String, Object> result = new HashMap<String, Object>();
        List<Object> mainData = new ArrayList<Object>();
        List<String> headers = new ArrayList<String>();

        Collections.addAll(headers, "Status", "Project Name", "Env", "Report Name", "Start Time", "End Time", "Duration");
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Object> varianceSubHeaders = new HashMap<>();
        Map<String, Object> statusSubType = new HashMap<>();
        statusSubType.put("subType", "falseVariance");
        data.put("Project Name", createCustomObject(StringUtils.capitalize(getSuite.getProject_name()), "text", getSuite.getProject_name(), "center"));
        data.put("Env", createCustomObject(StringUtils.capitalize(getSuite.getEnv()), "text", getSuite.getEnv(), "center"));
        data.put("Report Name", createCustomObject(StringUtils.capitalize(getSuite.getReport_name()), "text", getSuite.getReport_name(), "center"));
        Map<String, Object> timereport = new HashMap<>();
        timereport.put("subType", "datetime");
        data.put("Start Time", createCustomObject(getSuite.getS_start_time(), "date", getSuite.getS_start_time(), "center", timereport));
        if (getSuite.getS_end_time() != 0) {

            data.put("End Time", createCustomObject(getSuite.getS_end_time(), "date", getSuite.getS_end_time(), "center", timereport));
            data.put("Duration", createCustomObject(getDuration(getSuite.getS_start_time(), getSuite.getS_end_time()), "text", ((float) (getSuite.getS_end_time() - getSuite.getS_start_time()) / 1000), "center"));
        } else {
            data.put("End Time", createCustomObject("-", "text", getSuite.getS_end_time(), "center"));
            data.put("Duration", createCustomObject("-", "text", 0, "center"));
        }
        if (iconMap != null && iconMap.size() > 0) {
            data.put("ICON", iconMap);
            data.put("ISCLICKABLE", createCustomObject(false, "text", false, "left"));
        }
        mainData.add(data);
        result.put("headers", headers);
        result.put("data", mainData);
        return result;
    }

    public static Object createInfraHeadersData(SuiteExeDto getSuite) {
        Map<String, Object> result = new HashMap<>();
        List<String> headers = new ArrayList<>();
        List<Object> mainData = new ArrayList<>();
        String user = "";
        String machine = "";
        String run_type = "";
        String run_mode = "";
        Set<String> userSet = new HashSet<>();
        Set<String> machineSet = new HashSet<>();
        Set<String> run_typeSet = new HashSet<>();
        Set<String> run_modeSet = new HashSet<>();
        Collections.addAll(headers, "Executed By", "Executed On", "Run Type", "Run Mode");
        Map<String, Object> data = new HashMap<String, Object>();
        Query query = new Query(Criteria.where("s_run_id").is(getSuite.getS_run_id()));
        List<TestExeDto> testcaseList = mongoOperations.find(query, TestExeDto.class);
        for (TestExeDto t : testcaseList) {
            if (!userSet.contains(t.getInvoke_user())) {
                if (t.getInvoke_user() != null) {
                    user = user + t.getInvoke_user() + ",";
                    userSet.add(t.getInvoke_user());
                }
            }
            if (!run_modeSet.contains(t.getRun_mode())) {
                if (t.getRun_mode() != null) {
                    run_mode = run_mode + t.getRun_mode() + ",";
                    run_modeSet.add(t.getRun_mode());
                }
            }
            if (!run_typeSet.contains(t.getRun_type().toUpperCase(Locale.ROOT))) {
                if (t.getRun_type() != null) {
                    run_type = run_type + t.getRun_type() + ",";
                    run_typeSet.add(t.getRun_type().toUpperCase(Locale.ROOT));
                }
            }
            if (!machineSet.contains(t.getMachine())) {
                if (t.getMachine() != null) {
                    machine = machine + t.getMachine() + ",";
                    machineSet.add(t.getMachine());
                }
            }
        }
        if (!(user.equals("null,") || user.length() == 0)) {
            user = user.substring(0, user.length() - 1);
            data.put("Executed By", createCustomObject(user, "text", user, "left"));
        } else {
            data.put("Executed By", createCustomObject("-", "text", "-", "left"));
        }
        if (!(machine.equals("null,") || machine.length() == 0)) {
            machine = machine.substring(0, machine.length() - 1);
            data.put("Executed On", createCustomObject(machine, "text", machine, "left"));
        } else {
            data.put("Executed On", createCustomObject("-", "text", "-", "left"));
        }
        if (!(run_type.equals("null,") || run_type.length() == 0)) {
            run_type = run_type.substring(0, run_type.length() - 1);
            data.put("Run Type", createCustomObject(run_type, "text", run_type, "left"));
        } else {
            data.put("Run Type", createCustomObject("-", "text", "-", "left"));
        }
        if (!(run_mode.equals("null,") || run_mode.length() == 0)) {
            run_mode = run_mode.substring(0, run_mode.length() - 1);
            data.put("Run Mode", createCustomObject(run_mode, "text", run_mode, "left"));
        } else {
            data.put("Run Mode", createCustomObject("-", "text", "-", "left"));
        }
        mainData.add(data);
        result.put("headers", headers);
        result.put("data", mainData);
        return result;
    }

    public static Map<String, Object> Last5RunsStackedBarChartBySuiteExe(SuiteExeDto getSuite) {
        Query barChartQuery = new Query(Criteria.where("report_name").is(getSuite.getReport_name())
                .andOperator(Criteria.where("env").is(getSuite.getEnv())));
        Pageable barChartPageable = PageRequest.of(0, 5);
        Sort.Order barChartOrder = new Sort.Order(Sort.Direction.DESC, "s_start_time");
        barChartQuery.with(barChartPageable);
        barChartQuery.with(Sort.by(barChartOrder));
        List<SuiteExeDto> suiteExes = mongoOperations.find(barChartQuery, SuiteExeDto.class);
        if (suiteExes.size() > 0) {
            List<Long> passCount = new ArrayList<>();
            List<Long> failCount = new ArrayList<>();
            List<Long> warnCount = new ArrayList<>();
            List<Long> infoCount = new ArrayList<>();
            List<Long> errCount = new ArrayList<>();
            List<Long> exeCount = new ArrayList<>();
            List<Long> labels = new ArrayList<>();
            for (SuiteExeDto suiteExe : suiteExes) {
                Query varianceQuery = new Query(Criteria.where("varianceId").in(suiteExe.getVarianceIds())
                        .and("varianceStatus").is("ACTIVE").and("endDate").gt(new Date().getTime()));
                List<VarianceClassificationDto> varianceClassificationList = mongoOperations.find(varianceQuery,
                        VarianceClassificationDto.class);
                Map<Long, VarianceClassificationDto> variannceList = new HashMap<>();
                List<Long> varinaceIds = new ArrayList<>();
                for (VarianceClassificationDto varianceClassification : varianceClassificationList) {
                    varinaceIds.add(varianceClassification.getVarianceId());
                    variannceList.put(varianceClassification.getVarianceId(), varianceClassification);
                }
                Query query1 = new Query(Criteria.where("s_run_id").is(suiteExe.getS_run_id()));
                List<TestExeDto> testExeList = mongoOperations.find(query1, TestExeDto.class);
                Map<String, Long> statusMap = new HashMap<>();
                for (StatusColor statusColor : StatusColor.values()) {
                    statusMap.put(statusColor.toString(), 0L);
                }
                long totalCount = 0L;
                for (TestExeDto testExe : testExeList) {
                    if (testExe.getVarianceId() != null
                            || (testExe.getStepVarianceIds() != null && testExe.getStepVarianceIds().size() > 0)
                            || testExe.getClassificationDetails() != null) {
                        if (testExe.getVarianceId() != null
                                || (testExe.getStepVarianceIds() != null && testExe.getStepVarianceIds().size() > 0)) {
                            VarianceClassificationDto varianceClassification = variannceList
                                    .getOrDefault(testExe.getVarianceId(), null);
                            if (varianceClassification != null) {
                                testExe.setStatus("PASS");
                            }
                            if (checkoneListContainsElementOfAnotherList(varinaceIds,
                                    testExe.getStepVarianceIds())) {
                                testExe.setStatus(checkStatusOfTestCaseByStepsIfVarianceIsThere(
                                        testExe.getTc_run_id(), variannceList));
                            }
                        }
                    }
                    if (testExe.getStatus().toUpperCase().equals(StatusColor.PASS.toString())) {
                        long value = statusMap.get(StatusColor.PASS.toString()) + 1;
                        statusMap.put(StatusColor.PASS.toString(), value);
                        totalCount++;
                        continue;
                    }
                    if (testExe.getStatus().toUpperCase().equals(StatusColor.FAIL.toString())) {
                        long value = statusMap.get(StatusColor.FAIL.toString()) + 1;
                        statusMap.put(StatusColor.FAIL.toString(), value);
                        totalCount++;
                        continue;
                    }
                    if (testExe.getStatus().toUpperCase().equals(StatusColor.EXE.toString())) {
                        long value = statusMap.get(StatusColor.EXE.toString()) + 1;
                        statusMap.put(StatusColor.EXE.toString(), value);
                        totalCount++;
                        continue;
                    }
                    if (testExe.getStatus().toUpperCase().equals(StatusColor.ERR.toString())) {
                        long value = statusMap.get(StatusColor.ERR.toString()) + 1;
                        statusMap.put(StatusColor.ERR.toString(), value);
                        totalCount++;
                        continue;
                    }
                    if (testExe.getStatus().toUpperCase().equals(StatusColor.INFO.toString())) {
                        long value = statusMap.get(StatusColor.INFO.toString()) + 1;
                        statusMap.put(StatusColor.INFO.toString(), value);
                        totalCount++;
                        continue;
                    }
                    if (testExe.getStatus().toUpperCase().equals(StatusColor.WARN.toString())) {
                        long value = statusMap.get(StatusColor.WARN.toString()) + 1;
                        statusMap.put(StatusColor.WARN.toString(), value);
                        totalCount++;
                        continue;
                    }

                }

                for (StatusColor statusColor : StatusColor.values()) {
                    switch (statusColor.toString()) {
                        case "PASS":
                            passCount.add(statusMap.get(statusColor.toString()));
                            break;
                        case "FAIL":
                            failCount.add(statusMap.get(statusColor.toString()));
                            break;
                        case "WARN":
                            warnCount.add(statusMap.get(statusColor.toString()));
                            break;
                        case "INFO":
                            infoCount.add(statusMap.get(statusColor.toString()));
                            break;
                        case "ERR":
                            errCount.add(statusMap.get(statusColor.toString()));
                            break;
                        case "EXE":
                            exeCount.add(statusMap.get(statusColor.toString()));
                            break;
                    }

                }
                labels.add(suiteExe.getS_start_time());
            }

            Map<String, Object> suiteBarGraph = new HashMap<>();
            suiteBarGraph.put("labels", labels);
            List<Map<String, Object>> datasets = new ArrayList<>();

            for (StatusColor statusColor : StatusColor.values()) {
                Map<String, Object> datasetmap = new HashMap<>();
                datasetmap.put("label", statusColor.toString());
                switch (statusColor.toString().toUpperCase()) {
                    case "PASS":
                        datasetmap.put("data", passCount);
                        datasetmap.put("backgroundColor", statusColor.color);
                        break;
                    case "FAIL":
                        datasetmap.put("data", failCount);
                        datasetmap.put("backgroundColor", statusColor.color);
                        break;
                    case "WARN":
                        datasetmap.put("data", warnCount);
                        datasetmap.put("backgroundColor", statusColor.color);
                        break;
                    case "INFO":
                        datasetmap.put("data", infoCount);
                        datasetmap.put("backgroundColor", statusColor.color);
                        break;
                    case "ERR":
                        datasetmap.put("data", errCount);
                        datasetmap.put("backgroundColor", statusColor.color);
                        break;
                    case "EXE":
                        datasetmap.put("data", exeCount);
                        datasetmap.put("backgroundColor", statusColor.color);
                        break;
                }
                datasets.add(datasetmap);
            }
            suiteBarGraph.put("datasets", datasets);
            Map<String, Object> stackedBarChartType = new HashMap<>();
            stackedBarChartType.put("subType", "stacked_bar_chart");
            stackedBarChartType.put("heading", "Last 5 Suite Runs");
            return createCustomObject(suiteBarGraph, "chart", suiteExes.size(), "center",
                    stackedBarChartType);

        }
        return null;
    }

    public static Map<String, Object> testCaseInfoDoughnutChart(List<String> statues) {
        Map<String, Long> pieData = new HashMap<>();
        for (String status : statues) {
            pieData.put(status.toUpperCase(),
                    pieData.getOrDefault(status.toUpperCase(), 0L) + 1);

        }
        if (pieData != null && !pieData.keySet().isEmpty()) {
            List<String> labels = new ArrayList<>();
            labels.addAll(pieData.keySet());
            for (String key : labels) {
                pieData.replace(key, ((Number) pieData.get(key)).longValue());
            }
            long totalcount = statues.size();
            Map<String, Object> doughnutSubType = new HashMap<>();
            doughnutSubType.put("subType", "doughnut_chart");
            doughnutSubType.put("heading", "Total Testcase(s)");
            return createCustomObject(createDoughnutChart(pieData),
                    "chart", totalcount, "center", doughnutSubType);
        }
        return null;
    }

    public static Map<String, Object> categoryStackedBarChartByS_run_id(Map<String, Long> data, Set<String> category) {
        List<Long> passCount = new ArrayList<>();
        List<Long> failCount = new ArrayList<>();
        List<Long> warnCount = new ArrayList<>();
        List<Long> infoCount = new ArrayList<>();
        List<Long> errCount = new ArrayList<>();
        List<Long> exeCount = new ArrayList<>();
        long maxCount = 0;
        if (category.size() > 1) {
            for (String category1 : category) {
                Map<String, Long> barcount = new HashMap<>();
                for (StatusColor statusColor : StatusColor.values()) {
                    barcount.put(statusColor.toString(), 0L);
                    barcount.put(statusColor.toString(),
                            data.getOrDefault(category1.toUpperCase() + "_" + statusColor.toString(), 0L));
                }
                passCount.add(barcount.get(StatusColor.PASS.toString()));
                failCount.add(barcount.get(StatusColor.FAIL.toString()));
                warnCount.add(barcount.get(StatusColor.WARN.toString()));
                infoCount.add(barcount.get(StatusColor.INFO.toString()));
                errCount.add(barcount.get(StatusColor.ERR.toString()));
                exeCount.add(barcount.get(StatusColor.EXE.toString()));
            }
            Map<String, Object> categoryBarGraph = new HashMap<>();

            categoryBarGraph.put("labels", category);
            List<Map<String, Object>> datasets = new ArrayList<>();

            for (StatusColor statusColor : StatusColor.values()) {
                Map<String, Object> datasetmap = new HashMap<>();
                datasetmap.put("label", statusColor.toString());
                switch (statusColor.toString().toUpperCase()) {
                    case "PASS":
                        datasetmap.put("data", passCount);
                        datasetmap.put("backgroundColor", statusColor.color);
                        break;
                    case "FAIL":
                        datasetmap.put("data", failCount);
                        datasetmap.put("backgroundColor", statusColor.color);
                        break;
                    case "WARN":
                        datasetmap.put("data", warnCount);
                        datasetmap.put("backgroundColor", statusColor.color);
                        break;
                    case "INFO":
                        datasetmap.put("data", infoCount);
                        datasetmap.put("backgroundColor", statusColor.color);
                        break;
                    case "ERR":
                        datasetmap.put("data", errCount);
                        datasetmap.put("backgroundColor", statusColor.color);
                        break;
                    case "EXE":
                        datasetmap.put("data", exeCount);
                        datasetmap.put("backgroundColor", statusColor.color);
                        break;
                }
                datasets.add(datasetmap);
            }
            categoryBarGraph.put("datasets", datasets);
            Map<String, Object> stackedBarChartType = new HashMap<>();
            stackedBarChartType.put("subType", "stacked_bar_chart");
            stackedBarChartType.put("heading", "Category Wise Status");
            return createCustomObject(categoryBarGraph,
                    "chart", category.size(), "center", stackedBarChartType);
        } else {
            return null;
        }
    }

}
