package com.grs.api.controller;

import com.grs.api.model.UserInformation;
import com.grs.api.model.request.AddCentralDashboardRecipientDTO;
import com.grs.api.model.response.CentralDashboardRecipientDTO;
import com.grs.api.model.response.NudgeableGrievanceDTO;
import com.grs.api.model.response.RegisterDTO;
import com.grs.api.model.response.dashboard.*;
import com.grs.api.model.response.dashboard.latest.ComplaintsCountByTypeDTO;
import com.grs.api.model.response.dashboard.latest.GRSStatisticDTO;
import com.grs.api.model.response.dashboard.latest.SubOfficesStatisticsDTO;
import com.grs.api.model.response.dashboard.latest.TotalResolvedByMonth;
import com.grs.core.dao.GRSStatisticsDAO;
import com.grs.core.domain.grs.Tagid;
import com.grs.core.service.CellService;
import com.grs.core.service.DashboardService;
import com.grs.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private GRSStatisticsDAO grsStatisticsDAO;
    @Autowired
    private CellService cellService;

    @GetMapping("/office/{office_id}")
    public DashboardDataDTO getDashboardData(Authentication authentication, @PathVariable("office_id") Long officeId) {
        UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
        DashboardDataDTO dashboardDataDTO = DashboardDataDTO.builder()
                .groDashboardData(dashboardService.getGrievanceDataForGRODashboard(officeId, true))
                .build();
        if(dashboardService.hasAccessToAoAndSubOfficesDashboard(userInformation, officeId)) {
            dashboardDataDTO.setAoDashboardData(dashboardService.getGrievanceDataForAODashboard(officeId, true));
            if(!userInformation.getIsMobileLogin()) {
                dashboardDataDTO.setListOfChildOfficesData(dashboardService.getListOfChildOffices(officeId));
            }
        }
        return dashboardDataDTO;
    }

    @GetMapping("/office/{office_id}/grs-enabled-child-offices")
    public List<ChildOfficesDashboardNavigatorDTO> getListOfChildOffices(@PathVariable("office_id") Long officeId) {
        List<ChildOfficesDashboardNavigatorDTO> childOffices = dashboardService.getListOfChildOffices(officeId);
        return (childOffices != null && childOffices.size() > 0) ? childOffices : new ArrayList();
    }

    @GetMapping("/office/{office_id}/gro-aggregated-data")
    public GeneralDashboardDataDTO getAggregatedDataForGRO(@PathVariable("office_id") Long officeId) {
        return dashboardService.getSubOfficesAggregatedDataForGrievances(officeId);
    }

    @GetMapping("/office/{office_id}/ao-aggregated-data")
    public GeneralDashboardDataDTO getAggregatedDataForAO(@PathVariable("office_id") Long officeId) {
        return dashboardService.getSubOfficesAggregatedDataForAppeals(officeId);
    }

    @GetMapping("/office/{office_id}/grievances/medium-of-submission")
    public NameValuePairListDTO getGrievancesMediumOfSubmissionCounts(@PathVariable("office_id") Long officeId) {
        return NameValuePairListDTO.builder()
                .nameValuePairList(dashboardService.getGrievanceCountByMediumOfSubmission(officeId, 0L))
                .build();
    }

    @GetMapping("/office/{office_id}/appeals/medium-of-submission")
    public NameValuePairListDTO getAppealsMediumOfSubmissionCounts(@PathVariable("office_id") Long officeId) {
        return NameValuePairListDTO.builder()
                .nameValuePairList(dashboardService.getAppealCountByMediumOfSubmission(officeId, 0L))
                .build();
    }

    @GetMapping("/office/{office_id}/grievances/compare-with-last-month")
    public CompareWithLastMonthDTO getGrievancesComparisonDataWithLastMonth(@PathVariable("office_id") Long officeId) {
        return CompareWithLastMonthDTO.builder()
                .thisMonth(dashboardService.getMonthAndTypeWiseCountOfGrievanceByOfficeIdWithMonthDiff(officeId, 0))
                .lastMonth(dashboardService.getMonthAndTypeWiseCountOfGrievanceByOfficeIdWithMonthDiff(officeId, -1))
                .build();
    }

    @GetMapping("/office/{office_id}/appeals/compare-with-last-month")
    public CompareWithLastMonthDTO getAppealsComparisonDataWithLastMonth(@PathVariable("office_id") Long officeId) {
        return CompareWithLastMonthDTO.builder()
                .thisMonth(dashboardService.getMonthAndTypeWiseCountOfAppealByOfficeIdWithMonthDiff(officeId, 0))
                .lastMonth(dashboardService.getMonthAndTypeWiseCountOfAppealByOfficeIdWithMonthDiff(officeId, -1))
                .build();
    }

    @GetMapping("/office/{office_id}/grievances/count-by-sections")
    public ListOfGrievanceCountByItemDTO getGrievanceCountBySections(@PathVariable("office_id") Long officeId) {
        return ListOfGrievanceCountByItemDTO.builder()
                .list(dashboardService.countGrievanceOfAnOfficeByOfficeUnit(officeId))
                .build();
    }

    @GetMapping("/office/{office_id}/grievances/count-by-services")
    public ListOfGrievanceCountByItemDTO getGrievanceCountByServices(@PathVariable("office_id") Long officeId) {
        return ListOfGrievanceCountByItemDTO.builder()
                .list(dashboardService.countGrievanceOfAnOfficeByService(officeId))
                .build();
    }

    @GetMapping("/office/{office_id}/appeals/count-by-child-offices")
    public ListOfAppealCountByChildOfficesDTO getAppealsCountOfChildOffices(@PathVariable("office_id") Long officeId) {
        return ListOfAppealCountByChildOfficesDTO.builder()
                .list(dashboardService.getCountOfAppealsBySourceOffices(officeId))
                .build();
    }

    @GetMapping("/office/{office_id}/grievances/resolution-statistics")
    public GrievanceResolutionStatisticsDTO getStatisticsOfGrievanceResolution(@PathVariable("office_id") Long officeId) {
        return GrievanceResolutionStatisticsDTO.builder()
                .resolutionTypeInfo(dashboardService.getResolutionTypeInfo(officeId))
                .unacceptedGrievancesCounts(dashboardService.getUnacceptedGrievancesInfo(officeId))
                .currentMonthResolutions(dashboardService.getResolutionsInCurrentMonth(officeId, 0L))
                .build();
    }

    @GetMapping("/office/{office_id}/grievances/resolution-types")
    public ResolutionTypeInfoDTO getTypeOfGrievanceResolution(@PathVariable("office_id") Long officeId) {
        return dashboardService.getResolutionTypeInfo(officeId);
    }

    @GetMapping("/office/{office_id}/grievances/declined-counts")
    public UnacceptedGrievancesCountDTO getStatisticsOfDeclinedGrievances(@PathVariable("office_id") Long officeId) {
        return dashboardService.getUnacceptedGrievancesInfo(officeId);
    }

    @GetMapping("/office/{office_id}/grievances/current-month-resolution")
    public List<MonthlyGrievanceResolutionDTO> getGrievanceResolutionsOfCurrentMonth(@PathVariable("office_id") Long officeId) {
        return dashboardService.getResolutionsInCurrentMonth(officeId, 0L);
    }

    @GetMapping("/office/{office_id}/appeals/resolution-statistics")
    public List<MonthlyGrievanceResolutionDTO> getStatisticsOfAppealResolution(@PathVariable("office_id") Long officeId) {
        return dashboardService.getAppealResolutionsInCurrentMonth(officeId, 0L);
    }

    @GetMapping("/office/{office_id}/grievances/expired")
    public List<NudgeableGrievanceDTO> getExpiredGrievancesByOffice(@PathVariable("office_id") Long officeId) {
        return dashboardService.getTimeExpiredGrievanceDTOList(officeId);
    }

    @GetMapping("/office/{office_id}/appeals/expired")
    public List<NudgeableGrievanceDTO> getExpiredAppealsByOffice(@PathVariable("office_id") Long officeId) {
        return dashboardService.getTimeExpiredAppealDTOList(officeId);
    }

    @GetMapping("/central-data")
    public CentralDashboardDataDTO getCentralDashboardData() {
        return dashboardService.getCentralDashboardData();
    }

    @GetMapping("/yearly-data")
    public YearlyCounts getYearlyCount(){
        return dashboardService.getYearlyCounts();
    }

    @GetMapping("/central-dashboard/services/{service_id}/count-by-office")
    public List<ItemIdNameCountDTO> getServicesCountWithOfficeNameByServiceId(@PathVariable("service_id") Long serviceId) {
        return dashboardService.getServicesCountWithOfficeNameByServiceId(serviceId);
    }

    @GetMapping("/central-dashboard/count-by-all-ministries")
    public List<TotalAndResolvedCountDTO> getTotalSubmittedAndResolvedCountsOfMinistries() {
        return dashboardService.getTotalSubmittedAndResolvedCountsOfMinistries();
    }

    @GetMapping("/central-dashboard/subordinate-stat-all-ministries")
    public List<TotalAndResolvedCountDTO> getSubordinateTotalSubmittedAndResolvedCountsOfMinistries() {
        return dashboardService.getSubordinateTotalSubmittedAndResolvedCountsOfMinistries();
    }

    @GetMapping("/central-dashboard-recipients")
    public List<CentralDashboardRecipientDTO> getAllCentralDashboardRecipients() {
        return dashboardService.getAllCentralDashboardRecipients();
    }

    @PostMapping("/central-dashboard-recipients")
    public CentralDashboardRecipientDTO addNewCentralDashboardRecipients(
            @RequestBody AddCentralDashboardRecipientDTO dashboardRecipientDTO) {
        return dashboardService.addNewCentralDashboardRecipients(dashboardRecipientDTO);
    }

    @PutMapping("/central-dashboard-recipients/{id}/status/{status}")
    public Boolean changeCentralDashboardRecipientStatus(@PathVariable("id") Long id, @PathVariable("status") Boolean status) {
        return dashboardService.changeCentralDashboardRecipientStatus(id, status);
    }

    @DeleteMapping("/central-dashboard-recipients/{id}")
    public Boolean deleteCentralDashboardRecipient(@PathVariable("id") Long id) {
        return dashboardService.deleteCentralDashboardRecipient(id);
    }

    @GetMapping("/offices/{office_id}/current-month-register")
    public List<RegisterDTO> getDashboardDataForGrievanceRegister(@PathVariable("office_id") Long officeId) {
        return dashboardService.getDashboardDataForGrievanceRegister(officeId);
    }

    @GetMapping("/offices/{office_id}/current-month-appeal-register")
    public List<RegisterDTO> getDashboardDataForAppealRegister(@PathVariable("office_id") Long officeId) {
        return dashboardService.getDashboardDataForAppealRegister(officeId);
    }

    @GetMapping("/offices/{office_id}/register")
    public Page<RegisterDTO> getPaginatedDashboardDataForGrievanceRegister(@PathVariable("office_id") Long officeId, Pageable pageable) {
        return dashboardService.getPageableDashboardDataForGrievanceRegister(officeId, pageable);
    }

    @GetMapping("/offices/{office_id}/appeal-register")
    public Page<RegisterDTO> getPaginatedDashboardDataForAppealRegister(@PathVariable("office_id") Long officeId, Pageable pageable) {
        return dashboardService.getPageableDashboardDataForAppealRegister(officeId, pageable);
    }

    @GetMapping("/offices/{office_id}/appealed-complaints")
    public Page<RegisterDTO> getPaginatedDashboardDataForAppealedComplaints(@PathVariable("office_id") Long officeId, Pageable pageable) {
        return dashboardService.getPageableDashboardDataForAppealedComplaints(officeId, pageable);
    }

    @GetMapping("/tagid/list")
    public Page<NudgeDTO> getPaginatedDashboardDataForTagidList(Authentication authentication, Pageable pageable) {
        UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
        return dashboardService.getPageableDashboardDataForTagidList(userInformation, pageable);
    }

    /***************    New HOO Dashboard    *******************/

    @GetMapping("/offices/{office_id}/hoo-dashboard-data")
    public GRSStatisticDTO getCurrentMonthGRSStatisticsDTO(Authentication authentication, @PathVariable("office_id") Long officeId) {
        UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        return grsStatisticsDAO.getGRSStatistics(userInformation, officeId, year, month);
    }

    @GetMapping("/offices/{office_id}/hoo-child-offices-data")
    public List<SubOfficesStatisticsDTO> getChildOfficesStatisticsForCurrentMonth(@PathVariable("office_id") Long officeId) {
        return grsStatisticsDAO.getCurrentMonthChildOfficesStatistics(officeId);
    }

    @GetMapping("/cell-dashboard-data")
    public GRSStatisticDTO getCabinetCellStatisticsForCurrentMonth(Authentication authentication) {
        try {
            if(!Utility.isCellGRO(authentication)) {
                throw new Exception("Access denied for viewing cell dashboard data");
            }
            return grsStatisticsDAO.getGRSStatisticsOfCabinetCell();
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @GetMapping("/offices/{office_id}/grievances/count-by-status")
    public GeneralDashboardDataDTO getGrievanceCountByCurrentStatus(@PathVariable("office_id") Long officeId) {
        return dashboardService.getGrievanceDataForGRODashboard(officeId, false);
    }

    @GetMapping("/offices/{office_id}/grievances/current-year-total-resolved")
    public List<TotalResolvedByMonth> getTotalResolvedGrievancesByMonthOfCurrentYear(@PathVariable("office_id") Long officeId) {
        return dashboardService.getTotalResolvedGrievancesByMonthOfCurrentYear(officeId);
    }

    @GetMapping("/offices/{office_id}/appeals/count-by-status")
    public GeneralDashboardDataDTO getAppealCountByCurrentStatus(@PathVariable("office_id") Long officeId) {
        return dashboardService.getGrievanceDataForAODashboard(officeId, false);
    }

    @GetMapping("/offices/{office_id}/appeals/current-year-total-resolved")
    public List<TotalResolvedByMonth> getTotalResolvedAppealsByMonthOfCurrentYear(@PathVariable("office_id") Long officeId) {
        return dashboardService.getTotalResolvedAppealsByMonthOfCurrentYear(officeId);
    }

    @GetMapping("/field-coordinator-data")
    public List<SubOfficesStatisticsDTO> getGrievanceRelatedStatisticsForFieldCoordinator(Authentication authentication) {
        return grsStatisticsDAO.getOfficesStatisticsForFieldCoordinator(authentication);
    }

    @GetMapping("/field-coordinator-appeal-data")
    public ComplaintsCountByTypeDTO getAppealRelatedStatisticsForFieldCoordinator(Authentication authentication) {
        UserInformation userInformation = Utility.extractUserInformationFromAuthentication(authentication);
        return grsStatisticsDAO.getDivisionAppealStatisticsForFieldCoordinator(userInformation);
    }
}
