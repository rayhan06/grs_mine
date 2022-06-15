package com.grs.api.model.response.reports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MonthlyReportDTO {
    private String officeName;
    private Long officeId;
    private Long onlineSubmissionCount;
    private Long conventionalMethodSubmissionCount;
    private Long selfMotivatedAccusationCount;
    private Long inheritedFromLastMonthCount;
    private Long totalCount;
    private Long sentToOtherCount;
    private Long resolvedCount;
    private Long timeExpiredCount;
    private Long runningCount;
    private Double rate;
}
