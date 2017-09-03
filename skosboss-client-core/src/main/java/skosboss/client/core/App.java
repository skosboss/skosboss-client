package skosboss.client.core;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;

import skosboss.client.core.Rdf.SkosApi;
import skosboss.client.hydra_model.ExtOperation;
import skosboss.client.hydra_model.IriTemplate;
import skosboss.client.hydra_model.Operation;
import skosboss.client.hydra_model.ParseDescriptor;
import skosboss.client.hydra_model.Property;
import skosboss.client.hydra_model.RdfUtils;
import skosboss.client.hydra_model.Shape;
import skosboss.client.hydra_model.SupportedProperty;
import skosboss.client.hydra_model.TemplatedLink;
import skosboss.client.shacl.ParseShaclResult;
import skosboss.client.shacl.ShaclValidator;
import skosboss.client.shacl.ValidationReport;

// TODO move this to client-core

public class App implements Runnable {

	private static final ValueFactory f = SimpleValueFactory.getInstance();
	
	public static void main(String... args) {
		new App().run();
	}

	@Override
	public void run() {

		String folder = "D:\\development\\skos-api-metamodel\\demo\\pp\\";
		Model model = merge(
			load(folder + "pp.ttl"),
			load(folder + "EntryPoint.ttl")
		);

		Map<SupportedProperty, IriTemplate> properties =
			ParseDescriptor.create(model).run();

		
		// #####################################
		// #                                   #
		// #    GOAL: CREATE CONCEPT SCHEME    #
		// #                                   #
		// #####################################
		
		Model desiredAddedDiff = new ModelBuilder()
			
			.subject(f.createIRI("urn:concept-scheme"))
			.add(RDF.TYPE, SKOS.CONCEPT_SCHEME)
			.add(RDF.TYPE, RDFS.RESOURCE)
			.add(SkosApi.uri, "uri")
			.add(SkosApi.inProject, f.createLiteral("project id"))
			.add(SkosApi.title, f.createLiteral("concept scheme title"))
			
			.subject(f.createIRI("urn:concept"))
			.add(RDF.TYPE, SKOS.CONCEPT)
			.add(RDF.TYPE, RDFS.RESOURCE)
			.add(SkosApi.uri, "uri")
			.add(SKOS.TOP_CONCEPT_OF, f.createIRI("urn:concept-scheme"))
			
			.build();
		
		System.out.println("desired added diff:");
		printModel(desiredAddedDiff);
		System.out.println("******************");
		
		Function<SupportedProperty, Optional<ExtOperation>> getOperation = p -> {
			
			Property property = p.getProperty();
			if (!(property instanceof TemplatedLink))
				return Optional.empty();
			
			System.out.println("> prop " + property.getResource());
			
			TemplatedLink link = (TemplatedLink) property;
			Operation operation = link.getSupportedOperation();
			if (!(operation instanceof ExtOperation))
				return Optional.empty();
			
			return Optional.of((ExtOperation) operation);
		};
		
		// find ExtOperation with the desired 'addedDiff'
		properties.keySet().stream()
		.filter(p -> {
			
			Optional<ExtOperation> extOperation = getOperation.apply(p);
			if (!extOperation.isPresent()) return false;
			
			Shape addedDiffShape = extOperation.get().getAddedDiff();
			
			if (addedDiffShape == null) {
				System.out.println("op has NO added diff");
				return false;
			}
			
			Model addedDiff = addedDiffShape.getModel();
			System.out.println("op has added diff");
//			printModel(addedDiff);
			
			// check if 'addedDiff' shape matches 'desiredAddedDiff' graph
			Model result = ShaclValidator.create().validate(desiredAddedDiff, addedDiff);
			printShaclResult(result);
			ValidationReport report = ParseShaclResult.create(result).get();
			
			DetermineAddedSubModel x = new DetermineAddedSubModel(desiredAddedDiff, report);
			System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
			printModel(x.get());
			System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
			
//			report.getResults().forEach(r -> {
//				System.out.println("FOCUS NODE:");
//				System.out.println("ID in result: " + r.getFocusNode());
//				printModel(
//					new RdfUtils()
//						.getResourceTreeModel(
//							desiredAddedDiff,
//							r.getFocusNode()
//						)
//				);
//				System.out.println("######################################################");
//			});

			
			
			// check if validation errors are a problem
			report.getResults().forEach(r -> {
			
				IRI path = r.getResultPath();
				
				// TODO if path rdf:type does NOT occur in validation results,
				// assume resource was created.
				
				if (desiredAddedDiff.filter(null, path, null).isEmpty()) {
					
					// validation error is because the path is NOT in our
					// desired added diff. this is not a problem; the operation
					// will simply add MORE data than we intended.
					
				}
				
				else {
					
					// validation error is for a path that is present in our
					// desired added diff. this means f.e. wrong value.
					// => this operation does not provide a 'solution' for
					//    this property. look for another operation.
					
					// or it's an error due to the desired added diff.
					// having a property that's not present in the shape,
					// and the shape is closed.
					
					// TODO place this property in some bag of properties
					// we need to search another operation for.
					
				}
				
			});
			
			
			return report.getConforms();
			
		})
		.forEach(p -> {
			
			// TODO if so, proceed to execute operation according to 'template' below
			IriTemplate template = properties.get(p);

			System.out.println("exec template " + template);
			
		});
		
	}
	
	private void printShaclResult(Model result) {
		System.out.println("######################################################");
		System.out.println("SHACL VALIDATION RESULT:");
		printModel(result);
		System.out.println("######################################################");
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

	private Model merge(Model a, Model b) {
		Model model = new LinkedHashModel();
		model.addAll(a);
		model.addAll(b);
		return model;
	}

	private Model load(String path) {
		try (Reader reader = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
			return Rio.parse(reader, "http://none.com/", RDFFormat.TURTLE);
		}
		catch (IOException e) {
			throw new RuntimeException("error loading rdf model from " + path, e);
		}
	}

}
