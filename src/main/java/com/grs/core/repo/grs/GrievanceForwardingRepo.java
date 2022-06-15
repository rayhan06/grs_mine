package com.grs.core.repo.grs;

import com.grs.core.domain.grs.Grievance;
import com.grs.core.domain.grs.GrievanceForwarding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Created by Acer on 05-Oct-17.
 */
@Repository
public interface GrievanceForwardingRepo extends JpaRepository<GrievanceForwarding, Long>, JpaSpecificationExecutor<GrievanceForwarding> {
    public List<GrievanceForwarding> findByGrievance(Grievance grievance);

    public GrievanceForwarding findTopByGrievanceOrderByIdDesc(Grievance grievance);

    public GrievanceForwarding findByGrievanceAndToOfficeIdAndToOfficeUnitOrganogramIdAndIsCurrent(Grievance grievance, Long officeId, Long officeUnitOrganogramId, Boolean isCurrent);

    @Query(value = "select * from complaint_movements as cm \n" +
            "where cm.complaint_id=?1 and (cm.`action` like '%CLOSED%' or cm.`action` like '%REJECTED%') ORDER BY id desc limit 1",
            countQuery = "select count(*) from complaint_movements as cm \n" +
                    "where cm.complaint_id=?1 and (cm.`action` like '%CLOSED%' or cm.`action` like '%REJECTED%' or cm.`action` = 'FORWARDED_TO_AO') ORDER BY id desc limit 1",
            nativeQuery = true)
    public GrievanceForwarding findRecentlyClosedOrRejectedOne(Long grievanceId);

    public GrievanceForwarding findByIsCurrentAndToOfficeIdAndToOfficeUnitOrganogramIdAndGrievance(Boolean isCurrent, Long officeId, Long OfficeUnitOrganogramId, Grievance grievance);

    public GrievanceForwarding findByActionAndToOfficeIdAndToOfficeUnitOrganogramIdAndGrievance(String action, Long officeId, Long OfficeUnitOrganogramId, Grievance grievance);

    GrievanceForwarding findByGrievanceAndIsCurrentAndIsCommitteeHead(Grievance grievance, boolean isCurrent, boolean isCommitteeHead);

    public Page<GrievanceForwarding> findAll(Specification specification, Pageable pageable);

    public List<GrievanceForwarding> findAll(Specification specification);

    GrievanceForwarding findTopByGrievanceAndActionLikeOrderByIdDesc(Grievance grievance, String action);

    List<GrievanceForwarding> findByIsCurrentAndGrievanceAndIsCommitteeMember(boolean isCurrent, Grievance grievance, boolean isCommitteeMember);

    @Query(value = "select cm.* from complaint_movements as cm where complaint_id = ?1 AND\n" +
            "(cm.to_office_id = ?2 OR cm.from_office_id = ?2) AND\n" +
            "(cm.to_office_unit_organogram_id in ?3 OR cm.from_office_unit_organogram_id in ?3)AND\n" +
            "(cm.action NOT LIKE ?4)\n" +
            " ORDER BY cm.id DESC",
            countQuery = "select count(*) from complaint_movements as cm where complaint_id = ?1 AND\n" +
                    "(cm.to_office_id = ?2 OR cm.from_office_id = ?2) AND\n" +
                    " (cm.to_office_unit_organogram_id in ?3 OR cm.from_office_unit_organogram_id in ?3) AND\n" +
                    "(cm.action NOT LIKE ?4)\n" +
                    "ORDER BY cm.id DESC",
            nativeQuery = true)
    List<GrievanceForwarding> findByGrievanceIdAndOfficeAndOfficeUnitOrganogramInAndAction(Long grievanceId, Long officeId, List<Long> officeUnitOrganogramId, String action);

    @Query(value = "select cm.* from complaint_movements as cm where complaint_id = ?1 AND\n" +
            "(cm.to_employee_record_id = cm.from_employee_record_id OR cm.assigned_role LIKE ?2)\n" +
            " ORDER BY cm.id DESC",
            countQuery = "select count(*) from complaint_movements as cm where complaint_id = ?1 AND\n" +
                    "(cm.to_employee_record_id = cm.from_employee_record_id OR cm.assigned_role LIKE ?2)\n" +
                    " ORDER BY cm.id DESC",
            nativeQuery = true)
    List<GrievanceForwarding> findByGrievanceIdAndAssignedRole(Long grievanceId, String roleName);

    @Query(value = "select cm.* from complaint_movements as cm where complaint_id = ?1 AND\n" +
            "(cm.to_employee_record_id = cm.from_employee_record_id OR cm.assigned_role LIKE ?2 OR cm.action LIKE ?3)\n" +
            " ORDER BY cm.id DESC",
            countQuery = "select count(*) from complaint_movements as cm where complaint_id = ?1 AND\n" +
                    "(cm.to_employee_record_id = cm.from_employee_record_id OR cm.assigned_role LIKE ?2)\n" +
                    " ORDER BY cm.id DESC",
            nativeQuery = true)
    List<GrievanceForwarding> findByGrievanceIdAndAssignedRoleAndAction(Long grievanceId, String roleName, String action);

    @Query(value = "select cm.* from complaint_movements as cm where complaint_id = ?1 \n" +
            " ORDER BY cm.id DESC",
            countQuery = "select count(*) from complaint_movements as cm where complaint_id = ?1 \n" +
                    " ORDER BY cm.id DESC",
            nativeQuery = true)
    List<GrievanceForwarding> findByGrievanceId(Long grievanceId);

