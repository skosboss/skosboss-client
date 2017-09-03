package skosboss.client.core;

import java.util.Optional;

import org.eclipse.rdf4j.model.Model;

import skosboss.client.hydra_model.SupportedProperty;

class CycleResult {

	private Model desiredAddedDiff;
	private Optional<SupportedProperty> property;
	
	CycleResult(Model desiredAddedDiff, Optional<SupportedProperty> property) {
		this.desiredAddedDiff = desiredAddedDiff;
		this.property = property;
	}

	Model getDesiredAddedDiff() {
		return desiredAddedDiff;
	}

	Optional<SupportedProperty> getProperty() {
		return property;
	}
	
}
