package skosboss.client.hydra_model;

import java.util.Set;

public class HydraClass {

	private Set<SupportedProperty> supportedProperties;

	public HydraClass(
		Set<SupportedProperty> supportedProperties
	) {
		this.supportedProperties = supportedProperties;
	}

	public Set<SupportedProperty> getSupportedProperties() {
		return supportedProperties;
	}

}
