package skosboss.client.core;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;

import skosboss.client.core.Rdf.SkosApi;
import skosboss.client.hydra_model.ExtOperation;
import skosboss.client.hydra_model.IriTemplate;
import skosboss.client.hydra_model.Operation;
import skosboss.client.hydra_model.Property;
import skosboss.client.hydra_model.RdfUtils;
import skosboss.client.hydra_model.Shape;
import skosboss.client.hydra_model.SupportedProperty;
import skosboss.client.hydra_model.TemplatedLink;
import skosboss.client.shacl.ParseShaclResult;
import skosboss.client.shacl.ReturnShapeUtil;
import skosboss.client.shacl.Shacl;
import skosboss.client.shacl.ShaclValidator;
import skosboss.client.shacl.ValidationReport;

class Cycle {

	private Model desiredAddedDiff;
	private Set<SupportedProperty> properties;
	private Map<SupportedProperty, IriTemplate> templates;
	private ShaclValidator validator;
	private Optional<SupportedProperty> forceProperty;
	private OperationExecutor executor;
	
	private boolean debug() {
		return executor != null;
	}

	Cycle(
		Model desiredAddedDiff,
		Set<SupportedProperty> properties,
		Map<SupportedProperty, IriTemplate> templates,
		ShaclValidator validator,
		Optional<SupportedProperty> forceProperty,
		OperationExecutor executor
	) {
		this.desiredAddedDiff = desiredAddedDiff;
		this.properties = properties;
		this.templates = templates;
		this.validator = validator;
		this.forceProperty = forceProperty;
		this.executor = executor;
	}

	private Optional<ExtOperation> getOperation(SupportedProperty p) {

		Property property = p.getProperty();
		if (!(property instanceof TemplatedLink))
			return Optional.empty();
		
		TemplatedLink link = (TemplatedLink) property;
		Operation operation = link.getSupportedOperation();
		if (!(operation instanceof ExtOperation))
			return Optional.empty();
		
		return Optional.of((ExtOperation) operation);
	}
	
	private List<AddedTriples> determinePropertyAddedTriples(SupportedProperty p) {
		
		Optional<ExtOperation> extOperation = getOperation(p);
		if (!extOperation.isPresent()) return Collections.emptyList();
		
		Shape addedDiffShape = extOperation.get().getAddedDiff();
		
		if (addedDiffShape == null) {
//			System.out.println("\top has NO added diff");
			return Collections.emptyList();
		}
		
		Model addedDiff = addedDiffShape.getModel();
//		System.out.println("\top has added diff");
		
		List<AddedTriples> added = determineAddedTriples(addedDiff);
		
		// every entry in 'added' is 1 potential 'execution' of the operation.
		// for each entry, check if its required template values can be fulfilled.
		
		IriTemplate template = templates.get(p);
		
		return
		added.stream()
		.filter(a ->
			new CheckTemplateValuesPresent(
				a.getInstance(),
				template
			)
			.run()
		)
		.collect(Collectors.toList());
	}
	
	private class CheckTemplateValuesPresent {
		
		Resource instance;
		IriTemplate template;
		
		CheckTemplateValuesPresent(
			Resource instance,
			IriTemplate template
		) {
			this.instance = instance;
			this.template = template;
		}

		boolean isType(IRI type) {
			return desiredAddedDiff
				.contains(instance, RDF.TYPE, type);
		}
		
		Optional<String> getStringProperty(IRI property) {
			return
			Models.objectLiteral(
				desiredAddedDiff.filter(instance, property, null)
			)
			.map(l -> l.stringValue());
		}
		
		Optional<Resource> getResourceProperty(IRI property) {
			return
			Models.objectResource(
				desiredAddedDiff.filter(instance, property, null)
			);
		}
		
		Optional<Resource> getInverseResourceProperty(IRI property) {
			return
			Models.objectResource(
				desiredAddedDiff.filter(null, property, instance)
			);
		}
		
		Optional<Resource> getResourcePropertyOrInverse(IRI property, IRI inverse) {
			Optional<Resource> value = getResourceProperty(property);
			if (value.isPresent()) return value;
			return getInverseResourceProperty(inverse);
		}
		
		boolean isNoDummy(Value value) {
			return isNoDummy(value.stringValue());
		}
		
		boolean isNoDummy(String value) {
			return !value.startsWith("http://dummy/");
		}
		
