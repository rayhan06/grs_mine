package com.grs.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Acer on 10/4/2017.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInformation {
    private Long userId;
    private String username;
    private UserType userType;
    private OISFUserType oisfUserType;
    private GRSUserType grsUserType;
    private OfficeInformation officeInformation;
    private Boolean isAppealOfficer;
    private Boolean isOfficeAdmin;
    private Boolean isCentralDashboardUser;
    private Boolean isCellGRO;
    private Boolean isMobileLogin;
    private Boolean isMyGovLogin;
    private String token = "";
}
