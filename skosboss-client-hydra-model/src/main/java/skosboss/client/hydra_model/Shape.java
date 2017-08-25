package skosboss.client.hydra_model;

import org.eclipse.rdf4j.model.Model;

public class Shape {

	private Model model;
	
	public Shape(Model model) {
		this.model = model;
	}

	public Model getModel() {
		return model;
	}

	@Override
	public String toString() {
		return "Shape [model=" + model + "]";
	}
	
}
