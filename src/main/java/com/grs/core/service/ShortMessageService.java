package com.grs.core.service;

import com.google.gson.Gson;
import com.grs.api.model.request.BulkSMSAuthDTO;
import com.grs.api.model.request.BulkSMSInfoDTO;
import com.grs.api.model.request.BulkSMSRequestDTO;
import com.grs.api.model.response.ActionToRoleDTO;
import com.grs.api.model.response.SmsTemplateDTO;
import com.grs.core.dao.*;
import com.grs.core.domain.*;
import com.grs.core.domain.grs.*;
import com.grs.utils.BanglaConverter;
import com.grs.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Acer on 22-Dec-17.
 */
@Slf4j
@Service
public class ShortMessageService {
    @Autowired
    private SmsTemplateDAO smsTemplateDAO;
    @Autowired
    private ActionDAO actionDAO;
    @Autowired
    private GrievanceStatusDAO grievanceStatusDAO;
    @Autowired
    private GrsRoleDAO grsRoleDAO;
    @Autowired
    private ActionToRoleDAO actionToRoleDAO;
    @Autowired
    private MessageService messageService;
    @Autowired
    private GrsRoleToSmsDAO grsRoleToSmsDAO;
    @Autowired
    private Environment environment;

    @Value("${sms.gateway.user}")
    private String smsUser;

    @Value("${sms.gateway.password}")
    private String smsPassword;

