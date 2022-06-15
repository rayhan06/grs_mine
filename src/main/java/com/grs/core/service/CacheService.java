package com.grs.core.service;

import com.grs.api.model.response.officeSelection.OfficeSearchDTO;
import com.grs.utils.CacheUtil;
import com.grs.utils.CalendarUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CacheService {
    @Autowired
    private OfficeService officeService;
    @Autowired
    private CalendarUtil calendarUtil;
    @Autowired
    private OfficesGroService officesGroService;

    @Scheduled(fixedDelay = 3300000, initialDelay = 0) //Default delay 55 minutes
    public void updateOfficeSearchCacheContents() {
        log.info("Started Office Search cache updating on: " + (new Date()).toString());
        List<OfficeSearchDTO> allOfficeSearchDTOs = officeService.generateOfficeSearchingData(false);

        List<Long> officeIdsInOfficesGro = this.officesGroService.findAllOffficeIds();
        List<OfficeSearchDTO> grsEnabledOfficeSearchDTOs = allOfficeSearchDTOs.stream()
                .filter(office -> officeIdsInOfficesGro.contains(office.getId()))
                .collect(Collectors.toList());

        CacheUtil.setGrsEnabledOfficeSearchDTOList(grsEnabledOfficeSearchDTOs);
        CacheUtil.setAllOfficeSearchDTOList(allOfficeSearchDTOs);
        log.info("Finished Office Search cache updating on: " + (new Date()).toString());
    }

    @Scheduled(fixedDelay = 10500000, initialDelay = 2000) //Default delay 2hr 55 minutes
    public void updateMinistryDescendantsCacheContents() {
        log.info("Started Ministry Descendants cache updating on: " + (new Date()).toString());
        Map<String, Map> ministriesDescendantIds = officeService.generateDescendantOfficesIdListOfMinistries();
        Map<Long, List> officeIdMap = ministriesDescendantIds.get("officeIds");
        Map<Long, List> originIdMap = ministriesDescendantIds.get("originIds");;
        CacheUtil.setMinistryDescendantOffices(officeIdMap);
        CacheUtil.setMinistryDescendantOfficeOrigins(originIdMap);
        log.info("Finished Ministry Descendants cache updating on: " + (new Date()).toString());
    }

    @Scheduled(fixedDelay = 33000000, initialDelay = 0)
    public void updateGovtHolidays(){
        log.info("Started Government Holidays cashe, updation on" + (new Date()).toString());
        List<Date> holidays = null;
        try {
            holidays = calendarUtil.getHolidays();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        CacheUtil.setYearlyHolidayMapping(holidays);
        log.info("Finished Government Holidays cashe, updation on" + (new Date()).toString());
    }
}
