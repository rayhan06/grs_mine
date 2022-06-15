package com.grs.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.grs.api.exception_handler.ApiErrorEnum;
import com.grs.api.exception.DuplicateEmailException;
import com.grs.api.model.request.ComplainantDTO;
import com.grs.api.model.request.GrievanceWithoutLoginRequestDTO;
import com.grs.api.myGov.*;
import com.grs.api.sso.SSOPropertyReader;
import com.grs.utils.BanglaConverter;
import com.grs.utils.Constant;
//import com.grs.utils.DisableSSLCertificateCheckUtil;
import com.grs.utils.DisableSSLCertificateCheckUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Acer on 03-Jan-18.
 */
@Slf4j
@Service
public class MyGovConnectorService {

    @Autowired
    ComplainantService complainantService;

    private String authToken;
    private RestTemplate restTemplate;

    private String baseUrl = "";
    private String myGovComplaintApiBaseUri = "";

    private String clientId = "";
    private String authSecretKey = "";
    private String apiSecretKey = "";
    private long expirationTime = 0;
    private String cachedToken = "";

    @PostConstruct
    public void init() {
        restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            ClientHttpResponse response = execution.execute(request, body);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return response;
        });
        try {

            expirationTime = new Date().getTime();
            clientId = SSOPropertyReader.getInstance().getMygovClientId();
            authSecretKey = SSOPropertyReader.getInstance().getMygovClientSecret();
            apiSecretKey = SSOPropertyReader.getInstance().getMygovApiSecret();
            baseUrl = SSOPropertyReader.getInstance().getMyGovApiBaseUri();
            myGovComplaintApiBaseUri = SSOPropertyReader.getInstance().getMyGovComplaintApiBaseUri();
            authToken = "";
            DisableSSLCertificateCheckUtil.disableChecks();
//            authToken = (String) getAuthToken();
        } catch (Exception e) {
            authToken = "";
            e.printStackTrace();
        }
    }

    private String getApiToken() throws Exception {

        if (new Date().getTime() < expirationTime && !cachedToken.isEmpty()) return cachedToken;

        HttpHeaders headers;
        headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//        headers.add("Authorization", "Secret " + secretKey);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("client_id", clientId);
        map.add("api_key", apiSecretKey);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
        try {
            Map<String, String> resultObject = restTemplate.postForObject(baseUrl + "/token/create", request, Map.class);
            if (!resultObject.get("status").equalsIgnoreCase("success")) {
                throw new Exception(resultObject.get("message"));
            }
            cachedToken = (String)resultObject.get("token");
            expirationTime = new Date().getTime() + Constant.EXPIRATIONTIME;
            return resultObject.get("token");
        } catch (Exception e) {
            HashMap<String, Object> returnObject = new HashMap<String, Object>();
            returnObject.put("Exception in mygov api" , baseUrl + "/token/create");
            returnObject.put("Request body" , request);
            returnObject.put("Mygov Message" , e.getMessage());
            throw new Exception(new Gson().toJson(returnObject));
        }
    }

    public boolean checkUser(String phoneNumber) throws Exception {
        String apiToken = getApiToken();

        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//        headers.add("Authorization", "Bearer " + authorizationToken);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("token", apiToken);
        map.add("mobile", phoneNumber);
        log.info(phoneNumber + " " + baseUrl);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        try {
            Map<String, String> resultObject = restTemplate.postForObject(baseUrl + "/user/check", request, Map.class);
            if (!resultObject.get("status").equalsIgnoreCase("success")) {
                throw new Exception(resultObject.get("message"));
            }
            return Boolean.parseBoolean(resultObject.get("user_exists"));
        } catch (Exception e) {
            HashMap<String, Object> returnObject = new HashMap<String, Object>();
            returnObject.put("Exception in mygov api" , baseUrl + "/user/check");
            returnObject.put("Request body" , request);
            returnObject.put("Mygov Message" , e.getMessage());
            throw new Exception(new Gson().toJson(returnObject));
        }
    }

    public ComplainantDTO createUser(ComplainantDTO complainantDTO) throws Exception {
            String apiToken = getApiToken();
            Gson gson = new Gson();
            MyGovLoginResponse myGovLoginResponse = new MyGovLoginResponse();
            MyGovUserDTO myGovUserDTO = myGovLoginResponse.genericDTOToClaims(complainantDTO);
            myGovUserDTO.setToken(apiToken);


        try {
            Map<String, String> resultObject = restTemplate.postForObject(baseUrl + "/user/create", myGovUserDTO, Map.class);
            if (!resultObject.get("status").equalsIgnoreCase("success")) {
                if (Integer.parseInt(resultObject.get("code")) == ApiErrorEnum.DUPLICATE_EMAIL_EXCEPTION.getValue()) throw new DuplicateEmailException(resultObject.get(String.valueOf(ApiErrorEnum.DUPLICATE_EMAIL_EXCEPTION.getValue())));
                throw new Exception(resultObject.get("message"));
            }
            String json = gson.toJson(resultObject.get("data"));
            myGovUserDTO = gson.fromJson(json, MyGovUserDTO.class);
            complainantDTO = myGovLoginResponse.claimToGenericDTO(myGovUserDTO);

            return complainantDTO;
        } catch (Exception e) {
            HashMap<String, Object> returnObject = new HashMap<String, Object>();
            returnObject.put("Exception in mygov api" , baseUrl + "/user/create");
            returnObject.put("Request body" , myGovUserDTO);
            returnObject.put("Code" , e.getMessage());
            returnObject.put("Message" , e.getMessage());
            if (e instanceof DuplicateEmailException) throw new DuplicateEmailException(e.getMessage());
            throw new Exception(new Gson().toJson(returnObject));
        }
    }

    public MyGovComplaintResponseDTO createComplaint(GrievanceWithoutLoginRequestDTO grievanceWithoutLoginRequestDTO) throws Exception {
            grievanceWithoutLoginRequestDTO.setComplainantPhoneNumber(BanglaConverter.convertAllToEnglish(grievanceWithoutLoginRequestDTO.getComplainantPhoneNumber()));
            String complainantName = complainantService.findComplainantByPhoneNumber(grievanceWithoutLoginRequestDTO.getComplainantPhoneNumber()).getName();
            grievanceWithoutLoginRequestDTO.setName(complainantName);
            MyGovComplaintRequestDTO requestDTO = new MyGovComplaintRequestDTO();
            requestDTO = requestDTO.convertToMyGovComplaintRequestDTO(grievanceWithoutLoginRequestDTO);
            Gson gson = new Gson();

//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//        headers.add("Authorization", "Bearer " + authorizationToken);

        try {
            Map<String, String> resultObject = restTemplate.postForObject(myGovComplaintApiBaseUri + "/api/getUaid", requestDTO, Map.class);
            if (!resultObject.get("status").equalsIgnoreCase("success")) {
                throw new Exception(resultObject.get("message"));
            }
            String json = gson.toJson(resultObject);
            MyGovComplaintResponseDTO responseDTO = gson.fromJson(json, MyGovComplaintResponseDTO.class);

            return responseDTO;
        } catch (Exception e) {
            HashMap<String, Object> returnObject = new HashMap<String, Object>();
            returnObject.put("Exception in mygov api" , myGovComplaintApiBaseUri + "/api/getUaid");
            returnObject.put("Request body" , requestDTO);
            returnObject.put("Mygov Message" , e.getMessage());
            throw new Exception(new Gson().toJson(returnObject));
        }
    }
}
