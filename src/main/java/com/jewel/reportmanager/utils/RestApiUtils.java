package com.jewel.reportmanager.utils;

import com.google.gson.Gson;
import com.jewel.reportmanager.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.lang.reflect.Type;
import java.util.*;

@Slf4j
@Service
public class RestApiUtils {

    private static String projectManagerUrl;
    private static String gemUrl;

    @Value("${project.manager.url}")
    public void setProjectManagerUrl(String projectManagerUrl) {
        RestApiUtils.projectManagerUrl = projectManagerUrl;
    }

    @Value("${gem.url}")
    public void setGemUrl(String gemUrl) {
        RestApiUtils.gemUrl = gemUrl;
    }

    /**
     * Returns a list of project role pid(s) for pid, status and username.
     *
     * @param pid
     * @param status
     * @param username
     * @return List<Long>
     */
    public static List<Long> getProjectRolePidList(List<Long> pid, String status, String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("pid", pid);
        uriVariables.put("status", status);
        uriVariables.put("username", username);
        try {
            ResponseEntity response = RestClient.getApi(projectManagerUrl + "/v2/project/role/pid/status/username?pid={pid}&status={status}&username={username}", httpEntity, Response.class, uriVariables);
            Gson gson = new Gson();
            String json = gson.toJson(response.getBody());
            Map<String, Object> convertedMap = gson.fromJson(json, new TypeToken<Map<String, Object>>() {
            }.getType());
            Object data = convertedMap.get("data");
            gson = new Gson();
            Type type = new TypeToken<List<Long>>() {
            }.getType();

            return gson.fromJson(gson.toJson(data), type);
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("Project role pid(s) list is empty for pid: {}", pid);
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Returns a project role for pid, status and username.
     *
     * @param pid
     * @param status
     * @param username
     * @return ProjectRoleDto
     */
    public static ProjectRoleDto getProjectRoleEntity(Long pid, String username, String status) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("pid", pid);
        uriVariables.put("username", username);
        uriVariables.put("status", status);
        try {
            ResponseEntity response = RestClient.getApi(projectManagerUrl + "/v2/project/role/entity?pid={pid}&username={username}&status={status}", httpEntity, Response.class, uriVariables);
            Gson gson = new Gson();
            String json = gson.toJson(response.getBody());
            Map<String, Object> convertedMap = gson.fromJson(json, new TypeToken<Map<String, Object>>() {
            }.getType());
            Object data = convertedMap.get("data");
            gson = new Gson();
            Type type = new TypeToken<ProjectRoleDto>() {
            }.getType();

            return gson.fromJson(gson.toJson(data), type);
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("Project role is empty for pid: {}", pid);
            return null;
        }
    }

    /**
     * Returns a project role for pid and username.
     *
     * @param pid
     * @param username
     * @return ProjectRoleDto
     */
    public static ProjectRoleDto getProjectRoleByPidAndUsername(Long pid, String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("pid", pid);
        uriVariables.put("username", username);
        try {
            ResponseEntity response = RestClient.getApi(projectManagerUrl + "/v2/project/role/pid/username?pid={pid}&username={username}", httpEntity, Response.class, uriVariables);
            Gson gson = new Gson();
            String json = gson.toJson(response.getBody());
            Map<String, Object> convertedMap = gson.fromJson(json, new TypeToken<Map<String, Object>>() {
            }.getType());
            Object data = convertedMap.get("data");
            gson = new Gson();
            Type type = new TypeToken<ProjectRoleDto>() {
            }.getType();

            return gson.fromJson(gson.toJson(data), type);
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("Project role is empty for pid: {}", pid);
            return null;
        }
    }

    /**
     * Returns a list of project pid(s) for realCompanyName, status and username.
     *
     * @param pid
     * @param status
     * @param realCompanyName
     * @return List<Long>
     */
    public static List<Long> getProjectPidListForRealCompanyNameAndStatus(List<Long> pid, String status, String realCompanyName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("pid", pid);
        uriVariables.put("status", status);
        uriVariables.put("realCompanyName", realCompanyName);
        try {
            ResponseEntity response = RestClient.getApi(projectManagerUrl + "/v1/project/pid/status/realCompanyName?pid={pid}&status={status}&realCompanyName={realCompanyName}", httpEntity, Response.class, uriVariables);
            Gson gson = new Gson();
            String json = gson.toJson(response.getBody());
            Map<String, Object> convertedMap = gson.fromJson(json, new TypeToken<Map<String, Object>>() {
            }.getType());
            Object data = convertedMap.get("data");
            gson = new Gson();
            Type type = new TypeToken<List<Long>>() {
            }.getType();

            return gson.fromJson(gson.toJson(data), type);
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("Project pid(s) list is empty for pid: {}", pid);
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Returns a list of project pid(s) for pid, status and username.
     *
     * @param pid
     * @param status
     * @param username
     * @return List<Long>
     */
    public static List<Long> getProjectPidList(List<Long> pid, String status, String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("pid", pid);
        uriVariables.put("status", status);
        uriVariables.put("username", username);
        try {
            ResponseEntity response = RestClient.getApi(projectManagerUrl + "/v1/project/pid/status/username?pid={pid}&status={status}&username={username}", httpEntity, Response.class, uriVariables);
            Gson gson = new Gson();
            String json = gson.toJson(response.getBody());
            Map<String, Object> convertedMap = gson.fromJson(json, new TypeToken<Map<String, Object>>() {
            }.getType());
            Object data = convertedMap.get("data");
            gson = new Gson();
            Type type = new TypeToken<List<Long>>() {
            }.getType();

            return gson.fromJson(gson.toJson(data), type);
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("Project pid(s) list is empty for pid: {}", pid);
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Returns a list of project names for pid.
     *
     * @param pid
     * @return List<String>
     */
    public static List<String> getProjectNames(List<Long> pid) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("pid", pid);
        try {
            ResponseEntity response = RestClient.getApi(projectManagerUrl + "/v1/project/pid?pid={pid}", httpEntity, Response.class, uriVariables);
            Gson gson = new Gson();
            String json = gson.toJson(response.getBody());
            Map<String, Object> convertedMap = gson.fromJson(json, new TypeToken<Map<String, Object>>() {
            }.getType());
            Object data = convertedMap.get("data");
            gson = new Gson();
            Type type = new TypeToken<List<String>>() {
            }.getType();

            return gson.fromJson(gson.toJson(data), type);
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("Project names list is empty for pid: {}", pid);
            return Collections.EMPTY_LIST;
        }
    }


    /**
     * Returns a list of report names for pid, env, startTime, endTime and page no.
     *
     * @param p_id
     * @param env
     * @param s_start_time
     * @param s_end_time
     * @param pageNo
     * @return List<String>
     */
    public static List<String> getReportNames(List<Long> p_id, List<String> env, Long s_start_time, Long s_end_time, Integer pageNo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("p_id", p_id);
        uriVariables.put("env", env);
        uriVariables.put("s_start_time", s_start_time);
        uriVariables.put("s_end_time", s_end_time);
        uriVariables.put("pageNo", pageNo);
        try {
            ResponseEntity response = RestClient.getApi(gemUrl + "/v1/suiteExe/report-names?p_id={p_id}&env={env}&s_start_time={s_start_time}&s_end_time={s_end_time}&pageNo={pageNo}", httpEntity, Object.class, uriVariables);
            Gson gson = new Gson();
            String json = gson.toJson(response.getBody());
            Map<String, Object> convertedMap = gson.fromJson(json, new TypeToken<Map<String, Object>>() {
            }.getType());
            Object data = convertedMap.get("data");
            gson = new Gson();
            Type type = new TypeToken<List<String>>() {
            }.getType();

            return gson.fromJson(gson.toJson(data), type);
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("Report names list is empty for pid: {}, env: {}, start time: {}, end time: {} and pageNo: {}", p_id, env, s_start_time, s_end_time, pageNo);
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Returns a list of suite exes for pid, env, startTime, endTime, page no., sort and sortedColumn.
     *
     * @param p_id
     * @param env
     * @param s_start_time
     * @param s_end_time
     * @param pageNo
     * @param sort
     * @param sortedColumn
     * @return List<SuiteExeDto>
     */
    public static List<SuiteExeDto> getSuiteExes(List<Long> p_id, List<String> env, Long s_start_time, Long s_end_time, Integer pageNo, Integer sort, String sortedColumn) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("p_id", p_id);
        uriVariables.put("env", env);
        uriVariables.put("s_start_time", s_start_time);
        uriVariables.put("s_end_time", s_end_time);
        uriVariables.put("pageNo", pageNo);
        uriVariables.put("sort", sort);
        uriVariables.put("sortedColumn", sortedColumn);
        try {
            ResponseEntity response = RestClient.getApi(gemUrl + "/v1/suiteExe?p_id={p_id}&env={env}&s_start_time={s_start_time}&s_end_time={s_end_time}&pageNo={pageNo}&sort={sort}&sortedColumn={sortedColumn}", httpEntity, Object.class, uriVariables);
            Gson gson = new Gson();
            String json = gson.toJson(response.getBody());
            Map<String, Object> convertedMap = gson.fromJson(json, new TypeToken<Map<String, Object>>() {
            }.getType());
            Object data = convertedMap.get("data");
            gson = new Gson();
            Type type = new TypeToken<List<SuiteExeDto>>() {
            }.getType();

            return gson.fromJson(gson.toJson(data), type);
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("Suite exe list is empty for pid: {}, env: {}, start time: {}, end time: {} pageNo: {}, sort: {} and sortedColumn: {}", p_id, env, s_start_time, s_end_time, pageNo, sort, sortedColumn);
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Returns a list of s_run_ids for pid, env, startTime, endTime, page no., sort and sortedColumn.
     *
     * @param p_id
     * @param env
     * @param s_start_time
     * @param s_end_time
     * @param pageNo
     * @param sort
     * @param sortedColumn
     * @return List<String>
     */
    public static List<String> getS_Run_Ids(List<Long> p_id, List<String> env, Long s_start_time, Long s_end_time, Integer pageNo, Integer sort, String sortedColumn) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("p_id", p_id);
        uriVariables.put("env", env);
        uriVariables.put("s_start_time", s_start_time);
        uriVariables.put("s_end_time", s_end_time);
        uriVariables.put("pageNo", pageNo);
        uriVariables.put("sort", sort);
        uriVariables.put("sortedColumn", sortedColumn);
        try {
            ResponseEntity response = RestClient.getApi(gemUrl + "/v1/suiteExe/s_run_ids?p_id={p_id}&env={env}&s_start_time={s_start_time}&s_end_time={s_end_time}&pageNo={pageNo}&sort={sort}&sortedColumn={sortedColumn}", httpEntity, Object.class, uriVariables);
            Gson gson = new Gson();
            String json = gson.toJson(response.getBody());
            Map<String, Object> convertedMap = gson.fromJson(json, new TypeToken<Map<String, Object>>() {
            }.getType());
            Object data = convertedMap.get("data");
            gson = new Gson();
            Type type = new TypeToken<List<String>>() {
            }.getType();

            return gson.fromJson(gson.toJson(data), type);
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("s_run_ids list is empty for pid: {}, env: {}, start time: {}, end time: {} pageNo: {}, sort: {} and sortedColumn: {}", p_id, env, s_start_time, s_end_time, pageNo, sort, sortedColumn);
            return Collections.EMPTY_LIST;
        }
    }


    /**
     * Returns a count of suite exe for pid, env, startTime, endTime and page no.
     *
     * @param p_id
     * @param env
     * @param s_start_time
     * @param s_end_time
     * @return Long - count
     */
    public static Long getSuiteExeCount(List<Long> p_id, List<String> env, Long s_start_time, Long s_end_time) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("p_id", p_id);
        uriVariables.put("env", env);
        uriVariables.put("s_start_time", s_start_time);
        uriVariables.put("s_end_time", s_end_time);
        ResponseEntity response = RestClient.getApi(gemUrl + "/v1/suiteExe/count?p_id={p_id}&env={env}&s_start_time={s_start_time}&s_end_time={s_end_time}", httpEntity, Object.class, uriVariables);
        Gson gson = new Gson();
        String json = gson.toJson(response.getBody());
        Map<String, Object> convertedMap = gson.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
        Object data = convertedMap.get("data");
        gson = new Gson();
        Type type = new TypeToken<Long>() {
        }.getType();

        return gson.fromJson(gson.toJson(data), type);
    }

    /**
     * Returns suiteExe from s_run_id
     *
     * @param s_run_id
     * @return SuiteExe
     */
    public static SuiteExeDto getSuiteExe(String s_run_id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("s_run_id", s_run_id);
        try {
            return (SuiteExeDto) RestClient.getApi(gemUrl + "/v2/suitExe?s_run_id={s_run_id}", httpEntity, SuiteExeDto.class, uriVariables).getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("Suite exe is empty for s_run_id: {}", s_run_id);
            return null;
        }
    }

    /**
     * Returns suiteRun from s_run_id
     *
     * @param s_run_id
     * @return SuiteRun
     */
    public static SuiteRun getSuiteRun(String s_run_id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("s_run_id", s_run_id);
        try {
            ResponseEntity response = RestClient.getApi(gemUrl + "/v2/suiteRun?s_run_id={s_run_id}", httpEntity, Object.class, uriVariables);
            Gson gson = new Gson();
            String json = gson.toJson(response.getBody());
            Map<String, Object> convertedMap = gson.fromJson(json, new TypeToken<Map<String, Object>>() {
            }.getType());
            Object data = convertedMap.get("data");
            gson = new Gson();
            Type type = new TypeToken<SuiteRun>() {
            }.getType();

            return gson.fromJson(gson.toJson(data), type);
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("Suite run is empty for s_run_id: {}", s_run_id);
            return null;
        }
    }

    /**
     * Returns test exe from tc_run_id
     *
     * @param tc_run_id
     * @return TestExeDto
     */
    public static TestExeDto getTestExe(String tc_run_id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("tc_run_id", tc_run_id);
        try {
            ResponseEntity response = RestClient.getApi(gemUrl + "/v2/testcase?tc_run_id={tc_run_id}", httpEntity, Object.class, uriVariables);
            Gson gson = new Gson();
            String json = gson.toJson(response.getBody());
            Map<String, Object> convertedMap = gson.fromJson(json, new TypeToken<Map<String, Object>>() {
            }.getType());
            Object data = convertedMap.get("data");
            gson = new Gson();
            Type type = new TypeToken<TestExeDto>() {
            }.getType();

            return gson.fromJson(gson.toJson(data), type);
        } catch (HttpClientErrorException.BadRequest ex) {
            log.info("TestExe not found for tc_run_id: {}", tc_run_id);
            return null;
        }
    }

    /**
     * Returns suite from reportName and status
     *
     * @param reportName
     * @param status
     * @return SuiteRun
     */
    public static SuiteDto getSuiteByReportNameAndStatus(String reportName, String status) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("reportName", reportName);
        uriVariables.put("status", status);
        try {
            ResponseEntity response = RestClient.getApi(gemUrl + "/v2/suite/reportName/status?reportName={reportName}&status={status}", httpEntity, Object.class, uriVariables);
            Gson gson = new Gson();
            String json = gson.toJson(response.getBody());
            Map<String, Object> convertedMap = gson.fromJson(json, new TypeToken<Map<String, Object>>() {
            }.getType());
            Object data = convertedMap.get("data");
            gson = new Gson();
            Type type = new TypeToken<SuiteDto>() {
            }.getType();

            return gson.fromJson(gson.toJson(data), type);
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("Suite is empty for reportName: {} and status: {}", reportName, status);
            return null;
        }
    }

    /**
     * Returns project from realCompanyName, projectName and status.
     *
     * @param realCompanyName
     * @param projectName
     * @param status
     * @return ProjectDto
     */
    public static ProjectDto getProjectByRealCompanyNameAndProjectAndStatus(String realCompanyName, String projectName, String status) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("realCompanyName", realCompanyName);
        uriVariables.put("projectName", projectName);
        uriVariables.put("status", status);
        try {
            ResponseEntity response = RestClient.getApi(projectManagerUrl + "/v1/project/realCompanyName/projectName/status?realCompanyName={realCompanyName}&projectName={projectName}&status={status}", httpEntity, Object.class, uriVariables);
            Gson gson = new Gson();
            String json = gson.toJson(response.getBody());
            Map<String, Object> convertedMap = gson.fromJson(json, new TypeToken<Map<String, Object>>() {
            }.getType());
            Object data = convertedMap.get("data");
            gson = new Gson();
            Type type = new TypeToken<ProjectDto>() {
            }.getType();

            return gson.fromJson(gson.toJson(data), type);
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("Project is not found for realCompanyName: {}, projectName: {}, status: {}", realCompanyName, projectName, status);
            return null;
        }
    }

    /**
     * Returns column details for pid, report name and frameworks.
     *
     * @param pid
     * @param name
     * @param frameworks
     * @return List<String> - column details
     */
    public static List<String> findColumnMapping(Long pid, String name, List<String> frameworks) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("pid", pid);
        uriVariables.put("name", name);
        uriVariables.put("frameworks", frameworks);
        try {
            ResponseEntity response = RestClient.getApi(gemUrl + "/v2/column/pid/name/frameworks?pid={pid}&name={name}&frameworks={frameworks}", httpEntity, Object.class, uriVariables);
            Gson gson = new Gson();
            String json = gson.toJson(response.getBody());
            Map<String, Object> convertedMap = gson.fromJson(json, new TypeToken<Map<String, Object>>() {
            }.getType());
            Object data = convertedMap.get("data");
            gson = new Gson();
            Type type = new TypeToken<List<String>>() {
            }.getType();

            return gson.fromJson(gson.toJson(data), type);
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("Column details not found for pid: {}, name: {}, frameworks: {}", pid, name, frameworks);
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Returns List<TestExeDto> from s_run_id
     *
     * @param s_run_id
     * @return List<TestExeDto>
     */
    public static List<TestExeDto> getTestExeList(String s_run_id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("s_run_id", s_run_id);
        try {
            return (List<TestExeDto>) RestClient.getApi(gemUrl + "/v2/testExe/list?s_run_id={s_run_id}", httpEntity, new ParameterizedTypeReference<List<TestExeDto>>() {
            }, uriVariables).getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("Suite run is empty for s_run_id: {}", s_run_id);
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Returns a list of test exes for s_run_ids.
     *
     * @param
     * @return List<TestExeDto>
     */
    public static List<TestExeDto> getTestExeListForS_run_ids(List<String> s_run_ids) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("s_run_ids", s_run_ids);
        try {
            ResponseEntity response = RestClient.getApi(gemUrl + "/v1/testExe?s_run_ids={s_run_ids}", httpEntity, Object.class, uriVariables);
            Gson gson = new Gson();
            String json = gson.toJson(response.getBody());
            Map<String, Object> convertedMap = gson.fromJson(json, new TypeToken<Map<String, Object>>() {
            }.getType());
            Object data = convertedMap.get("data");
            gson = new Gson();
            Type type = new TypeToken<List<TestExeDto>>() {
            }.getType();

            return gson.fromJson(gson.toJson(data), type);
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("Suite exe list is empty for s_run_ids: {}", s_run_ids);
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Update suite exe for s_run_id.
     *
     * @param s_run_id
     * @param suiteExeDto
     */
    public static void updateSuiteExe(String s_run_id, SuiteExeDto suiteExeDto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(suiteExeDto, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("s_run_id", s_run_id);
        RestClient.putApi(gemUrl + "/v2/suitExe/update?s_run_id={s_run_id}", httpEntity, SuiteExeDto.class, uriVariables).getBody();
    }

    /**
     * Returns project from pid and status.
     *
     * @param pid
     * @param status
     * @return project
     */
    public static ProjectDto getProjectByPidAndStatus(Long pid, String status) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("pid", pid);
        uriVariables.put("status", status);
        return (ProjectDto) RestClient.getApi(projectManagerUrl + "/v2/project/pid/status?pid={pid}&status={status}", httpEntity, ProjectDto.class, uriVariables).getBody();
    }

}
