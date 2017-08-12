package skosboss.client.hydra_model;

import org.eclipse.rdf4j.model.IRI;

public class IriTemplateMapping {

	private String variable;
	private IRI property;
	private boolean required;
	
	public IriTemplateMapping(String variable, IRI property, boolean required) {
		this.variable = variable;
		this.property = property;
		this.required = required;
	}

	public String getVariable() {
		return variable;
	}

	public IRI getProperty() {
		return property;
	}

	public boolean isRequired() {
		return required;
	}

	@Override
	public String toString() {
		return "IriTemplateMapping [variable=" + variable + ", property=" + property + ", required=" + required + "]";
	}

}
