package skosboss.client.hydra_model;

import java.util.Set;

public class IriTemplate {

	private String template;
	private VariableRepresentation variableRepresentation;
	private Set<IriTemplateMapping> mappings;
	
	public IriTemplate(
		String template,
		VariableRepresentation variableRepresentation,
		Set<IriTemplateMapping> mappings
	) {
		this.template = template;
		this.variableRepresentation = variableRepresentation;
		this.mappings = mappings;
	}

	public String getTemplate() {
		return template;
	}

	public VariableRepresentation getVariableRepresentation() {
		return variableRepresentation;
	}

	public Set<IriTemplateMapping> getMappings() {
		return mappings;
	}

	@Override
	public String toString() {
		return "IriTemplate [template=" + template + ", variableRepresentation=" + variableRepresentation
			+ ", mappings=" + mappings + "]";
	}

}
