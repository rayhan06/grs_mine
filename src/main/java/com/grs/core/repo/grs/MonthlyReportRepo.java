package com.grs.core.repo.grs;

import com.grs.core.domain.grs.MonthlyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonthlyReportRepo extends JpaRepository<MonthlyReport, Long> {

    MonthlyReport findByOfficeIdAndYearAndMonth(Long officeId, Integer year, Integer month);

    List<MonthlyReport> findByOfficeIdAndYear(Long officeId, Integer year);

    Integer countByMonthAndYear(Integer month, Integer year);
}
