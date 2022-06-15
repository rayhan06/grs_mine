package com.grs.core.service;

import com.grs.api.model.GrievanceForwardingDTO;
import com.grs.api.model.UserInformation;
import com.grs.api.model.request.AddCentralDashboardRecipientDTO;
import com.grs.api.model.response.CentralDashboardRecipientDTO;
import com.grs.api.model.response.EmployeeOrganogramDTO;
import com.grs.api.model.response.NudgeableGrievanceDTO;
import com.grs.api.model.response.RegisterDTO;
import com.grs.api.model.response.dashboard.*;
import com.grs.api.model.response.dashboard.latest.TotalResolvedByMonth;
import com.grs.api.model.response.roles.SingleRoleDTO;
import com.grs.core.dao.CentralDashboardRecipientDAO;
import com.grs.core.dao.DashboardDataDAO;
import com.grs.core.dao.MonthlyReportDAO;
import com.grs.core.dao.TagidDAO;
import com.grs.core.domain.*;
import com.grs.core.domain.grs.*;
import com.grs.core.domain.projapoti.*;
import com.grs.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DashboardService {
    @Autowired
    private OfficeService officeService;
    @Autowired
    private OfficesGroService officesGroService;
    @Autowired
    private GrievanceService grievanceService;
    @Autowired
    private GrievanceForwardingService grievanceForwardingService;
    @Autowired
    private CitizenCharterService citizenCharterService;
    @Autowired
    private DashboardDataDAO dashboardDataDAO;
    @Autowired
    private MessageService messageService;
    @Autowired
    private CentralDashboardRecipientDAO centralDashboardRecipientDAO;
    @Autowired
    private ComplainantService complainantService;
    @Autowired
    private UserService userService;
    @Autowired
    private MonthlyReportDAO monthlyReportDAO;
    @Autowired
    private TagidDAO tagidDAO;

    @Transactional("transactionManager")
    public DashboardData putDashboardDataRecord(GrievanceForwarding grievanceForwarding) {
        List<GrievanceCurrentStatus> forwardingStatusList = getForwardedStatusList();
        Grievance grievance = grievanceForwarding.getGrievance();
        Long officeId;
        GrievanceCurrentStatus currentStatus = grievance.getGrievanceCurrentStatus();
        if(currentStatus.name().contains("APPEAL")) {
            officeId = grievance.getCurrentAppealOfficeId();
        } else if(grievance.getSendToAoOfficeId() != null) {
            officeId = grievance.getSendToAoOfficeId();
        } else {
            officeId = grievance.getOfficeId();
        }
        if (forwardingStatusList.contains(currentStatus)) {
            officeId = grievanceForwarding.getFromOfficeId();
            Boolean isForwardedToAo = currentStatus == GrievanceCurrentStatus.FORWARDED_TO_AO ? true : false;
            saveDashboardData(grievance, grievanceForwarding.getToOfficeId(), GrievanceCurrentStatus.NEW, isForwardedToAo, grievanceForwarding.getCurrentStatus());
        } else if(currentStatus == GrievanceCurrentStatus.APPEAL) {
            return saveDashboardData(grievance, grievanceForwarding.getToOfficeId(), currentStatus, false, grievanceForwarding.getCurrentStatus());
        }
        if(grievanceForwarding.getAction().equals("RETAKE")) {
            DashboardData dashboardData = dashboardDataDAO.findByOfficeIdAndGrievanceId(officeId, grievance.getId());
            dashboardDataDAO.delete(dashboardData);
            return null;
        }
        return saveDashboardData(grievance, officeId, currentStatus, false, grievanceForwarding.getCurrentStatus());
    }

    public DashboardData saveDashboardData(Grievance grievance, Long officeId, GrievanceCurrentStatus currentStatus, Boolean isForwardedToAo, GrievanceCurrentStatus complainantMovementStatus) {
        DashboardData dashboardData = dashboardDataDAO.findByOfficeIdAndGrievanceId(officeId, grievance.getId());
        if (
                complainantMovementStatus.name().equals("NUDGE")
                        || complainantMovementStatus.name().equals("GRO_CHANGED")
        )
        {
            return dashboardData;
        }
        if (isForwardedToAo || currentStatus == GrievanceCurrentStatus.APPEAL || dashboardData == null) {
            String subject = isForwardedToAo ? grievance.getSubject() : (messageService.isCurrentLanguageInEnglish() ? "Others" : "অন্যান্য");
            CitizenCharter citizenCharter = citizenCharterService.findByOfficeAndService(
                    grievance.getOfficeId(),
                    grievance.getServiceOrigin()
            );
            MediumOfSubmission medium = MediumOfSubmission.ONLINE;
            if(currentStatus == GrievanceCurrentStatus.NEW && grievance.getIsOfflineGrievance()) {
                medium = MediumOfSubmission.CONVENTIONAL_METHOD;
            } else if(currentStatus == GrievanceCurrentStatus.NEW && grievance.getIsSelfMotivatedGrievance()){
                medium = MediumOfSubmission.SELF_MOTIVATED_ACCEPTANCE;
            }

            if (
                    complainantMovementStatus.name().contains("APPEAL")
            )
            {
                dashboardData = dashboardDataDAO.findAppealByOfficeIdAndGrievanceId(officeId, grievance.getId());

                if (dashboardData != null) {
                    dashboardData.setOfficeId(officeId);
                    dashboardData.setCitizenCharter(citizenCharter);
                    dashboardData.setServiceId(citizenCharter != null ? citizenCharter.getServiceOrigin().getId() : null);
                    dashboardData.setSubmissionDate(grievance.getSubmissionDate());
                    dashboardData.setClosureDate(getMaximumDateToBeClosed(grievance));
                }
                else {
                    dashboardData = DashboardData.builder()
                            .officeId(officeId)
                            .grievanceId(grievance.getId())
                            .complainantId(grievance.getComplainantId())
                            .subject(subject)
                            .trackingNumber(grievance.getTrackingNumber())
                            .citizenCharter(citizenCharter)
                            .serviceId(citizenCharter != null ? citizenCharter.getServiceOrigin().getId() : null)
                            .submissionDate(grievance.getSubmissionDate())
                            .closureDate(getMaximumDateToBeClosed(grievance))
                            .grievanceType(grievance.getGrievanceType())
                            .forwarded(false)
                            .mediumOfSubmission(medium)
                            .build();
                }

            }
            else
            {
                dashboardData = DashboardData.builder()
                        .officeId(officeId)
                        .grievanceId(grievance.getId())
                        .complainantId(grievance.getComplainantId())
                        .subject(subject)
                        .trackingNumber(grievance.getTrackingNumber())
                        .citizenCharter(citizenCharter)
                        .serviceId(citizenCharter != null ? citizenCharter.getServiceOrigin().getId() : null)
                        .submissionDate(grievance.getSubmissionDate())
                        .closureDate(getMaximumDateToBeClosed(grievance))
                        .grievanceType(grievance.getGrievanceType())
                        .forwarded(false)
                        .mediumOfSubmission(medium)
                        .build();
            }
        }
        if(!StringUtil.isValidString(dashboardData.getCaseNumber())) {
            dashboardData.setCaseNumber(grievance.getCaseNumber());
        }
        if(currentStatus.equals(GrievanceCurrentStatus.APPEAL)) {
            dashboardData.setAppealFromOfficeId(grievance.getOfficeId());
            dashboardData.setAcceptedDate(new Date());
            dashboardData.setMediumOfSubmission(MediumOfSubmission.ONLINE);
        } else if(currentStatus == GrievanceCurrentStatus.ACCEPTED) {
            dashboardData.setAcceptedDate(new Date());
        } else if(getForwardedStatusList().contains(currentStatus)) {
            dashboardData.setForwarded(true);
        } else if(currentStatus.name().startsWith("CLOSED")) {
            dashboardData.setGroDecision(grievance.getGroDecision());
            dashboardData.setGroIdentifiedCause(grievance.getGroIdentifiedCause());
            dashboardData.setGroSuggestion(grievance.getGroSuggestion());
        } else if(currentStatus.name().startsWith("APPEAL_CLOSED")) {
            dashboardData.setAoDecision(grievance.getAppealOfficerDecision());
            dashboardData.setAoIdentifiedCause(grievance.getAppealOfficerIdentifiedCause());
            dashboardData.setAoSuggestion(grievance.getAppealOfficerSuggestion());
        }
        EmployeeOrganogramDTO employeeOrganogramDTO = grievanceService.getSODetail(grievance.getId());
        OfficeUnit officeUnit = null;
        Long officeUnitOrganogramId = employeeOrganogramDTO.getOfficeUnitOrganogramId();
        if(officeUnitOrganogramId != null) {
            officeUnit = officeService.getOfficeUnitOrganogramById(officeUnitOrganogramId).getOfficeUnit();
        }
        dashboardData.setOfficeUnitId(officeUnit != null ? officeUnit.getId() : null);
        dashboardData.setComplaintStatus(currentStatus);
        dashboardData.setClosedDate(getActualClosedDate(currentStatus));
        dashboardData.setUpdatedAt(new Date());
        return dashboardDataDAO.save(dashboardData);
    }

    public List<GrievanceCurrentStatus> getForwardedStatusList() {
        return new ArrayList() {{
            add(GrievanceCurrentStatus.FORWARDED_IN);
            add(GrievanceCurrentStatus.FORWARDED_OUT);
            add(GrievanceCurrentStatus.FORWARDED_TO_AO);
        }};
    }

    public Boolean hasAccessToAoAndSubOfficesDashboard(UserInformation userInformation, Long officeId) {
        return officeService.hasAccessToAoAndSubOfficesDashboard(userInformation, officeId);
    }

    public Date getMaximumDateToBeClosed(Grievance grievance) {
        GrievanceCurrentStatus status = grievance.getGrievanceCurrentStatus();
        if (status.equals(GrievanceCurrentStatus.NEW)) {
            LocalDate maxDateToResolve = LocalDate.now().plusDays(CalendarUtil.getWorkDaysCountAfter(new Date(), (int) Constant.GRIEVANCE_EXPIRATION_TIME));
            return Date.from(maxDateToResolve.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } else if (status.equals(GrievanceCurrentStatus.APPEAL)) {
            LocalDate maxDateToResolve = LocalDate.now().plusDays(CalendarUtil.getWorkDaysCountAfter(new Date(), (int) Constant.APPEAL_EXPIRATION_TIME));
            return Date.from(maxDateToResolve.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } else {
            return null;
        }
    }

    public GeneralDashboardDataDTO constructGeneralDashboardDataDTO(Long total, Long resolved, Long declined, Long running, Long unresolved, boolean isAppealType) {
        GeneralDashboardDataDTO generalDashboardDataDTO = GeneralDashboardDataDTO.builder()
                .total(NameValuePairDTO.builder()
                        .name("প্রাপ্ত অভিযোগ")
                        .value(total)
                        .build())
                .resolved(NameValuePairDTO.builder()
                        .name("নিষ্পন্ন")
                        .value(resolved)
                        .color("#008000")
                        .build())
                .declined(NameValuePairDTO.builder()
                        .name(isAppealType ? "নথিজাত" : "অন্য দপ্তরে প্রেরিত")
                        .value(declined)
                        .color("#8A2BE2")
                        .build())
                .running(NameValuePairDTO.builder()
                        .name("চলমান")
                        .value(running)
                        .color("#EED202")
                        .build())
                .unresolved(NameValuePairDTO.builder()
                        .name("সময় অতিক্রান্ত")
                        .value(unresolved)
                        .color("#ED2939")
                        .build())
                .build();
        return generalDashboardDataDTO;
    }

    public GeneralDashboardDataDTO getGrievanceDataForGRODashboard(Long officeId, boolean includeTotalAndRating) {
        Long monthDiff = 0L;
        Long totalComplaintsCount = null;
        if(includeTotalAndRating) {
            totalComplaintsCount = dashboardDataDAO.countTotalComplaintsByOfficeIdV2(officeId, monthDiff);
        }
        Long resolvedComplaintsCount = dashboardDataDAO.countResolvedComplaintsByOfficeId(officeId, monthDiff);
        Long unresolvedComplaintsCount = dashboardDataDAO.countTimeExpiredComplaintsByOfficeId(officeId);
        Long runningComplaintsCount = dashboardDataDAO.countRunningGrievancesByOfficeId(officeId, monthDiff);
        Long declinedComplaintsCount = dashboardDataDAO.countDeclinedGrievancesByOfficeId(officeId, monthDiff);

        GeneralDashboardDataDTO groDashboardData =  constructGeneralDashboardDataDTO(totalComplaintsCount, resolvedComplaintsCount, declinedComplaintsCount, runningComplaintsCount, unresolvedComplaintsCount, false);
        if(includeTotalAndRating) {
            DashboardRatingDTO ratingDTO = dashboardDataDAO.countAvgRatingOfComplaintsByOfficeId(officeId);
            groDashboardData.setRating(ratingDTO);
        }
        return groDashboardData;
    }

    public GeneralDashboardDataDTO getGrievanceDataForAODashboard(Long officeId, boolean includeTotalAndRating) {
        Long monthDiff = 0L;
        Long totalComplaintsCount = null;
        if(includeTotalAndRating) {
            totalComplaintsCount = dashboardDataDAO.countTotalAppealsByOfficeId(officeId, monthDiff);
        }
        Long resolvedComplaintsCount = dashboardDataDAO.countResolvedAppealsByOfficeId(officeId, monthDiff);
        Long unresolvedComplaintsCount = dashboardDataDAO.countAllTimeExpiredAppealsByOfficeId(officeId);
        Long runningComplaintsCount = dashboardDataDAO.countAllRunningAppealsByOfficeId(officeId, monthDiff);

        GeneralDashboardDataDTO aoDashboardData =  constructGeneralDashboardDataDTO(totalComplaintsCount, resolvedComplaintsCount, null, runningComplaintsCount, unresolvedComplaintsCount, false);
        if(includeTotalAndRating) {
            DashboardRatingDTO ratingDTO = dashboardDataDAO.countAvgRatingOfAppealsByOfficeId(officeId);
            aoDashboardData.setRating(ratingDTO);
        }
        return aoDashboardData;
    }

    public List<NameValuePairDTO> getGrievanceCountByMediumOfSubmission(Long officeId, Long monthDiff) {
        List<NameValuePairDTO> countByMediumOfSubmissionList = new ArrayList();
        Long byConventionalMethodCount = dashboardDataDAO.countComplaintsByOfficeAndMediumOfSubmission(officeId, MediumOfSubmission.CONVENTIONAL_METHOD, monthDiff);
        Long byOnlineCount = dashboardDataDAO.countComplaintsByOfficeAndMediumOfSubmission(officeId, MediumOfSubmission.ONLINE, monthDiff);
        Long fromLastMonthCount = getGrievanceAscertainCountOfPreviousMonthV2(officeId, monthDiff);
        countByMediumOfSubmissionList.add(NameValuePairDTO.builder().name("প্রচলিত পদ্ধতিতে").value(byConventionalMethodCount).build());
        countByMediumOfSubmissionList.add(NameValuePairDTO.builder().name("অনলাইনে").value(byOnlineCount).build());
        countByMediumOfSubmissionList.add(NameValuePairDTO.builder().name("পূর্ববর্তী মাসের জের").value(fromLastMonthCount).build());
        return countByMediumOfSubmissionList;
    }

    public List<NameValuePairDTO> getAppealCountByMediumOfSubmission(Long officeId, Long monthDiff) {
        List<NameValuePairDTO> countByMediumOfSubmissionList = new ArrayList();
        Long byOnlineCount = dashboardDataDAO.countAppealsByOfficeAndMediumOfSubmission(officeId, MediumOfSubmission.ONLINE, monthDiff);
        Long fromLastMonthCount = getAppealAscertainCountOfPreviousMonth(officeId, monthDiff);
        countByMediumOfSubmissionList.add(NameValuePairDTO.builder().name("অনলাইনে").value(byOnlineCount).build());
        countByMediumOfSubmissionList.add(NameValuePairDTO.builder().name("পূর্ববর্তী মাসের জের").value(fromLastMonthCount).build());
        return countByMediumOfSubmissionList;
    }

    public List<GrievanceCountByItemDTO> countGrievanceOfAnOfficeByService(Long officeId) {
        List<GrievanceCountByItemDTO> servicesWithZeroCount = officeService.getGrievanceCountByCitizensCharter(officeId);
        List resultList = dashboardDataDAO.getListOfGrievanceCountByServiceId(officeId);
        List<GrievanceCountByService> grievanceCountByServiceList = new ArrayList();
        resultList.stream().forEach(item -> {
            Object[] objectArray = (Object[]) item;
            Long id = objectArray[0] != null ? ((BigInteger) objectArray[0]).longValue() : null;
            Long count = ((BigInteger) objectArray[1]).longValue();
            grievanceCountByServiceList.add(GrievanceCountByService.builder()
                    .citizenCharterId(id)
                    .count(count)
                    .build());
        });
        List<GrievanceCountByItemDTO> servicesWithGrievanceCount = grievanceCountByServiceList.stream()
                .map(source -> {
                    CitizenCharter citizenCharter = null;
                    if(source.getCitizenCharterId() != null) {
                        citizenCharter = citizenCharterService.findOne(source.getCitizenCharterId());
                    }
                    Long id;
                    String nameBangla, nameEnglish;
                    if(citizenCharter != null) {
                        id = citizenCharter.getId();
                        nameBangla = citizenCharter.getServiceNameBangla();
                        nameEnglish = citizenCharter.getServiceNameEnglish();
                    } else {
                        id = 0L;
                        nameBangla = "অন্যান্য";
                        nameEnglish = "others";
                    }
                    servicesWithZeroCount.removeIf(obj -> obj.getId().equals(id));
                    return GrievanceCountByItemDTO.builder()
                            .id(id)
                            .grievanceCount(source.getCount())
                            .nameBangla(nameBangla)
                            .nameEnglish(nameEnglish)
                            .build();
                }).collect(Collectors.toList());
        return new ArrayList<GrievanceCountByItemDTO>() {{
            addAll(servicesWithGrievanceCount);
            addAll(servicesWithZeroCount);
        }};
    }

    public List<GrievanceCountByItemDTO> countGrievanceOfAnOfficeByOfficeUnit(Long officeId) {
        List<GrievanceCountByItemDTO> officeUnitsWithZeroCount = officeService.getListOfOfficeUnitsByOfficeId(officeId);
        List resultList = dashboardDataDAO.getListOfGrievanceCountByOfficeUnitId(officeId);
        List<GrievanceCountByOfficeUnit> grievanceCountByOfficeUnitList = new ArrayList();
        resultList.stream().forEach(item -> {
            Object[] objectArray = (Object[]) item;
            Long id = objectArray[0] != null ? ((BigInteger) objectArray[0]).longValue() : null;
            Long count = ((BigInteger) objectArray[1]).longValue();
            grievanceCountByOfficeUnitList.add(GrievanceCountByOfficeUnit.builder()
                    .officeUnitId(id)
                    .count(count)
                    .build());
        });
        List<GrievanceCountByItemDTO> officeUnitsWithGrievanceCount = grievanceCountByOfficeUnitList.stream()
                .map(source -> {
                    Long id;
                    String nameBangla, nameEnglish;
                    if (source.getOfficeUnitId() != null) {
                        id = source.getOfficeUnitId();
                        GrievanceCountByItemDTO countByItemDTO = officeUnitsWithZeroCount.stream().filter(x -> Objects.equals(x.getId(), id)).findAny()
                                .orElse(null);
                        nameBangla = countByItemDTO.getNameBangla();
                        nameEnglish = countByItemDTO.getNameEnglish();
                    } else {
                        id = 0L;
                        nameBangla = "অন্যান্য";
                        nameEnglish = "Others";
                    }
                    officeUnitsWithZeroCount.removeIf(obj -> obj.getId().equals(id));
                    return GrievanceCountByItemDTO.builder()
                            .id(id)
                            .grievanceCount(source.getCount())
                            .nameBangla(nameBangla)
                            .nameEnglish(nameEnglish)
                            .build();
                })
                .collect(Collectors.toList());

        return new ArrayList<GrievanceCountByItemDTO>() {{
            addAll(officeUnitsWithGrievanceCount);
            addAll(officeUnitsWithZeroCount);
        }};
    }

    public Date getActualClosedDate(GrievanceCurrentStatus currentStatus) {
        List<GrievanceCurrentStatus> resolvableStatusList = new ArrayList<GrievanceCurrentStatus>() {{
            add(GrievanceCurrentStatus.CLOSED_ACCUSATION_INCORRECT);
            add(GrievanceCurrentStatus.CLOSED_ACCUSATION_PROVED);
            add(GrievanceCurrentStatus.CLOSED_ANSWER_OK);
            add(GrievanceCurrentStatus.CLOSED_INSTRUCTION_EXECUTED);
            add(GrievanceCurrentStatus.CLOSED_SERVICE_GIVEN);
            add(GrievanceCurrentStatus.CLOSED_OTHERS);
            add(GrievanceCurrentStatus.REJECTED);

            add(GrievanceCurrentStatus.APPEAL_CLOSED_ACCUSATION_INCORRECT);
            add(GrievanceCurrentStatus.APPEAL_CLOSED_OTHERS);
            add(GrievanceCurrentStatus.APPEAL_CLOSED_ACCUSATION_PROVED);
            add(GrievanceCurrentStatus.APPEAL_CLOSED_ANSWER_OK);
            add(GrievanceCurrentStatus.APPEAL_CLOSED_INSTRUCTION_EXECUTED);
            add(GrievanceCurrentStatus.APPEAL_CLOSED_SERVICE_GIVEN);
        }};
        if (resolvableStatusList.contains(currentStatus)) {
            return new Date();
        }
        return null;

    }

    public ResolutionTypeInfoDTO getResolutionTypeInfo(Long officeId) {
        return ResolutionTypeInfoDTO.builder()
                .acceptedGrievanceCount(dashboardDataDAO.countAcceptedGrievancesByOfficeIdAndMonthDiff(officeId))
                .trueGrievanceCount(dashboardDataDAO.countResolvedGrievancesByOfficeIdAndIsReal(officeId))
                .fakeGrievanceCount(dashboardDataDAO.countResolvedGrievancesByOfficeIdAndIsNotReal(officeId))
                .departmentalRecommendationCount(0L)
                .build();
    }

    public UnacceptedGrievancesCountDTO getUnacceptedGrievancesInfo(Long officeId) {
        return UnacceptedGrievancesCountDTO.builder()
                .sendToAOCount(dashboardDataDAO.countComplaintsByOfficeIdAndStatus(officeId, GrievanceCurrentStatus.FORWARDED_TO_AO))
                .sendToOtherOfficesCount(dashboardDataDAO.countComplaintsByOfficeIdAndStatus(officeId, GrievanceCurrentStatus.FORWARDED_OUT))
                .sendToChildOfficesCount(dashboardDataDAO.countComplaintsByOfficeIdAndStatus(officeId, GrievanceCurrentStatus.FORWARDED_IN))
                .rejectedGrievanceCount(dashboardDataDAO.countComplaintsByOfficeIdAndStatus(officeId, GrievanceCurrentStatus.REJECTED))
                .build();
    }

    public List<MonthlyGrievanceResolutionDTO> getCurrentMonthResolutionsAsList(List<DashboardData> dashboardDataList) {
        List<MonthlyGrievanceResolutionDTO> currentMonthResolutions = new ArrayList<>();
        Boolean isEnglish = messageService.getCurrentLanguageCode().equals("en");
        dashboardDataList.forEach(dashboardData -> {
            Grievance grievance = grievanceService.findGrievanceById(dashboardData.getGrievanceId());
            CitizenCharter citizenCharter = dashboardData.getCitizenCharter();
            String serviceName;
            if(citizenCharter != null) {
                serviceName = isEnglish ? citizenCharter.getServiceNameEnglish() : citizenCharter.getServiceNameBangla();
            } else {
                serviceName = isEnglish ? "others" : "অন্যান্য";
            }
            if (grievance != null && grievance.getServiceOrigin() == null) {
                serviceName = grievance.getOtherService();
            }
            MonthlyGrievanceResolutionDTO resolution = MonthlyGrievanceResolutionDTO.builder()
                    .id(dashboardData.getId())
                    .subject(dashboardData.getSubject())
                    .serviceName(serviceName)
                    .closedDate(dashboardData.getClosedDate())
                    .groIdentifiedCause(dashboardData.getGroIdentifiedCause())
                    .groDecision(dashboardData.getGroDecision())
                    .groSuggestion(dashboardData.getGroSuggestion())
                    .aoIdentifiedCause(dashboardData.getAoIdentifiedCause())
                    .aoDecision(dashboardData.getAoDecision())
                    .aoSuggestion(dashboardData.getAoSuggestion())
                    .build();
            currentMonthResolutions.add(resolution);
        });
        return currentMonthResolutions;
    }

    public List<MonthlyGrievanceResolutionDTO> getResolutionsInCurrentMonth(Long officeId, Long monthDiff) {
        List<DashboardData> dashboardDataList = dashboardDataDAO.getResolvedGrievancesOfCurrentMonthByOfficeId(officeId, monthDiff);
        return getCurrentMonthResolutionsAsList(dashboardDataList);
    }

    public List<MonthlyGrievanceResolutionDTO> getAppealResolutionsInCurrentMonth(Long officeId, Long monthDiff) {
        List<DashboardData> dashboardDataList = dashboardDataDAO.getResolvedAppealsOfCurrentMonthByOfficeId(officeId, monthDiff);
        return getCurrentMonthResolutionsAsList(dashboardDataList);
    }

    public List<ExpiredGrievanceInfoDTO> getExpiredGrievancesInformationAsList(List<DashboardData> dashboardDataList, Boolean isAppeal) {
        List<ExpiredGrievanceInfoDTO> expiredGrievanceInfoList = new ArrayList<>();
        Boolean isEnglish = messageService.getCurrentLanguageCode().equals("en");
        dashboardDataList.forEach(dashboardData -> {
            Grievance grievance = grievanceService.findGrievanceById(dashboardData.getGrievanceId());
            CitizenCharter citizenCharter = dashboardData.getCitizenCharter();
            String serviceName;
            if(citizenCharter != null) {
                serviceName = isEnglish ? citizenCharter.getServiceNameEnglish() : citizenCharter.getServiceNameBangla();
            } else {
                serviceName = isEnglish ? "others" : "অন্যান্য";
            }
            if (grievance != null && grievance.getServiceOrigin() == null) {
                serviceName = grievance.getOtherService();
            }

            ExpiredGrievanceInfoDTO resolution = ExpiredGrievanceInfoDTO.builder()
                    .id(dashboardData.getId())
                    .subject(dashboardData.getSubject())
                    .serviceName(serviceName)
                    .closureDate(getClosureDateFromCreatedDate(dashboardData.getCreatedAt(), isAppeal))
                    .currentLocationList(getGrievanceCurrentLocation(grievance))
                    .build();
            expiredGrievanceInfoList.add(resolution);
        });
        return expiredGrievanceInfoList;
    }

    public Date getClosureDateFromCreatedDate(Date createdDate, Boolean isAppeal) {
        Long additionalDays = isAppeal ? Constant.APPEAL_EXPIRATION_TIME  : Constant.GRIEVANCE_EXPIRATION_TIME;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(createdDate);
        calendar.add(Calendar.DATE, Math.toIntExact(CalendarUtil.getWorkDaysCountAfter(createdDate, Math.toIntExact(additionalDays))));
        return calendar.getTime();
    }

    public List<GrievanceCurrentLocationDTO> getGrievanceCurrentLocation(Grievance grievance) {
        Boolean isEnglish = messageService.isCurrentLanguageInEnglish();
        List<GrievanceForwarding> grievanceForwardingList = grievanceForwardingService.findByGrievanceAndIsCurrent(grievance, true);
        List<GrievanceCurrentLocationDTO> grievanceCurrentLocationList = new ArrayList();
        grievanceForwardingList.forEach(grievanceForwarding -> {
            Office office = officeService.findOne(grievanceForwarding.getToOfficeId());
            OfficeUnit officeUnit = officeService.getOfficeUnitById(grievanceForwarding.getToOfficeUnitId());
            GrievanceCurrentLocationDTO currentLocationDTO = GrievanceCurrentLocationDTO.builder()
                    .officeName(isEnglish ? office.getNameEnglish() : office.getNameBangla())
                    .officeUnitName(officeUnit == null ? "" : (isEnglish ? officeUnit.getUnitNameEnglish() : officeUnit.getUnitNameBangla()))
                    .waitingFrom(grievanceForwarding.getCreatedAt())
                    .build();
            grievanceCurrentLocationList.add(currentLocationDTO);
        });
        return grievanceCurrentLocationList;
    }

    public List<NudgeableGrievanceDTO> getTimeExpiredGrievanceDTOList(Long officeId) {
        List<DashboardData> dataList = dashboardDataDAO.getTimeExpiredGrievancesByOfficeId(officeId);
        return convertToNudgeableGrievanceList(dataList);
    }

    public List<NudgeableGrievanceDTO> getTimeExpiredAppealDTOList(Long officeId) {
        List<DashboardData> dataList = dashboardDataDAO.getTimeExpiredAppealsByOfficeId(officeId);
        return convertToNudgeableGrievanceList(dataList);
    }

    public List<NudgeableGrievanceDTO> convertToNudgeableGrievanceList(List<DashboardData> dashboardDataList) {
        List<NudgeableGrievanceDTO> timeExpiredGrievances = dashboardDataList.stream()
                .map(dashboardData -> {
                    Grievance grievance = grievanceService.findGrievanceById(dashboardData.getGrievanceId());
                    return NudgeableGrievanceDTO.builder()
                            .grievance(grievanceService.convertToGrievanceDTO(grievance))
                            .grievanceCurrentLocationList(getGrievanceCurrentLocation(grievance))
                            .build();
                }).collect(Collectors.toList());
        return timeExpiredGrievances;
    }

    public List<ExpiredGrievanceInfoDTO> getTimeExpiredAppealsList(Long officeId) {
        List<DashboardData> dashboardDataList = dashboardDataDAO.getTimeExpiredAppealsByOfficeId(officeId);
        return getExpiredGrievancesInformationAsList(dashboardDataList, true);
    }

    public List<AppealCountByOfficeDTO> getCountOfAppealsBySourceOffices(Long officeId) {
        List<AppealCountByOfficeDTO> resultList = new ArrayList();
        List countObjectList = dashboardDataDAO.getCountOfAppealsBySourceOffices(officeId);
        List<Long> idList = new ArrayList();
        List<TotalAndResolvedCountDTO> grievanceCounts = new ArrayList();
        if(countObjectList.size() > 0) {
            for(int i = 0; i < countObjectList.size(); i++) {
                Object[] objectArray = (Object[]) countObjectList.get(i);
                Long id = ((BigInteger) objectArray[0]).longValue();
                idList.add(id);
                TotalAndResolvedCountDTO totalAndResolvedCountDTO = TotalAndResolvedCountDTO.builder()
                        .officeId(id)
                        .totalCount(((BigInteger) objectArray[1]).longValue())
                        .build();
                grievanceCounts.add(totalAndResolvedCountDTO);
            }
            List<Office> offices = officeService.findByOfficeIdInList(idList);
            offices.stream().forEach(office -> {
                TotalAndResolvedCountDTO grievanceCount = grievanceCounts.stream().filter(obj -> obj.getOfficeId().equals(office.getId())).findFirst().orElse(null);
                AppealCountByOfficeDTO countByOffice =  AppealCountByOfficeDTO.builder()
                        .id(office.getId())
                        .nameBangla(office.getNameBangla())
                        .nameEnglish(office.getNameEnglish())
                        .grievanceCount(grievanceCount != null ? grievanceCount.getTotalCount() : 0L)
                        .build();
                resultList.add(countByOffice);
            });
        }
        return resultList;
    }

    public String getYearAndMonthWithMonthDiff(Integer monthDiff) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, monthDiff);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(calendar.getTime());
    }

    public MonthAndTypeWiseCountDTO formatMonthAndTypeWiseCountForGrievanceAndAppeal(List countObjectList, String year, String month) {
        MonthAndTypeWiseCountDTO monthAndTypeWiseCountDTO = MonthAndTypeWiseCountDTO.builder()
                .year(year)
                .month(month)
                .build();
        for(int i = 0; i < countObjectList.size(); i++) {
            Object[] objectArray = (Object[]) countObjectList.get(i);
            String type = (String) objectArray[0];
            TotalAndResolvedCountDTO totalAndResolvedCountDTO = TotalAndResolvedCountDTO.builder()
                    .totalCount(((BigInteger) objectArray[1]).longValue())
                    .resolvedCount(((BigInteger) objectArray[2]).longValue())
                    .build();
            if(type.equals("STAFF")) {
                monthAndTypeWiseCountDTO.setStaffCounts(totalAndResolvedCountDTO);
            } else if (type.equals("DAPTORIK")) {
                monthAndTypeWiseCountDTO.setDaptorikCounts(totalAndResolvedCountDTO);
            } else {
                monthAndTypeWiseCountDTO.setNagorikCounts(totalAndResolvedCountDTO);
            }
        }
        return monthAndTypeWiseCountDTO;
    }

    public MonthAndTypeWiseCountDTO getMonthAndTypeWiseCountOfGrievanceByOfficeIdWithMonthDiff(Long officeId, Integer monthDiff) {
        String yearAndMonth = getYearAndMonthWithMonthDiff(monthDiff);
        String year = yearAndMonth.substring(0,4);
        String month = yearAndMonth.substring(5,7);
        List countObjectList = dashboardDataDAO.getTotalAndResolvedGrievanceCountWithTypeByMonthAndYear(officeId, monthDiff);
        return formatMonthAndTypeWiseCountForGrievanceAndAppeal(countObjectList, year, month);
    }

    public MonthAndTypeWiseCountDTO getMonthAndTypeWiseCountOfAppealByOfficeIdWithMonthDiff(Long officeId, Integer monthDiff) {
        String yearAndMonth = getYearAndMonthWithMonthDiff(monthDiff);
        String year = yearAndMonth.substring(0,4);
        String month = yearAndMonth.substring(5,7);
        List countObjectList = dashboardDataDAO.getTotalAndResolvedAppealCountWithTypeByMonthAndYear(officeId, monthDiff);
        return formatMonthAndTypeWiseCountForGrievanceAndAppeal(countObjectList, year, month);
    }

    public Long getGrievanceAscertainCountOfPreviousMonth(Long officeId, Long monthDiff) {
        return dashboardDataDAO.getGrievanceAscertainCountOfPreviousMonth(officeId, monthDiff);
    }

    public Long getGrievanceAscertainCountOfPreviousMonthV2(Long officeId, Long monthDiff) {
        return dashboardDataDAO.getGrievanceAscertainCountOfPreviousMonthV2(officeId, monthDiff);
    }

    public List<Long> getComplaintIdsContainRatingInCurrentMonth(Long officeId, Boolean isAppeal) {
        List<BigInteger> complaintsIdList;
        if(isAppeal) {
            complaintsIdList = dashboardDataDAO.getIdsOfAppealsContainRatingInCurrentMonth(officeId);
        } else {
            complaintsIdList = dashboardDataDAO.getIdsOfGrievancesContainRatingInCurrentMonth(officeId);
        }
        return complaintsIdList.stream()
                .map(id -> id.longValue())
                .collect(Collectors.toList());
    }

    public Long getMonthlyComplaintsCountByOfficeIdAndMediumOfSubmission(Long officeId, MediumOfSubmission mediumOfSubmission, Long monthDiff) {
        return dashboardDataDAO.countComplaintsByOfficeAndMediumOfSubmission(officeId, mediumOfSubmission, monthDiff);
    }

    public Long getMonthlyAppealsCountByOfficeIdAndMediumOfSubmission(Long officeId, MediumOfSubmission mediumOfSubmission, Long monthDiff) {
        return dashboardDataDAO.countAppealsByOfficeAndMediumOfSubmission(officeId, mediumOfSubmission, monthDiff);
    }

    public Long getAppealAscertainCountOfPreviousMonth(Long officeId, Long monthDiff) {
        return dashboardDataDAO.getAppealsAscertainCountOfPreviousMonth(officeId, monthDiff);
    }

    public Long getMonthlyAppealsCountByOfficeIdAndMediumOfSubmissionV2(Long officeId, MediumOfSubmission mediumOfSubmission, Long monthDiff) {
        return dashboardDataDAO.countAppealsByOfficeAndMediumOfSubmissionV2(officeId, mediumOfSubmission, monthDiff);
    }

    public Long getAppealAscertainCountOfPreviousMonthV2(Long officeId, Long monthDiff) {
        return dashboardDataDAO.getAppealsAscertainCountOfPreviousMonthV2(officeId, monthDiff);
    }

    public List<ChildOfficesDashboardNavigatorDTO> getGrsEnabledOfficesFromOfficeList(List<Office> offices) {
        List<Long> officeIdList = offices.stream()
                .map(Office::getId)
                .collect(Collectors.toList());
        List<Long> grsEnabledOfficeIdList = officesGroService.getGRSEnabledOfficeIdFromOfficeIdList(officeIdList);
        List<ChildOfficesDashboardNavigatorDTO> childOfficesNameIdList = new ArrayList();
        if(offices.size() > 0) {
            Boolean isEnglish = messageService.isCurrentLanguageInEnglish();
            offices.stream().forEach(office -> {
                Long id = office.getId();
                if(grsEnabledOfficeIdList.contains(id)) {
                    ChildOfficesDashboardNavigatorDTO nameIdDTO = ChildOfficesDashboardNavigatorDTO.builder()
                            .id(id)
                            .name(isEnglish ? office.getNameEnglish() : office.getNameBangla())
                            .enabled(true)
                            .build();
                    childOfficesNameIdList.add(nameIdDTO);
                }
            });
        }
        return childOfficesNameIdList;
    }

    public List<ChildOfficesDashboardNavigatorDTO> getListOfChildOffices(Long officeId) {
        if (officeService.getChildCountByParentOfficeId(officeId) == 0 || officeService.isUpazilaLevelOffice(officeId)) {
            return null;
        }
        List<Office> offices = officeService.getOfficesByParentOfficeId(officeId);
        return getGrsEnabledOfficesFromOfficeList(offices);
    }

    public GeneralDashboardDataDTO getSubOfficesAggregatedDataForGrievances(Long officeId) {
        if(officeService.getChildCountByParentOfficeId(officeId) == 0 || officeService.isUpazilaLevelOffice(officeId)) {
            return constructGeneralDashboardDataDTO(0L, 0L, 0L, 0L, 0L, false);
        } else {
            Long monthDiff = 0L;
            Long totalCount = 0L;
            Long resolvedCount = 0L;
            Long declinedCount = 0L;
            Long runningCount = 0L;
            Long unresolvedCount = 0L;
            List<Office> offices = officeService.getOfficesByParentOfficeId(officeId);
            List<Long> officeIdList = offices.stream()
                    .map(Office::getId)
                    .collect(Collectors.toList());
            List<Long> grsEnabledChildOfficeIdList = officesGroService.getGRSEnabledOfficeIdFromOfficeIdList(officeIdList);
            for(Long id: grsEnabledChildOfficeIdList) {
                GeneralDashboardDataDTO subAggregatedData = getSubOfficesAggregatedDataForGrievances(id);
                totalCount += (dashboardDataDAO.countTotalComplaintsByOfficeIdV2(id, monthDiff) + subAggregatedData.getTotal().getValue());
                resolvedCount += (dashboardDataDAO.countResolvedComplaintsByOfficeId(id, monthDiff) + subAggregatedData.getResolved().getValue());
                declinedCount += (dashboardDataDAO.countDeclinedGrievancesByOfficeId(id, monthDiff) + subAggregatedData.getDeclined().getValue());
                runningCount += (dashboardDataDAO.countRunningGrievancesByOfficeId(id, monthDiff) + subAggregatedData.getRunning().getValue());
                unresolvedCount += (dashboardDataDAO.countTimeExpiredComplaintsByOfficeId(id) + subAggregatedData.getUnresolved().getValue());
            }
            return constructGeneralDashboardDataDTO(totalCount, resolvedCount, declinedCount, runningCount, unresolvedCount, false);
        }
    }

    public GeneralDashboardDataDTO getSubOfficesAggregatedDataForAppeals(Long officeId) {
        if(officeService.getChildCountByParentOfficeId(officeId) > 0 && !officeService.isZilaLevelOffice(officeId)) {
            Long monthDiff = 0L;
            Long totalCount = 0L;
            Long resolvedCount = 0L;
            Long declinedCount = 0L;
            Long runningCount = 0L;
            Long unresolvedCount = 0L;
            List<Office> offices = officeService.getOfficesByParentOfficeId(officeId);
            List<Long> officeIdList = offices.stream()
                    .map(Office::getId)
                    .collect(Collectors.toList());
            List<Long> grsEnabledChildOfficeIdList = officesGroService.getGRSEnabledOfficeIdFromOfficeIdList(officeIdList);
            for(Long id: grsEnabledChildOfficeIdList) {
                GeneralDashboardDataDTO subAggregatedData = getSubOfficesAggregatedDataForAppeals(id);
                totalCount += (dashboardDataDAO.countTotalAppealsByOfficeId(id, monthDiff) + subAggregatedData.getTotal().getValue());
                resolvedCount += (dashboardDataDAO.countResolvedAppealsByOfficeId(id, monthDiff) + subAggregatedData.getResolved().getValue());
                declinedCount += (dashboardDataDAO.countDeclinedAppealsByOfficeId(id) + subAggregatedData.getDeclined().getValue());
                runningCount += (dashboardDataDAO.countAllRunningAppealsByOfficeId(id, monthDiff) + subAggregatedData.getRunning().getValue());
                unresolvedCount += (dashboardDataDAO.countAllTimeExpiredAppealsByOfficeId(id) + subAggregatedData.getUnresolved().getValue());
            }
            return constructGeneralDashboardDataDTO(totalCount, resolvedCount, declinedCount, runningCount, unresolvedCount, true);
        } else {
            return constructGeneralDashboardDataDTO(0L, 0L, 0L, 0L, 0L, true);
        }
    }

    public void getFeedbackForDashboardData(Grievance grievance) {
        DashboardData dashboardData = dashboardDataDAO.findByOfficeIdAndGrievanceId(grievance.getOfficeId(), grievance.getId());
        dashboardData.setRating(grievance.getRating());
        this.dashboardDataDAO.save(dashboardData);
    }

    public void getAppealFeedbackForDashboardData(Grievance grievance) {
        GrievanceForwardingDTO grievanceForwardingDTO = this.grievanceForwardingService.getLatestForwardingEntry(grievance.getId());
        DashboardData dashboardData = dashboardDataDAO.findTopByOfficeIdAndGrievanceId(grievanceForwardingDTO.getToOfficeId(), grievance.getId());
        dashboardData.setRating(grievance.getAppealRating());
        this.dashboardDataDAO.save(dashboardData);
    }

    public List<ItemIdNameCountDTO> getCitizenCharterServicesByComplaintFrequency() {
        Object[] objects = dashboardDataDAO.getCitizenCharterServicesByComplaintFrequency();
        List<ItemIdNameCountDTO> services = new ArrayList();
        for(Object o: objects) {
            Object[] objectArray = (Object[]) o;
            if(objectArray != null) {
                Long serviceId = ((BigInteger) objectArray[0]).longValue();
                String serviceName = (String) objectArray[1];
                Long count = objectArray[2] != null ? ((BigInteger) objectArray[2]).longValue() : 0L;
                services.add(ItemIdNameCountDTO.builder()
                        .id(serviceId)
                        .name(serviceName)
                        .grievanceCount(count)
                        .build());
            }
        }
        return services;
    }

    public List<ItemIdNameCountDTO> getServicesCountWithOfficeNameByServiceId(Long serviceId) {
        Object[] objects = dashboardDataDAO.getServiceCountWithOfficeNameByServiceId(0L, serviceId);
        List<ItemIdNameCountDTO> serviceCountByOffices = new ArrayList();
        for(Object o: objects) {
            Object[] objectArray = (Object[]) o;
            if(objectArray != null) {
                Long officeId = ((BigInteger) objectArray[0]).longValue();
                Long count = objectArray[1] != null ? ((BigInteger) objectArray[1]).longValue() : 0L;
                serviceCountByOffices.add(ItemIdNameCountDTO.builder()
                        .id(officeId)
                        .grievanceCount(count)
                        .build());
            }
        }
        List<Long> officeIds = serviceCountByOffices.stream()
                .map(ItemIdNameCountDTO::getId)
                .collect(Collectors.toList());
        List<Office> offices = officeService.findByOfficeIdInList(officeIds);
        for(ItemIdNameCountDTO item: serviceCountByOffices) {
            Office office = offices.stream()
                    .filter(o -> o.getId().equals(item.getId()))
                    .findFirst()
                    .orElse(null);
            item.setName(office.getNameBangla());
        }
        return serviceCountByOffices;
    }

    public List<TotalAndResolvedCountDTO> getTotalSubmittedAndResolvedCountsOfMinistries() {
        List<ChildOfficesDashboardNavigatorDTO> ministryLevelOffices = getGrsEnabledMinistryLevelOffices();
        List<Long> officeIds = ministryLevelOffices.stream()
                .map(ChildOfficesDashboardNavigatorDTO::getId)
                .collect(Collectors.toList());
        Object[] objects = dashboardDataDAO.getTotalSubmittedAndResolvedCountByOfficeIdInList(officeIds);
        List<TotalAndResolvedCountDTO> listOfOfficesWithCounts = new ArrayList();
        ministryLevelOffices.forEach(ministry -> {
            listOfOfficesWithCounts.add(
                    TotalAndResolvedCountDTO.builder()
                            .officeId(ministry.getId())
                            .officeName(ministry.getName())
                            .resolvedCount(0L)
                            .totalCount(0L)
                            .rate(0D)
                            .build()
            );
        });
        for(Object o: objects) {
            Object[] objectArray = (Object[]) o;
            if(objectArray != null) {
                Long officeId = ((BigInteger) objectArray[0]).longValue();
                Long total = objectArray[1] != null ? ((BigInteger) objectArray[1]).longValue() : 0L;
                Long resolved = objectArray[2] != null ? ((BigInteger) objectArray[2]).longValue() : 0L;
                Long expired = objectArray[3] != null ? ((BigInteger) objectArray[3]).longValue() : 0L;
                TotalAndResolvedCountDTO officeCountDTO = listOfOfficesWithCounts.stream()
                        .filter(m -> m.getOfficeId().equals(officeId))
                        .findFirst()
                        .orElse(null);
                officeCountDTO.setResolvedCount(resolved);
                officeCountDTO.setTotalCount(total);
                officeCountDTO.setExpiredCount(expired);
                if(total > 0) {
                    Double rate = (resolved * 100.00D) / total;
                    officeCountDTO.setRate((Double) (Math.round(rate * 100.0) / 100.0));
                }
            }
        }
        Collections.sort(listOfOfficesWithCounts, (o1, o2) -> {
            int rateComparisonValue = o2.getRate().compareTo(o1.getRate());
            if(rateComparisonValue == 0) {
                return o2.getTotalCount().compareTo(o1.getTotalCount());
            }
            return rateComparisonValue;
        });
        return listOfOfficesWithCounts;
    }

    public List<ChildOfficesDashboardNavigatorDTO> getGrsEnabledMinistryLevelOffices() {
        List<OfficeLayer> officeLayerList = officeService.getOfficeLayersByLayerLevel(1);
        List<Office> offices = officeService.getOfficesByOfficeLayer(officeLayerList,false);
        return getGrsEnabledOfficesFromOfficeList(offices);
    }

    public CentralDashboardDataDTO getCentralDashboardData() {
        return CentralDashboardDataDTO.builder()
                .total(dashboardDataDAO.countTotalGrievances(0L))
                .resolved(dashboardDataDAO.countResolvedGrievances(0L))
                .ascertain(dashboardDataDAO.countGrievanceAscertainFromLastMonth(0L))
                .timeExpiredComplaints(dashboardDataDAO.countTImeExpiredComplaints())
                .totalAppeal(dashboardDataDAO.countTotalAppeals(0L))
                .resolvedAppeal(dashboardDataDAO.countResolvedAppeals(0L))
                .timeExpiredAppeal(dashboardDataDAO.countTimeExpiredAppeals())
                .grievanceListDTO(getCitizenCharterServicesByComplaintFrequency())
                .build();
    }

    public List<CentralDashboardRecipientDTO> getAllCentralDashboardRecipients() {
        List<CentralDashboardRecipient> recipientList = centralDashboardRecipientDAO.findAll();
        return recipientList.stream()
                .map(this::convertToCentralDashboardRecipientDTO)
                .collect(Collectors.toList());
    }

    public CentralDashboardRecipientDTO convertToCentralDashboardRecipientDTO(CentralDashboardRecipient recipient) {
        Long officeId = recipient.getOfficeId();
        Long officeUnitOrganogramId = recipient.getOfficeUnitOrganogramId();
        EmployeeOffice employeeOffice = officeService.findEmployeeOfficeByOfficeAndOfficeUnitOrganogramAndStatus(officeId, officeUnitOrganogramId, true);
        if(employeeOffice == null){
            return CentralDashboardRecipientDTO.builder()
                    .id(recipient.getId())
                    .nameBangla("তালিকাভুক্ত ব্যক্তিকে পাওয়া যাচ্ছেনা")
                    .nameEnglish("")
                    .designation("")
                    .officeId(0L)
                    .officeNameBangla("")
                    .officeNameEnglish("")
                    .officeUnitId(0L)
                    .officeUnitNameBangla("")
                    .officeUnitNameEnglish("")
                    .phoneNumber("")
                    .email("")
                    .status(recipient.getStatus())
                    .build();
        }
        SingleRoleDTO role = officeService.findSingleRole(officeId, officeUnitOrganogramId);
        EmployeeRecord employeeRecord = employeeOffice.getEmployeeRecord();
        return CentralDashboardRecipientDTO.builder()
                .id(recipient.getId())
                .nameBangla(employeeRecord.getNameBangla())
                .nameEnglish(employeeRecord.getNameEnglish())
                .designation(role.getDesignation())
                .officeId(role.getOfficeId())
                .officeNameBangla(role.getOfficeNameBangla())
                .officeNameEnglish(role.getOfficeNameEnglish())
                .officeUnitId(role.getOfficeUnitId())
                .officeUnitNameBangla(role.getOfficeUnitNameBangla())
                .officeUnitNameEnglish(role.getOfficeUnitNameEnglish())
                .phoneNumber(role.getPhone())
                .email(role.getEmail())
                .status(recipient.getStatus())
                .build();
    }

    public CentralDashboardRecipientDTO addNewCentralDashboardRecipients(AddCentralDashboardRecipientDTO dashboardRecipientDTO) {
        Long officeId = dashboardRecipientDTO.getOfficeId();
        Long officeUnitOrganogramId = dashboardRecipientDTO.getOfficeUnitOrganogramId();
        CentralDashboardRecipient recipient = centralDashboardRecipientDAO.findByOfficeIdAndOfficeUnitOrganogramId(officeId, officeUnitOrganogramId);
        if(recipient != null) {
            return null;
        }
        recipient = CentralDashboardRecipient.builder()
                .officeId(officeId)
                .officeUnitOrganogramId(officeUnitOrganogramId)
                .status(true)
                .build();
        centralDashboardRecipientDAO.save(recipient);
        CentralDashboardRecipientDTO recipientDTO = convertToCentralDashboardRecipientDTO(recipient);
        return recipientDTO;
    }

    public Boolean changeCentralDashboardRecipientStatus(Long id, Boolean status) {
        try {
            CentralDashboardRecipient recipient = centralDashboardRecipientDAO.findOne(id);
            recipient.setStatus(status);
            centralDashboardRecipientDAO.save(recipient);
            return true;
        } catch (NullPointerException npe) {
            return false;
        }
    }

    public Boolean deleteCentralDashboardRecipient(Long id) {
        try {
            centralDashboardRecipientDAO.delete(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, String> getComplainantInfoAndServiceName(DashboardData dashboardData) {
        String email = "", phoneNumber = "", name = "", serviceName = "";
        Map<String, String> map = new HashMap();
        if (dashboardData.getGrievanceType().equals(ServiceType.NAGORIK)) {
            if(dashboardData.getComplainantId() > 0L) {
                Complainant complainant = complainantService.findOne(dashboardData.getComplainantId());
                if (complainant != null) {
                    email = complainant.getEmail();
                    phoneNumber = complainant.getPhoneNumber();
                    name = complainant.getName();
                }
            }
        } else {
            EmployeeRecord employeeRecord = this.officeService.findEmployeeRecordById(dashboardData.getComplainantId());
            email = employeeRecord.getPersonalEmail();
            phoneNumber = employeeRecord.getPersonalMobile();
            name = employeeRecord.getNameBangla();
        }
        if(dashboardData.getServiceId() != null) {
            ServiceOrigin serviceOrigin = officeService.getServiceOrigin(dashboardData.getServiceId());
            serviceName = (serviceOrigin != null ? serviceOrigin.getServiceNameBangla() : "");
        } else {
            Grievance grievance = grievanceService.findGrievanceById(dashboardData.getGrievanceId());
            serviceName = (grievance != null ? grievance.getOtherService() : "");
        }
        map.put("name", name);
        map.put("email", email);
        map.put("phoneNumber", phoneNumber);
        map.put("serviceName", serviceName);
        return map;
    }

    public List<RegisterDTO> getDashboardDataForGrievanceRegister(Long officeId) {
        List<DashboardData> dashboardDataList = dashboardDataDAO.getDashboardDataForCurrentMonthGrievanceRegister(0L, officeId);
        List<RegisterDTO> registerEntries = new ArrayList();
        long index = 0;
        for(DashboardData dashboardData: dashboardDataList) {
            Map<String, String> complainantAndServiceInfo = getComplainantInfoAndServiceName(dashboardData);
            RegisterDTO registerDTO = RegisterDTO.builder()
                    .id(++index)
                    .dateBng(BanglaConverter.getDateBanglaFromEnglish(DateTimeConverter.convertDateToString(dashboardData.getAcceptedDate())))
                    .subject(dashboardData.getSubject())
                    .grievanceId(dashboardData.getGrievanceId())
                    .complainantEmail(complainantAndServiceInfo.get("email"))
                    .complainantMobile(complainantAndServiceInfo.get("phoneNumber"))
                    .complainantName(complainantAndServiceInfo.get("name"))
                    .caseNumber(dashboardData.getCaseNumber())
                    .service(complainantAndServiceInfo.get("serviceName"))
                    .serviceType(dashboardData.getGrievanceType())
                    .medium(dashboardData.getMediumOfSubmission())
                    .closingOrRejectingDateBng(BanglaConverter.getDateBanglaFromEnglish(DateTimeConverter.convertDateToString(dashboardData.getClosedDate())))
                    .closingOrRejectingDateEng(DateTimeConverter.convertDateToString(dashboardData.getClosedDate()))
                    .rootCause(dashboardData.getGroIdentifiedCause())
                    .remedyMeasures(dashboardData.getGroDecision())
                    .preventionMeasures(dashboardData.getGroSuggestion())
                    .build();
            registerEntries.add(registerDTO);
        }
        return registerEntries;
    }

    public RegisterDTO convertDashboardDataToRegisterDTO(DashboardData dashboardData) {
        Map<String, String> complainantAndServiceInfo = getComplainantInfoAndServiceName(dashboardData);
        Date closedDate = dashboardData.getClosedDate();
        if(dashboardData.getComplaintStatus().toString().contains("FORWARDED")){
            closedDate = dashboardData.getUpdatedAt();
        }
        return RegisterDTO.builder()
                .id(dashboardData.getGrievanceId())
                .dateBng(BanglaConverter.getDateBanglaFromEnglish(DateTimeConverter.convertDateToString(dashboardData.getAcceptedDate())))
                .subject(dashboardData.getSubject())
                .grievanceId(dashboardData.getGrievanceId())
                .complainantEmail(complainantAndServiceInfo.get("email"))
                .complainantMobile(complainantAndServiceInfo.get("phoneNumber"))
                .complainantName(complainantAndServiceInfo.get("name"))
                .caseNumber(dashboardData.getCaseNumber())
                .trackingNumber(dashboardData.getTrackingNumber())
                .service(complainantAndServiceInfo.get("serviceName"))
                .serviceType(dashboardData.getGrievanceType())
                .medium(dashboardData.getMediumOfSubmission())
                .currentStatus(dashboardData.getComplaintStatus())
                .closingOrRejectingDateBng(BanglaConverter.getDateBanglaFromEnglish(DateTimeConverter.convertDateToString(closedDate)))
                .closingOrRejectingDateEng(DateTimeConverter.convertDateToString(closedDate))
                .rootCause(dashboardData.getGroIdentifiedCause())
                .remedyMeasures(dashboardData.getGroDecision())
                .preventionMeasures(dashboardData.getGroSuggestion())
                .build();
    }

    public RegisterDTO convertDashboardDataToAppealRegisterDTO(DashboardData dashboardData) {
        RegisterDTO registerDTO = convertDashboardDataToRegisterDTO(dashboardData);
        DashboardData dashboardDataInGrievancePhase = dashboardDataDAO.getDashboardDataForGrievancePhaseOfAppeal(dashboardData.getGrievanceId());
        Date grievanceClosingDate = dashboardDataInGrievancePhase != null ? dashboardDataInGrievancePhase.getClosedDate() : null;
        registerDTO.setClosingDateInGrievancePhase(grievanceClosingDate != null ? BanglaConverter.getDateBanglaFromEnglish(DateTimeConverter.convertDateToString(grievanceClosingDate)) : null);
        return registerDTO;
    }

    public Page<RegisterDTO> getPageableDashboardDataForGrievanceRegister(Long officeId, Pageable pageable) {
        Page<DashboardData> dashboardDataList = dashboardDataDAO.getPageableDashboardDataForGrievanceRegister(officeId, pageable);
        return dashboardDataList.map(this::convertDashboardDataToRegisterDTO);
    }

    public Page<RegisterDTO> getPageableDashboardDataForAppealRegister(Long officeId, Pageable pageable) {
        Page<DashboardData> dashboardDataList = dashboardDataDAO.getPageableDashboardDataAppealRegister(officeId, pageable);
        return dashboardDataList.map(this::convertDashboardDataToAppealRegisterDTO);
    }

    public Page<RegisterDTO> getPageableDashboardDataForAppealedComplaints(Long officeId, Pageable pageable) {
        Page<DashboardData> dashboardDataList = dashboardDataDAO.getPageableDashboardDataForAppealedComplaints(officeId, pageable);
        return dashboardDataList.map(this::convertDashboardDataToRegisterDTO);
    }

    public Page<NudgeDTO> getPageableDashboardDataForTagidList(UserInformation userInformation, Pageable pageable) {
        Long officeId = userInformation.getOfficeInformation().getOfficeId();
        Long officeUnitOrganogramId = userInformation.getOfficeInformation().getOfficeUnitOrganogramId();
        Page<Tagid> dashboardDataList = tagidDAO.findByOfficeIdAndOfficeUnitOrganogramId(officeId, officeUnitOrganogramId, pageable);
        return dashboardDataList.map(this::convertToNudgeDTO);
    }

    private NudgeDTO convertToNudgeDTO(Tagid tagid){
        Grievance grievance = this.grievanceService.findGrievanceById(tagid.getComplaintId());
        return NudgeDTO.builder()
                .currentStatus(BanglaConverter.convertGrievanceStatusToBangla(grievance.getGrievanceCurrentStatus()))
                .dateOfNudge(BanglaConverter.getDateBanglaFromEnglish(DateTimeConverter.convertDateToString(tagid.getGivingDate())))
                .grievanceSubmissionDate(BanglaConverter.getDateBanglaFromEnglish(DateTimeConverter.convertDateToString(grievance.getSubmissionDate())))
                .officeName(tagid.getOfficeName())
                .subject(grievance.getSubject())
                .trackingNumber(grievance.getTrackingNumber())
                .id(grievance.getId())
                .build();
    }

    public List<RegisterDTO> getDashboardDataForAppealRegister(Long officeId) {
        List<DashboardData> dashboardDataList = dashboardDataDAO.getDashboardDataForCurrentMonthAppealRegister(0L, officeId);
        List<RegisterDTO> registerEntries = new ArrayList();
        long index = 0;
        for(DashboardData dashboardData: dashboardDataList) {
            Map<String, String> complainantAndServiceInfo = getComplainantInfoAndServiceName(dashboardData);
            DashboardData dashboardDataInGrievancePhase = dashboardDataDAO.getDashboardDataForGrievancePhaseOfAppeal(dashboardData.getGrievanceId());
            Date grievanceClosingDate = dashboardDataInGrievancePhase != null ? dashboardDataInGrievancePhase.getClosedDate() : null;
            RegisterDTO registerDTO = RegisterDTO.builder()
                    .id(++index)
                    .dateBng(BanglaConverter.getDateBanglaFromEnglish(DateTimeConverter.convertDateToString(dashboardData.getCreatedAt())))
                    .subject(dashboardData.getSubject())
                    .grievanceId(dashboardData.getGrievanceId())
                    .complainantEmail(complainantAndServiceInfo.get("email"))
                    .complainantMobile(complainantAndServiceInfo.get("phoneNumber"))
                    .complainantName(complainantAndServiceInfo.get("name"))
                    .caseNumber(dashboardData.getCaseNumber())
                    .service(complainantAndServiceInfo.get("serviceName"))
                    .serviceType(dashboardData.getGrievanceType())
                    .medium(dashboardData.getMediumOfSubmission())
                    .closingDateInGrievancePhase(grievanceClosingDate != null ? BanglaConverter.getDateBanglaFromEnglish(DateTimeConverter.convertDateToString(grievanceClosingDate)) : null)
                    .closingOrRejectingDateBng(BanglaConverter.getDateBanglaFromEnglish(DateTimeConverter.convertDateToString(dashboardData.getClosedDate())))
                    .rootCause(dashboardData.getAoIdentifiedCause())
                    .remedyMeasures(dashboardData.getAoDecision())
                    .preventionMeasures(dashboardData.getAoSuggestion())
                    .build();
            registerEntries.add(registerDTO);
        }
        return registerEntries;
    }

    public Long countTotalComplaintsByOfficeId(Long officeId, Long monthDiff) {
        return dashboardDataDAO.countTotalComplaintsByOfficeIdV2(officeId, monthDiff);
    }

    public Long countResolvedComplaintsByOfficeId(Long officeId, Long monthDiff) {
        return dashboardDataDAO.countResolvedComplaintsByOfficeId(officeId, monthDiff);
    }

    public Long countTimeExpiredComplaintsByOfficeId(Long officeId, Long monthDiff) {
        return dashboardDataDAO.countTimeExpiredComplaintsByOfficeId(officeId, monthDiff);
    }

    public Long countAllTimeExpiredComplaintsByOfficeId(Long officeId, Long monthDiff) {
        return dashboardDataDAO.countAllTimeExpiredComplaintsByOfficeId(officeId, monthDiff);
    }

    public Long countRunningGrievancesByOfficeId(Long officeId, Long monthDiff) {
        return dashboardDataDAO.countRunningGrievancesByOfficeId(officeId, monthDiff);
    }

    public Long countAllRunningGrievancesByOfficeId(Long officeId, Long monthDiff) {
        return dashboardDataDAO.countAllRunningGrievancesByOfficeId(officeId, monthDiff);
    }

    public Long countDeclinedGrievancesByOfficeId(Long officeId, Long monthDiff) {
        return dashboardDataDAO.countDeclinedGrievancesByOfficeId(officeId, monthDiff);
    }

    public Long countTotalAppealsByOfficeId(Long officeId, Long monthDiff) {
        return dashboardDataDAO.countTotalAppealsByOfficeId(officeId, monthDiff);
    }

    public Long countResolvedAppealsByOfficeId(Long officeId, Long monthDiff) {
        return dashboardDataDAO.countResolvedAppealsByOfficeId(officeId, monthDiff);
    }

    public Long countRunningAppealsByOfficeId(Long officeId, Long monthDiff) {
        return dashboardDataDAO.countRunningAppealsByOfficeId(officeId, monthDiff);
    }

    public Long countTimeExpiredAppealsByOfficeId(Long officeId, Long monthDiff) {
        return dashboardDataDAO.countTimeExpiredAppealsByOfficeId(officeId);
    }

    public Long countTotalComplaintsByOfficeIdV2(Long officeId, Long monthDiff) {
        return dashboardDataDAO.countTotalComplaintsByOfficeIdV2(officeId, monthDiff);
    }

    public Long countResolvedComplaintsByOfficeIdV2(Long officeId, Long monthDiff) {
        return dashboardDataDAO.countResolvedComplaintsByOfficeIdV2(officeId, monthDiff);
    }

    public Long countTimeExpiredComplaintsByOfficeIdV2(Long officeId, Long monthDiff) {
        return dashboardDataDAO.countTimeExpiredComplaintsByOfficeIdV2(officeId, monthDiff);
    }

    public Long countRunningGrievancesByOfficeIdV2(Long officeId, Long monthDiff) {
        return dashboardDataDAO.countRunningGrievancesByOfficeIdV2(officeId, monthDiff);
    }

    public Long countDeclinedGrievancesByOfficeIdV2(Long officeId, Long monthDiff) {
        return dashboardDataDAO.countDeclinedGrievancesByOfficeIdV2(officeId, monthDiff);
    }

    public Long countTotalAppealsByOfficeIdV2(Long officeId, Long monthDiff) {
        return dashboardDataDAO.countTotalAppealsByOfficeIdV2(officeId, monthDiff);
    }

    public Long countResolvedAppealsByOfficeIdV2(Long officeId, Long monthDiff) {
        return dashboardDataDAO.countResolvedAppealsByOfficeIdV2(officeId, monthDiff);
    }

    public Long countTimeExpiredAppealsByOfficeIdV2(Long officeId, Long monthDiff) {
        return dashboardDataDAO.countTimeExpiredAppealsByOfficeIdV2(officeId, monthDiff);
    }

    public Long countRunningAppealsByOfficeIdV2(Long officeId, Long monthDiff) {
        return dashboardDataDAO.countRunningAppealsByOfficeIdV2(officeId, monthDiff);
    }

    public List<TotalResolvedByMonth> getTotalResolvedGrievancesByMonthOfCurrentYear(Long officeId) {
        List<TotalResolvedByMonth> totalResolvedByMonthList = new ArrayList();
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        List<MonthlyReport> reportList = monthlyReportDAO.findByOfficeIdAndYear(officeId, currentYear);
        for(int i=0; i < 12; i++) {
            TotalResolvedByMonth totalResolvedByMonth = new TotalResolvedByMonth();
            totalResolvedByMonth.month = i;
            Integer monthNumber = i+1;
            MonthlyReport monthlyReport = reportList.stream().filter(r -> r.getMonth().equals(monthNumber)).findFirst().orElse(null);
            if(monthlyReport != null) {
                totalResolvedByMonth.total = monthlyReport.getTotalCount();
                totalResolvedByMonth.resolved = monthlyReport.getResolvedCount();
            }
            if(i == currentMonth) {
                totalResolvedByMonth.total = dashboardDataDAO.countTotalComplaintsByOfficeIdV2(officeId, 0L);
                totalResolvedByMonth.resolved = dashboardDataDAO.countResolvedComplaintsByOfficeId(officeId, 0L);
            }
            totalResolvedByMonthList.add(totalResolvedByMonth);
        }
        return totalResolvedByMonthList;
    }

    public List<TotalResolvedByMonth> getTotalResolvedAppealsByMonthOfCurrentYear(Long officeId) {
        List<TotalResolvedByMonth> totalResolvedByMonthList = new ArrayList();
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        List<MonthlyReport> reportList = monthlyReportDAO.findByOfficeIdAndYear(officeId, currentYear);
        for(int i=0; i < 12; i++) {
            TotalResolvedByMonth totalResolvedByMonth = new TotalResolvedByMonth();
            totalResolvedByMonth.month = i;
            Integer monthNumber = i+1;
            MonthlyReport monthlyReport = reportList.stream().filter(r -> r.getMonth().equals(monthNumber)).findFirst().orElse(null);
            if(monthlyReport != null) {
                totalResolvedByMonth.total = monthlyReport.getAppealTotalCount();
                totalResolvedByMonth.resolved = monthlyReport.getAppealResolvedCount();
            }
            if(i == currentMonth) {
                totalResolvedByMonth.total = dashboardDataDAO.countTotalAppealsByOfficeId(officeId, 0L);
                totalResolvedByMonth.resolved = dashboardDataDAO.countResolvedAppealsByOfficeId(officeId, 0L);
            }
            totalResolvedByMonthList.add(totalResolvedByMonth);
        }
        return totalResolvedByMonthList;
    }

    public YearlyCounts getYearlyCounts() {
        return YearlyCounts.builder()
                .totalComplaint(dashboardDataDAO.countTotalGrievancesYearly())
                .totalForwarded(dashboardDataDAO.countForwardedGrievancesYearly())
                .totalResolved(dashboardDataDAO.countResolvedGrievancesYearly())
                .build();
    }

    public List<TotalAndResolvedCountDTO> getSubordinateTotalSubmittedAndResolvedCountsOfMinistries() {
        List<ChildOfficesDashboardNavigatorDTO> ministryLevelOffices = getGrsEnabledMinistryLevelOffices();
        List<Long> officeIds = ministryLevelOffices.stream()
                .map(ChildOfficesDashboardNavigatorDTO::getId)
                .collect(Collectors.toList());
        List<TotalAndResolvedCountDTO> totalAndResolvedCountDTOS = new ArrayList<>();
        officeIds.forEach( officeId -> {
            GeneralDashboardDataDTO dataDTO = getSubOfficesAggregatedDataForGrievances(officeId);
            Double rate = 0D;
            Long total = dataDTO.getTotal().getValue();
            Long resolved = dataDTO.getResolved().getValue();
            Long forwarded = dataDTO.getDeclined().getValue();
//            if (total > 0 && total - forwarded > 0) {
//                rate = Math.round(resolved / (total - forwarded)) * 100D;
//            }
            if (total > 0) {
                rate = Math.round((resolved + forwarded) / total) * 100D;
            }
            totalAndResolvedCountDTOS.add(
                    TotalAndResolvedCountDTO.builder()
                            .officeId(officeId)
                            .totalCount(dataDTO.getTotal().getValue())
                            .resolvedCount(dataDTO.getResolved().getValue())
                            .expiredCount(dataDTO.getUnresolved().getValue())
                            .rate(rate)
                            .build()
            );
        });
        return totalAndResolvedCountDTOS;
    }

    public Long countResolvedComplaintsByOfficeIdAndYearAndMonth(Long officeId, String year, String month) {
        return this.dashboardDataDAO.countResolvedComplaintsByOfficeIdAndYearAndMonth(officeId, year, month);
    }

    public Long countTimeExpiredComplaintsByOfficeIdAndYearAndMonth(Long officeId, String year, String month) {
        return this.dashboardDataDAO.countTimeExpiredComplaintsByOfficeIdAndYearAndMonth(officeId, year, month);
    }

    public Long countRunningGrievancesByOfficeIdAndYearAndMonth(Long officeId, String year, String month) {
        return this.dashboardDataDAO.countRunningGrievancesByOfficeIdAndYearAndMonth(officeId, year, month);
    }

    public Long countTotalComplaintsByOfficeIdAndYearAndMonth(Long officeId, String year, String month) {
        return this.dashboardDataDAO.countTotalComplaintsByOfficeIdAndYearAndMonth(officeId, year, month);
    }

    public Long countDeclinedGrievancesByOfficeIdAndYearAndMonth(Long officeId, String year, String month) {
        return this.dashboardDataDAO.countDeclinedGrievancesByOfficeIdAndYearAndMonth(officeId, year, month);
    }

    public Long getGrievanceAscertainCountbyOfficeIdAndYearAndMonth(Long officeId, String year, String month) {
        return this.dashboardDataDAO.getGrievanceAscertainCountbyOfficeIdAndYearAndMonth(officeId, year, month);
    }

    public Long getMonthlyComplaintsCountByOfficeIdAndMediumOfSubmissionAndYearAndMonth(Long officeId, MediumOfSubmission mediumOfSubmission, String year, String month) {
        return this.dashboardDataDAO.getMonthlyComplaintsCountByOfficeIdAndMediumOfSubmissionAndYearAndMonth(officeId, mediumOfSubmission, year, month);
    }

    public Long countResolvedAppealByOfficeIdAndYearAndMonth(Long officeId, String year, String month) {
        return this.dashboardDataDAO.countResolvedAppealByOfficeIdAndYearAndMonth(officeId, year, month);
    }

    public Long countTimeExpiredAppealByOfficeIdAndYearAndMonth(Long officeId, String year, String month) {
        return this.dashboardDataDAO.countTimeExpiredAppealByOfficeIdAndYearAndMonth(officeId, year, month);
    }

    public Long countRunningAppealByOfficeIdAndYearAndMonth(Long officeId, String year, String month) {
        return this.dashboardDataDAO.countRunningAppealByOfficeIdAndYearAndMonth(officeId, year, month);
    }

    public Long countTotalAppealByOfficeIdAndYearAndMonth(Long officeId, String year, String month) {
        return this.dashboardDataDAO.countTotalAppealByOfficeIdAndYearAndMonth(officeId, year, month);
    }

    public Long getAppealAscertainCountByOfficeIdAndYearAndMonth(Long officeId, String year, String month) {
        return this.dashboardDataDAO.getAppealAscertainCountByOfficeIdAndYearAndMonth(officeId, year, month);
    }

    public Long getMonthlyAppealCountByOfficeIdAndMediumOfSubmissionAndYearAndMonth(Long officeId, MediumOfSubmission selfMotivatedAcceptance, String year, String month) {

        return this.dashboardDataDAO.getMonthlyAppealCountByOfficeIdAndMediumOfSubmissionAndYearAndMonth(officeId, selfMotivatedAcceptance, year, month);
    }
}