    @Async("threadPoolTaskExecutorForSMS")
    public void sendSMS(String phoneNumber, String content) {
        try {
            if(!Boolean.valueOf(environment.getProperty("environment.production"))) {
                return;
            }
            phoneNumber = BanglaConverter.convertToEnglish(phoneNumber);
            final String uri = "http://bulkmsg.teletalk.com.bd/api/sendSMS";

            BulkSMSAuthDTO auth = new BulkSMSAuthDTO();
            BulkSMSInfoDTO smsInfo = new BulkSMSInfoDTO();
            BulkSMSRequestDTO requestDTO = new BulkSMSRequestDTO();

            List<String> msisdn = Arrays.asList(phoneNumber);

            auth.setUsername(smsUser);
            auth.setPassword(smsPassword);
            auth.setAcode("1005093");

            smsInfo.setMessage(content);
            smsInfo.setIs_unicode("1");
            smsInfo.setMasking("8801552146224");
            smsInfo.setMsisdn(msisdn);

            requestDTO.setAuth(auth);
            requestDTO.setSmsInfo(smsInfo);

            Gson gson = new Gson();
            String requestString = gson.toJson(requestDTO);

            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getMessageConverters()
                    .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
            String result = restTemplate.postForObject(uri, requestString, String.class);

            if(!result.replaceAll("<[^>]+>", "").toUpperCase().contains("SUCCESS")) {
                log.error("SMS sending failed with response: " + result);
            }
            log.info("SMS Sent at: " + phoneNumber);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Async("threadPoolTaskExecutorForSMS")
    public void sendSMSUsingDB(String phoneNumber, SmsTemplate smsTemplate, Grievance grievance) {
        ActionToRole actionToRole = smsTemplate.getActionToRole();
        String body = null;
        String trackingNumber = (StringUtil.isValidString(grievance.getTrackingNumber())
                && (grievance.getTrackingNumber().startsWith("01"))) ?
                grievance.getTrackingNumber().substring(11) :
                grievance.getTrackingNumber();
        if (smsTemplate.getLanguage().toString().equals("BANGLA")) {
            body = findSmsTemplate(actionToRole).getSmsTemplateBodyBng() + " Tracking number: " + trackingNumber;
        } else {
            body = findSmsTemplate(actionToRole).getSmsTemplateBodyEng() + " Tracking number: " + trackingNumber;
        }
        try {
            final String uri = "http://bulkmsg.teletalk.com.bd/api/sendSMS";

            BulkSMSAuthDTO auth = new BulkSMSAuthDTO();
            BulkSMSInfoDTO smsInfo = new BulkSMSInfoDTO();
            BulkSMSRequestDTO requestDTO = new BulkSMSRequestDTO();

            List<String> msisdn = Arrays.asList(phoneNumber);

            auth.setUsername(smsUser);
            auth.setPassword(smsPassword);
            auth.setAcode("1005093");

            smsInfo.setMessage(body);
            smsInfo.setIs_unicode("1");
            smsInfo.setMasking("8801552146224");
            smsInfo.setMsisdn(msisdn);

            requestDTO.setAuth(auth);
            requestDTO.setSmsInfo(smsInfo);

            Gson gson = new Gson();
            String requestString = gson.toJson(requestDTO);

            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getMessageConverters()
                    .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
            String result = restTemplate.postForObject(uri, requestString, String.class);
            log.info("SMS Sent at: " + phoneNumber);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Page<SmsTemplate> findAllSmsTemplates(Pageable pageable) {
        Page<SmsTemplate> smsTemplates = this.smsTemplateDAO.findAll(pageable);
        return smsTemplates;
    }

    public Boolean saveAllSmsTemplates(SmsTemplateDTO smsTemplateDTO, ActionToRoleDTO actionToRoleDTO) {
        Action action = this.actionDAO.getActionByActionName(actionToRoleDTO.getAction());
        GrievanceStatus grievanceStatus = this.grievanceStatusDAO.findByName(actionToRoleDTO.getGrievanceStatus());
        GrsRole grsRole = this.grsRoleDAO.findByRole(actionToRoleDTO.getRole());
        ActionToRole actionToRole = this.actionToRoleDAO.findByGrievanceStatusAndRoleAndAction(grievanceStatus, grsRole, action);

        String actionBng = actionToRoleDTO.getAction();
        String codeAction = actionBng.toLowerCase().replace('_', '.');
        String roleName = actionToRoleDTO.getRole();
        String codeRole = roleName.toLowerCase().replace('_', '.');
        String statusName = actionToRoleDTO.getGrievanceStatus();
        String codeStatus = statusName.toLowerCase().replace('_', '.');
        String templateName = messageService.getMessage(codeStatus) + " অভিযোগের জন্য " + messageService.getMessage(codeRole) + " এর " + messageService.getMessage(codeAction) + " পদক্ষেপ";
        Integer countSmsTemplate = this.smsTemplateDAO.countBySmsTemplateName(templateName);
        if (!countSmsTemplate.equals(0)) {
            return false;
        }
        SmsTemplate smsTemplate = SmsTemplate.builder()
                .smsTemplateName(templateName)
                .smsTemplateBodyEng(smsTemplateDTO.getSmsTemplateBodyEng())
                .smsTemplateBodyBng(smsTemplateDTO.getSmsTemplateBodyBng())
                .status(smsTemplateDTO.getStatus())
                .language(LanguageStatus.valueOf(smsTemplateDTO.getLanguage()))
                .actionToRole(actionToRole)
                .build();
        List<String> recipients = smsTemplateDTO.getRecipient();
        this.smsTemplateDAO.saveSmsTemplate(smsTemplate, recipients);
        return true;
    }

    public Boolean updateSmsTemplate(Long smsTemplateID, SmsTemplateDTO smsTemplateDTO) {
        List<String> recipients = smsTemplateDTO.getRecipient();
        SmsTemplate smsTemplate = this.getSmsTemplate(smsTemplateID);
        smsTemplate.setSmsTemplateBodyBng(smsTemplateDTO.getSmsTemplateBodyBng());
        smsTemplate.setSmsTemplateBodyEng(smsTemplateDTO.getSmsTemplateBodyEng());
        smsTemplate.setStatus(smsTemplateDTO.getStatus());
        smsTemplate.setLanguage(LanguageStatus.valueOf(smsTemplateDTO.getLanguage()));
        this.smsTemplateDAO.updateSmsTemplate(smsTemplate, recipients);
        return true;
    }

    public SmsTemplate getSmsTemplate(Long smsTemplateID) {
        return this.smsTemplateDAO.findOne(smsTemplateID);
    }

    public SmsTemplate findSmsTemplate(ActionToRole actionToRole) {
        return this.smsTemplateDAO.findByActionToRole(actionToRole);
    }

    public SmsTemplateDTO convertToSmsTemplateDTO(SmsTemplate smsTemplate) {
        return SmsTemplateDTO.builder()
                .id(smsTemplate.getId())
                .smsTemplateName(smsTemplate.getSmsTemplateName())
                .smsTemplateBodyBng(smsTemplate.getSmsTemplateBodyBng())
                .smsTemplateBodyEng(smsTemplate.getSmsTemplateBodyEng())
                .status(smsTemplate.getStatus())
                .language(String.valueOf(smsTemplate.getLanguage()))
                //.actionToRoleId(smsTemplate.getActionToRole().getId())
                .build();
    }

    public SmsTemplateDTO convertToSmsTemplateDTOWithRecipient(SmsTemplate smsTemplate) {
        List<String> activeSmsRecipients = this.grsRoleToSmsDAO.findBySmsTemplateAndStatus(smsTemplate, true)
                .stream()
                .map(x -> {
                    return x.getGrsRole();
                })
                .collect(Collectors.toList());
        return SmsTemplateDTO.builder()
                .id(smsTemplate.getId())
                .smsTemplateName(smsTemplate.getSmsTemplateName())
                .smsTemplateBodyBng(smsTemplate.getSmsTemplateBodyBng())
                .smsTemplateBodyEng(smsTemplate.getSmsTemplateBodyEng())
                .status(smsTemplate.getStatus())
                .language(String.valueOf(smsTemplate.getLanguage()))
                .recipient(activeSmsRecipients)
                .build();

    }
}
