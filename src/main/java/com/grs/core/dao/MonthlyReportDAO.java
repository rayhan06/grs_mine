package com.grs.core.dao;

import com.grs.api.model.response.reports.GrievanceAndAppealMonthlyReportDTO;
import com.grs.api.model.response.reports.MonthlyReportDTO;
import com.grs.core.domain.grs.MonthlyReport;
import com.grs.core.repo.grs.MonthlyReportRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class MonthlyReportDAO {
    @Autowired
    private MonthlyReportRepo monthlyReportRepo;

    public MonthlyReport findOne(Long id) {
        return monthlyReportRepo.findOne(id);
    }

    public MonthlyReport findByOfficeIdAndYearAndMonth(Long officeId, Integer year, Integer month) {
        return monthlyReportRepo.findByOfficeIdAndYearAndMonth(officeId, year, month);
    }

    public List<MonthlyReport> findByOfficeIdAndYear(Long officeId, Integer year) {
        return monthlyReportRepo.findByOfficeIdAndYear(officeId, year);
    }

    public Integer countGeneratedReportByMonthAndYear(Integer month, Integer year) {
        return monthlyReportRepo.countByMonthAndYear(month, year);
    }

    public MonthlyReport save(MonthlyReport monthlyReport) {
        return monthlyReportRepo.save(monthlyReport);
    }

    public List<MonthlyReport> save(List<MonthlyReport> allMonthlyReports) {
        return monthlyReportRepo.save(allMonthlyReports);
    }

    public MonthlyReport convertToMonthlyReport(GrievanceAndAppealMonthlyReportDTO dto) {
        MonthlyReportDTO grievanceReportDTO = dto.getMonthlyGrievanceReport();
        MonthlyReportDTO appealReportDTO = dto.getMonthlyAppealReport();
        return MonthlyReport.builder()
                .officeId(dto.getOfficeId())
                .year(dto.getYear())
                .month(dto.getMonth())
                .onlineSubmissionCount(grievanceReportDTO.getOnlineSubmissionCount())
                .conventionalMethodSubmissionCount(grievanceReportDTO.getConventionalMethodSubmissionCount())
                .selfMotivatedAccusationCount(grievanceReportDTO.getSelfMotivatedAccusationCount())
                .inheritedFromLastMonthCount(grievanceReportDTO.getInheritedFromLastMonthCount())
                .totalCount(grievanceReportDTO.getTotalCount())
                .sentToOtherCount(grievanceReportDTO.getSentToOtherCount())
                .resolvedCount(grievanceReportDTO.getResolvedCount())
                .timeExpiredCount(grievanceReportDTO.getTimeExpiredCount())
                .runningCount(grievanceReportDTO.getRunningCount())
                .resolveRate(grievanceReportDTO.getRate())
                .appealOnlineSubmissionCount(appealReportDTO.getOnlineSubmissionCount())
                .appealInheritedFromLastMonthCount(appealReportDTO.getInheritedFromLastMonthCount())
                .appealTotalCount(appealReportDTO.getTotalCount())
                .appealResolvedCount(appealReportDTO.getResolvedCount())
                .appealTimeExpiredCount(appealReportDTO.getTimeExpiredCount())
                .appealRunningCount(appealReportDTO.getRunningCount())
                .appealResolveRate(appealReportDTO.getRate())
                .createdAt(new Date())
                .build();
    }

    public GrievanceAndAppealMonthlyReportDTO convertToGrievanceAndAppealMonthlyReportDTO(MonthlyReport report, Boolean hasAppealReport) {
        Double rate = 0d;
//        Long excludeFromTotal = runningGrievanceCount + sentToOtherOfficeCount;
        Long totalDecided = report.getResolvedCount() + report.getSentToOtherCount();
        if (report.getTotalCount() > 0) {
            rate = (double) (((double) totalDecided / (double) report.getTotalCount()) * 100);
            rate = (double) Math.round(rate * 100) / 100;
        }
        MonthlyReportDTO grievanceMonthlyReportDTO = MonthlyReportDTO.builder()
                .onlineSubmissionCount(report.getOnlineSubmissionCount())
                .conventionalMethodSubmissionCount(report.getConventionalMethodSubmissionCount())
                .selfMotivatedAccusationCount(report.getSelfMotivatedAccusationCount())
                .inheritedFromLastMonthCount(report.getInheritedFromLastMonthCount())
                .totalCount(report.getTotalCount())
                .sentToOtherCount(report.getSentToOtherCount())
                .resolvedCount(report.getResolvedCount())
                .timeExpiredCount(report.getTimeExpiredCount())
                .runningCount(report.getRunningCount())
//                .rate(report.getResolveRate())
                .rate(rate)
                .build();
        MonthlyReportDTO appealMonthlyReportDTO = null;
        if(hasAppealReport) {
            rate = 0d;
//        if (totalSubmitted > runningGrievanceCount) {
//            rate = (double) ((resolvedCount / (totalSubmitted - runningGrievanceCount)) * 100);
//            rate = (double) Math.round(rate * 100) / 100;
//        }
            if (report.getAppealTotalCount() > 0) {
                rate = (double) (((double) report.getAppealResolvedCount() / ( (double) report.getAppealTotalCount())) * 100);
                rate = (double) Math.round(rate * 100) / 100;
            }
            appealMonthlyReportDTO = MonthlyReportDTO.builder()
                    .onlineSubmissionCount(report.getAppealOnlineSubmissionCount())
                    .inheritedFromLastMonthCount(report.getAppealInheritedFromLastMonthCount())
                    .totalCount(report.getAppealTotalCount())
                    .resolvedCount(report.getAppealResolvedCount())
                    .timeExpiredCount(report.getAppealTimeExpiredCount())
                    .runningCount(report.getAppealRunningCount())
//                    .rate(report.getAppealResolveRate())
                    .rate(rate)
                    .build();
        }
        return GrievanceAndAppealMonthlyReportDTO.builder()
                .officeId(report.getOfficeId())
                .officeName(report.getOfficeName())
                .year(report.getYear())
                .month(report.getMonth())
                .monthlyGrievanceReport(grievanceMonthlyReportDTO)
                .monthlyAppealReport(appealMonthlyReportDTO)
                .build();
    }
}
