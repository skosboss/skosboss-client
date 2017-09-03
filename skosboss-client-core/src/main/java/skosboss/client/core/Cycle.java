package skosboss.client.core;

import java.io.StringWriter;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;

import skosboss.client.hydra_model.ExtOperation;
import skosboss.client.hydra_model.IriTemplate;
import skosboss.client.hydra_model.Operation;
import skosboss.client.hydra_model.Property;
import skosboss.client.hydra_model.Shape;
import skosboss.client.hydra_model.SupportedProperty;
import skosboss.client.hydra_model.TemplatedLink;
import skosboss.client.shacl.ParseShaclResult;
import skosboss.client.shacl.Shacl;
import skosboss.client.shacl.ShaclValidator;
import skosboss.client.shacl.ValidationReport;

class Cycle {

	private Model desiredAddedDiff;
	private Set<SupportedProperty> properties;
	private Map<SupportedProperty, IriTemplate> templates;
	private ShaclValidator validator;
	
	Cycle(
		Model desiredAddedDiff,
		Set<SupportedProperty> properties,
		Map<SupportedProperty, IriTemplate> templates,
		ShaclValidator validator
	) {
		this.desiredAddedDiff = desiredAddedDiff;
		this.properties = properties;
		this.templates = templates;
		this.validator = validator;
	}

	private Optional<ExtOperation> getOperation(SupportedProperty p) {

		Property property = p.getProperty();
		if (!(property instanceof TemplatedLink))
			return Optional.empty();
		
		System.out.println("> prop " + property.getResource());
		
		TemplatedLink link = (TemplatedLink) property;
		Operation operation = link.getSupportedOperation();
		if (!(operation instanceof ExtOperation))
			return Optional.empty();
		
		return Optional.of((ExtOperation) operation);
	}
	
	private Optional<Model> determinePropertyAddedTriples(SupportedProperty p) {
		
		IriTemplate template = templates.get(p);
		if (!areTemplatePropertyValuesPresent(template))
			return Optional.empty();
		
		Optional<ExtOperation> extOperation = getOperation(p);
		if (!extOperation.isPresent()) return Optional.empty();;
		
		Shape addedDiffShape = extOperation.get().getAddedDiff();
		
		if (addedDiffShape == null) {
			System.out.println("op has NO added diff");
			return Optional.empty();
		}
		
		Model addedDiff = addedDiffShape.getModel();
		System.out.println("op has added diff");
		
		Model added = determineAddedTriples(addedDiff);
		
		return Optional.of(added);
	}
	
	/**
	 * Returns true if property values are present for all properties used
	 * in the template's mappings, or false otherwise.
	 * @param template
	 * @return
	 */
	private boolean areTemplatePropertyValuesPresent(IriTemplate template) {
		return template.getMappings().stream()
			.map(m -> m.getProperty())
			.map(p -> getPropertyValue(p))
			.allMatch(v -> v.isPresent());
	}
	
	private Optional<Object> getPropertyValue(IRI property) {
		
		// TODO
		
		return Optional.of("(((empty)))");
	}
	
	private Model determineAddedTriples(Model addedDiff) {
		
		// check if 'addedDiff' shape matches 'desiredAddedDiff' graph
		Model result = validator.validate(desiredAddedDiff, addedDiff);
//		printShaclResult(result);
		ValidationReport report = ParseShaclResult.create(result).get();
		
		IRI targetClass = getTargetClass(addedDiff);
		System.out.println("TARGET CLASS: " + targetClass);
		
		Model added = new DetermineAddedSubModel(desiredAddedDiff, report, targetClass).get();
		
		System.out.println("$$$$$$ TRIPLES THAT WOULD BE ADDED BY EXECUTING THIS $$$$$$");
		printModel(added);
		System.out.println("$$$$$$ ============================================= $$$$$$");
		
		return added;
	}
	
	private IRI getTargetClass(Model shape) {
		return
		Models.objectIRI(
			shape.filter(null,  Shacl.targetClass, null)
		)
		.orElseThrow(() ->
			new RuntimeException("shacl shape must have a target class")
		);
	}
	
	CycleResult run() {
		
		// TODO make it so we can consider multiple paths;
		// so instead of just picking 'best property' here,
		// ALL properties (that qualify) must be an option.
		
		return
			
		getBestProperty().map(p -> {
			
			// create new state
			
			Model newDesiredAddedDiff = new LinkedHashModel(desiredAddedDiff);
			
			p.getRight().forEach(newDesiredAddedDiff::remove);
			
			// TODO add stuff from Pano here
			
			System.out.println("SELECTED PROPERTY " + p.getLeft().getTitle());
			
			System.out.println("*** REMAINING 'desired added diff': ***");
			printModel(newDesiredAddedDiff);
			System.out.println("***************************************");
			
			return new CycleResult(
				newDesiredAddedDiff,
				Optional.of(p.getLeft())
			);
		})
		
		.orElse(new CycleResult(
			desiredAddedDiff,
			Optional.empty()
		));
	}
	
	private Optional<Pair<SupportedProperty, Model>> getBestProperty() {
		return
			
		// get added models for each property;
		// the triples that would be created on the server
		// if this property/operation would be executed.
		properties.stream().map(p -> Pair.of(p, determinePropertyAddedTriples(p)))
		
		// unwrap optionals
		.filter(p -> p.getRight().isPresent())
		.map(p -> Pair.of(p.getLeft(), p.getRight().get()))
		
		// sort by size of model
		.sorted((a, b) -> b.getRight().size() - a.getRight().size())
		
		.findFirst();
	}
	
	private String asTurtle(Model model) {
		StringWriter writer = new StringWriter();
		WriterConfig config = new WriterConfig();
//		config.set(BasicWriterSettings.PRETTY_PRINT, true);
		Rio.write(model, writer, RDFFormat.TURTLE, config);
		return writer.toString();
	}
	
	private void printModel(Model model) {
		System.out.println(asTurtle(model));
	}
	
}