    @Query(value = "select cm.* from complaint_movements as cm where complaint_id = ?1 AND\n" +
            "(cm.to_office_id = ?2 OR cm.from_office_id = ?2) AND\n" +
            "(cm.to_office_unit_organogram_id in ?3 OR cm.from_office_unit_organogram_id in ?3)AND\n" +
            "(cm.action NOT LIKE ?4) AND\n" +
            "(cm.created_at >= ?5 and cm.created_at <= ?6)\n" +
            " ORDER BY cm.id DESC",
            countQuery = "select count(*) from complaint_movements as cm where complaint_id = ?1 AND\n" +
                    "(cm.to_office_id = ?2 OR cm.from_office_id = ?2) AND\n" +
                    " (cm.to_office_unit_organogram_id in ?3 OR cm.from_office_unit_organogram_id in ?3) AND\n" +
                    "(cm.action NOT LIKE ?4)AND\n" +
                    "(cm.created_at >= ?5 and cm.created_at <= ?6)\n" +
                    "ORDER BY cm.id DESC",
            nativeQuery = true)
    List<GrievanceForwarding> findByGrievanceIdAndOfficeAndOfficeUnitOrganogramInAndActionAndCreatedAt(Long grievanceId, Long officeId, List<Long> officeUnitOrganogramId, String action, Date start, Date finish);

    List<GrievanceForwarding> findByGrievanceAndActionLikeOrderByIdDesc(Grievance grievance, String action);

    @Query(value = "select * from complaint_movements where complaint_id = ?1 and action like ?2 and current_status like ?3", nativeQuery = true)
    GrievanceForwarding findByGrievanceAndActionLikeAndCurrentStatusLike(Long grievanceId, String action, String currentStatus);

    @Query(value = "select * from complaint_movements where complaint_id = ?1 and action like ?2 and current_status not like ?3", nativeQuery = true)
    GrievanceForwarding findByGrievanceAndActionLikeAndCurrentStatusNotLike(Long grievanceId, String action, String currentStatus);

    List<GrievanceForwarding> findByGrievanceAndIsCurrent(Grievance grievance, Boolean isCurrent);

    @Query(value = "select *  from complaint_movements where complaint_id = ?1 group by  to_office_id, to_office_unit_organogram_id",
            countQuery = "select *  from complaint_movements where complaint_id = ?1 group by  to_office_id, to_office_unit_organogram_id",
            nativeQuery = true)
    List<GrievanceForwarding> getDistinctToEmployeeRecordIdByGrievance(Long grievanceId);

    @Query(value = "select *  from complaint_movements  where complaint_id = ?1 AND from_employee_record_id = to_employee_record_id  AND assigned_role = 'COMPLAINANT'\n" +
            "ORDER BY id DESC LIMIT 0,1",
            nativeQuery = true)
    GrievanceForwarding getLatestComplainantMovement(Long grievanceId);

    @Query(
            value = "SELECT * FROM complaint_movements WHERE \n" +
                    "( (complaint_movements.to_office_id = ?1 AND complaint_movements.to_office_unit_organogram_id = ?2) OR \n" +
                    "(complaint_movements.from_office_id = ?1 AND complaint_movements.from_office_unit_organogram_id = ?2) ) AND \n" +
                    "complaint_movements.complaint_id IN (SELECT id FROM complaints WHERE complaints.office_id = ?1) And \n" +
                    "complaint_movements.is_current=1",
            nativeQuery = true
    )
    List<GrievanceForwarding> getAllMovementsOfPreviousGRO(Long groOfficeId, Long groOfficeUnitOrganogramId);

    List<GrievanceForwarding> findByGrievanceAndActionLikeAndFromOfficeIdOrderByIdDesc(Grievance complaint, String action, Long officeId);

    @Query(value = "select * from complaint_movements cm where cm.to_office_id = ?1 and cm.to_office_unit_organogram_id = ?2 " +
            "and cm.is_current = ?3 and cm.is_cc = ?4 and cm.is_seen = ?5\n" +
            "and cm.`action` not like ('%CLOSED%') and cm.`action` not like '%APPEAL%' and cm.`action` not like '%REJECTED%'\n" +
            "and cm.complaint_id in (select id from complaints c where c.office_id = ?1 \n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\tand c.current_status not like ('%CLOSED%') \n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\tand c.current_status not like ('%REJECTED%')\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\tand c.current_status not like ('%APPEAL%'))" +
            "group by cm.complaint_id", nativeQuery = true)
    List<GrievanceForwarding> getUnseenInboxOrCCCount(Long officeId, Long officeUnitOrganogramId, Boolean isCurrent, Boolean isCC, Boolean isSeen);

    @Query(value = "select count(*) from complaint_movements cm cross join complaints c on c.id = cm.complaint_id\n" +
            "\t where cm.to_office_id = ?1 and cm.to_office_unit_organogram_id = ?2\n" +
            "            and cm.is_current = ?3 and cm.is_seen = ?4 and cm.is_cc = 0\n" +
            "            and cm.`action` not like ('%CLOSED%')\n" +
            "and cm.complaint_id in (select id from complaints cc where cc.office_id = ?1 \n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\tand cc.current_status not like ('%CLOSED%') \n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\tand cc.current_status not like ('%REJECTED%'))\n" +
            "            and c.current_appeal_office_id = ?1 and c.current_appeal_office_unit_organogram_id = ?2" , nativeQuery = true)
    Long getUnseenAppealCount(Long officeId, Long officeUnitOrganogramId, Boolean isCurrent, Boolean isSeen);
}