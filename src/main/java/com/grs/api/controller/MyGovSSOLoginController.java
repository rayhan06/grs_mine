package com.grs.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.grs.api.config.security.GrantedAuthorityImpl;
import com.grs.api.config.security.OISFUserDetailsServiceImpl;
import com.grs.api.config.security.TokenAuthenticationServiceUtil;
import com.grs.api.config.security.UserDetailsImpl;
import com.grs.api.model.OISFUserType;
import com.grs.api.model.OfficeInformation;
import com.grs.api.model.UserInformation;
import com.grs.api.model.UserType;
import com.grs.api.model.request.ComplainantDTO;
import com.grs.api.myGov.MyGovAppLoginResponse;
import com.grs.api.myGov.MyGovLoginResponse;
import com.grs.api.myGov.MyGovUserDTO;
import com.grs.api.sso.*;
import com.grs.core.config.IDP_Client;
import com.grs.core.dao.*;
import com.grs.core.domain.RedirectMap;
import com.grs.core.domain.grs.Complainant;
import com.grs.core.domain.grs.GrsRole;
import com.grs.core.domain.grs.OfficesGRO;
import com.grs.core.domain.grs.Permission;
import com.grs.core.service.*;
import com.grs.utils.Constant;
import com.grs.utils.CookieUtil;
import com.grs.utils.StringUtil;
import com.grs.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class MyGovSSOLoginController {
    @Autowired
    private CellService cellService;
    @Autowired
    private EmployeeOfficeDAO employeeOfficeDAO;
    @Autowired
    private OISFUserDetailsServiceImpl userDetailsService;
    @Autowired
    private GrsRoleDAO grsRoleDAO;
    @Autowired
    private ESBConnectorService esbConnectorService;
    @Autowired
    private OfficesGroDAO officesGroDAO;
    @Autowired
    private CentralDashboardRecipientDAO centralDashboardRecipientDAO;
    @Autowired
    private FcmService fcmService;
    @Autowired
    private CellMemberDAO cellMemberDAO;
    @Autowired
    private ComplainantService complainantService;
    @Autowired
    private GrievanceService grievanceService;
    @Autowired
    private GrsRoleDAO roleDAO;

    @RequestMapping(value = "/mygovlogout", method = RequestMethod.GET)
    public ModelAndView getLogoutPage() throws Exception {


        IDP_Client idp = new IDP_Client(SSOPropertyReader.getInstance().getBaseUri() + Constant.myGovLogoutRedirectSuffix);
        String url = idp.logoutRequest();
        return new ModelAndView("redirect:" + url);
    }




    @RequestMapping(value = "/afterLoginFromMyGov", method = RequestMethod.POST)
    public void redirectAfterMyGovLoginSuccessPOST(HttpServletResponse response, HttpServletRequest request, Authentication authentication) throws IOException {

        try {
            IDP_Client idp = new IDP_Client();
            String myGovloginUserJson = idp.responseRequest(request);

            if (myGovloginUserJson == null) {
                response.sendRedirect("/error-page");
            }
            else {
                MyGovLoginResponse myGovLoginResponse = new MyGovAppLoginResponse();
                ComplainantDTO complainantDTO = myGovLoginResponse.parseResponse(myGovloginUserJson);

                Gson gson = new Gson();
                MyGovUserDTO myGovUserDTO = gson.fromJson(myGovloginUserJson, MyGovUserDTO.class);

                Complainant currentComplainant = this.complainantService.insertComplainantWithoutLogin(complainantDTO);

                UserInformation userInformation = this.grievanceService.generateUserInformationForComplainant(currentComplainant);
                userInformation.setIsMyGovLogin(true);
                userInformation.setToken(myGovUserDTO.getToken());

                GrsRole role  = roleDAO.findByRole("COMPLAINANT");
                List<String> permissions = new ArrayList(){{
                    add("ADD_PUBLIC_GRIEVANCES");
                    add("DO_APPEAL");
                }};
                List<GrantedAuthorityImpl> grantedAuthorities = role.getPermissions().stream()
//                .filter(x ->(!permissions.contains(x.getName()))).collect(Collectors.toList())
//                .stream()
                        .map(permission -> {
                            return GrantedAuthorityImpl.builder()
                                    .role(permission.getName())
                                    .build();
                        }).collect(Collectors.toList());

                UserDetailsImpl userDetails = UserDetailsImpl.builder()
                        .username(currentComplainant.getUsername())
                        .password(currentComplainant.getPassword())
                        .isAccountAuthenticated(currentComplainant.isAuthenticated())
                        .grantedAuthorities(grantedAuthorities)
                        .userInformation(userInformation)
                        .build();

                TokenAuthenticationServiceUtil.addAuthenticationForMyGov(userDetails, request, response);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @RequestMapping(value = "/afterLogoutFromMyGov", method = RequestMethod.POST)
    public void redirectAfterMyGovLogoutSuccessPOST(HttpServletResponse response, HttpServletRequest request, Authentication authentication) throws Exception {

        IDP_Client idp = new IDP_Client(SSOPropertyReader.getInstance().getBaseUri());
        String url = idp.responseRequest(request);


        try {
//            SessionService sessionService = new SessionService();
//            AppSessionRepository appSessionRepository = AppSessionRepository.getInstance();
//            AppSessionDTO appSessionDTO = appSessionRepository.getSessionDetails(request.getSession(false).getId()).orElse(null);
//            if (appSessionDTO != null) {
//                sessionService.delete(appSessionDTO.getRealm(), appSessionDTO.getUsername());
//                AppSessionRepository.getAppSessions().remove(appSessionDTO.getId());
//            }
            request.getSession().invalidate();
            CookieUtil.clear(response, Constant.HEADER_STRING);

            if (url != null) response.sendRedirect(url);
            response.sendRedirect("/error-page");
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("/error-page");
        }

    }

}