		Optional<Object> getPropertyValue(IRI property) {
			
			if (property.equals(SkosApi.parent)) {
				if (isType(SKOS.CONCEPT)) {
					
					Optional<Resource> parent =
						Stream.of(
						
							// check for broader concept
							getResourcePropertyOrInverse(SKOS.BROADER, SKOS.NARROWER),
						
							// check for containing concept scheme
							getResourcePropertyOrInverse(SKOS.TOP_CONCEPT_OF, SKOS.HAS_TOP_CONCEPT)
							
						)
						.filter(v -> v.isPresent())
						.map(v -> v.get())
						.filter(this::isNoDummy) // filter out dummy parents
						.findFirst();
					
					return Optional.ofNullable(parent.map(r -> r.stringValue()).orElse(null));
				}
			}
			
			else if (property.equals(SkosApi.inProject)) {
				return Optional.of("1DF18289-11B0-0001-446A-151092184050");
			}
			
			else if (property.equals(SkosApi.title)) {
				if (isType(SKOS.CONCEPT_SCHEME)) {
					return Optional.ofNullable(
						getStringProperty(SkosApi.title).orElse(null)
					);
				}
			}
			
			else if (property.equals(SKOS.PREF_LABEL)) {
				if (isType(SKOS.CONCEPT)) {
					return Optional.ofNullable(
						getStringProperty(SKOS.PREF_LABEL).orElse(null)
					);
				}
			}
			
			else if (property.equals(SKOS.ALT_LABEL)) {
				if (isType(SKOS.CONCEPT)) {
					return Optional.ofNullable(
						getStringProperty(SKOS.ALT_LABEL).orElse(null)
					);
				}
			}
			
			else if (property.equals(SkosApi.uri)) {
				if (isType(SKOS.CONCEPT)) {
					
					Optional<String> uri = getStringProperty(SkosApi.uri);
					if (uri.isPresent())
						return Optional.of(uri.get());
					
					return Optional.of(instance.stringValue());
				}
			}
			
			System.out.println("NOTE: could NOT find value for property "
				+ "[" + property + "] for instance [" + instance + "]");
			
			return Optional.empty();
		}
		
		Map<String, Object> getTemplateValues() {
			return
			template.getMappings().stream()
				.filter(m -> m.isRequired())
				.collect(Collectors.toMap(
					m -> m.getVariable(),
					m ->
						getPropertyValue(m.getProperty())
						.orElseThrow(() -> new RuntimeException(
							"could not obtain template value for property [" + 
								m.getProperty() + "] for instance [" + instance + "]")
						)
				));
		}
		
		boolean run() {
			return
			template.getMappings().stream()
				.filter(m -> m.isRequired())
				.map(m -> m.getProperty())
				.map(p -> getPropertyValue(p))
				.allMatch(v -> v.isPresent());
		}
	}
	
	private RdfUtils utils = new RdfUtils(); // TODO inject
	
	private static class AddedTriples {
		
		Model added;
		Resource instance;
		
		AddedTriples(Model added, Resource instance) {
			this.added = added;
			this.instance = instance;
		}

		Model getAdded() {
			return added;
		}

		Resource getInstance() {
			return instance;
		}
	}
	
