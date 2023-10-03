package com.jewel.reportmanager.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jewel.reportmanager.dto.*;
import com.jewel.reportmanager.entity.RuleApi;
import com.jewel.reportmanager.entity.VarianceClassification;
import com.jewel.reportmanager.enums.StatusColor;
import com.jewel.reportmanager.enums.UserRole;
import com.jewel.reportmanager.exception.CustomDataException;
import com.mongodb.BasicDBObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.jewel.reportmanager.enums.OperationType.Failure;
import static com.jewel.reportmanager.enums.ProjectAccessType.ADMIN;
import static com.jewel.reportmanager.utils.ReportResponseConstants.USER_DETAILS_NOT_FOUND;
import static javax.accessibility.AccessibleState.ACTIVE;

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
            throw new CustomDataException(USER_DETAILS_NOT_FOUND, null, Failure, HttpStatus.OK);
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

    public static ProjectDto validateProject(Long pid) {
        Query query = new Query(Criteria.where("pid").is(pid).and("status").is("ACTIVE"));
        return getProjectByPidAndStatus(pid, ACTIVE.toString());
    }

    public static ProjectDto getProjectByPidAndStatus(Long pid, String status) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("pid", pid);
        uriVariables.put("status", status);
        return (ProjectDto) RestClient.getApi(projectManagerUrl + "/v2/project/pid/status?pid={pid}&status={status}", httpEntity, Project.class, uriVariables).getBody();
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

    public static double brokenIndex(List<TestExeCommonDto> testExes) {

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
        // System.out.println(count+""+getSuites.size());
        if (count == testExes.size()) {
            return 1;
        }
        double res = Math.round((float) (numenator / denom) * 100.0) / 100.0;
        return res;
    }

    public static String averageFixTime(List<TestExeCommon> testExes) {

        TestExeCommon curr = null;
        TestExeCommon prev = null;
        List<Long> averageTimes = new ArrayList<Long>();

        for (TestExeCommon testExe : testExes) {

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
            // if (Math.floor((((average % 86400))%3600) / 60) != 0) {
            // ans = ans + df.format(Math.floor( ((average % 86400))%3600) / 60) + " min ";
            // }
            // if (Math.floor((((average%86400)%3600)%60)) != 0) {
            // ans = ans + df.format((((average%86400)%3600)%60)) + " sec(s)";
            // }

            res = ans;
        }

        if (res.equals("") || sum == 0) {
            res = "Never Broken";
        }
        return res;
    }

    public static String lastRunStatus(List<TestExeCommon> testExes) {
        return testExes.get(0).getStatus();
    }

    public static String getFailingSince(List<TestExeCommon> testExes, double brokenIndex) {
        if (brokenIndex == 0) {
            return "No Issues";
        } else if (brokenIndex == 1) {
            return "Never Fixed";
        } else {
            int count = 0;
            for (TestExeCommon testExe : testExes) {
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

    public static Long getLastPass(List<TestExeCommon> testExes) {
        for (TestExeCommon testExe : testExes) {
            if (testExe.getStatus().equalsIgnoreCase("PASS")) {
                // System.out.println(testExe.getTc_run_id());
                return testExe.getStart_time();
            }

        }
        return 0L;
    }

    public static String getDownTime(List<TestExeCommon> testExes) {
        if (testExes.get(0).getStatus().equalsIgnoreCase("PASS")) {
            return "No Issues";
        } else {
            TestExeCommon prev = null;
            long firstFailTime = 0;
            for (TestExeCommon testExe : testExes) {
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
                // if (Math.floor((((downTime % 86400))%3600) / 60) != 0) {
                // ans = ans + df.format(Math.floor( ((downTime % 86400))%3600) / 60) + " min ";
                // }
                // if (Math.floor((((downTime%86400)%3600)%60)) != 0) {
                // ans = ans + df.format((((downTime%86400)%3600)%60)) + " sec(s)";
                // }

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

        Map<String, Object> map = new HashMap<String, Object>();
        List<String> labels = new ArrayList<String>();
        labels.addAll(res.keySet());

        List<Object> datasets = new ArrayList<Object>();
        Map<String, Object> datasetValue = new HashMap<String, Object>();
        List<Long> data = new ArrayList<Long>();
        List<String> backgroundColor = new ArrayList<String>();
        List<String> borderColor = new ArrayList<String>();
        for (String status : labels) {
            data.add((Long) res.get(status));
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

    public double getScore(double brokenIndex) {
        return Math.round((999 - (999 * (0.35 * brokenIndex))) * 100.0) / 100.0;
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

        long count = mongoOperations.count(query, TestExe.class);
        return count;
    }

    public Map<String, Long> getStatuswiseCountReportName(String report_name, long starttime, long endtime) {
        Map<String, Long> res = new HashMap<String, Long>();
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<Criteria>();
        criteria.add(Criteria.where("report_name").is(report_name));
        criteria.add(Criteria.where("start_time").gt(starttime));
        criteria.add(Criteria.where("end_time").lt(endtime));

        // if(!(status.equalsIgnoreCase("TOTAL"))){
        // criteria.add(Criteria.where("status").is(status));
        // }
        // query.addCriteria(new Criteria().andOperator(criteria.toArray(new
        // Criteria[criteria.size()])));
        List<String> disticntStatus = mongoOperations.findDistinct(query, "status", TestExeCommon.class, String.class);
        // long totalcount=mongoOperations.count(query, SuiteExe.class);
        // res.put("TOTAL RUN", totalcount);
        for (String status : disticntStatus) {
            Query queryStatus = new Query();
            List<Criteria> criteriaStatus = new ArrayList<Criteria>();
            criteriaStatus.add(Criteria.where("report_name").is(report_name));
            criteriaStatus.add(Criteria.where("start_time").gt(starttime));
            criteriaStatus.add(Criteria.where("end_time").lt(endtime));
            criteriaStatus.add(Criteria.where("status").is(status));
            queryStatus.addCriteria(new Criteria().andOperator(criteriaStatus.toArray(new Criteria[criteria.size()])));
            res.put(status.toUpperCase(), mongoOperations.count(queryStatus, TestExeCommon.class));

        }
        // res.put("TOTAL RUN", mongoOperations.count(query, SuiteExe.class));
        return res;
    }

    public static List<TestExeCommon> getSortedList(List<TestExeCommon> list) {
        List<TestExeCommon> testExes = new ArrayList<TestExeCommon>();
        testExes.addAll(list);
        Collections.sort(testExes, new TimeComparatorTestExeCommon());
        return testExes;
    }

    public TestExe createManualTestcase(Test test, String s_run_id, HttpServletRequest request) {
        Long start_time = System.currentTimeMillis();
        List<Map<String, Object>> data = new ArrayList<>();
        ObjectMapper oMapper = new ObjectMapper();

        TestExe testExe = new TestExe();
        testExe.setS_run_id(s_run_id);
        testExe.setTc_run_id(test.getName() + "_" + UUID.randomUUID());
        testExe.setName(test.getName());
        testExe.setTestcase_id(test.getTc_id());
        testExe.setStatus(StatusColor.EXE.toString());
        testExe.setStart_time(start_time);
        testExe.setCategory(test.getCategory());
        testExe.setProduct_type(test.getTestcaseType().name());
        testExe.setRun_type(test.getTestcaseType().name());
        testExe.setRun_mode("JEWEL");
        List<Map<String, Object>> metadata = new ArrayList<>();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("TESTCASE NAME", test.getName());
        metadata.add(map1);
        testExe.setMeta_data(metadata);
        List<Object> steps = new ArrayList<>();
        int index = 1;
        for (TestSteps testStep : test.getTestSteps()) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("id", index);
            map.put("status", StatusColor.EXE.toString());
            map.put("step name", testStep.getName());
            map.put("step description", testStep.getDescription());
            map.put("expected result", testStep.getExpectedResult());
            steps.add(map);
            index++;
        }
        Steps steps1 = new Steps();
        steps1.setTc_run_id(testExe.getTc_run_id());
        steps1.setSteps(steps);
        steps1.setS_run_id(testExe.getS_run_id());
        stepsRepository.save(steps1);
        return testExe;
    }

    private List<Map<String, Object>> getLinkedSteps(List<Map<String, Object>> data, Long isolatedVersionId, int id,
                                                     boolean finalSteps) {
        if (data == null) {
            return null;
        }
        System.out.println("working");
        Query query = new Query(Criteria.where("isolatedVersionId").is(isolatedVersionId).and("isDeleted").is(false)
                .and("testcaseType").is(TestCaseType.MANUAL.toString()));
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
                                                                       Map<Long, VarianceClassification> data) {
        Query query = new Query(Criteria.where("tc_run_id").is(tc_run_id));
        Steps steps = mongoOperations.findOne(query, Steps.class);
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

    public List<Object> addVarianceInSteps(List<Object> steps, SuiteExeDto suite, TestExeDto testExe) {
        Query varianceQuery = new Query(Criteria.where("pid").is(suite.getP_id()).and("reportName")
                .is(suite.getReport_name()).and("category").is("Step").and("env").is(suite.getEnv())
                .and("varianceStatus").is("ACTIVE").and("endDate").gt(suite.getS_start_time()));
        List<VarianceClassification> varianceClassifications = mongoOperations.find(varianceQuery,
                VarianceClassification.class);
        List<Object> data = steps;

        int index = 0;
        for (Object step : steps) {
            Map<String, Object> stepData = (Map<String, Object>) step;
            stepData.put("id", index + 1);
            data.set(index, stepData);
            index++;
        }
        for (VarianceClassification varianceClassification : varianceClassifications) {
            index = 0;
            for (Object step : steps) {
                Map<String, Object> stepData = (Map<String, Object>) step;
                if (varianceClassification.getMatch().equalsIgnoreCase("EXACT") && ((stepData.containsKey("step name")
                        && stepData.get("step name").toString().equals(varianceClassification.getName()))
                        || (stepData.containsKey("Step Name")
                        && stepData.get("Step Name").toString().equals(varianceClassification.getName())))) {
                    if (stepData.containsKey("status")
                            && (stepData.get("status").toString().equalsIgnoreCase("FAIL")
                            || stepData.get("status").toString().equalsIgnoreCase("ERR"))) {
                        stepData.put("VARIANCEID", varianceClassification.getVarianceId());
                        Set<Long> varianceIds = suite.getVarianceIds();
                        varianceIds.add(varianceClassification.getVarianceId());
                        suite.setVarianceIds(varianceIds);
                        List<Long> testVarianceIds = testExe.getStepVarianceIds();
                        testVarianceIds.add(varianceClassification.getVarianceId());
                        testExe.setStepVarianceIds(testVarianceIds);
                        data.set(index, stepData);
                    }
                } else if (varianceClassification.getMatch().equalsIgnoreCase("Like")
                        && ((stepData.containsKey("step name")
                        && stepData.get("step name").toString().toUpperCase()
                        .contains(varianceClassification.getName().toUpperCase()))
                        || (stepData.containsKey("Step Name")
                        && stepData.get("Step Name").toString().toUpperCase()
                        .equals(varianceClassification.getName().toUpperCase())))
                        && (stepData.get("status").toString().equalsIgnoreCase("FAIL")
                        || stepData.get("status").toString().equalsIgnoreCase("ERR"))) {
                    stepData.put("VARIANCEID", varianceClassification.getVarianceId());
                    Set<Long> varianceIds = suite.getVarianceIds();
                    varianceIds.add(varianceClassification.getVarianceId());
                    suite.setVarianceIds(varianceIds);
                    List<Long> testVarianceIds = testExe.getStepVarianceIds();
                    testVarianceIds.add(varianceClassification.getVarianceId());
                    testExe.setStepVarianceIds(testVarianceIds);
                    data.set(index, stepData);
                }
                index++;
            }
        }
        return data;
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

    class TimeComparatorTestExeCommon implements Comparator<TestExeCommonDto> {

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


    public static Object createInfraAndUserHeaders(List<TestExeDto> testcaseList, SuiteExeDto getSuite, String dataCategory) {
        StringBuilder machine = new StringBuilder();
        String os = getSuite.getOs();
        StringBuilder base_user = new StringBuilder();
        StringBuilder token_user = new StringBuilder();
        String framework_version = getSuite.getFramework_version();
        String framework_name = getSuite.getFramework_name();

        Set<String> machineSet = new HashSet<>();
        Set<String> baseUserSet = new HashSet<>();
        Set<String> tokenUserSet = new HashSet<>();
        Map<String, Object> data = new HashMap<>();

        if (dataCategory.equalsIgnoreCase("infraDetails")) {
            for (TestExeDto t : testcaseList) {
                if (t.getMachine() != null && (!machineSet.contains(t.getMachine()))) {
                    machine.append(t.getMachine()).append(", ");
                    machineSet.add(t.getMachine());

                }

            }

            if (!(machine.toString().equals("null, ") || machine.length() == 0)) {
                machine = new StringBuilder(machine.substring(0, machine.length() - 2));
                data.put("Machine", machine.toString());
            } else if (getSuite.getMachine() != null) {
                data.put("Machine", getSuite.getMachine());
            } else {
                data.put("Machine", null);
            }

            if (!(os == null || os.length() == 0)) {
                data.put("OS", os);
            } else {
                data.put("OS", null);
            }

            if (!(framework_name == null || framework_name.length() == 0) && !(framework_version == null || framework_version.length() == 0)) {
                data.put("Framework", framework_name + " " + framework_version);
            }


        }

        if (dataCategory.equalsIgnoreCase("userDetails")) {

            for (TestExeDto t : testcaseList) {
                if (t.getToken_user() != null && (!tokenUserSet.contains(t.getToken_user().toArray(new String[t.getToken_user().size()])[0]))) {
                    token_user.append(t.getToken_user().toArray(new String[t.getToken_user().size()])[0]).append(",");
                    tokenUserSet.add(t.getToken_user().toArray(new String[t.getToken_user().size()])[0]);

                }
                if (t.getBase_user() != null && (!baseUserSet.contains(t.getBase_user()))) {
                    base_user.append(t.getBase_user()).append(",");
                    baseUserSet.add(t.getBase_user());

                }

            }

            if (!(base_user.toString().equals("null,") || base_user.length() == 0)) {
                base_user = new StringBuilder(base_user.substring(0, base_user.length() - 1));
                data.put("Machine Base User", base_user.toString());
            } else if (getSuite.getUser() != null) {
                data.put("Machine base user", getSuite.getUser());
            } else {
                data.put("Machine Base User", null);
            }

            if (!(token_user.toString().equals("null,") || token_user.length() == 0)) {
                token_user = new StringBuilder(token_user.substring(0, token_user.length() - 1));
                data.put("Jewel Token User", token_user.toString());
            } else {
                data.put("Jewel Token User", null);
            }
        }
        return data;
    }

    public static Object createExecutionDetailsHeaders(List<TestExeDto> testcaseList) {
        StringBuilder run_type = new StringBuilder();
        StringBuilder run_mode = new StringBuilder();
        Set<String> run_typeSet = new HashSet<>();
        Set<String> run_modeSet = new HashSet<>();
        Map<String, Object> data = new HashMap<String, Object>();
        for (TestExeDto t : testcaseList) {
            if (!run_modeSet.contains(t.getRun_mode())) {
                run_mode.append(t.getRun_mode()).append(", ");
                run_modeSet.add(t.getRun_mode());
            }
            if (!run_typeSet.contains(t.getRun_type().toUpperCase(Locale.ROOT))) {
                if (t.getRun_type() != null) {
                    run_type.append(t.getRun_type().toUpperCase(Locale.ROOT)).append(", ");
                    run_typeSet.add(t.getRun_type().toUpperCase(Locale.ROOT));
                }
            }


        }
        if (!(run_mode.toString().equals("null, ") || run_mode.length() == 0)) {
            run_mode = new StringBuilder(run_mode.substring(0, run_mode.length() - 2));
            data.put("Run Mode", run_mode.toString());
        } else {
            data.put("Run Mode", null);
        }

        if (!(run_type.toString().equals("null, ") || run_type.length() == 0)) {
            run_type = new StringBuilder(run_type.substring(0, run_type.length() - 2));
            // System.out.println(user.getClass().getSimpleName());
            data.put("Run Type", run_type.toString());
        } else {
            data.put("Run Type", null);
        }

        if (testcaseList!=null && !testcaseList.isEmpty() && testcaseList.get(0).getJob_name() != null) {
            data.put("Job Name", testcaseList.get(0).getJob_name());
        } else {
            data.put("Job Name", null);
        }

        return data;

    }

    public static Object createExecutionInfoHeaders(SuiteExeDto getSuite) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("Status", getSuite.getStatus());
        data.put("Project Name", StringUtils.capitalize(getSuite.getProject_name()));
        data.put("Env", StringUtils.capitalize(getSuite.getEnv()));
        data.put("Report Name", StringUtils.capitalize(getSuite.getReport_name()));

        return data;

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

    public static Object createBuildHeaders(SuiteExeDto getSuite) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("Build ID", getSuite.getBuild_id());
        data.put("Sprint Name", getSuite.getSprint_name());

        return data;
    }

    public static Object createTimeReportHeaders(List<TestExeDto> testcaseList, SuiteExeDto getSuite) {
        Map<String, Object> timeSubType = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        timeSubType.put("Clock Start Time", getSuite.getS_start_time());
        if (getSuite.getS_end_time() != 0) {

            timeSubType.put("Clock End Time", getSuite.getS_end_time());
            timeSubType.put("Total Duration", getDuration(getSuite.getS_start_time(), getSuite.getS_end_time()));
        } else {
            timeSubType.put("Clock End Time", null);
            timeSubType.put("Total Duration", null);
        }

        data.put("Clock Time", timeSubType);
        timeSubType = new HashMap<>();

        long minStartTime = testcaseList.get(0).getStart_time();
        long maxEndTime = testcaseList.get(0).getEnd_time();

        for (TestExeDto test : testcaseList) {
            if (test.getStart_time() < minStartTime) {
                minStartTime = test.getStart_time();
            }
            if (test.getEnd_time() > maxEndTime) {
                maxEndTime = test.getEnd_time();
            }
        }

        timeSubType.put("Automation Start Time", minStartTime);
        if (maxEndTime != 0) {

            timeSubType.put("Automation End Time", maxEndTime);
            timeSubType.put("Total Duration", getDuration(minStartTime, maxEndTime));
        } else {
            timeSubType.put("Automation End Time", null);
            timeSubType.put("Total Duration", null);
        }
        data.put("Automation Time", timeSubType);

        return data;
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
}
