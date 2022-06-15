package com.grs.core.dao;

import com.grs.api.model.GRSUserType;
import com.grs.api.model.UserInformation;
import com.grs.api.model.UserType;
import com.grs.api.model.request.FeedbackRequestDTO;
import com.grs.api.model.request.GrievanceRequestDTO;
import com.grs.api.model.response.GenericResponse;
import com.grs.api.model.response.grievance.GrievanceDTO;
import com.grs.core.domain.*;
import com.grs.core.domain.grs.CitizenCharter;
import com.grs.core.domain.grs.Complainant;
import com.grs.core.domain.grs.Grievance;
import com.grs.core.domain.grs.GrievanceForwarding;
import com.grs.core.domain.projapoti.Office;
import com.grs.core.repo.grs.GrievanceForwardingRepo;
import com.grs.core.repo.grs.GrievanceRepo;
import com.grs.core.service.ComplainantService;
import com.grs.utils.CacheUtil;
import com.grs.utils.DateTimeConverter;
import com.grs.utils.StringUtil;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Acer on 9/14/2017.
 */
@Slf4j
@Service
public class GrievanceDAO {
    @Autowired
    private GrievanceRepo grievanceRepo;
    @Autowired
    private GrievanceForwardingRepo grievanceForwardingRepo;
    @Autowired
    private CitizenCharterDAO citizenCharterDAO;
    @Autowired
    private ServiceOriginDAO serviceOriginDAO;
    @Autowired
    private OfficeDAO officeDAO;
    @Autowired
    private ComplainantService complainantService;

