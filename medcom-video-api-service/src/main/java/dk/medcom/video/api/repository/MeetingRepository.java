package dk.medcom.video.api.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dk.medcom.video.api.dao.Meeting;
import dk.medcom.video.api.dao.MeetingUser;
import dk.medcom.video.api.dao.Organisation;

public interface MeetingRepository extends CrudRepository<Meeting, Long> {
	List<Meeting> findByOrganisationAndStartTimeBetween(Organisation organisation, Date startTime, Date endTime);
	
	List<Meeting> findByOrganizedByAndStartTimeBetween(MeetingUser organizedBy, Date startTime, Date endTime);
	
	Meeting findOneByUuid(String uuid);

	List<Meeting> findByOrganizedByAndSubject(MeetingUser organizedBy, String subject);

	List<Meeting> findByOrganisationAndSubject(Organisation organisation, String subject);

	List<Meeting> findByOrganisationAndOrganizedBy(Organisation userOrganisation, MeetingUser organizedBy);

	List<Meeting> findByOrganizedBy(MeetingUser organizedBy);

	@Query("select s.meeting from SchedulingInfo s inner join s.meeting m where s.uriWithDomain = ?2 and m.organisation = ?1")
	List<Meeting> findByUriWithDomainAndOrganisation(Organisation userOrganisation, String uriWithDomain);

	@Query("select s.meeting from SchedulingInfo s inner join s.meeting m where s.uriWithDomain = ?2 and m.organizedBy = ?1")
	List<Meeting> findByUriWithDomainAndOrganizedBy(MeetingUser organizedBy, String uriWithDomain);

	@Query("select l.meeting from MeetingLabel l inner join l.meeting m where l.label = ?2 and m.organisation = ?1")
    List<Meeting> findByLabelAndOrganisation(Organisation userOrganisation, String label);

	@Query("select l.meeting from MeetingLabel l inner join l.meeting m where l.label = ?2 and m.organizedBy = ?1")
	List<Meeting> findByLabelAndOrganizedBy(MeetingUser organizedBy, String label);
}
