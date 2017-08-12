package skosboss.client.hydra_model;

import org.eclipse.rdf4j.model.Resource;

public class TemplatedLink implements Property {

	private Resource resource;
	private Operation supportedOperation;

	public TemplatedLink(
		Resource resource,
		Operation supportedOperation
	) {
		this.resource = resource;
		this.supportedOperation = supportedOperation;
	}

	@Override
	public Resource getResource() {
		return resource;
	}

	public Operation getSupportedOperation() {
		return supportedOperation;
	}

	@Override
	public String toString() {
		return "TemplatedLink [resource=" + resource + ", supportedOperation=" + supportedOperation + "]";
	}

}
