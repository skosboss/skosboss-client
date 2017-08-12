package skosboss.client.hydra_model;

import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;

class ParseDescriptor {

	private static final ValueFactory f = SimpleValueFactory.getInstance();
	
	private Model model;

	ParseDescriptor(Model model) {
		this.model = model;
	}
	
	void run() {
		

		// find instance of hydra:ApiDocumentation
		Resource doc = findApiDocumentation();
		
		// get entry point of hydra:ApiDocumentation instance
		Resource entryPoint = getEntryPoint(doc);
		System.out.println(entryPoint);
		
		// determine hydra classes the entry point is an instance of
		Set<Resource> classes = determineHydraClasses(entryPoint);
		System.out.println("hydra classes of entrypoint:\n" + classes);
		
		// gather all properties (hydra:supportedProperty) of said classes
		Set<Resource> properties = gatherSupportedProperties(classes);
		System.out.println("hydra properties of entry point classes:");
		System.out.println(properties);
		
		
		
	}
	
	private Set<Resource> gatherSupportedProperties(Set<Resource> classes) {
		return
		classes.stream().flatMap(c ->
		model
			.filter(c, f.createIRI(Hydra.supportedProperty), null).objects().stream()
			.map(o -> (Resource) o)
		)
		.collect(Collectors.toSet());
	}

	private Set<Resource> determineHydraClasses(Resource entryPoint) {
		
		Set<Resource> classes =
			model
				.filter(entryPoint, RDF.TYPE, null).objects().stream()
				.map(o -> (Resource) o)
				.collect(Collectors.toSet());
		
		return
		classes.stream()
			.filter(c -> model.contains(c, RDF.TYPE, f.createIRI(Hydra.Class)))
			.collect(Collectors.toSet());
	}

	private Resource getEntryPoint(Resource doc) {
		return (Resource)
		Models.object(
			model.filter(doc, f.createIRI(Hydra.entrypoint), null)
		)
		.orElseThrow(() ->
			new RuntimeException("resource " + doc + " has no hydra:entrypoint")
		);
	}
	
	private Resource findApiDocumentation() {
		return
		Models.subject(
			model.filter(null, RDF.TYPE, f.createIRI(Hydra.ApiDocumentation))
		)
		.orElseThrow(() ->
			new RuntimeException("no instance of hydra:ApiDocumentation found")
		);
	}
	
}
