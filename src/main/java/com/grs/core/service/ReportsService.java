package com.grs.core.service;

import com.grs.api.model.UserInformation;
import com.grs.api.model.response.reports.GrievanceAndAppealMonthlyReportDTO;
import com.grs.api.model.response.reports.GrievanceMonthlyReportsDTO;
import com.grs.api.model.response.reports.MonthlyReportDTO;
import com.grs.core.dao.MonthlyReportDAO;
import com.grs.core.dao.ReportsDAO;
import com.grs.core.domain.MediumOfSubmission;
import com.grs.core.domain.grs.MonthlyReport;
import com.grs.core.domain.grs.OfficesGRO;
import com.grs.core.domain.projapoti.Office;
import com.grs.core.domain.projapoti.OfficeLayer;
import com.grs.core.domain.projapoti.OfficeMinistry;
import com.grs.utils.Constant;
import com.grs.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Acer on 22-Feb-18.
 */
@Slf4j
@Service
public class ReportsService {
    @Autowired
    private ReportsDAO reportsDAO;
    @Autowired
    private OfficeService officeService;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private MonthlyReportDAO monthlyReportDAO;
    @Autowired
    private OfficesGroService officesGroService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private ShortMessageService shortMessageService;
    @Autowired
    private GeneralSettingsService generalSettingsService;
    int updated = 0;

    public GrievanceMonthlyReportsDTO getGrievanceMonthlyReportsSummarry(String month, Office office) {
        Date date = new Date(Long.valueOf(month));
        Long officeId = office.getId();
        Long runningCount = this.reportsDAO.countRunningGrievancesByOfficeIdAndDateInBetween(officeId, date),
                pendingCount = this.reportsDAO.countUnresolvedGrievancesByOfficeIdAndDateInBetween(officeId, date),
                resolvedCount = this.reportsDAO.countResolvedGrievancesByOfficeIdAndDateInBetween(officeId, date);


        return GrievanceMonthlyReportsDTO.builder()
                .countByWebsite(this.reportsDAO.countByOfficeAndMediumOfSubmissionAndDateInBetween(officeId, MediumOfSubmission.ONLINE.name(), date))
                .countByCallcenter(this.reportsDAO.countByOfficeAndMediumOfSubmissionAndDateInBetween(officeId, MediumOfSubmission.CALL_CENTER.name(), date))
                .countByOrthodox(this.reportsDAO.countByOfficeAndMediumOfSubmissionAndDateInBetween(officeId, MediumOfSubmission.CONVENTIONAL_METHOD.name(), date))
                .countBySelfMotivatedWay(this.reportsDAO.countByOfficeAndMediumOfSubmissionAndDateInBetween(officeId, MediumOfSubmission.SELF_MOTIVATED_ACCEPTANCE.name(), date))
                .pendingGrievanceOfPrevoiusMonth(this.reportsDAO.countByOfficeAndMediumOfSubmissionAndDateInBetween(officeId, MediumOfSubmission.FROM_LAST_MONTH.name(), date))
                .countByResolvedStatus(resolvedCount)
                .countByRunningStatus(runningCount)
                .countByPendingStatus(pendingCount)
                .resolutionRate((long) ((resolvedCount * 100.0) / (pendingCount + resolvedCount + runningCount)))
                .officeName(office.getNameBangla())
                .build();
    }

    public List<GrievanceMonthlyReportsDTO> getMonthlyFieldCoordinationReports(String month, UserInformation userInformation) {
        Long fieldCoordinatorsLayerLevel = userInformation.getOfficeInformation().getLayerLevel();
        List<Long> officeIdListByGeoId = null;
        if (fieldCoordinatorsLayerLevel.equals(Constant.layerThree)) {
            officeIdListByGeoId = this.officeService.getOfficeIdListByGeoDivisionId(userInformation.getOfficeInformation().getGeoDivisionId(), fieldCoordinatorsLayerLevel);
        } else if (fieldCoordinatorsLayerLevel.equals(Constant.layerFour)) {
            officeIdListByGeoId = this.officeService.getOfficeIdListByGeoDistrictId(userInformation.getOfficeInformation().getGeoDistrictId(), fieldCoordinatorsLayerLevel);
        }
        List<GrievanceMonthlyReportsDTO> grievanceMonthlyReportsDTOList = this.officeService
                .getGRSenabledOfficesFromOffices(officeIdListByGeoId)
                .stream()
                .map(x -> {
                    return this.getGrievanceMonthlyReportsSummarry(month, x);
                })
                .collect(Collectors.toList());
        return grievanceMonthlyReportsDTOList;
    }

