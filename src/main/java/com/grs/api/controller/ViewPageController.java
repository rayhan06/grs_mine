package com.grs.api.controller;

import com.grs.api.config.security.OISFUserDetailsServiceImpl;
import com.grs.api.model.*;
import com.grs.api.model.request.GrsUserDTO;
import com.grs.api.model.request.SuperAdminDTO;
import com.grs.api.model.response.*;
import com.grs.api.model.response.menu.SubMenuDTO;
import com.grs.api.model.response.officeSelection.OfficeSearchDTO;
import com.grs.api.sso.AppLoginRequest;
import com.grs.api.sso.SSOPropertyReader;
import com.grs.core.config.IDP_Client;
import com.grs.core.dao.CentralDashboardRecipientDAO;
import com.grs.core.dao.GrsRoleDAO;
import com.grs.core.dao.OfficesGroDAO;
import com.grs.core.domain.RedirectMap;
import com.grs.core.domain.ServiceType;
import com.grs.core.domain.grs.*;
import com.grs.core.domain.projapoti.Office;
import com.grs.core.service.*;
import com.grs.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.tomcat.util.codec.binary.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Tanvir on 8/10/2017.
 */
@Slf4j
@Controller
public class ViewPageController {
    @Autowired
    private OfficeService officeService;
    @Autowired
    private ComplainantService complainantService;
    @Autowired
    private OccupationService occupationService;
    @Autowired
    private GrsUserService grsUserService;
    @Autowired
    private EducationService educationService;
    @Autowired
    private CitizenCharterService citizenCharterService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private ShortMessageService shortMessageService;
    @Autowired
    private EmailSmsSettingsService emailSmsSettingsService;
    @Autowired
    private ModelViewService modelViewService;
    @Autowired
    private GeneralSettingsService generalSettingsService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private ESBConnectorService esbConnectorService;
    @Autowired
    private OISFUserDetailsServiceImpl userDetailsService;
    @Autowired
    private GrsRoleDAO grsRoleDAO;
    @Autowired
    private OfficesGroDAO officesGroDAO;
    @Autowired
    private CentralDashboardRecipientDAO centralDashboardRecipientDAO;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView firstPage(HttpServletRequest request, Authentication authentication, Model model) {
//        return new ModelAndView("redirect:/login?a=0");
        return modelViewService.returnViewsForNormalPages(authentication, model, request, "index");
    }

    @RequestMapping(value = "/login/success", method = RequestMethod.POST)
    public void redirectAfterLoginSuccessPOST(HttpServletResponse response, HttpServletRequest request, Authentication authentication) throws IOException {
        if (authentication != null) {
            UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
            boolean isOthersComplainant = Utility.isUserAnOthersComplainant(authentication);
            String lastSavedUrl = (String) request.getSession(false).getAttribute("prev_url");
            log.info(lastSavedUrl);
            if (userInformation.getUserType().equals(UserType.OISF_USER)) {
                if ((StringUtil.isValidString(lastSavedUrl) && lastSavedUrl.contains("/addStaffGrievances.do"))) {
                    request.getSession().setAttribute("prev_url", "");
                    response.sendRedirect((lastSavedUrl));
                } else {
                    if (userInformation.getOisfUserType().equals(OISFUserType.HEAD_OF_OFFICE)) {
                        response.sendRedirect("/dashboard.do");
                    } else {
                        response.sendRedirect("/viewGrievances.do");
                    }
                }
            } else if (userInformation.getUserType().equals(UserType.COMPLAINANT) || isOthersComplainant) {
                if ((StringUtil.isValidString(lastSavedUrl)
                        && (lastSavedUrl.contains("/addPublicGrievances.do")
                        || lastSavedUrl.contains("/complainForOthers.do")))) {
                    request.getSession().setAttribute("prev_url", "");
                    response.sendRedirect(lastSavedUrl);
                } else {
                    response.sendRedirect("/viewGrievances.do");
                }
            } else if (userInformation.getUserType().equals(UserType.SYSTEM_USER)) {
                response.sendRedirect("/viewOffice.do");
            }
        } else {
            response.sendRedirect("/");
        }
    }

    @RequestMapping(value = "/login/success", method = RequestMethod.GET)
    public void redirectAfterLoginSuccess(HttpServletResponse response, HttpServletRequest request, Authentication authentication) throws IOException {
        if (authentication != null) {
            UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
            boolean isOthersComplainant = Utility.isUserAnOthersComplainant(authentication);
            String lastSavedUrl = (String) request.getSession(false).getAttribute("prev_url");
            log.info(lastSavedUrl);
            if (userInformation.getUserType().equals(UserType.OISF_USER)) {
                if ((StringUtil.isValidString(lastSavedUrl) && lastSavedUrl.contains("/addStaffGrievances.do"))) {
                    request.getSession().setAttribute("prev_url", "");
                    response.sendRedirect((lastSavedUrl));
                } else {
                    if (userInformation.getOisfUserType().equals(OISFUserType.HEAD_OF_OFFICE)) {
                        response.sendRedirect("/dashboard.do");
                    } else {
                        response.sendRedirect("/viewGrievances.do");
                    }
                }
            } else if (userInformation.getUserType().equals(UserType.COMPLAINANT) || isOthersComplainant) {
                if ((StringUtil.isValidString(lastSavedUrl)
                        && (lastSavedUrl.contains("/addPublicGrievances.do")
                        || lastSavedUrl.contains("/complainForOthers.do")))) {
                    request.getSession().setAttribute("prev_url", "");
                    response.sendRedirect(lastSavedUrl);
                } else {
                    response.sendRedirect("/viewGrievances.do");
                }
            } else if (userInformation.getUserType().equals(UserType.SYSTEM_USER)) {
                response.sendRedirect("/viewOffice.do");
            }
        } else {
            response.sendRedirect("/");
        }
    }