	private List<AddedTriples> determineAddedTriples(Model addedDiff) {
		
		IRI targetClass = getTargetClass(addedDiff);
//		System.out.println("\tTARGET CLASS: " + targetClass);
		
		Set<Resource> instances =
			desiredAddedDiff.filter(null, RDF.TYPE, targetClass).subjects();
		
		return
		instances.stream()
			.<Optional<AddedTriples>>map(i -> {
				
				Model instanceDesiredAddedDiff = utils.getResourceTreeModel(desiredAddedDiff, i, false);
				
				// check if 'addedDiff' shape matches 'desiredAddedDiff' graph
				Model result = validator.validate(instanceDesiredAddedDiff, addedDiff);
	//			printShaclResult(result);
				ValidationReport report = ParseShaclResult.create(result).get();
				
				Model added = new DetermineAddedSubModel(instanceDesiredAddedDiff, report, targetClass).get();
				
				if (added.isEmpty())
					return Optional.empty();
				
				return Optional.of(
					new AddedTriples(added, i)
				);
			})
			.filter(a -> a.isPresent())
			.map(a -> a.get())
			.collect(Collectors.toList());
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
	
	Model exec(PropertyExecution p) {
	
		SupportedProperty property = p.getProperty();
		
		IriTemplate template = templates.get(property);
		System.out.println("execute template: " + template.getTemplate());

		Map<String, Object> values =
			new CheckTemplateValuesPresent(p.getInstance(), template).getTemplateValues();
		
		TemplatedOperation operation = new TemplatedOperation(template, values);
		try {
			return executor.execute(operation).getBody();
		}
		catch (IOException e) {
			throw new RuntimeException("failed to execute operation", e);
		}
	}
	
	CycleResult evaluateProperty(PropertyExecution p) {
		
		SupportedProperty property = p.getProperty();
		
		// the sub-set of the 'desired added diff' this operation would add
		Model addedTriples = p.getAdded();
		
		System.out.println("SELECTED PROPERTY/OPERATION: " + property.getTitle());
		System.out.println("$$$ " + p.getInstance() + " /// " + p.getProperty().getTitle());
		System.out.println("triples this operation would add (sub-set of 'desired added diff'):\n");
		printModel(addedTriples);
		System.out.println("*************************************************");
		
		// determine effect of the return shape, replacing dummy
		// resources by generated 'actual' resources
		ExtOperation operation = getOperation(property).get(); // NOTE: we know operation is present at this point
		IRI targetClass = getTargetClass(operation.getAddedDiff().getModel());
		Resource target = Models.subject(addedTriples.filter(null, RDF.TYPE, targetClass)).get(); // NOTE: assuming this exists
		
		Optional<Resource> replacement;

		if (executor != null) {
			
			Model effect = exec(p);

			// TODO add 'effect' to client state
			
			// find out which resource was replaced
			replacement = Models.subject(effect.filter(null, RDF.TYPE, targetClass));
			
			System.out.println("############# EFFECT ###################");
			printModel(effect);
			System.out.println("##########################################");
			
		}
		
		else if (operation.getReturnShape() != null) {
			
			Model returnShape = operation.getReturnShape().getModel();
			
			Model effect = ReturnShapeUtil.predictReturnShapeEffect(addedTriples, returnShape, target);
			
			// TODO add 'effect' to client state
			
			// find out which resource was replaced
			replacement = Models.subject(effect.filter(null, RDF.TYPE, targetClass));
		}
		else replacement = Optional.empty();
		
		// create new state
		
		// 'addedTriples' will be removed to obtain new state.
		// find all rdf:type triples it contains (and thus will be removed),
		// so we can restore them later if needed
		Model addedTypeTriples = addedTriples.filter(null, RDF.TYPE, null);
		
		Model newDesiredAddedDiff = new LinkedHashModel(desiredAddedDiff);
		addedTriples.forEach(newDesiredAddedDiff::remove);
		
		// for each removed rdf:type triple, check if its subject is used
		// as a subject in any triples still present in the new state.
		// if so, re-add the rdf:type triple.
		addedTypeTriples.stream().filter(t ->
			newDesiredAddedDiff.contains(t.getSubject(), null, null)
		)
		.forEach(newDesiredAddedDiff::add);
		
		// replace the replaced resource in the remaining 'desired added diff'
		ValueFactory f = SimpleValueFactory.getInstance();
		Model newDesiredAddedDiffReplaced =
			replacement.<Model>map(r ->
				newDesiredAddedDiff.stream().map(s -> {
					if (s.getSubject().equals(target))
						return f.createStatement(r, s.getPredicate(), s.getObject());
					if (s.getObject().equals(target))
						return f.createStatement(s.getSubject(), s.getPredicate(), r);
					return s;
				})
				.collect(
					Collectors.toCollection(() -> new LinkedHashModel())
				)
			)
			.orElse(newDesiredAddedDiff);
		
		
//		System.out.println("SELECTED PROPERTY " + property.getTitle());
		
		System.out.println("remaining 'desired added diff':\n");
		printModel(newDesiredAddedDiffReplaced);
		System.out.println("***************************************");
		
		return new CycleResult(
			newDesiredAddedDiffReplaced,
			Optional.of(property)
		);		
	}
	
	CycleResult run() {
		
		// TODO make it so we can consider multiple paths;
		// so instead of just picking 'best property' here,
		// ALL properties (that qualify) must be an option.
		
		return
			
		getBestProperty().map(this::evaluateProperty)
		
		.orElse(new CycleResult(
			desiredAddedDiff,
			Optional.empty()
		));
	}
	
	static class PropertyExecution {
		
		SupportedProperty property;
		Model added;
		Resource instance;
		
		PropertyExecution(
			SupportedProperty property,
			Model added,
			Resource instance
		) {
			this.property = property;
			this.added = added;
			this.instance = instance;
		}

		SupportedProperty getProperty() {
			return property;
		}

		Model getAdded() {
			return added;
		}

		Resource getInstance() {
			return instance;
		}
	}
	
	private Optional<PropertyExecution> getPropertyExecution(SupportedProperty p) {
		return
		determinePropertyAddedTriples(p).stream()
		.map(x -> new PropertyExecution(p, x.getAdded(), x.getInstance()))
		
		// sort by size of model
		.sorted((a, b) ->
			b.getAdded().size() -
			a.getAdded().size()
		)
		
		.findFirst();
	}
	
	private Optional<PropertyExecution> getBestProperty() {
		
		if (forceProperty.isPresent())
			return getPropertyExecution(forceProperty.get());
		
		return
			
		// get added models for each property;
		// the triples that would be created on the server
		// if this property/operation would be executed.
		properties.stream()
			.flatMap(p ->
				determinePropertyAddedTriples(p).stream()
					.map(x -> new PropertyExecution(p, x.getAdded(), x.getInstance()))
			)
		
		// sort by size of model
		.sorted((a, b) ->
			b.getAdded().size() -
			a.getAdded().size()
		)
		
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
		
		model.setNamespace("skos", SKOS.NAMESPACE);
		model.setNamespace("dmy", "http://dummy/");
		model.setNamespace("some", "http://someuri/");
		model.setNamespace("skos-api", SkosApi.namespace);

		System.out.println(asTurtle(model));
	}
	
}
