package dk.medcom.video.api.context;

import java.util.List;
import java.util.Map;

public class SessionData {

	public Map<String,List<String>> UserAttributes;

	public Map<String, List<String>> getUserAttributes() {
		return UserAttributes;
	}

	public void setUserAttributes(Map<String, List<String>> userAttributes) {
		UserAttributes = userAttributes;
	}
	
	public String getUserAttribute(String userAttribute) {
		if (UserAttributes != null && UserAttributes.containsKey(userAttribute)) {
			if (UserAttributes.get(userAttribute).size() > 0) {
				return UserAttributes.get(userAttribute).get(0);
			}
		}
		return null;
	}
}