    public MonthlyReportDTO getGrievanceMonthlyReport(Long officeId, Long monthDiff) {
        Long totalSubmitted = dashboardService.countTotalComplaintsByOfficeIdV2(officeId, monthDiff);
        Long resolvedCount = dashboardService.countResolvedComplaintsByOfficeId(officeId, monthDiff);
        Long timeExpiredCount = dashboardService.countTimeExpiredComplaintsByOfficeId(officeId, monthDiff);
        Long runningGrievanceCount = dashboardService.countRunningGrievancesByOfficeId(officeId, monthDiff);
        Long sentToOtherOfficeCount = dashboardService.countDeclinedGrievancesByOfficeId(officeId, monthDiff);
        Double rate = 0d;
//        Long excludeFromTotal = runningGrievanceCount + sentToOtherOfficeCount;
        Long totalDecided = resolvedCount + sentToOtherOfficeCount;
        if (totalSubmitted > 0) {
            rate = (double) ((totalDecided / totalSubmitted) * 100);
            rate = (double) Math.round(rate * 100) / 100;
        }
        return MonthlyReportDTO.builder()
                .officeId(officeId)
                .onlineSubmissionCount(dashboardService.getMonthlyComplaintsCountByOfficeIdAndMediumOfSubmission(officeId, MediumOfSubmission.ONLINE, monthDiff))
                .conventionalMethodSubmissionCount(dashboardService.getMonthlyComplaintsCountByOfficeIdAndMediumOfSubmission(officeId, MediumOfSubmission.CONVENTIONAL_METHOD, monthDiff))
                .selfMotivatedAccusationCount(dashboardService.getMonthlyComplaintsCountByOfficeIdAndMediumOfSubmission(officeId, MediumOfSubmission.SELF_MOTIVATED_ACCEPTANCE, monthDiff))
                .inheritedFromLastMonthCount(dashboardService.getGrievanceAscertainCountOfPreviousMonthV2(officeId, monthDiff))
                .totalCount(totalSubmitted)
                .sentToOtherCount(sentToOtherOfficeCount)
                .resolvedCount(resolvedCount)
                .runningCount(runningGrievanceCount)
                .timeExpiredCount(timeExpiredCount)
                .rate(rate)
                .build();
    }

    public MonthlyReportDTO getAppealMonthlyReport(Long officeId, Long monthDiff) {
        Long totalSubmitted = dashboardService.countTotalAppealsByOfficeIdV2(officeId, monthDiff);
        Long resolvedCount = dashboardService.countResolvedAppealsByOfficeIdV2(officeId, monthDiff);
        Long timeExpiredCount = dashboardService.countTimeExpiredAppealsByOfficeId(officeId, monthDiff);
        Long runningGrievanceCount = dashboardService.countRunningAppealsByOfficeId(officeId, monthDiff);
        Double rate = 0d;
//        if (totalSubmitted > runningGrievanceCount) {
//            rate = (double) ((resolvedCount / (totalSubmitted - runningGrievanceCount)) * 100);
//            rate = (double) Math.round(rate * 100) / 100;
//        }
        if (totalSubmitted > 0) {
            rate = (double) ((resolvedCount / (totalSubmitted)) * 100);
            rate = (double) Math.round(rate * 100) / 100;
        }
        return MonthlyReportDTO.builder()
                .officeId(officeId)
                .onlineSubmissionCount(dashboardService.getMonthlyAppealsCountByOfficeIdAndMediumOfSubmission(officeId, MediumOfSubmission.ONLINE, monthDiff))
                .inheritedFromLastMonthCount(dashboardService.getAppealAscertainCountOfPreviousMonth(officeId, monthDiff))
                .totalCount(totalSubmitted)
                .resolvedCount(resolvedCount)
                .runningCount(runningGrievanceCount)
                .timeExpiredCount(timeExpiredCount)
                .rate(rate)
                .build();
    }

    public MonthlyReportDTO getGrievanceMonthlyReportForGenerate(Long officeId, Long monthDiff) {
        Long totalSubmitted = dashboardService.countTotalComplaintsByOfficeIdV2(officeId, monthDiff);
        Long resolvedCount = dashboardService.countResolvedComplaintsByOfficeIdV2(officeId, monthDiff);
        Long timeExpiredCount = dashboardService.countTimeExpiredComplaintsByOfficeIdV2(officeId, monthDiff);
        Long runningGrievanceCount = dashboardService.countRunningGrievancesByOfficeIdV2(officeId, monthDiff);
        Long sentToOtherOfficeCount = dashboardService.countDeclinedGrievancesByOfficeIdV2(officeId, monthDiff);
        Double rate = 0d;
//        Long excludeFromTotal = runningGrievanceCount + sentToOtherOfficeCount;
        Long totalDecided = resolvedCount + sentToOtherOfficeCount;
        if (totalSubmitted > 0) {
            rate = (double) ((totalDecided / totalSubmitted) * 100);
            rate = (double) Math.round(rate * 100) / 100;
        }
        return MonthlyReportDTO.builder()
                .officeId(officeId)
                .onlineSubmissionCount(dashboardService.getMonthlyComplaintsCountByOfficeIdAndMediumOfSubmission(officeId, MediumOfSubmission.ONLINE, monthDiff))
                .conventionalMethodSubmissionCount(dashboardService.getMonthlyComplaintsCountByOfficeIdAndMediumOfSubmission(officeId, MediumOfSubmission.CONVENTIONAL_METHOD, monthDiff))
                .selfMotivatedAccusationCount(dashboardService.getMonthlyComplaintsCountByOfficeIdAndMediumOfSubmission(officeId, MediumOfSubmission.SELF_MOTIVATED_ACCEPTANCE, monthDiff))
                .inheritedFromLastMonthCount(dashboardService.getGrievanceAscertainCountOfPreviousMonthV2(officeId, monthDiff))
                .totalCount(totalSubmitted)
                .sentToOtherCount(sentToOtherOfficeCount)
                .resolvedCount(resolvedCount)
                .runningCount(runningGrievanceCount)
                .timeExpiredCount(timeExpiredCount)
                .rate(rate)
                .build();
    }

