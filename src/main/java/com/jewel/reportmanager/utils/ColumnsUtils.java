package com.jewel.reportmanager.utils;

import com.jewel.reportmanager.dto.ProjectDto;
import com.jewel.reportmanager.dto.ProjectRoleDto;
import com.jewel.reportmanager.dto.UserDto;
import com.jewel.reportmanager.enums.UserRole;
import com.jewel.reportmanager.service.JwtHelperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static com.jewel.reportmanager.utils.ReportConstants.ACTIVE_STATUS;

public class ColumnsUtils {

    private static String projectManagerUrl;

    @Value("${project.manager.url}")
    private void setProjectManagerUrl(String projectManagerUrl) {
        ColumnsUtils.projectManagerUrl = projectManagerUrl;
    }

    private static JwtHelperService jwtHelperService;

    @Autowired
    public void setUserFolderRepository(JwtHelperService jwtHelperService){
        ColumnsUtils.jwtHelperService = jwtHelperService;
    }


    public static String getUserNameFromToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7, bearerToken.length());
            return jwtHelperService.getUserNameFromJwtToken(token);
        }
        return null;
    }

    public static ProjectDto getProjectByPidAndStatus(Long pid, String status) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SecurityContextHolder.getContext().getAuthentication().getCredentials().toString());
        HttpEntity httpEntity = new HttpEntity(null, headers);
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("pid", pid);
        uriVariables.put("status", status);
        return (ProjectDto) RestClient.getApi(projectManagerUrl + "/v1/project/pid/status?pid={pid}&status={status}",httpEntity,ProjectDto.class,uriVariables).getBody();
    }

    public static boolean validateRoleWithViewerAccess(UserDto user, ProjectDto project) {
        if (project == null) {
            return false;
        }
        ProjectRoleDto projectRole = RestApiUtils.getProjectRoleEntity(project.getPid(), user.getUsername(), ACTIVE_STATUS);
        return projectRole != null || ((user.getRole().equalsIgnoreCase(UserRole.ADMIN.toString()) && project.getRealcompanyname().equalsIgnoreCase(user.getRealCompany())) || user.getRole().equalsIgnoreCase(UserRole.SUPER_ADMIN.toString()));
    }
}
