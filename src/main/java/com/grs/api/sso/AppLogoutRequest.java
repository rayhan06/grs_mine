package com.grs.api.sso;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * Created by USER on 11-Mar-18.
 */
public class AppLogoutRequest {
    SSOPropertyReader ssoPropertyReader;

    public AppLogoutRequest()throws Exception{
        ssoPropertyReader = SSOPropertyReader.getInstance();
    }

    public String buildLogoutRequest(){
        UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromUriString(ssoPropertyReader.getIdpUrl() + "/" + SSOConstants.SSO_LOGOUT_RNDPOINT);
        urlBuilder.queryParam("login_url",ssoPropertyReader.getLoginPageUri());
        return urlBuilder.build().encode().toUriString();
    }
}