    public MonthlyReportDTO getAppealMonthlyReportForGenerate(Long officeId, Long monthDiff) {
        Long totalSubmitted = dashboardService.countTotalAppealsByOfficeIdV2(officeId, monthDiff);
        Long resolvedCount = dashboardService.countResolvedAppealsByOfficeIdV2(officeId, monthDiff);
        Long timeExpiredCount = dashboardService.countTimeExpiredAppealsByOfficeIdV2(officeId, monthDiff);
        Long runningGrievanceCount = dashboardService.countRunningAppealsByOfficeIdV2(officeId, monthDiff);
        Double rate = 0d;
//        if (totalSubmitted > runningGrievanceCount) {
//            rate = (double) ((resolvedCount / (totalSubmitted - runningGrievanceCount)) * 100);
//            rate = (double) Math.round(rate * 100) / 100;
//        }
        if (totalSubmitted > 0) {
            rate = (double) ((resolvedCount / (totalSubmitted)) * 100);
            rate = (double) Math.round(rate * 100) / 100;
        }
        return MonthlyReportDTO.builder()
                .officeId(officeId)
                .onlineSubmissionCount(dashboardService.getMonthlyAppealsCountByOfficeIdAndMediumOfSubmissionV2(officeId, MediumOfSubmission.ONLINE, monthDiff))
                .inheritedFromLastMonthCount(dashboardService.getAppealAscertainCountOfPreviousMonthV2(officeId, monthDiff))
                .totalCount(totalSubmitted)
                .resolvedCount(resolvedCount)
                .runningCount(runningGrievanceCount)
                .timeExpiredCount(timeExpiredCount)
                .rate(rate)
                .build();
    }

    public GrievanceAndAppealMonthlyReportDTO getMonthlyReport(Long officeId, int year, int month, long layerLevel) {
        GrievanceAndAppealMonthlyReportDTO grievanceAndAppealMonthlyReportDTO;
        Calendar calendar = Calendar.getInstance();
        int reportMonth = 12 * year + month;
        int currentMonth = 12 * calendar.get(Calendar.YEAR) + (calendar.get(Calendar.MONTH) + 1);
        int monthDiff = reportMonth - currentMonth;
        OfficesGRO officesGRO = this.officesGroService.findOfficesGroByOfficeId(officeId);
        String officeName = officesGRO == null ? "" : officesGRO.getOfficeNameBangla();
        Boolean hasAppealReport = layerLevel < Constant.districtLayerLevel && officeService.hasChildOffice(officeId);
        if (monthDiff == 0) {
            MonthlyReportDTO appealReportDTO = null;
            if (hasAppealReport) {
                appealReportDTO = getAppealMonthlyReport(officeId, 0L);
            }
            grievanceAndAppealMonthlyReportDTO = GrievanceAndAppealMonthlyReportDTO.builder()
                    .officeId(officeId)
                    .year(year)
                    .month(month)
                    .monthlyGrievanceReport(getGrievanceMonthlyReport(officeId, 0L))
                    .monthlyAppealReport(appealReportDTO)
                    .officeName(officeName)
                    .build();
        } else {
            MonthlyReport report = monthlyReportDAO.findByOfficeIdAndYearAndMonth(officeId, year, month);
            if (report != null) {
                grievanceAndAppealMonthlyReportDTO = monthlyReportDAO.convertToGrievanceAndAppealMonthlyReportDTO(report, hasAppealReport);
            } else {
                grievanceAndAppealMonthlyReportDTO = GrievanceAndAppealMonthlyReportDTO.builder()
                        .officeId(officeId)
                        .officeName(officeName)
                        .year(year)
                        .month(month)
                        .build();
            }
        }
        return grievanceAndAppealMonthlyReportDTO;
    }

