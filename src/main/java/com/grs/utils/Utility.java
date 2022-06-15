package com.grs.utils;

import com.grs.api.config.security.CustomAuthenticationToken;
import com.grs.api.model.GRSUserType;
import com.grs.api.model.OISFUserType;
import com.grs.api.model.UserInformation;
import com.grs.api.model.UserType;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.security.core.Authentication;

/**
 * Created by Acer on 16-Oct-17.
 */
public class Utility {
    public static UserInformation extractUserInformationFromAuthentication(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        CustomAuthenticationToken customAuthenticationToken = (CustomAuthenticationToken) authentication;
        UserInformation userInformation = customAuthenticationToken.getUserInformation();
        return userInformation;
    }

    public static Boolean isUserAnGRSUser(Authentication authentication) {
        UserInformation userInformation = extractUserInformationFromAuthentication(authentication);
        return userInformation.getUserType().equals(UserType.COMPLAINANT);
    }

    public static Boolean isUserAnOthersComplainant(Authentication authentication) {
        UserInformation userInformation = extractUserInformationFromAuthentication(authentication);
        GRSUserType grsUserType = userInformation.getGrsUserType();
        if (grsUserType == null) {
            return false;
        }
        return grsUserType.equals(GRSUserType.OTHERS_COMPLAINANT);
    }

    public static Boolean isUserAnGRSUserOrOthersComplainant(Authentication authentication) {
        UserInformation userInformation = extractUserInformationFromAuthentication(authentication);
        GRSUserType grsUserType = userInformation.getGrsUserType();
        if (grsUserType == null) {
            return userInformation.getUserType().equals(UserType.COMPLAINANT);
        }
        return grsUserType.equals(GRSUserType.OTHERS_COMPLAINANT);
    }

    public static Boolean isUserAnOisfUser(Authentication authentication) {
        UserInformation userInformation = extractUserInformationFromAuthentication(authentication);
        return (userInformation.getUserType().equals(UserType.OISF_USER));
    }

    public static Boolean isUserAnGROUser(Authentication authentication) {
        UserInformation userInformation = extractUserInformationFromAuthentication(authentication);
        if (userInformation.getOfficeInformation() != null && userInformation.getOisfUserType() == OISFUserType.GRO) {
            return true;
        }
        return false;
    }

    public static Boolean isCellGRO(Authentication authentication) {
        UserInformation userInformation = extractUserInformationFromAuthentication(authentication);
        if (userInformation.getUserType().equals(UserType.OISF_USER)) {
            return userInformation.getIsCellGRO();
        }
        return false;
    }

    public static Boolean canViewDashboard(Authentication authentication) {
        return !isUserAnGRSUser(authentication) && (isUserAnGROUser(authentication)
                || isUserAHOOUser(authentication)
                || isUserACentralDashboardRecipient(authentication)
                || isCellGRO(authentication));
    }

    public static Boolean isServiceOfficer(Authentication authentication) {
        UserInformation userInformation = extractUserInformationFromAuthentication(authentication);
        if (userInformation.getOfficeInformation() != null && userInformation.getOisfUserType() == OISFUserType.SERVICE_OFFICER) {
            return true;
        }
        return false;
    }

    public static Boolean isUserAHOOUser(Authentication authentication) {
        UserInformation userInformation = extractUserInformationFromAuthentication(authentication);
        if (userInformation.getOfficeInformation() != null && userInformation.getOisfUserType() == OISFUserType.HEAD_OF_OFFICE) {
            return true;
        }
        return false;
    }

    public static Boolean isDivisionLevelFC(Authentication authentication) {
        UserInformation userInformation = extractUserInformationFromAuthentication(authentication);
        if (userInformation.getOfficeInformation() != null && userInformation.getOisfUserType() == OISFUserType.HEAD_OF_OFFICE) {
            Long officeOriginId = userInformation.getOfficeInformation().getOfficeOriginId();
            return officeOriginId.equals(Constant.DIVISION_FIELD_COORDINATOR_OFFICE_ORIGIN_ID);
        }
        return false;
    }

    public static Boolean isDistrictLevelFC(Authentication authentication) {
        UserInformation userInformation = extractUserInformationFromAuthentication(authentication);
        if (userInformation.getOfficeInformation() != null && userInformation.getOisfUserType() == OISFUserType.HEAD_OF_OFFICE) {
            Long officeOriginId = userInformation.getOfficeInformation().getOfficeOriginId();
            return officeOriginId.equals(Constant.DISTRICT_FIELD_COORDINATOR_OFFICE_ORIGIN_ID);
        }
        return false;
    }

    public static Boolean isUserACentralDashboardRecipient(Authentication authentication) {
        UserInformation userInformation = extractUserInformationFromAuthentication(authentication);
        if (userInformation.getIsCentralDashboardUser() != null) {
            return userInformation.getIsCentralDashboardUser();
        }
        return false;
    }

    public static Boolean isLoggedInFromMobile(Authentication authentication) {
        UserInformation userInformation = extractUserInformationFromAuthentication(authentication);
        if (userInformation.getIsMobileLogin() != null) {
            return userInformation.getIsMobileLogin();
        }
        return false;
    }

    public static Boolean isFieldCoordinator(Authentication authentication) {
        UserInformation userInformation = extractUserInformationFromAuthentication(authentication);
        if (userInformation.getOfficeInformation() != null &&
                userInformation.getOfficeInformation().getOfficeMinistryId().equals(Constant.ministryIdFive) &&
                (userInformation.getOfficeInformation().getLayerLevel().equals(Constant.layerThree) || userInformation.getOfficeInformation().getLayerLevel().equals(Constant.layerFour)) &&
                userInformation.getOisfUserType() == OISFUserType.HEAD_OF_OFFICE) {
            return true;
        }
        return false;
    }

    public static Boolean isUserASuperAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        UserInformation userInformation = extractUserInformationFromAuthentication(authentication);
        return userInformation.getUserType().equals(UserType.SYSTEM_USER);
    }

    public static boolean isNumber(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            int d = Integer.parseInt(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}
