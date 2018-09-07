//TODO Lene: TASK: valider input længde med anotering
//TODO Lene: TASK: valider input not null
//TODO Lene: TASK: Formatering af dato gøres med anotering - måske den som bruges allerede kan bruges
package dk.medcom.video.api.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

public class MeetingDto {

	public String subject;
	public String uuid;
	public MeetingUserDto createdBy;

	//TODO Lene: MANGEL: tjek om mulighed for "strict" format så den ikke tillader år som 20181
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") 	//Date format should be: "2018-07-12T09:00:00
	public Date startTime;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")		//Date format should be: "2018-07-12T09:00:00
	public Date endTime;
	
	public String description;

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
