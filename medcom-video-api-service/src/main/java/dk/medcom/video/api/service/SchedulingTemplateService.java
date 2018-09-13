package dk.medcom.video.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.medcom.video.api.context.UserContextService;
import dk.medcom.video.api.controller.exceptions.RessourceNotFoundException;
import dk.medcom.video.api.dao.SchedulingTemplate;
import dk.medcom.video.api.repository.SchedulingTemplateRepository;

@Component
public class SchedulingTemplateService {

	@Autowired
	SchedulingTemplateRepository schedulingTemplateRepository;
	
	
	@Autowired
	UserContextService userService;
	

	public SchedulingTemplate getSchedulingTemplate() throws RessourceNotFoundException{
		
		
		//userService.getUserContext().getUserOrganisation();
		

		Iterable<SchedulingTemplate> schedulingTemplates = schedulingTemplateRepository.findAll();
		
		
		if (!schedulingTemplates.iterator().hasNext()) {
			throw new RessourceNotFoundException("schedulingTemplate", "any"); //TODO Lene: MANGEL: hvad skal parm 2 være her?  //TODO Lene: MANGEL: pt returners     "path": "/api/meeting" i postman
		} else {
			SchedulingTemplate schedulingTemplate = schedulingTemplates.iterator().next();
			return schedulingTemplate;
		}
			
		
	}

}