    @RequestMapping(value = "/services.do", method = RequestMethod.GET)
    public ModelAndView servicesPage(HttpServletRequest request, Authentication authentication, Model model) {
        model.addAttribute("searchableOffices", officeService.getGrsEnabledOfficeSearchingData());
        return modelViewService.returnViewsForNormalPages(authentication, model, request, "services");
    }

    @RequestMapping(value = "/groInformation.do", method = RequestMethod.GET)
    public ModelAndView groInformationPage(HttpServletRequest request, Authentication authentication, Model model) {
        model.addAttribute("searchableOffices", officeService.getGrsEnabledOfficeSearchingData());
        return modelViewService.returnViewsForNormalPages(authentication, model, request, "groInformation");
    }

    @RequestMapping(value = "/dashboardMobile.do", method = RequestMethod.GET)
    public ModelAndView getDashboardPage(HttpServletRequest request, Authentication authentication, Model model) {
        UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
        String fragmentName = "";
        String viewName = "admin";
        if (userInformation.getUserType().equals(UserType.COMPLAINANT)) {
            Boolean isBlacklisted = complainantService.isBlacklistedUser(authentication);
            model.addAttribute("isBlacklisted", isBlacklisted);
            fragmentName = "dashboardCitizen";
        } else if (userInformation.getUserType().equals(UserType.OISF_USER)) {
            if (userInformation.getOisfUserType().equals(OISFUserType.GRO) || userInformation.getOisfUserType().equals(OISFUserType.HEAD_OF_OFFICE) || userInformation.getIsCentralDashboardUser()) {
                String requestParams = request.getParameter("params");
                OfficeInformation officeInformation = userInformation.getOfficeInformation();
                Long officeId = officeInformation.getOfficeId();
                String officeName = messageService.isCurrentLanguageInEnglish() ? officeInformation.getOfficeNameEnglish() : officeInformation.getOfficeNameBangla();
                Boolean isDrilledDown = false;
                if (StringUtil.isValidString(requestParams)) {
                    String decodedParams = StringUtils.newStringUtf8(Base64.decodeBase64(requestParams.substring(20)));
                    Long childOfficeId = Long.parseLong(decodedParams);
                    Office childOffice = officeService.findOne(childOfficeId);
                    List<Long> parentOfficeIds = officeService.getAncestorOfficeIds(childOfficeId);
                    if (childOffice != null && (parentOfficeIds.contains(officeId) || userInformation.getIsCentralDashboardUser())) {
                        officeId = childOfficeId;
                        officeName = messageService.isCurrentLanguageInEnglish() ? childOffice.getNameEnglish() : childOffice.getNameBangla();
                        isDrilledDown = true;
                    } else {
                        return new ModelAndView("redirect:/error-page");
                    }
                }
                model.addAttribute("officeId", officeId);
                model.addAttribute("officeName", officeName);
                model.addAttribute("currentMonthYear", messageService.getCurrentMonthYearAsString());
                model.addAttribute("isDrilledDown", isDrilledDown);
                model.addAttribute("canViewAppealAndSubOfficeDashboard", officeService.hasAccessToAoAndSubOfficesDashboard(userInformation, officeId));
                fragmentName = "dashboardGro";
            } else if (userInformation.getOisfUserType().equals(OISFUserType.SUPER_ADMIN)) {
                fragmentName = "dashboardSuperAdmin";
                viewName = "superAdmin";
            } else {
                fragmentName = "dashboard";
            }
        } else if (userInformation.getUserType().equals(UserType.SYSTEM_USER)) {
            if (userInformation.getGrsUserType().equals(GRSUserType.SUPER_ADMIN)) {
                return new ModelAndView("redirect:/viewOffice.do");
            }
        }
        if (authentication != null) {
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "dashboard",
                    fragmentName,
                    viewName);
        } else {
            return new ModelAndView("redirect:/error-page");
        }
    }

    @RequestMapping(value = "/centralDashboard.do", method = RequestMethod.GET)
    public ModelAndView getCentralDashboardPage(HttpServletRequest request, Authentication authentication, Model model) {
        if (authentication != null) {
            UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
            if (userInformation.getIsCentralDashboardUser()) {
                model.addAttribute("currentMonthYear", messageService.getCurrentMonthYearAsString());
                return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                        authentication,
                        request,
                        "dashboard",
                        "dashboardCentral",
                        "admin");
            }
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ModelAndView getLoginPage(Authentication authentication) {
        if (authentication == null) {
            return new ModelAndView("redirect:/login?a=0");
        } else {
            UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
            if (userInformation.getUserType().equals(UserType.OISF_USER) &&
                    (userInformation.getOisfUserType().equals(OISFUserType.GRO) || userInformation.getOisfUserType().equals(OISFUserType.SERVICE_OFFICER))) {
                return new ModelAndView("redirect:/viewGrievances.do");

            } else {
                return new ModelAndView("redirect:/dashboard.do");
            }
        }
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET, params = "a")
    public ModelAndView getLoginPage(HttpServletRequest request,
                                     Model model,
                                     @RequestParam String a,
                                     @RequestParam(value = "", defaultValue = "", required = false) String redirectUrl,
                                     @RequestParam(value = "", defaultValue = "", required = false) String phoneNumber,
                                     Authentication authentication,
                                     @CookieValue(value = "prev_url", defaultValue = "") String previousUrl) throws Exception {


        RedirectMap redirectMap = RedirectMap.DASHBOARD;
        if(StringUtil.isValidString(previousUrl)) {
            log.info( "previous url from cookie: " + previousUrl);
            redirectMap = RedirectMap.get(previousUrl);

            request.getSession().setAttribute("prev_url", previousUrl);
            log.info(request.getSession().getAttribute("prev_url").toString());

        }

        if (a.compareTo("1") == 0) {
            try {
                String landingPageUrl = request.getParameter("landing_page_uri");
                if (StringUtil.isValidString(redirectUrl)) {
                    landingPageUrl = redirectUrl;
                }
                log.info(landingPageUrl);
                AppLoginRequest appLoginRequest = new AppLoginRequest();
                appLoginRequest.setLandingPageUrl(landingPageUrl);

                String authnRequest = appLoginRequest.buildAuthnRequest(redirectMap);
//                String authnRequest = appLoginRequest.buildAuthnRequest();
                request.getSession().setAttribute("nonce", appLoginRequest.getNonce());
                log.error("Applogin request nonce : " + appLoginRequest.getNonce() + "\nSession nonce: " + request.getSession().getAttribute("nonce"));
                return new ModelAndView("redirect:" + authnRequest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (authentication != null) {
            UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
            if (userInformation.getUserType().equals(UserType.OISF_USER) &&
                    (userInformation.getOisfUserType().equals(OISFUserType.GRO) || userInformation.getOisfUserType().equals(OISFUserType.SERVICE_OFFICER))) {
                return new ModelAndView("redirect:/viewGrievances.do");

            } else {
                return new ModelAndView("redirect:/viewGrievances.do");
            }
        }

        model.addAttribute("userNameText", "login");

        SubMenuDTO subMenuUsernameDTO, subMenuPasswordDTO, formTitleDTO;
        if (a.equals("1")) {
            subMenuUsernameDTO = SubMenuDTO.builder().nameEnglish("Username").nameBangla("ইউজারনেম").link("javascript:;").build();
            subMenuPasswordDTO = SubMenuDTO.builder().nameEnglish("Password").nameBangla("পাসওয়ার্ড").link("javascript:;").build();
            formTitleDTO = SubMenuDTO.builder().nameEnglish("Administrative login").nameBangla("প্রশাসনিক লগইন").link("javascript:;").build();
        } else if (a.equals("5")) {
            IDP_Client idp = new IDP_Client(SSOPropertyReader.getInstance().getBaseUri() + Constant.myGovLoginRedirectSuffix);
            String url = idp.loginRequest();
            return new ModelAndView("redirect:" + url);
        } else {
            subMenuUsernameDTO = SubMenuDTO.builder().nameEnglish("Mobile number (in Bangla/English)").nameBangla("মোবাইল নম্বর (বাংলায়/ইংরেজিতে)").link("javascript:;").build();
            subMenuPasswordDTO = SubMenuDTO.builder().nameEnglish("Pin Code (in English)").nameBangla("পিনকোড (ইংরেজিতে)").link("javascript:;").build();
            formTitleDTO = SubMenuDTO.builder().nameEnglish("Complainant Login").nameBangla("অভিযোগকারী লগইন").link("javascript:;").build();
        }
        String languageCode = CookieUtil.getValue(request, "lang");
        model.addAttribute("lang", languageCode);
        model.addAttribute("usernameFieldText", subMenuUsernameDTO);
        model.addAttribute("passwordFieldText", subMenuPasswordDTO);
        model.addAttribute("formTitleText", formTitleDTO);
        model.addAttribute("valueOfA", a);
        model.addAttribute("redirectUrl", redirectUrl);
        model.addAttribute("phoneNumber", phoneNumber);
        return new ModelAndView("login");
    }

    @RequestMapping(value = "/viewRegister.do", method = RequestMethod.GET)
    public ModelAndView getRegisterPage(HttpServletRequest request, Authentication authentication, Model model) {
        if (authentication != null) {
            UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
            Long officeId = userInformation.getOfficeInformation().getOfficeId();
            String requestParams = request.getParameter("params");
            if (StringUtil.isValidString(requestParams)) {
                Long officeIdParam = StringUtil.decodeOfficeIdOnDashboardDrillDown(requestParams);
                Office office = officeService.findOne(officeIdParam);
                if (office != null) {
                    officeId = officeIdParam;
                }
            }
            model.addAttribute("officeId", officeId);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "register",
                    "viewRegister",
                    "admin"
            );
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "/viewAppealRegister.do", method = RequestMethod.GET)
    public ModelAndView getAppealRegisterPage(HttpServletRequest request, Authentication authentication, Model model) {
        if (authentication != null) {
            UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
            Long officeId = userInformation.getOfficeInformation().getOfficeId();
            model.addAttribute("officeId", officeId);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "register",
                    "viewAppealRegister",
                    "admin"
            );
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "/viewReports.do", method = RequestMethod.GET)
    public ModelAndView getReportPage(HttpServletRequest request, Authentication authentication, Model model) {
        if (authentication != null) {
            UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
            OfficeInformation officeInformation = userInformation.getOfficeInformation();
            String requestParams = request.getParameter("params");
            Long officeId = officeInformation.getOfficeId();
            String officeName = messageService.isCurrentLanguageInEnglish() ? officeInformation.getOfficeNameEnglish() : officeInformation.getOfficeNameBangla();
            Boolean isDrilledDown = false;
            if (StringUtil.isValidString(requestParams)) {
                String decodedParams = StringUtils.newStringUtf8(Base64.decodeBase64(requestParams.substring(20)));
                Long childOfficeId = Long.parseLong(decodedParams);
                Office childOffice = officeService.findOne(childOfficeId);
                List<Long> parentOfficeIds = officeService.getAncestorOfficeIds(childOfficeId);
                if (childOffice != null && (parentOfficeIds.contains(officeId) || userInformation.getIsCentralDashboardUser())) {
                    officeId = childOfficeId;
                    officeName = messageService.isCurrentLanguageInEnglish() ? childOffice.getNameEnglish() : childOffice.getNameBangla();
                    isDrilledDown = true;
                } else {
                    return new ModelAndView("redirect:/error-page");
                }
            }
            model.addAttribute("officeId", officeId);
            model.addAttribute("officeName", officeName);
            model.addAttribute("isDrilledDown", isDrilledDown);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "reports",
                    "viewReports",
                    "admin"
            );
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "viewCitizenCharter.do", method = RequestMethod.GET)
    public ModelAndView getViewCitizenCharterPage(HttpServletRequest request, Authentication authentication, Model model) {
        if (authentication != null) {
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "citizencharter",
                    "viewCitizenCharter",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "editCitizenCharter.do", method = RequestMethod.GET)
    public ModelAndView getEditCitizenCharterPage(HttpServletRequest request, Authentication authentication, Model model, @RequestParam(value = "id") Long id) {
        if (authentication != null) {
            CitizenCharter citizenCharter = citizenCharterService.findOne(id);
            CitizenCharterDTO citizenCharterDTO = officeService.getCitizenCharterDTOFromCitizenCharter(citizenCharter);
            citizenCharterDTO.setOfficeId(citizenCharter.getOfficeId());
            model.addAttribute("service", citizenCharterDTO);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "citizencharter",
                    "addCitizenCharter",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "addCitizenCharter.do", method = RequestMethod.GET)
    public ModelAndView getAddCitizenCharterPage(HttpServletRequest httpServletRequest, Authentication authentication, Model model) {
        Map<String, ServiceType> serviceTypes = new HashMap<>();
        serviceTypes.put("citizen", ServiceType.NAGORIK);
        serviceTypes.put("official", ServiceType.DAPTORIK);
        serviceTypes.put("internal", ServiceType.STAFF);
        if (authentication != null) {
            model.addAttribute("serviceTypes", serviceTypes);
            CitizenCharter citizenCharter = new CitizenCharter();
            CitizenCharterDTO citizenCharterDTO = officeService.getCitizenCharterDTOFromCitizenCharter(citizenCharter);
            model.addAttribute("service", citizenCharterDTO);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    httpServletRequest,
                    "citizencharter",
                    "addCitizenCharter",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "addCitizenCharterOrigin.do", method = RequestMethod.GET)
    public ModelAndView getAddCitizenCharterOriginPage(HttpServletRequest request,
                                                       Authentication authentication,
                                                       Model model,
                                                       @RequestParam("officeOriginId") Long officeOriginId,
                                                       @RequestParam("id") Long id) {
        id = (id != null) ? id : 0L;
        if (authentication != null) {
            Map<String, ServiceType> serviceTypes = new HashMap() {{
                put("citizen", ServiceType.NAGORIK);
                put("official", ServiceType.DAPTORIK);
                put("internal", ServiceType.STAFF);
            }};
            ServiceOriginDTO serviceOriginDTO = officeService.getServiceOriginDTObyId(id);
            List<OfficeOriginUnitDTO> officeOriginUnitList = officeService.getOfficeOriginUnitDTOListByOfficeOriginId(officeOriginId);
            Long officeOriginUnitOrganogramId = serviceOriginDTO.getOfficeOriginUnitOrganogramId();
            if (officeOriginUnitOrganogramId != null && officeOriginUnitOrganogramId > 0) {
                List<OfficeOriginUnitOrganogramDTO> officeOriginUnitOrganogramList = officeService.getOfficeOriginUnitOrganogramDTOListByOfficeOriginUnitId(serviceOriginDTO.getOfficeOriginUnitId());
                model.addAttribute("officeOriginUnitOrganogramList", officeOriginUnitOrganogramList);
            }
            model.addAttribute("service", serviceOriginDTO);
            model.addAttribute("officeOriginId", officeOriginId);
            model.addAttribute("serviceTypes", serviceTypes);
            model.addAttribute("officeOriginUnitList", officeOriginUnitList);
            return new ModelAndView("citizencharter/addCitizenCharterOrigin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "viewEmailTemplate.do", method = RequestMethod.GET)
    public ModelAndView getViewEmailTemplatePage(Authentication authentication, Model model, HttpServletRequest request) {
        if (authentication != null) {
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "emailTemplates",
                    "viewEmailTemplate",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "addEmailTemplate.do", method = RequestMethod.GET)
    public ModelAndView getAddEmailTemplatePage(HttpServletRequest httpServletRequest, Authentication authentication, Model model) {
        if (authentication != null) {
            model.addAttribute("grsRoles", emailSmsSettingsService.convertToGrsRoleList());
            model.addAttribute("grievanceStatuses", emailSmsSettingsService.convertToGrievanceStatusList());
            model.addAttribute("actions", emailSmsSettingsService.convertToActionList());
            EmailTemplate emailTemplate = new EmailTemplate();
            EmailTemplateDTO emailTemplateDTO = emailService.convertToEmailTemplateDTO(emailTemplate);
            model.addAttribute("emailTemplate", emailTemplateDTO);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    httpServletRequest,
                    "emailTemplates",
                    "addEmailTemplate",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "editEmailTemplate.do", method = RequestMethod.GET)
    public ModelAndView getEditEmailTemplatePage(HttpServletRequest httpServletRequest,
                                                 Authentication authentication,
                                                 Model model,
                                                 @RequestParam(value = "id") Long id) {
        if (authentication != null) {
            EmailTemplate emailTemplate = this.emailService.getEmailTemplate(id);
            EmailTemplateDTO emailTemplateDTO = this.emailService.convertToEmailTemplateDTOWithRecipient(emailTemplate);
            model.addAttribute("emailTemplate", emailTemplateDTO);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    httpServletRequest,
                    "emailTemplates",
                    "addEmailTemplate",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "viewSmsTemplate.do", method = RequestMethod.GET)
    public ModelAndView getViewSmsTemplatePage(Authentication authentication, Model model, HttpServletRequest request) {
        if (authentication != null) {
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "smsTemplates",
                    "viewSmsTemplate",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "addSmsTemplate.do", method = RequestMethod.GET)
    public ModelAndView getAddSmsTemplatePage(HttpServletRequest httpServletRequest, Authentication authentication, Model model) {
        if (authentication != null) {
            model.addAttribute("grsRoles", emailSmsSettingsService.convertToGrsRoleList());
            model.addAttribute("grievanceStatuses", emailSmsSettingsService.convertToGrievanceStatusList());
            model.addAttribute("actions", emailSmsSettingsService.convertToActionList());
            SmsTemplate smsTemplate = new SmsTemplate();
            SmsTemplateDTO smsTemplateDTO = shortMessageService.convertToSmsTemplateDTO(smsTemplate);
            model.addAttribute("smsTemplate", smsTemplateDTO);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    httpServletRequest,
                    "smsTemplates",
                    "addSmsTemplate",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "editSmsTemplate.do", method = RequestMethod.GET)
    public ModelAndView getEditSmsTemplatePage(HttpServletRequest httpServletRequest,
                                               Authentication authentication,
                                               Model model,
                                               @RequestParam(value = "id") Long id) {
        if (authentication != null) {
            SmsTemplate smsTemplate = this.shortMessageService.getSmsTemplate(id);
            SmsTemplateDTO smsTemplateDTO = this.shortMessageService.convertToSmsTemplateDTOWithRecipient(smsTemplate);
            model.addAttribute("smsTemplate", smsTemplateDTO);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    httpServletRequest,
                    "smsTemplates",
                    "addSmsTemplate",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "viewOccupations.do", method = RequestMethod.GET)
    public ModelAndView getViewOccupationsPage(Authentication authentication, Model model, HttpServletRequest request) {
        if (authentication != null) {
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "occupation",
                    "viewOccupations",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "addOccupations.do", method = RequestMethod.GET)
    public ModelAndView getAddOccupationsPage(HttpServletRequest httpServletRequest, Authentication authentication, Model model) {
        if (authentication != null) {
            Occupation occupation = new Occupation();
            OccupationDTO occupationDTO = occupationService.convertToOccupationDTO(occupation);
            model.addAttribute("occupation", occupationDTO);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    httpServletRequest,
                    "occupation",
                    "addOccupations",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "editOccupations.do", method = RequestMethod.GET)
    public ModelAndView getEditOccupationsPage(HttpServletRequest httpServletRequest,
                                               Authentication authentication,
                                               Model model,
                                               @RequestParam(value = "id") Long id) {
        if (authentication != null) {
            Occupation occupation = occupationService.getOccupation(id);
            OccupationDTO occupationDTO = occupationService.convertToOccupationDTO(occupation);
            model.addAttribute("occupation", occupationDTO);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    httpServletRequest,
                    "occupation",
                    "addOccupations",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "viewEducation.do", method = RequestMethod.GET)
    public ModelAndView getViewEducationPage(Authentication authentication, Model model, HttpServletRequest request) {
        if (authentication != null) {
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "education",
                    "viewEducation",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "addEducation.do", method = RequestMethod.GET)
    public ModelAndView getAddEducationPage(HttpServletRequest httpServletRequest, Authentication authentication, Model model) {
        if (authentication != null) {
            Education education = new Education();
            EducationDTO educationDTO = educationService.convertToEducationDTO(education);
            model.addAttribute("education", educationDTO);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    httpServletRequest,
                    "education",
                    "addEducation",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "editEducation.do", method = RequestMethod.GET)
    public ModelAndView getEditEducationPage(HttpServletRequest httpServletRequest,
                                             Authentication authentication,
                                             Model model,
                                             @RequestParam(value = "id") Long id) {
        if (authentication != null) {
            Education education = educationService.getEducation(id);
            EducationDTO educationDTO = educationService.convertToEducationDTO(education);
            model.addAttribute("education", educationDTO);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    httpServletRequest,
                    "education",
                    "addEducation",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "viewServices.do", method = RequestMethod.GET)
    public ModelAndView getViewServicesPage(Authentication authentication, Model model, HttpServletRequest request) {
        if (authentication != null) {
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "service",
                    "viewServices",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "addServices.do", method = RequestMethod.GET)
    public ModelAndView getAddServicePage(HttpServletRequest httpServletRequest, Authentication authentication, Model model) {
        if (authentication != null) {
            ServiceOrigin serviceOrigin = new ServiceOrigin();
            ServiceOriginDTO serviceOriginDTO = officeService.getServiceDTOFromService(serviceOrigin);
            model.addAttribute("service", serviceOriginDTO);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    httpServletRequest,
                    "service",
                    "addService",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "editServices.do", method = RequestMethod.GET)
    public ModelAndView getEditServicePage(HttpServletRequest httpServletRequest,
                                           Authentication authentication,
                                           Model model,
                                           @RequestParam(value = "id") Long id) {
        if (authentication != null) {
            ServiceOrigin serviceOrigin = officeService.getServiceOrigin(id);
            ServiceOriginDTO serviceOriginDTO = officeService.getServiceDTOFromService(serviceOrigin);
            model.addAttribute("service", serviceOriginDTO);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    httpServletRequest,
                    "service",
                    "addService",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "setupEmailSMS.do", method = RequestMethod.GET)
    public ModelAndView getSetupEmailSMSPage(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(value = "id") Long id) {
        if (authentication != null) {
            EmailSmsSettings emailSmsSettings = emailSmsSettingsService.getEmailSmsSettings(id);
            EmailSmsSettingsDTO emailSmsSettingsDTO = this.emailSmsSettingsService.getEmailSmsSettingsDTO(emailSmsSettings);
            model.addAttribute("settings", emailSmsSettingsDTO);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "emailSmsSettings",
                    "setupEmailSMS",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "manageOffice.do", method = RequestMethod.GET)
    public ModelAndView getManageOfficePage(Authentication authentication, Model model, HttpServletRequest request) {
        if (authentication != null) {
            UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
            String viewName = "";
            OfficeSetupDTO officeSetupDTO = new OfficeSetupDTO();
            model.addAttribute("office", officeSetupDTO);
            if (userInformation.getOisfUserType() != null && userInformation.getIsOfficeAdmin()) {
                viewName = "admin";
                OfficeInformation officeInformation = userInformation.getOfficeInformation();
                Boolean isMinistryOrDivisionLevelOffice = officeService.isMinistryOrDivisionLevelOffice(officeInformation.getOfficeId());
                model.addAttribute("manageOwnOffice", true);
                model.addAttribute("officeId", officeInformation.getOfficeId());
                model.addAttribute("officeName", officeInformation.getOfficeNameBangla());
                model.addAttribute("officeOriginId", officeInformation.getOfficeOriginId());
                model.addAttribute("officeUnitOrganogramId", officeInformation.getOfficeUnitOrganogramId());
                model.addAttribute("canNotChangeAO", !isMinistryOrDivisionLevelOffice);
            } else if (userInformation.getGrsUserType() != null && userInformation.getGrsUserType().equals(GRSUserType.SUPER_ADMIN)) {
                viewName = "superAdmin";
            } else {
                return new ModelAndView("redirect:/error-page");
            }
            model.addAttribute("searchableOffices", officeService.getOfficeSearchingData());
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "management",
                    "officeManagement",
                    viewName);
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "manageSubOffices.do", method = RequestMethod.GET)
    public ModelAndView getManageSubOfficesPage(Authentication authentication, Model model, HttpServletRequest request) {
        if (authentication != null) {
            UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
            OfficeSetupDTO officeSetupDTO = new OfficeSetupDTO();
            model.addAttribute("office", officeSetupDTO);
            if (userInformation.getOisfUserType() != null && userInformation.getIsOfficeAdmin()) {
                OfficeInformation officeInformation = userInformation.getOfficeInformation();
                model.addAttribute("showAllOffices", true);
                model.addAttribute("showChildOfficesOnly", true);
                model.addAttribute("officeId", officeInformation.getOfficeId());
                model.addAttribute("officeOriginId", officeInformation.getOfficeOriginId());
                model.addAttribute("officeUnitOrganogramId", officeInformation.getOfficeUnitOrganogramId());
                model.addAttribute("searchableOffices", officeService.getDescendantOfficeSearchingData());
            } else {
                return new ModelAndView("redirect:/error-page");
            }
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "management",
                    "officeManagement",
                    "admin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "manageCitizenCharter.do", method = RequestMethod.GET)
    public ModelAndView getManageCitizenCharterPage(Authentication authentication, Model model, HttpServletRequest request) {
        if (authentication != null) {
            UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
            String viewName = "";
            CitizensCharterOrigin citizensCharterOrigin = new CitizensCharterOrigin();
            CitizensCharterOriginDTO citizensCharterOriginDTO = officeService.convertToOfficeOriginInfoDTO(citizensCharterOrigin);
            model.addAttribute("office", citizensCharterOriginDTO);
            model.addAttribute("officeOriginInfoId", citizensCharterOriginDTO.getId());
            if (userInformation.getUserType().equals(UserType.OISF_USER)) {
                if (!userInformation.getIsOfficeAdmin()) {
                    return new ModelAndView("redirect:/error-page");
                }
                model.addAttribute("showChildOfficesOnly", true);
                model.addAttribute("showAllOffices", true);
                viewName = "admin";
            } else {
                viewName = "superAdmin";
            }
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "management",
                    "citizenCharterSetup",
                    viewName);
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "viewOffice.do", method = RequestMethod.GET)
    public ModelAndView getGRSOfficePage(Authentication authentication, Model model, HttpServletRequest request) {
        if (authentication != null) {
            model.addAttribute("showAllOffices", false);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "management",
                    "viewGRSOffice",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "viewAllOffice.do", method = RequestMethod.GET)
    public ModelAndView getAllGRSOfficePage(Authentication authentication, Model model, HttpServletRequest request) {
        if (authentication != null) {
            List<BaseObjectDTO> offices = this.officesGroDAO.findAll().stream().map((OfficesGRO officesGRO) -> {
                String name = officesGRO.getOfficeId() == 0 ? "অভিযোগ ব্যবস্থাপনা সেল" : officesGRO.getOfficeNameBangla();
                return BaseObjectDTO.builder()
                        .id(officesGRO.getId())
                        .name(name)
                        .build();
            }).collect(Collectors.toList());
            model.addAttribute("grsOffices", offices);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "management",
                    "viewAllGRSOffice",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "manageCell.do", method = RequestMethod.GET)
    public ModelAndView getGRSCellPage(Authentication authentication, Model model, HttpServletRequest request) {
        if (authentication != null) {
            model.addAttribute("searchableOffices", officeService.getGrsEnabledOfficeSearchingData());
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "management",
                    "cellManagement",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "manageCentralDashboardReceivers.do", method = RequestMethod.GET)
    public ModelAndView getCentralDashboardReceiverSettings(Authentication authentication, Model model, HttpServletRequest request) {
        if (authentication != null) {
            model.addAttribute("searchableOffices", officeService.getGrsEnabledOfficeSearchingData());
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "settings",
                    "centralDashboardReceiverSettings",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "/provideNudge.do", method = RequestMethod.GET)
    public ModelAndView getTimeExpiredGrievancesPage(HttpServletRequest request, Authentication authentication, Model model) {
        if (authentication != null) {
            UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
            Long officeId = userInformation.getOfficeInformation().getOfficeId();
            String type = request.getParameter("type");
            String requestParams = request.getParameter("params");
            if (StringUtil.isValidString(requestParams)) {
                Long officeIdParam = StringUtil.decodeOfficeIdOnDashboardDrillDown(requestParams);
                Office office = officeService.findOne(officeIdParam);
                if (office != null) {
                    officeId = officeIdParam;
                }
            }
            model.addAttribute("isAppeal", type != null && type.equals("appeal"));
            model.addAttribute("officeId", officeId);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "nudge",
                    "viewTimeExpiredGrievances",
                    "admin"
            );
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "uploadCitizensCharter.do", method = RequestMethod.GET)
    public ModelAndView uploadCitizensCharter(HttpServletRequest request, Authentication authentication, Model model) {
        if (authentication != null) {
            String viewName = "";
            UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
            if (userInformation.getUserType().equals(UserType.OISF_USER)) {
                if (!userInformation.getIsOfficeAdmin()) {
                    return new ModelAndView("redirect:/error-page");
                }
                model.addAttribute("showAllOffices", true);
                model.addAttribute("showChildOfficesOnly", true);
                viewName = "admin";
            } else {
                viewName = "superAdmin";
            }
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "citizencharter",
                    "uploadCitizensCharter",
                    viewName);
        }
        return new ModelAndView("redirect:/error-page");
    }

//    @RequestMapping(value = "/profile.do", method = RequestMethod.GET)
//    public ModelAndView getUpdateProfilePage(Authentication authentication, Model model, HttpServletRequest request) {
//        UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
//        if (!userInformation.getUserType().equals(UserType.COMPLAINANT)) {
//            return new ModelAndView("redirect:/error-page");
//        }
//        this.complainantService.getComplaintDTO(userInformation);
//        if (authentication != null) {
//            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
//                    authentication,
//                    request,
//                    "profile",
//                    "updateProfile",
//                    "admin");
//        }
//        return new ModelAndView("redirect:/error-page");
//    }

    @RequestMapping(value = "fileUploadSetup.do", method = RequestMethod.GET)
    public ModelAndView getFileUploadSetupForm(HttpServletRequest request, Authentication authentication, Model model) {
        if (authentication != null) {
            Integer maxFileSize = generalSettingsService.getMaximumFileSize();
            String fileTypes = generalSettingsService.getAllowedFileTypes();
            List<String> allowedFileTypes = Arrays.asList(fileTypes.split("\\|"));
            model.addAttribute("maxFileSize", maxFileSize);
            model.addAttribute("allowedFileTypes", allowedFileTypes);
            model.addAttribute("fileSizeLabel", generalSettingsService.getAllowedFileSizeLabel());
            model.addAttribute("fileTypesLabel", generalSettingsService.getAllowedFileTypesLabel());
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "settings",
                    "fileSettings",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "systemNotificationSettings.do", method = RequestMethod.GET)
    public ModelAndView getSystemNotificationSettingsForm(HttpServletRequest request, Authentication authentication, Model model) {
        if (authentication != null) {
            String email = generalSettingsService.getSettingsValueByFieldName(Constant.SYSTEM_NOTIFICATION_EMAIL);
            String phoneNumber = generalSettingsService.getSettingsValueByFieldName(Constant.SYSTEM_NOTIFICATION_PHONE_NUMBER);
            model.addAttribute("email", email);
            model.addAttribute("phoneNumber", phoneNumber);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "settings",
                    "systemNotificationSettings",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "viewRatingsAndFeedback.do", method = RequestMethod.GET)
    public ModelAndView viewAllRatingsAndFeedback(Authentication authentication, Model model, HttpServletRequest request) {
        if (authentication != null) {
            UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
            Long officeId = userInformation.getOfficeInformation().getOfficeId();
            String type = request.getParameter("type");
            String requestParams = request.getParameter("params");
            if (StringUtil.isValidString(requestParams)) {
                Long officeIdParam = StringUtil.decodeOfficeIdOnDashboardDrillDown(requestParams);
                Office office = officeService.findOne(officeIdParam);
                if (office != null) {
                    officeId = officeIdParam;
                }
            }
            model.addAttribute("type", type);
            model.addAttribute("officeId", officeId);
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "grievances",
                    "viewRatingsAndFeedback",
                    "admin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "dashboard.do", method = RequestMethod.GET)
    public ModelAndView getGeneralDashboard(Authentication authentication, Model model, HttpServletRequest request) {
        if (authentication != null) {
            UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
            if (Utility.canViewDashboard(authentication)) {
                Boolean isMobileLogin = Utility.isLoggedInFromMobile(authentication);
                if(isMobileLogin){
                    return new ModelAndView("redirect:/dashboardMobile.do");
                }
                String requestParams = request.getParameter("params");
                OfficeInformation officeInformation = userInformation.getOfficeInformation();
                Long officeId = officeInformation.getOfficeId();
                Office userOffice = this.officeService.getOffice(officeId);
                Integer divisionId = userOffice.getDivisionId();
                Integer districtId = userOffice.getDistrictId();
                Integer upazilaId = userOffice.getUpazilaId();
                Long officeLevel = officeInformation.getLayerLevel();
                Boolean isMinistryOrDivisionLevelOffice = officeService.isMinistryOrDivisionLevelOffice(officeId);
                String officeName = messageService.isCurrentLanguageInEnglish() ? officeInformation.getOfficeNameEnglish() : officeInformation.getOfficeNameBangla();
                Boolean isDrilledDown = false;
                if (StringUtil.isValidString(requestParams)) {
                    String decodedParams = StringUtils.newStringUtf8(Base64.decodeBase64(requestParams.substring(20)));
                    Long targetOfficeId = Long.parseLong(decodedParams);
                    Office targetOffice = officeService.findOne(targetOfficeId);
                    List<Long> parentOfficeIds = officeService.getAncestorOfficeIds(targetOfficeId);
                    if (targetOffice != null && ((parentOfficeIds.contains(officeId) || userInformation.getIsCentralDashboardUser()) || officeService.canViewDashboardAsFieldCoordinator(authentication, targetOfficeId))) {
                        officeId = targetOfficeId;
                        officeName = messageService.isCurrentLanguageInEnglish() ? targetOffice.getNameEnglish() : targetOffice.getNameBangla();
                        isDrilledDown = true;
                    } else {
                        return new ModelAndView("redirect:/error-page");
                    }
                }
                List<OfficeSearchDTO> officeSearchDTOList = officeService.getGrsEnabledOfficeSearchingData();
                Boolean isMinistryLevelGro = false;
                if (isMinistryOrDivisionLevelOffice && Utility.isUserAnGROUser(authentication)) {
                    isMinistryLevelGro = true;
                    officeSearchDTOList = officeService.getDescendantOfficeSearchingData();
                    model.addAttribute("showAllOffices", true);
                    model.addAttribute("showChildOfficesOnly", true);
                }
                model.addAttribute("officeId", officeId);
                model.addAttribute("officeName", officeName);
                model.addAttribute("currentMonthYear", messageService.getCurrentMonthYearAsString());
                model.addAttribute("currentYear", messageService.getCurrentYearString());
                model.addAttribute("currentDateMonthYear", messageService.getCurrentDateMonthYearAsString());
                model.addAttribute("isDrilledDown", isDrilledDown);
                model.addAttribute("canViewGrievanceDashboard", Utility.isUserAnGROUser(authentication) || Utility.isUserAHOOUser(authentication) || Utility.isUserACentralDashboardRecipient(authentication));
                model.addAttribute("canViewAppealAndSubOfficeDashboard", officeService.hasAccessToAoAndSubOfficesDashboard(userInformation, officeId));
                model.addAttribute("isDivisionLevelFC", Utility.isDivisionLevelFC(authentication));
                model.addAttribute("isDistrictLevelFC", Utility.isDistrictLevelFC(authentication));
                model.addAttribute("isMinistryLevelGro", isMinistryLevelGro);
                model.addAttribute("searchableOffices", officeSearchDTOList);
                model.addAttribute("officeLevel", officeLevel);
                model.addAttribute("divisionId", divisionId);
                model.addAttribute("districtId", districtId);
                model.addAttribute("upazilaId", upazilaId);
                return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                        authentication,
                        request,
                        "dashboard2",
                        "hooDashboard",
                        "dashboard");
            }
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "grsApplicationPrivacyPolicy.do", method = RequestMethod.GET)
    public ModelAndView getGrsApplicationPrivacyPolicy(Authentication authentication, Model model, HttpServletRequest request) {
        Map attrs = new HashMap() {{
            put("isProductionMode", true);
            put("grsUser", true);
            put("superAdmin", true);
        }};
        model.addAllAttributes(attrs);
        return new ModelAndView("grsPrivacyPolicy");
    }

    @RequestMapping(value = "grsUsers.do", method = RequestMethod.GET)
    public ModelAndView getUsersCount(Authentication authentication, Model model, HttpServletRequest request){
        if (authentication != null){
            model.addAttribute("userCount", BanglaConverter.convertToBanglaDigit(complainantService.countAll()));
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    request,
                    "grsUser",
                    "viewGrsUserCount",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

    @RequestMapping(value = "addGrsUsers.do", method = RequestMethod.GET)
    public ModelAndView getAddGrsUserPage(HttpServletRequest httpServletRequest, Authentication authentication, Model model) {
        if (authentication != null) {
            SuperAdmin superAdmin = new SuperAdmin();
            GrsUserDTO superAdminDTO = grsUserService.convertToSuperAdminDTO(superAdmin);
            model.addAttribute("grsUserFormData", superAdminDTO);
            model.addAttribute("showAllOffices", false);
            model.addAttribute("searchableOffices", officeService.getGrsEnabledOfficeSearchingData());
            return modelViewService.addNecessaryAttributesAndReturnViewPage(model,
                    authentication,
                    httpServletRequest,
                    "grsUser",
                    "addGrsUser",
                    "superAdmin");
        }
        return new ModelAndView("redirect:/error-page");
    }

}