    public List<GrievanceAndAppealMonthlyReportDTO> getChildOfficesLastMonthReport(Long officeId) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        List<Office> offices = officeService.getOfficesByParentOfficeId(officeId);
        List<Long> officeIdList = offices.stream()
                .map(Office::getId)
                .collect(Collectors.toList());
        List<Long> grsEnabledOfficeIdList = officesGroService.getGRSEnabledOfficeIdFromOfficeIdList(officeIdList);
        List<GrievanceAndAppealMonthlyReportDTO> reportList = new ArrayList();
        grsEnabledOfficeIdList.stream().forEach(id -> {
            GrievanceAndAppealMonthlyReportDTO grievanceAndAppealMonthlyReportDTO = null;
            Office office = offices.stream().filter(o -> {
                return o.getId().equals(id);
            }).findFirst().orElse(null);
            Boolean hasAppealReport = office.getOfficeLayer().getLayerLevel() < Constant.districtLayerLevel && officeService.hasChildOffice(id);
            MonthlyReport report = monthlyReportDAO.findByOfficeIdAndYearAndMonth(id, year, month);
            if (report != null) {
                grievanceAndAppealMonthlyReportDTO = monthlyReportDAO.convertToGrievanceAndAppealMonthlyReportDTO(report, hasAppealReport);
            } else {
                grievanceAndAppealMonthlyReportDTO = GrievanceAndAppealMonthlyReportDTO.builder()
                        .officeId(id)
                        .month(month)
                        .year(year)
                        .monthlyGrievanceReport(null)
                        .monthlyAppealReport(null)
                        .build();
            }
            grievanceAndAppealMonthlyReportDTO.setOfficeName(office.getNameBangla());
            reportList.add(grievanceAndAppealMonthlyReportDTO);
        });
        return reportList;
    }

    // second minute hour day-of-month month day-of-week
    @Scheduled(cron = "0 5 6 1 */1 *")
    public void generateReportsAtEndOfMonth() {
        List<OfficesGRO> grsIncorporatedOffices = officesGroService.getCurrentlyGrsEnabledOffices();
        log.info("Monthly report generation started at " + (new Date()).toString());
        List<String> reportGeneratedForOffices = new ArrayList();
        String email = generalSettingsService.getSettingsValueByFieldName(Constant.SYSTEM_NOTIFICATION_EMAIL);
        String phoneNumber = generalSettingsService.getSettingsValueByFieldName(Constant.SYSTEM_NOTIFICATION_PHONE_NUMBER);
        grsIncorporatedOffices.stream().forEach(grsOffice -> {
            try {
                Long officeId = grsOffice.getOfficeId();
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MONTH, -1);
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;
                MonthlyReport monthlyReport = monthlyReportDAO.findByOfficeIdAndYearAndMonth(officeId, year, month);
                if (monthlyReport == null) {
                    GrievanceAndAppealMonthlyReportDTO reportDTO = GrievanceAndAppealMonthlyReportDTO.builder()
                            .officeId(officeId)
                            .year(year)
                            .month(month)
                            .monthlyGrievanceReport(getGrievanceMonthlyReportForGenerate(officeId, -1L))
                            .monthlyAppealReport(getAppealMonthlyReportForGenerate(officeId, -1L))
                            .build();
                    monthlyReport = monthlyReportDAO.convertToMonthlyReport(reportDTO);
                    monthlyReport = monthlyReportDAO.save(monthlyReport);
                    if (monthlyReport.getId() != null) {
                        reportGeneratedForOffices.add(grsOffice.getOfficeNameBangla() + "\n");
                    }
                }
            } catch (Exception e) {
                emailService.sendEmail(email, "GRS monthly reports", "Report of last month has been generated for \n\n" + String.join("\n", reportGeneratedForOffices));
                shortMessageService.sendSMS(phoneNumber, "GRS monthly reports generated for " + reportGeneratedForOffices.size() + " Offices (Please check email for details)");
                log.error("Error occurred during report generation of office " + grsOffice.getOfficeNameBangla());
                log.error(e.getMessage());
            }
        });
        if (reportGeneratedForOffices.size() > 0) {
            emailService.sendEmail(email, "GRS monthly reports", "Report of last month has been generated for \n\n" + String.join("\n", reportGeneratedForOffices));
            shortMessageService.sendSMS(phoneNumber, "GRS monthly reports generated for " + reportGeneratedForOffices.size() + " Offices (Please check email for details)");
        } else {
            emailService.sendEmail(email, "GRS monthly reports", "Cannot generate monthly report");
            shortMessageService.sendSMS(phoneNumber, "Cannot generate monthly report");
        }
        log.info("Monthly report generation finished at " + (new Date()).toString());
    }

    // second minute hour day-of-month month day-of-week
    //    @Scheduled(cron = "0 4/4 * * * *")
    public void regenerateReportsAtEndOfMonth() {
        System.out.println("## mr1");
        if (updated == 1) return;
        System.out.println("## mr2");
        updated = 1;
        List<OfficesGRO> grsIncorporatedOffices = officesGroService.getCurrentlyGrsEnabledOffices();
        log.info("Monthly report generation started at " + (new Date()).toString());
        List<String> reportGeneratedForOffices = new ArrayList();
        String email = generalSettingsService.getSettingsValueByFieldName(Constant.SYSTEM_NOTIFICATION_EMAIL);
        String phoneNumber = generalSettingsService.getSettingsValueByFieldName(Constant.SYSTEM_NOTIFICATION_PHONE_NUMBER);
        int allMonthCountBeforeYear2021 = -26;
        int monthInYear2021 =  -8 ;
        int totalMonthsForAdd = allMonthCountBeforeYear2021 + monthInYear2021;
        List<MonthlyReport> allMonthlyReports = new ArrayList<>();
        for (long monthForAdd = -1; monthForAdd >= totalMonthsForAdd; monthForAdd--) {
            long finalMonthForAdd = monthForAdd;
            grsIncorporatedOffices.stream().forEach(grsOffice -> {
                try {
                    Long officeId = grsOffice.getOfficeId();
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MONTH, (int) finalMonthForAdd);
                    int year = calendar.get(Calendar.YEAR);
                    int month = calendar.get(Calendar.MONTH) + 1;
                    MonthlyReport monthlyReport = monthlyReportDAO.findByOfficeIdAndYearAndMonth(officeId, year, month);
                    if (monthlyReport == null) {
                        GrievanceAndAppealMonthlyReportDTO reportDTO = GrievanceAndAppealMonthlyReportDTO.builder()
                                .officeId(officeId)
                                .year(year)
                                .month(month)
                                .monthlyGrievanceReport(getGrievanceMonthlyReportForGenerate(officeId, finalMonthForAdd))
                                .monthlyAppealReport(getAppealMonthlyReportForGenerate(officeId, finalMonthForAdd))
                                .build();
                        monthlyReport = monthlyReportDAO.convertToMonthlyReport(reportDTO);
                        allMonthlyReports.add(monthlyReport);
                    }
                } catch (Exception e) {
    //                emailService.sendEmail(email, "GRS monthly reports", "Report of last month has been generated for \n\n" + String.join("\n", reportGeneratedForOffices));
    //                shortMessageService.sendSMS(phoneNumber, "GRS monthly reports generated for " + reportGeneratedForOffices.size() + " Offices (Please check email for details)");
                    log.error("Error occurred during report generation of office " + grsOffice.getOfficeNameBangla());
                    log.error(e.getMessage());
                }
            });
            System.out.println("done for : " + finalMonthForAdd);
        }
        monthlyReportDAO.save(allMonthlyReports);
