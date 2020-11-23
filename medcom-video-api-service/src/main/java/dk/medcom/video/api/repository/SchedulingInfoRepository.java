package dk.medcom.video.api.repository;

import dk.medcom.video.api.dao.SchedulingInfo;
import dk.medcom.video.api.dto.ProvisionStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;


public interface SchedulingInfoRepository extends CrudRepository<SchedulingInfo, Long> {
	
	List<SchedulingInfo> findAll();
	
	public SchedulingInfo findOneByUuid(String uuid);
	
	public SchedulingInfo findOneByUriWithoutDomain(String UriWithoutDomain);
	
	@Query("SELECT s FROM SchedulingInfo s INNER JOIN s.meeting m WHERE ((s.vMRStartTime > ?1 and s.vMRStartTime < ?2) OR (m.endTime > ?1 and m.endTime < ?2)) AND s.provisionStatus = ?3")
	public List<SchedulingInfo> findAllWithinAdjustedTimeIntervalAndStatus(Date fromStartTime, Date toEndTime, ProvisionStatus provisionStatus);

	@Query("SELECT s FROM SchedulingInfo s WHERE s.vMRStartTime < ?1 AND s.provisionStatus = ?2")
	List<SchedulingInfo> findAllWithinStartTimeLessThenAndStatus(Date fromStartTime, ProvisionStatus provisionStatus);

	@Query("SELECT s FROM SchedulingInfo s INNER JOIN s.meeting m WHERE m.endTime < ?1 AND s.provisionStatus = ?2")
	List<SchedulingInfo> findAllWithinEndTimeLessThenAndStatus(Date toEndTime, ProvisionStatus provisionStatus);

	@Query(value = "SELECT s.id FROM scheduling_info s WHERE (s.organisation_id = ?1 AND s.provision_status = ?2 AND s.meetings_id IS NULL) LIMIT 1 FOR UPDATE", nativeQuery=true)
	List<BigInteger> findByMeetingIsNullAndOrganisationAndProvisionStatus(Long organisationId, String provisionStatus);

    List<SchedulingInfo> findByMeetingIsNullAndProvisionStatus(ProvisionStatus provisionStatus);
}
