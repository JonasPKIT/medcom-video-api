//TODO: Database indexes matching requests?
package dk.medcom.video.api.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.medcom.video.api.dao.Meeting;
import dk.medcom.video.api.dao.MeetingUser;
import dk.medcom.video.api.dao.Organisation;

public interface MeetingRepository extends CrudRepository<Meeting, Long> {
	List<Meeting> findByOrganisationAndStartTimeBetween(Organisation organisation, Date startTime, Date endTime);
	
	List<Meeting> findByOrganizedByAndStartTimeBetween(MeetingUser organizedBy, Date startTime, Date endTime);
	
	Meeting findOneByUuid(String uuid);
}
