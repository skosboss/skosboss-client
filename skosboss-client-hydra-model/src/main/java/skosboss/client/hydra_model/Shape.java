package skosboss.client.hydra_model;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;

public class Shape {

	private Model model;
	private Resource resource;
	
	public Shape(Model model, Resource resource) {
		this.model = model;
		this.resource = resource;
	}

	public Model getModel() {
		return model;
	}

	public Resource getResource() {
		return resource;
	}

	@Override
	public String toString() {
		return "Shape [model=" + model + ", resource=" + resource + "]";
	}
	
}
