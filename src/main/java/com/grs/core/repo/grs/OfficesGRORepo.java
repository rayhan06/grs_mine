package com.grs.core.repo.grs;

import com.grs.core.domain.grs.OfficesGRO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;

/**
 * Created by Acer on 08-Oct-17.
 */
@Repository
public interface OfficesGRORepo extends JpaRepository<OfficesGRO, Long> {
    public OfficesGRO findByOfficeId(Long id);

    public List<OfficesGRO> findByAppealOfficerOfficeUnitOrganogramId(Long officeUnitOrganogramId);

    public List<OfficesGRO> findByAdminOfficeUnitOrganogramId(Long officeUnitOrganogramId);

    public Integer countByOfficeId(Long officeId);

    @Query(nativeQuery = true, value = "select office_id from offices_gro where office_id in ?1")
    List<BigInteger> findGRSEnabledOfficeIdIn(List<Long> officeIdList);

    List<OfficesGRO> findByAppealOfficeIdAndAppealOfficerOfficeUnitOrganogramId(Long officeId, Long officeUnitOrganogramId);
}
