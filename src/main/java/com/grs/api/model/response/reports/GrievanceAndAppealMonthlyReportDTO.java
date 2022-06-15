package com.grs.api.model.response.reports;

import com.grs.api.model.response.dashboard.MonthlyGrievanceResolutionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GrievanceAndAppealMonthlyReportDTO {
    private Long officeId;
    private Integer year;
    private Integer month;
    private String officeName;
    private Integer officeLevel;
    private MonthlyReportDTO monthlyGrievanceReport;
    private MonthlyReportDTO monthlyAppealReport;
}
