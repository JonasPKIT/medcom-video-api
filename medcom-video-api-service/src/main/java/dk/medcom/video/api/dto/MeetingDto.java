package dk.medcom.video.api.dto;

import java.util.Date;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

import com.fasterxml.jackson.annotation.JsonFormat;

import dk.medcom.video.api.controller.MeetingController;
import dk.medcom.video.api.controller.SchedulingInfoController;
import dk.medcom.video.api.controller.exceptions.PermissionDeniedException;
import dk.medcom.video.api.controller.exceptions.RessourceNotFoundException;
import dk.medcom.video.api.dao.Meeting;
import dk.medcom.video.api.dao.MeetingUser;

public class MeetingDto extends ResourceSupport {

	public String subject;
	public String uuid;
	public MeetingUserDto createdBy;

	//TODO Lene: MANGEL: tjek om mulighed for "strict" format så den ikke tillader år som 20181.
	//se: https://stackoverflow.com/questions/48934700/jackson-date-deserialization-invalid-day-of-month
	//kan det passe at det har noget med version af jackson at gøre?
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") 	//Date format should be: "2018-07-12T09:00:00
	public Date startTime;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")		//Date format should be: "2018-07-12T09:00:00
	public Date endTime;
	
	public String description;
	
	public MeetingDto(Meeting meeting) {
		
		subject = meeting.getSubject();
		uuid = meeting.getUuid();
		
		MeetingUser meetingUser = meeting.getMeetingUser();
		MeetingUserDto meetingUserDto = new MeetingUserDto(meetingUser);
		
		createdBy = meetingUserDto;
		startTime = meeting.getStartTime();
		endTime = meeting.getEndTime();
		description = meeting.getDescription();

		try {  //TODO Lene: hvordan håndteres dette? For linket i sig selv skaber vel ikke en exception før på evt. kaldetidspunkt
			Link selfLink = linkTo(methodOn(MeetingController.class).getMeetingByUUID(uuid)).withRel("self");
			add(selfLink);
		} catch (RessourceNotFoundException e) {
		} catch (PermissionDeniedException e) {
		}
		
		try {  //TODO Lene: hvordan håndteres dette? For linket i sig selv skaber vel ikke en exception før på evt. kaldetidspunkt
			Link schedulingInfoLink = linkTo(methodOn(SchedulingInfoController.class).getSchedulingInfoByUUID(uuid)).withRel("scheduling-info");
			add(schedulingInfoLink);
		} catch (RessourceNotFoundException e) {
		} catch (PermissionDeniedException e) {
		} 
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public void setCreatedBy(MeetingUserDto meetingUserDto) {
		this.createdBy =  meetingUserDto;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getStartTime() {
		return startTime;
	}
 
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	
	public Date getEndTime() {
		return endTime;
	}
	
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	
}