//        if (reportGeneratedForOffices.size() > 0) {
//            emailService.sendEmail(email, "GRS monthly reports", "Report of last month has been generated for \n\n" + String.join("\n", reportGeneratedForOffices));
//            shortMessageService.sendSMS(phoneNumber, "GRS monthly reports generated for " + reportGeneratedForOffices.size() + " Offices (Please check email for details)");
//        } else {
//            emailService.sendEmail(email, "GRS monthly reports", "Cannot generate monthly report");
//            shortMessageService.sendSMS(phoneNumber, "Cannot generate monthly report");
//        }
        log.info("Monthly report generation finished at " + (new Date()).toString());
    }

    public Integer countGeneratedReportByMonthAndYear(Integer month, Integer year) {
        return monthlyReportDAO.countGeneratedReportByMonthAndYear(month, year);
    }

    public List<GrievanceAndAppealMonthlyReportDTO> getCustomReport(long layerLevel, long officeId, int fromYear, int fromMonth, int toYear, int toMonth) {
        List<GrievanceAndAppealMonthlyReportDTO> reportList = new ArrayList();
        int from = 12 * fromYear + fromMonth;
        int to = 12 * toYear + toMonth;
        for (; from <= to; from++) {
            GrievanceAndAppealMonthlyReportDTO reportDTO = getMonthlyReport(officeId, fromYear, fromMonth, layerLevel);
            reportList.add(reportDTO);
            if (fromMonth == 12) {
                fromMonth = 1;
                fromYear++;
            } else {
                fromMonth++;
            }
        }
        return reportList;
    }

    public List<GrievanceAndAppealMonthlyReportDTO> getMultipleOfficesMergedReport(List<Office> childOffices, Integer fromYear, Integer fromMonth, Integer toYear, Integer toMonth) {
        List<GrievanceAndAppealMonthlyReportDTO> grievanceAndAppealMonthlyReportDTOS = new ArrayList<>();
        for (Office childOffice : childOffices) {
            Long totalOnline = 0L;
            Long totalSelfMotivated = 0L;
            Long totalConventional = 0L;
            Long totalNew = 0L;
            Long totalInherited = -1L;
            Long sendToOtherOffices = 0L;
            Long totalResolved = 0L;
            Long totalRunning = 0L;
            Long timeExpired = 0L;
            Long totalNewAppeal = 0L;
            Long totalInheritedAppeal = -1L;
            Long totalResolvedAppeal = 0L;
            Long totalRunningAppeal = 0L;
            Long timeExpiredAppeal = 0L;
            Boolean hasAppealReportFlag = false;
            Double rate = 0d;
            Double rateAppeal = 0d;


            for(GrievanceAndAppealMonthlyReportDTO reportDTO: getCustomReport(childOffice.getOfficeLayer().getLayerLevel(), childOffice.getId(), fromYear, fromMonth, toYear, toMonth)) {
                MonthlyReportDTO monthlyGrievanceReport = reportDTO.getMonthlyGrievanceReport();
                MonthlyReportDTO monthlyAppealReport = reportDTO.getMonthlyAppealReport();
                if (monthlyGrievanceReport != null) {
                    if(totalInherited == -1) {
                        totalInherited = monthlyGrievanceReport.getInheritedFromLastMonthCount();
                    }
                    totalOnline += monthlyGrievanceReport.getOnlineSubmissionCount();
                    totalSelfMotivated += monthlyGrievanceReport.getSelfMotivatedAccusationCount();
                    totalConventional += monthlyGrievanceReport.getConventionalMethodSubmissionCount();
                    totalNew += (monthlyGrievanceReport.getOnlineSubmissionCount()
                            + monthlyGrievanceReport.getConventionalMethodSubmissionCount()
                            + monthlyGrievanceReport.getSelfMotivatedAccusationCount());
                    sendToOtherOffices += monthlyGrievanceReport.getSentToOtherCount();
                    totalResolved += monthlyGrievanceReport.getResolvedCount();
                    totalRunning = monthlyGrievanceReport.getRunningCount();
                    timeExpired = monthlyGrievanceReport.getTimeExpiredCount();
                }
                if (monthlyAppealReport != null) {
                    if(totalInheritedAppeal == -1) {
                        totalInheritedAppeal = monthlyAppealReport.getInheritedFromLastMonthCount();
                    }
                    totalNewAppeal += monthlyAppealReport.getOnlineSubmissionCount();
                    totalResolvedAppeal += monthlyAppealReport.getResolvedCount();
                    totalRunningAppeal = monthlyAppealReport.getRunningCount();
                    timeExpiredAppeal = monthlyAppealReport.getTimeExpiredCount();
                    hasAppealReportFlag = true;
                }
            }

//            if (totalNew > totalRunning + sendToOtherOffices) {
//                rate = (double) ((totalResolved * 1.0 / (totalNew - (totalRunning + sendToOtherOffices))) * 100);
//                rate = (double) Math.round(rate * 100) / 100;
//            }

            if (totalNew + totalInherited > 0) {
                rate = (double) (((totalResolved + sendToOtherOffices) * 1.0 / (totalNew + totalInherited)) * 100);
                rate = (double) Math.round(rate * 100) / 100;
            }

//            if (totalNewAppeal > totalRunningAppeal) {
//                rateAppeal = (double) ((totalResolvedAppeal * 1.0 / (totalNewAppeal - totalRunningAppeal)) * 100);
//                rateAppeal = (double) Math.round(rateAppeal * 100) / 100;
//            }

            if (totalNewAppeal + totalInheritedAppeal > 0) {
                rateAppeal = (double) ((totalResolvedAppeal * 1.0 / (totalNewAppeal + totalInheritedAppeal)) * 100);
                rateAppeal = (double) Math.round(rateAppeal * 100) / 100;
            }
            totalInherited = totalInherited == -1 ? 0 : totalInherited;
            totalInheritedAppeal = totalInheritedAppeal == -1 ? 0 : totalInheritedAppeal;

            grievanceAndAppealMonthlyReportDTOS.add(
                    GrievanceAndAppealMonthlyReportDTO.builder()
                            .month(fromMonth)
                            .officeId(childOffice.getId())
                            .officeName(childOffice.getNameBangla())
                            .officeLevel(childOffice.getOfficeLayer().getLayerLevel())
                            .year(fromYear)
                            .monthlyGrievanceReport(
                                    MonthlyReportDTO.builder()
                                            .onlineSubmissionCount(totalOnline)
                                            .selfMotivatedAccusationCount(totalSelfMotivated)
                                            .conventionalMethodSubmissionCount(totalConventional)
                                            .inheritedFromLastMonthCount(totalInherited)
                                            .totalCount(totalNew + totalInherited)
                                            .sentToOtherCount(sendToOtherOffices)
                                            .resolvedCount(totalResolved)
                                            .runningCount(totalRunning)
                                            .rate(rate)
                                            .timeExpiredCount(timeExpired)
                                            .build()
                            )
                            .monthlyAppealReport(
                                    MonthlyReportDTO.builder()
                                            .onlineSubmissionCount(totalNewAppeal)
                                            .inheritedFromLastMonthCount(totalInheritedAppeal)
                                            .totalCount(totalNewAppeal + totalInheritedAppeal)
                                            .resolvedCount(totalResolvedAppeal)
                                            .runningCount(totalRunningAppeal)
                                            .timeExpiredCount(timeExpiredAppeal)
                                            .rate(rateAppeal)
                                            .build()
                            )
                            .build()
            );
        }
        return grievanceAndAppealMonthlyReportDTOS.stream().sorted(Comparator.comparingInt(GrievanceAndAppealMonthlyReportDTO::getOfficeLevel)).collect(Collectors.toList());
    }

    public List<GrievanceAndAppealMonthlyReportDTO> getMinistryBasedReport(Long officeId, Integer fromYear, Integer fromMonth, Integer toYear, Integer toMonth) {
        Office office = this.officeService.getOffice(officeId);
        List<Long> officeIds = this.officesGroService.findAllOffficeIds();
        List<Office> childOffices = this.officeService.getDescendantOfficesByMinistryId(office.getOfficeMinistry()).stream().filter(o -> officeIds.contains(o.getId())).collect(Collectors.toList());
        return getMultipleOfficesMergedReport(childOffices, fromYear, fromMonth, toYear, toMonth);
    }

    public List<GrievanceAndAppealMonthlyReportDTO> getLayerWiseBasedReport(Integer level, Integer fromYear, Integer fromMonth, Integer toYear, Integer toMonth) {
        List<Office> childOffices = this.officeService.getOfficesByLayerLevel(level, true);
        return getMultipleOfficesMergedReport(childOffices, fromYear, fromMonth, toYear, toMonth);
    }

    public List<GrievanceAndAppealMonthlyReportDTO> getLocationBasedReport(Authentication authentication, Integer division, Integer district, Integer upazilla, Integer fromYear, Integer fromMonth, Integer toYear, Integer toMonth) {
        List<Office> offices;
        UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
        Office usersOffice = this.officeService.getOffice(userInformation.getOfficeInformation().getOfficeId());
        OfficeLayer officeLayer = usersOffice.getOfficeLayer();
        OfficeMinistry officeMinistry = usersOffice.getOfficeMinistry();
        boolean allOfficeFlag = officeLayer.getId() == 21 || officeLayer.getId() == 22 || officeLayer.getId() == 23;
        List<Long> officeIds = this.officesGroService.findAllOffficeIds();

        offices = getOfficesWithGeoLocationAndMinistry(division, district, upazilla, officeMinistry);
        if(allOfficeFlag){
            List<Office> otherMinistryOffices = getOfficesWithGeoLocation(division, district, upazilla);
            offices.addAll(otherMinistryOffices);
        }
        List<Office> filteredOffices = offices.stream().filter(o -> officeIds.contains(o.getId())).distinct().collect(Collectors.toList());
        return getMultipleOfficesMergedReport(filteredOffices, fromYear, fromMonth, toYear, toMonth);
    }

    private List<Office> getOfficesWithGeoLocation(Integer divisionId, Integer districtId, Integer upazillaId){
        if(districtId == 0 && upazillaId == 0){
            return this.officeService.getDivisionLevelOffices(divisionId);
        } else if (districtId > 0 && upazillaId ==0){
            return this.officeService.getDistrictLevelOffices(divisionId, districtId);
        } else {
            return this.officeService.getUpazilaLevelOffices(divisionId, districtId, upazillaId);
        }
    }

    private List<Office> getOfficesWithGeoLocationAndMinistry(Integer divisionId, Integer districtId, Integer upazillaId, OfficeMinistry officeMinistry){
        if(districtId == 0 && upazillaId == 0){
            return this.officeService.findByDivisionIdAndOfficeMinistry(divisionId, officeMinistry);
        } else if (districtId > 0 && upazillaId ==0){
            return this.officeService.findByDivisionIdAndDistrictIdAndOfficeMinistry(divisionId, districtId, officeMinistry);
        } else {
            return this.officeService.findByDivisionIdAndDistrictIdAndUpazilaIdAndOfficeMinistry(divisionId, districtId, upazillaId, officeMinistry);
        }
    }

    public void regenerateReports(String year, String month) {
        List<OfficesGRO> grsIncorporatedOffices = officesGroService.getCurrentlyGrsEnabledOffices();
        log.info("Monthly report generation started at " + (new Date()).toString());
        List<String> reportGeneratedForOffices = new ArrayList();
        String email = generalSettingsService.getSettingsValueByFieldName(Constant.SYSTEM_NOTIFICATION_EMAIL);
        String phoneNumber = generalSettingsService.getSettingsValueByFieldName(Constant.SYSTEM_NOTIFICATION_PHONE_NUMBER);
        grsIncorporatedOffices.stream().forEach(grsOffice -> {
            try {
                Long officeId = grsOffice.getOfficeId();
                MonthlyReport previousReport = monthlyReportDAO.findByOfficeIdAndYearAndMonth(officeId, Integer.parseInt(year), Integer.parseInt(month));
                MonthlyReport monthlyReport;
                GrievanceAndAppealMonthlyReportDTO reportDTO = GrievanceAndAppealMonthlyReportDTO.builder()
                        .officeId(officeId)
                        .year(Integer.parseInt(year))
                        .month(Integer.parseInt(month))
                        .monthlyGrievanceReport(generateGrievanceMonthlyReport(officeId, year, month))
                        .monthlyAppealReport(generateAppealMonthlyReport(officeId, year, month))
                        .build();
                monthlyReport = monthlyReportDAO.convertToMonthlyReport(reportDTO);
                if(previousReport != null){
                    monthlyReport.setId(previousReport.getId());
                }
                monthlyReport = monthlyReportDAO.save(monthlyReport);
                if (monthlyReport.getId() != null) {
                    reportGeneratedForOffices.add(grsOffice.getOfficeNameBangla() + "\n");
                }
            } catch (Exception e) {
//                emailService.sendEmail(email, "GRS monthly reports", "Report of last month has been generated for \n\n" + String.join("\n", reportGeneratedForOffices));
//                shortMessageService.sendSMS(phoneNumber, "GRS monthly reports generated for " + reportGeneratedForOffices.size() + " Offices (Please check email for details)");
                log.error("Error occurred during report generation of office " + grsOffice.getOfficeNameBangla());
                log.error(e.getMessage());
            }
        });
        if (reportGeneratedForOffices.size() > 0) {
//            emailService.sendEmail(email, "GRS monthly reports", "Report of last month has been generated for \n\n" + String.join("\n", reportGeneratedForOffices));
//            shortMessageService.sendSMS(phoneNumber, "GRS monthly reports generated for " + reportGeneratedForOffices.size() + " Offices (Please check email for details)");
        } else {
//            emailService.sendEmail(email, "GRS monthly reports", "Cannot generate monthly report");
//            shortMessageService.sendSMS(phoneNumber, "Cannot generate monthly report");
        }
        log.info("Monthly report generation finished at " + (new Date()).toString());
    }

    private MonthlyReportDTO generateAppealMonthlyReport(Long officeId, String year, String month) {
        Long totalSubmitted = dashboardService.countTotalAppealByOfficeIdAndYearAndMonth(officeId, year, month);
        Long resolvedCount = dashboardService.countResolvedAppealByOfficeIdAndYearAndMonth(officeId, year, month);
        Long timeExpiredCount = dashboardService.countTimeExpiredAppealByOfficeIdAndYearAndMonth(officeId, year, month);
        Long runningGrievanceCount = dashboardService.countRunningAppealByOfficeIdAndYearAndMonth(officeId, year, month);
        Double rate = 0d;
//        if (totalSubmitted > runningGrievanceCount) {
        if (totalSubmitted > 0) {
//            rate = (double) (((resolvedCount * 1.0) / (totalSubmitted - runningGrievanceCount)) * 100);
            rate = (double) (((resolvedCount * 1.0) / (totalSubmitted)) * 100);
            rate = (double) Math.round(rate * 100) / 100;
        }
        return MonthlyReportDTO.builder()
                .officeId(officeId)
                .onlineSubmissionCount(dashboardService.getMonthlyAppealCountByOfficeIdAndMediumOfSubmissionAndYearAndMonth(officeId, MediumOfSubmission.ONLINE, year, month))
                .inheritedFromLastMonthCount(dashboardService.getAppealAscertainCountByOfficeIdAndYearAndMonth(officeId, year, month))
                .totalCount(totalSubmitted)
                .resolvedCount(resolvedCount)
                .runningCount(runningGrievanceCount)
                .timeExpiredCount(timeExpiredCount)
                .rate(rate)
                .build();
//       return null;
    }

    private MonthlyReportDTO generateGrievanceMonthlyReport(Long officeId, String year, String month) {
        Long totalSubmitted = dashboardService.countTotalComplaintsByOfficeIdAndYearAndMonth(officeId, year, month);
        Long resolvedCount = dashboardService.countResolvedComplaintsByOfficeIdAndYearAndMonth(officeId, year, month);
        Long timeExpiredCount = dashboardService.countTimeExpiredComplaintsByOfficeIdAndYearAndMonth(officeId, year, month);
        Long runningGrievanceCount = dashboardService.countRunningGrievancesByOfficeIdAndYearAndMonth(officeId, year, month);
        Long sentToOtherOfficeCount = dashboardService.countDeclinedGrievancesByOfficeIdAndYearAndMonth(officeId, year, month);
        Long ascertainCount = dashboardService.getGrievanceAscertainCountbyOfficeIdAndYearAndMonth(officeId, year, month);
        Long onlineSubmissionCount = dashboardService.getMonthlyComplaintsCountByOfficeIdAndMediumOfSubmissionAndYearAndMonth(officeId, MediumOfSubmission.ONLINE, year, month);
        Long conventionalSubmissionCount = dashboardService.getMonthlyComplaintsCountByOfficeIdAndMediumOfSubmissionAndYearAndMonth(officeId, MediumOfSubmission.CONVENTIONAL_METHOD, year, month);
        Long selfMotivatedSubmissionCount = dashboardService.getMonthlyComplaintsCountByOfficeIdAndMediumOfSubmissionAndYearAndMonth(officeId, MediumOfSubmission.SELF_MOTIVATED_ACCEPTANCE, year, month);
        Double rate = 0d;
//        Long excludeFromTotal = runningGrievanceCount + sentToOtherOfficeCount;
        Long decidedTotal = resolvedCount + sentToOtherOfficeCount;
        if (totalSubmitted + ascertainCount > 0) {
            rate = (double) (((decidedTotal * 1.0) / (totalSubmitted + ascertainCount)) * 100);
            rate = (double) Math.round(rate * 100) / 100;
        }
        return MonthlyReportDTO.builder()
                .officeId(officeId)
                .onlineSubmissionCount(onlineSubmissionCount)
                .conventionalMethodSubmissionCount(conventionalSubmissionCount)
                .selfMotivatedAccusationCount(selfMotivatedSubmissionCount)
                .inheritedFromLastMonthCount(ascertainCount)
                .totalCount(totalSubmitted + ascertainCount)
                .sentToOtherCount(sentToOtherOfficeCount)
                .resolvedCount(resolvedCount)
                .runningCount(runningGrievanceCount)
                .timeExpiredCount(timeExpiredCount)
                .rate(rate)
                .build();
    }
}
