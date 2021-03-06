package dk.medcom.video.api.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class UpdateSchedulingInfoDto {
	 
	@NotNull
	private ProvisionStatus provisionStatus;
	@Size(max=200, message="provision status description should have a maximum of 200 characters")
	private String provisionStatusDescription;
	private String provisionVmrId;

	public UpdateSchedulingInfoDto() {	
	}
	
	public ProvisionStatus getProvisionStatus() {
		return provisionStatus;
	}

	public void setProvisionStatus(ProvisionStatus provisionStatus) {
		this.provisionStatus = provisionStatus;
	}
	
	public String getProvisionStatusDescription() {
		return provisionStatusDescription;
	}

	public void setProvisionStatusDescription(String provisionStatusDescription) {
		this.provisionStatusDescription = provisionStatusDescription;
	}

	public String getProvisionVmrId() {
		return provisionVmrId;
	}

	public void setProvisionVmrId(String provisionVmrId) {
		this.provisionVmrId = provisionVmrId;
	}
	
}
