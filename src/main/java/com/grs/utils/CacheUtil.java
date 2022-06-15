package com.grs.utils;

import com.grs.api.model.response.officeSelection.OfficeSearchDTO;

import java.util.*;

public class CacheUtil {
    public static Map<String, Object> apiAccessTokens = new HashMap();
    private static Map<Long, List> ministryDescendantOffices = new HashMap();
    private static Map<Long, List> ministryDescendantOfficeOrigins = new HashMap();
    private static List<OfficeSearchDTO> grsEnabledOfficeSearchDTOList = new ArrayList();
    private static List<OfficeSearchDTO> allOfficeSearchDTOList = new ArrayList();
    private static List<Date> yearlyHolidayMapping = new ArrayList<>();
    private static Long trackingNumber = 0L;

    public static synchronized void updateTrackingNumber(){
        trackingNumber += 1;
    }

    public static synchronized Long getTrackingNumber(){
        return trackingNumber;
    }

    public static synchronized void setTrackingNumber(Long number){
        trackingNumber = number;
    }

    public static synchronized  void setYearlyHolidayMapping(List<Date> dates){
        yearlyHolidayMapping = dates;
    }

    public static synchronized void setMinistryDescendantOffices(Map<Long, List> descendants) {
        ministryDescendantOffices = descendants;
    }

    public static synchronized void setMinistryDescendantOfficeOrigins(Map<Long, List> descendants) {
        ministryDescendantOfficeOrigins = descendants;
    }

    public static synchronized void setGrsEnabledOfficeSearchDTOList(List<OfficeSearchDTO> list) {
        grsEnabledOfficeSearchDTOList = list;
    }

    public static synchronized void setAllOfficeSearchDTOList(List<OfficeSearchDTO> list) {
        allOfficeSearchDTOList = list;
    }

    public static synchronized  List<Date> getYearlyHolidayMapping(){ return yearlyHolidayMapping; }

    public static synchronized Map<Long, List> getMinistryDescendantOffices() {
        return ministryDescendantOffices;
    }

    public static synchronized Map<Long, List> getMinistryDescendantOfficeOrigins() {
        return ministryDescendantOfficeOrigins;
    }

    public static synchronized List<OfficeSearchDTO> getGrsEnabledOfficeSearchDTOList() {
        return grsEnabledOfficeSearchDTOList;
    }

    public static synchronized List<OfficeSearchDTO> getAllOfficeSearchDTOList() {
        return allOfficeSearchDTOList;
    }
}
