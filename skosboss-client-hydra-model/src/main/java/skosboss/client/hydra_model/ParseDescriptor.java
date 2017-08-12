package skosboss.client.hydra_model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
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
	
	Map<SupportedProperty, IriTemplate> run() {
		
		// find instance of hydra:ApiDocumentation
		Resource doc = findApiDocumentation();
		
		// get entry point of hydra:ApiDocumentation instance
		Resource entryPoint = getEntryPoint(doc);
		System.out.println(entryPoint);
		
		// determine hydra classes the entry point is an instance of
		Set<Resource> classes = determineHydraClasses(entryPoint);
		System.out.println("hydra classes of entrypoint:\n" + classes);
		
		// gather all properties (hydra:supportedProperty) of said classes
		Set<Resource> propertyResources = gatherSupportedProperties(classes);
		System.out.println("hydra properties of entry point classes:");
		System.out.println(propertyResources);
		
		// parse 'properties' into SupportedProperty instances
		List<SupportedProperty> properties =
			propertyResources.stream()
				.map(this::parseSupportedProperty)
				.collect(Collectors.toList());
		
		properties.forEach(System.out::println);
		
		// parse iri templates of the entry point that
		// correspond to the properties defined on its hydra class.
		Map<SupportedProperty, IriTemplate> templates =
			parseTemplates(entryPoint, properties);
		
		System.out.println(templates);
		
		return templates;
	}
	
	private IriTemplate parseIriTemplate(Resource resource) {
		
		String template = getStringProperty(resource, Rdf.Hydra.template, "");
		
		if (!model.contains(
			resource,
			Rdf.Hydra.variableRepresentation,
			Rdf.Hydra.BasicRepresentation
		))
			throw new RuntimeException("resource " + resource + " does not have "
				+ "hydra:BasicRepresentation as its hydra:variableRepresentation");
		
		Set<IriTemplateMapping> mappings =
			Models.objectResources(
				model.filter(resource, Rdf.Hydra.mapping, null)
			)
			.stream().map(this::parseMapping)
			.collect(Collectors.toSet());
		
		return new IriTemplate(
			template,
			VariableRepresentation.BASIC_REPRESENTATION,
			mappings
		);
	}
	
	private IriTemplateMapping parseMapping(Resource resource) {
		boolean required = getBooleanProperty(resource, Rdf.Hydra.required, false);
		String variable = getStringProperty(resource, Rdf.Hydra.variable, "");
		IRI property = getIriProperty(resource, Rdf.Hydra.property, null);
		return new IriTemplateMapping(variable, property, required);
	}
	
	private Map<SupportedProperty, IriTemplate> parseTemplates(
		Resource entryPoint,
		List<SupportedProperty> properties
	) {
		return
		properties.stream().collect(Collectors.toMap(
			p -> p,
			p -> {
				IRI predicate = (IRI) p.getProperty().getResource();
				return
				Models.objectResource(
					model.filter(entryPoint, predicate, null)
				)
				.map(this::parseIriTemplate)
				.orElseThrow(() -> new RuntimeException("could not parse entrypoint "
					+ "object of predicate " + predicate + " as IriTemplate"));
			}
		));
	}

	private <T> T getProperty(
		Resource subject,
		IRI predicate,
		Function<Value, T> extract,
		T defaultValue
	) {
		return
			Models.object(model.filter(subject, predicate, null))
			.map(extract).orElse(defaultValue);
	}
	
	private boolean getBooleanProperty(
		Resource subject,
		IRI predicate,
		boolean defaultValue
	) {
		return getProperty(
			subject,
			predicate,
			v -> ((Literal) v).booleanValue(),
			defaultValue
		);
	}
	
	private IRI getIriProperty(
		Resource subject,
		IRI predicate,
		IRI defaultValue
		) {
		return getProperty(
			subject,
			predicate,
			v -> (IRI) v,
			defaultValue
		);
	}
	
	@SuppressWarnings("unused")
	private Resource getResourceProperty(
		Resource subject,
		IRI predicate,
		Resource defaultValue
	) {
		return getProperty(
			subject,
			predicate,
			v -> (Resource) v,
			defaultValue
		);
	}
	
	private Shape getShapeProperty(
		Resource subject,
		IRI predicate,
		Shape defaultValue
	) {
		return getProperty(
			subject,
			predicate,
			v -> new Shape(model, (Resource) v),
			defaultValue
		);
	}
	
	private String getStringProperty(
		Resource subject,
		IRI predicate,
		String defaultValue
	) {
		return getProperty(
			subject,
			predicate,
			v -> ((Literal) v).stringValue(),
			defaultValue
		);
	}
	
	private int getIntegerProperty(
		Resource subject,
		IRI predicate,
		int defaultValue
	) {
		return getProperty(
			subject,
			predicate,
			v -> ((Literal) v).intValue(),
			defaultValue
		);
	}
	
	private StatusCode parseStatusCode(Resource resource) {
		String description = getStringProperty(resource, Rdf.Hydra.description, "");
		int code = getIntegerProperty(resource, Rdf.Hydra.code, 0);
		return new StatusCode(code, description);
	}
	
	private ExtOperation parseExtOperation(Resource resource) {
		
		String method = getStringProperty(resource, Rdf.Hydra.method, "");
		
		Set<StatusCode> statusCodes =
			Models.objectResources(model.filter(resource, Rdf.Hydra.statusCodes, null))
				.stream().map(this::parseStatusCode)
				.collect(Collectors.toSet());
		
		Shape returnShape = getShapeProperty(resource, Rdf.HydraExt.returnShape, null);
		Shape addedDiff = getShapeProperty(resource, Rdf.HydraExt.returnShape, null);
		Shape deletedDiff = getShapeProperty(resource, Rdf.HydraExt.returnShape, null);
		
		return new ExtOperation(
			null,
			method,
			statusCodes,
			null, // TODO returns
			returnShape,
			addedDiff,
			deletedDiff
		);
	}
	
	private TemplatedLink parseTemplatedLink(Resource resource) {
		
		ExtOperation operation =
			Models.objectResource(
				model.filter(resource, Rdf.Hydra.supportedOperation, null)
			)
			.map(this::parseExtOperation)
			.orElseThrow(() -> new RuntimeException("templated link " + resource
				+ " does not have a hydra:supportedOperation"));
		
		return new TemplatedLink(resource, operation);
	}
	
	private Property parseProperty(Resource resource) {
		
		Map<IRI, Function<Resource, Property>> parserMap = new LinkedHashMap<>();

		parserMap.put(Rdf.Hydra.TemplatedLink, this::parseTemplatedLink);
		
		return
			model.filter(resource, RDF.TYPE, null)
				.objects().stream()
				.filter(v -> parserMap.containsKey(v))
				.map(v -> parserMap.get(v))
				.map(p -> p.apply(resource))
				.findFirst()
				.orElseThrow(() ->
					new RuntimeException(
						"resource " + resource + " does not have an rdf:type that is "
						+ "a supported sub-type of rdf:Property, such as hydra:TemplatedLink")
				);
	}
	
	private SupportedProperty parseSupportedProperty(Resource resource) {
		boolean readable = getBooleanProperty(resource, Rdf.Hydra.readable, false);
		boolean writable = getBooleanProperty(resource, Rdf.Hydra.writable, false);
		Property property =
			Models.object(model.filter(resource, Rdf.Hydra.property, null))
				.map(v -> (Resource) v).map(this::parseProperty)
				.orElse(null);
		String title = getStringProperty(resource, Rdf.Hydra.title, "");
		return new SupportedProperty(property, readable, writable, title);
	}
	
	private Set<Resource> gatherSupportedProperties(Set<Resource> classes) {
		return
		classes.stream().flatMap(c ->
		model
			.filter(c, Rdf.Hydra.supportedProperty, null).objects().stream()
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
			model.filter(doc, Rdf.Hydra.entrypoint, null)
		)
		.orElseThrow(() ->
			new RuntimeException("resource " + doc + " has no hydra:entrypoint")
		);
	}
	
	private Resource findApiDocumentation() {
		return
		Models.subject(
			model.filter(null, RDF.TYPE, Rdf.Hydra.ApiDocumentation)
		)
		.orElseThrow(() ->
			new RuntimeException("no instance of hydra:ApiDocumentation found")
		);
	}
	
}
