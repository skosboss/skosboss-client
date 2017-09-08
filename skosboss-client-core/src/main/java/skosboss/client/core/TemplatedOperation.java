package skosboss.client.core;

import com.damnhandy.uri.template.UriTemplate;
import java.util.Map;
import skosboss.client.hydra_model.IriTemplate;

public class TemplatedOperation {
	
	private IriTemplate iriTemplate;
	private Map<String, Object> mappingProperties;
	
	public TemplatedOperation(IriTemplate iriTemplate, Map<String, Object> mappingProperties) {
		this.iriTemplate = iriTemplate;
		this.mappingProperties = mappingProperties;
	}
	
	public String getIri() {
		String templateString = iriTemplate.getTemplate();
		return UriTemplate.fromTemplate(templateString)
				.set(mappingProperties)
				.expand();
	}
}