    public Page<Grievance> findAll(Pageable pageable) {
        return this.grievanceRepo.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Grievance findOne(Long id) {
        return this.grievanceRepo.findOne(id);
    }

    public Grievance save(Grievance grievance) {
        return this.grievanceRepo.save(grievance);
    }

    public void save(List<Grievance> grievances) {
        this.grievanceRepo.save(grievances);
    }

    public Grievance addGrievance(UserInformation userInformation, GrievanceRequestDTO grievanceRequestDTO) {
        boolean isGrsUser = false;
        Boolean offlineGrievanceUploaded = false;
        Long uploaderGroOfficeUnitOrganogramId = null;
        Long userId;
        if (userInformation == null) {
            userId = 0L;
            isGrsUser = true;
        } else {
            isGrsUser = userInformation.getUserType().equals(UserType.COMPLAINANT);
            userId = isGrsUser ? userInformation.getUserId() : userInformation.getOfficeInformation().getEmployeeRecordId();
        }
        boolean isAnonymous = (grievanceRequestDTO.getIsAnonymous() != null && grievanceRequestDTO.getIsAnonymous());
        if(grievanceRequestDTO.getOfflineGrievanceUpload() != null && grievanceRequestDTO.getOfflineGrievanceUpload()){
            offlineGrievanceUploaded = true;
            uploaderGroOfficeUnitOrganogramId = userInformation.getOfficeInformation().getOfficeUnitOrganogramId();
            if(!isAnonymous){
                Complainant complainant = this.complainantService.findComplainantByPhoneNumber(grievanceRequestDTO.getPhoneNumber());
                userId = complainant.getId();
            }
            isGrsUser = true;
        }
        Long officeId = Long.valueOf(grievanceRequestDTO.getOfficeId());
        CitizenCharter citizenCharter = this.citizenCharterDAO.findByOfficeIdAndServiceId(Long.valueOf(grievanceRequestDTO.getOfficeId()), Long.valueOf(grievanceRequestDTO.getServiceId()));
        GrievanceCurrentStatus currentStatus = (officeId == 0 ? GrievanceCurrentStatus.CELL_NEW : GrievanceCurrentStatus.NEW);
        Grievance grievance = Grievance.builder()
                .details(grievanceRequestDTO.getBody())
//                .submissionDate(DateTimeConverter.convertToDate(grievanceRequestDTO.getSubmissionDate()))
                .subject(grievanceRequestDTO.getSubject())
                .officeId(officeId)
                .serviceOrigin(citizenCharter == null ? null : citizenCharter.getServiceOrigin())
                .serviceOriginBeforeForward(citizenCharter == null ? null : citizenCharter.getServiceOrigin())
                .grsUser(isGrsUser)
                .complainantId(isAnonymous ? 0L : userId)
                .grievanceType(grievanceRequestDTO.getServiceType())
                .grievanceCurrentStatus(currentStatus)
                .isAnonymous(isAnonymous)
                .trackingNumber(StringUtil.isValidString(grievanceRequestDTO.getServiceTrackingNumber()) ? grievanceRequestDTO.getServiceTrackingNumber() : this.getTrackingNumber())
                .otherService(grievanceRequestDTO.getServiceOthers())
                .otherServiceBeforeForward(grievanceRequestDTO.getServiceOthers())
                .serviceReceiver(grievanceRequestDTO.getServiceReceiver())
                .serviceReceiverRelation(grievanceRequestDTO.getRelation())
                .isOfflineGrievance(offlineGrievanceUploaded)
                .uploaderOfficeUnitOrganogramId(uploaderGroOfficeUnitOrganogramId)
                .isSelfMotivatedGrievance(grievanceRequestDTO.getIsSelfMotivated() != null && grievanceRequestDTO.getIsSelfMotivated())
                .sourceOfGrievance(grievanceRequestDTO.getSourceOfGrievance())
                .build();

        grievance.setStatus(true);
        return this.save(grievance);
    }

    public Grievance addGrievanceForOthers(UserInformation userInformation, GrievanceRequestDTO grievanceRequestDTO, long userIdFromToken, String sourceOfGrievance) {
        boolean isGrsUser = false;
        Boolean offlineGrievanceUploaded = false;
        Long uploaderGroOfficeUnitOrganogramId = null;
        Long userId;
        if (userInformation == null) {
            userId = 0L;
            isGrsUser = true;
        } else {
            isGrsUser = userInformation.getUserType().equals(UserType.COMPLAINANT);
            userId = isGrsUser ? userInformation.getUserId() : userInformation.getOfficeInformation().getEmployeeRecordId();
        }
        boolean isAnonymous = (grievanceRequestDTO.getIsAnonymous() != null && grievanceRequestDTO.getIsAnonymous());
        if(grievanceRequestDTO.getOfflineGrievanceUpload() != null && grievanceRequestDTO.getOfflineGrievanceUpload() && userInformation != null){
            offlineGrievanceUploaded = true;
            uploaderGroOfficeUnitOrganogramId = userInformation.getOfficeInformation().getOfficeUnitOrganogramId();
            if(!isAnonymous){
//                Complainant complainant = this.complainantService.findComplainantByPhoneNumber(grievanceRequestDTO.getPhoneNumber());
//                userId = complainant.getId();
            }
//            isGrsUser = true;
        }
        Long officeId = Long.valueOf(grievanceRequestDTO.getOfficeId());
        CitizenCharter citizenCharter = this.citizenCharterDAO.findByOfficeIdAndServiceId(Long.valueOf(grievanceRequestDTO.getOfficeId()), Long.valueOf(grievanceRequestDTO.getServiceId()));
        GrievanceCurrentStatus currentStatus = (officeId == 0 ? GrievanceCurrentStatus.CELL_NEW : GrievanceCurrentStatus.NEW);
        Grievance grievance = Grievance.builder()
                .details(grievanceRequestDTO.getBody())
//                .submissionDate(DateTimeConverter.convertToDate(grievanceRequestDTO.getSubmissionDate()))
                .subject(grievanceRequestDTO.getSubject())
                .officeId(officeId)
                .serviceOrigin(citizenCharter == null ? null : citizenCharter.getServiceOrigin())
                .serviceOriginBeforeForward(citizenCharter == null ? null : citizenCharter.getServiceOrigin())
                .grsUser(isGrsUser)
                .complainantId(isAnonymous ? 0L : userId)
                .grievanceType(grievanceRequestDTO.getServiceType())
                .grievanceCurrentStatus(currentStatus)
                .isAnonymous(isAnonymous)
                .trackingNumber(StringUtil.isValidString(grievanceRequestDTO.getServiceTrackingNumber()) ? grievanceRequestDTO.getServiceTrackingNumber() : this.getTrackingNumber())
                .otherService(grievanceRequestDTO.getServiceOthers())
                .otherServiceBeforeForward(grievanceRequestDTO.getServiceOthers())
                .serviceReceiver(grievanceRequestDTO.getServiceReceiver())
                .serviceReceiverRelation(grievanceRequestDTO.getRelation())
                .isOfflineGrievance(grievanceRequestDTO.getOfflineGrievanceUpload())
                .uploaderOfficeUnitOrganogramId(uploaderGroOfficeUnitOrganogramId)
                .isSelfMotivatedGrievance(grievanceRequestDTO.getIsSelfMotivated() != null && grievanceRequestDTO.getIsSelfMotivated())
                .sourceOfGrievance(sourceOfGrievance)
                .build();

        grievance.setStatus(true);
        grievance.setCreatedBy(userIdFromToken);
        return this.save(grievance);
    }

    //add
    @Synchronized public String getTrackingNumber() {
        CacheUtil.updateTrackingNumber();
        Long count = CacheUtil.getTrackingNumber();
        return String.valueOf(count);
    }

    public String getCaseNumber(Long officeId) {
        DateFormat df = new SimpleDateFormat("yyyy");
        Long count = this.grievanceRepo.countByOfficeId(officeId) + 1;
        String caseNumber = df.format(new Date()) + String.valueOf(officeId) + String.format("%05d", count);
        return caseNumber;
    }

    public Page<Grievance> findByOfficeId(Pageable pageable, Long officeId) {
        GrievanceCurrentStatus grievanceCurrentStatus = GrievanceCurrentStatus.NEW;
        return this.grievanceRepo.findByOfficeIdAndGrievanceCurrentStatusNotOrderByCreatedAtAsc(officeId, grievanceCurrentStatus, pageable);
    }

    public Page<Grievance> findByComplainantId(Long userId, Boolean grsUser, Pageable pageable) {
        return this.grievanceRepo.findByComplainantIdAndGrsUserOrderByUpdatedAtDesc(userId, pageable, grsUser);
    }

    public Page<Grievance> findByCreatedByAndSourceOfGrievance(Long userId, Pageable pageable, String sourceOfGrievance) {
        return this.grievanceRepo.findByCreatedByAndSourceOfGrievanceOrderByUpdatedAtDesc(userId, pageable, sourceOfGrievance);
    }

    public List<Grievance> findByCreatedByAndSourceOfGrievance(Long userId, String sourceOfGrievance) {
        return this.grievanceRepo.findByCreatedByAndSourceOfGrievanceOrderByUpdatedAtDesc(userId, sourceOfGrievance);
    }

    public Long getResolvedGrievancesCountByOfficeId(Long officeId) {
        return grievanceRepo.getCountOfResolvedGrievancesByOfficeId(officeId);
    }

    public Long getCountOfUnresolvedGrievancesByOfficeId(Long officeId) {
        return grievanceRepo.getCountOfUnresolvedGrievancesByOfficeId(officeId);
    }

    public Long getCountOfRunningGrievancesByOfficeId(Long officeId) {
        return grievanceRepo.getCountOfRunningGrievancesByOfficeId(officeId);
    }

    public Long getSubmittedGrievancesCountByOffice(Long officeId) {
        return grievanceRepo.countAllByOfficeId(officeId);
    }

    public Page<Grievance> findByTrackingNumber(String trackingNumber, Pageable pageable) {
        return this.grievanceRepo.findByTrackingNumber(trackingNumber, pageable);
    }

    public Grievance findByTrackingNumber(String trackingNumber) {
        return this.grievanceRepo.findByTrackingNumber(trackingNumber);
    }

    @Transactional("transactionManager")
    public Grievance feedbackAgainstGrievance(Grievance grievance, FeedbackRequestDTO feedbackRequestDTO) {
        grievance.setIsRatingGiven(true);
        grievance.setRating(feedbackRequestDTO.getRating());
        grievance.setFeedbackComments(feedbackRequestDTO.getUserComments());
        Grievance gr = grievanceRepo.save(grievance);
        return gr;
    }

    public Grievance feedbackAgainstAppealGrievance(Grievance grievance, FeedbackRequestDTO feedbackRequestDTO) {
        grievance.setIsAppealRatingGiven(true);
        grievance.setAppealRating(feedbackRequestDTO.getRating());
        grievance.setAppealFeedbackComments(feedbackRequestDTO.getUserComments());
        grievance = this.grievanceRepo.save(grievance);
        return grievance;
    }

    public List<Grievance> findByIdIn(List<Long> grievanceIds) {
        return this.grievanceRepo.findByIdIn(grievanceIds);
    }

    public Long countByOfficeIdAndServiceOriginId(Long officeId, Long serviceOriginId) {
        return grievanceRepo.countByOfficeIdAndServiceOriginId(officeId, serviceOriginId);
    }

    public List<Grievance> findByOfficeIdAndStatus(Long officeId){
        return this.grievanceRepo.findByOfficeIdAndStatus(officeId);
    }

    public Grievance findByTrackingNumberAndComplaintId(String trackingNumber, Long complainantid) {
        return this.grievanceRepo.findByTrackingNumberAndComplainantId(trackingNumber, complainantid);
    }

    public List<Grievance> getAllGrievanceOfCell() {
        return this.grievanceRepo.findByCellOffice();

    }
}
