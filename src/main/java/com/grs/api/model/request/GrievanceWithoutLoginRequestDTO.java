package com.grs.api.model.request;

import com.grs.core.domain.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by Acer on 9/19/2017.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GrievanceWithoutLoginRequestDTO {
    private String complainantPhoneNumber = "";
    private String name = "";
    private String email = "";

    private String serviceTrackingNumber;
    private String officeId;
    private String serviceId;
    private String submissionDate;
    private String subject;
    private String body;
    private String relation;
    private String serviceReceiver;
    private String serviceOthers;
    private Boolean isAnonymous = false;
    private ServiceType serviceType;
    private List<FileDTO> files;
    private Boolean offlineGrievanceUpload = false;
    private String PhoneNumber = null;
    private Boolean isSelfMotivated;
    private String SourceOfGrievance;
    private String user;
    private String secret;
    public int submittedThroughApi = 0;
}
