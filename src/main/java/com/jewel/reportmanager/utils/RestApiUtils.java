package com.jewel.reportmanager.utils;

import com.google.gson.Gson;
import com.jewel.reportmanager.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            Type listType = new TypeToken<List<Long>>() {
            }.getType();

            return gson.fromJson(gson.toJson(data), listType);
        } catch (HttpClientErrorException.NotFound ex) {
            log.debug("Project role pid(s) list is empty for pid: {}", pid);
            return new ArrayList<>();
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
            Type listType = new TypeToken<List<Long>>() {
            }.getType();

            return gson.fromJson(gson.toJson(data), listType);
        } catch (HttpClientErrorException.NotFound ex) {
            log.debug("Project pid(s) list is empty for pid: {}", pid);
            return new ArrayList<>();
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
            Type listType = new TypeToken<List<Long>>() {
            }.getType();

            return gson.fromJson(gson.toJson(data), listType);
        } catch (HttpClientErrorException.NotFound ex) {
            log.debug("Project pid(s) list is empty for pid: {}", pid);
            return new ArrayList<>();
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
            Type listType = new TypeToken<List<String>>() {
            }.getType();

            return gson.fromJson(gson.toJson(data), listType);
        } catch (HttpClientErrorException.NotFound ex) {
            log.debug("Project names list is empty for pid: {}", pid);
            return new ArrayList<>();
        }
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
            log.debug("Suite exe is empty for s_run_id: {}", s_run_id);
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
            log.debug("Suite run is empty for s_run_id: {}", s_run_id);
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
            log.debug("Suite is empty for reportName: {} and status: {}", reportName, status);
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
            log.debug("Project is not found for realCompanyName: {}, projectName: {}, status: {}", realCompanyName, projectName, status);
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
            log.debug("Column details not found for pid: {}, name: {}, frameworks: {}", pid, name, frameworks);
            return null;
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
            log.debug("Suite run is empty for s_run_id: {}", s_run_id);
            return null;
        }
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
