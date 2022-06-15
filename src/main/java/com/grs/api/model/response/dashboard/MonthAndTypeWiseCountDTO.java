package com.grs.api.model.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MonthAndTypeWiseCountDTO {
    String year;
    String month;
    TotalAndResolvedCountDTO nagorikCounts;
    TotalAndResolvedCountDTO daptorikCounts;
    TotalAndResolvedCountDTO staffCounts;
}
