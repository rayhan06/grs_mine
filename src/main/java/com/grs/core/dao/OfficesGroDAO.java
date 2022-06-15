package com.grs.core.dao;

import com.grs.api.model.response.OfficesGroDTO;
import com.grs.core.domain.grs.OfficesGRO;
import com.grs.core.domain.projapoti.Office;
import com.grs.core.repo.grs.OfficesGRORepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Acer on 08-Oct-17.
 */
@Slf4j
@Service
public class OfficesGroDAO {
    @Autowired
    private OfficeDAO officeDAO;
    @Autowired
    private OfficesGRORepo officesGRORepo;

    public OfficesGRO findOfficesGROByOfficeId(Long id) {
        return this.officesGRORepo.findByOfficeId(id);
    }

    public List<OfficesGRO> findByAppealOfficeUnitOrganogramId(Long officeUnitOrganogramId){
        return this.officesGRORepo.findByAppealOfficerOfficeUnitOrganogramId(officeUnitOrganogramId);
    }

    public List<OfficesGRO> findByAdminOfficeUnitOrganogramId(Long officeUnitOrganogramId){
        return this.officesGRORepo.findByAdminOfficeUnitOrganogramId(officeUnitOrganogramId);
    }

    public OfficesGRO save(OfficesGRO officesGRO) {
        return officesGRORepo.save(officesGRO);
    }

    public OfficesGroDTO convertToOfficesGroDTO(OfficesGRO officesGRO) {
        if(officesGRO == null || officesGRO.getOfficeId() == null) {
            return null;
        }
        Office office = this.officeDAO.findOne(officesGRO.getOfficeId());
        return OfficesGroDTO.builder()
                .id(officesGRO.getId())
                .officeId(officesGRO.getOfficeId())
                .officeName(office != null ? office.getNameBangla() : null)
                .groOfficeId(officesGRO.getGroOfficeId())
                .groOfficeUnitOrganogramId(officesGRO.getGroOfficeUnitOrganogramId())
                .appealOfficeId(officesGRO.getAppealOfficeId())
                .appealOfficerOfficeUnitOrganogramId(officesGRO.getAppealOfficerOfficeUnitOrganogramId())
                .adminOfficeId(officesGRO.getAdminOfficeId())
                .adminOfficeUnitOrganogramId(officesGRO.getAdminOfficeUnitOrganogramId())
                .isAppealOfficer(officesGRO.getIsAppealOfficer())
                .build();
    }

    public List<OfficesGRO> findAll() {
        return this.officesGRORepo.findAll();
    }

    public List<Long> getGRSEnabledOfficeIdFromOfficeIdList(List<Long> officeIdList) {
        List<BigInteger> resultList = officesGRORepo.findGRSEnabledOfficeIdIn(officeIdList);
        return resultList.stream()
                .map(BigInteger::longValue)
                .collect(Collectors.toList());
    }

    public List<OfficesGRO> findByAppealOfficeAndAppealOfficeUnitOrganogramId(Long officeId, Long officeUnitOrganogramId) {
        return this.officesGRORepo.findByAppealOfficeIdAndAppealOfficerOfficeUnitOrganogramId(officeId, officeUnitOrganogramId);
    }

    public Page<OfficesGRO> findAllOffices(Pageable pageable) {
        return this.officesGRORepo.findAll(pageable);
    }

    public OfficesGRO findOne(Long id) {
        return this.officesGRORepo.findOne(id);
    }

    public Page<OfficesGRO> findActiveOffices(Pageable pageable) {
        List<OfficesGRO> setupOffices = new ArrayList<>();
        List<OfficesGRO> officesGROes = this.findAll();
        officesGROes.forEach(officesGRO -> {
            if(officesGRO.getGroOfficeId()!= null && officesGRO.getGroOfficeUnitOrganogramId()!=null && officesGRO.getAppealOfficeId()!=null && officesGRO.getAppealOfficerOfficeUnitOrganogramId()!=null){
                setupOffices.add(officesGRO);
            }
        });
        Page<OfficesGRO> offices = new PageImpl<OfficesGRO>(setupOffices);
        return offices;
    }

    public List<OfficesGRO> findActiveOffices() {
        List<OfficesGRO> setupOffices = new ArrayList<>();
        List<OfficesGRO> officesGROes = this.findAll();
        officesGROes.forEach(officesGRO -> {
            if(officesGRO.getGroOfficeId()!= null && officesGRO.getGroOfficeUnitOrganogramId()!=null && officesGRO.getAppealOfficeId()!=null && officesGRO.getAppealOfficerOfficeUnitOrganogramId()!=null){
                setupOffices.add(officesGRO);
            }
        });
//        List<OfficesGRO> offices = new PageImpl<OfficesGRO>(setupOffices);
        return setupOffices;
    }

    public OfficesGRO findByOfficeId(Long officeId) {
        return this.officesGRORepo.findByOfficeId(officeId);
    }

}
