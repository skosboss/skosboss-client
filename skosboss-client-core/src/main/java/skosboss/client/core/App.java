package skosboss.client.core;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import skosboss.client.core.Rdf.SkosApi;
import skosboss.client.hydra_model.ExtOperation;
import skosboss.client.hydra_model.IriTemplate;
import skosboss.client.hydra_model.Operation;
import skosboss.client.hydra_model.ParseDescriptor;
import skosboss.client.hydra_model.Property;
import skosboss.client.hydra_model.Shape;
import skosboss.client.hydra_model.SupportedProperty;
import skosboss.client.hydra_model.TemplatedLink;

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
			.subject(f.createBNode())
			.add(RDF.TYPE, SKOS.CONCEPT_SCHEME)
			.add(SkosApi.uri, f.createIRI("urn:new-uri"))
			.add(SkosApi.inProject, f.createLiteral("project id"))
			.add(SkosApi.title, f.createLiteral("concept scheme title"))
			.build();
		
		System.out.println("desired added diff:");
		printModel(desiredAddedDiff);
		
		System.out.println("PROPS: " + properties.keySet().size());
		
		// find ExtOperation with the desired 'addedDiff'
		properties.keySet().stream()
		.filter(p -> {
			
			Property property = p.getProperty();
			if (!(property instanceof TemplatedLink))
				return false;
			
			TemplatedLink link = (TemplatedLink) property;
			Operation operation = link.getSupportedOperation();
			if (!(operation instanceof ExtOperation))
				return false;
			
			ExtOperation extOperation = (ExtOperation) operation;
			Shape addedDiff = extOperation.getAddedDiff();
			
			if (addedDiff == null) {
				System.out.println("op has NO added diff");
				return false;
			}
			
			System.out.println("OPERATION ADDED DIFF:");
			
			Model isolatedAddedDiff = addedDiff.getModel();
			
			printModel(isolatedAddedDiff);
			
			// TODO check if 'addedDiff' shape matches 'desiredAddedDiff' graph
			
			// TODO if so, proceed to execute operation according to 'template' below
			IriTemplate template = properties.get(p);
			
			
			return true;
		})
		.forEach(x -> {});
		
	}
	
	private String asTurtle(Model model) {
		StringWriter writer = new StringWriter();
		Rio.write(model, writer, RDFFormat.TURTLE);
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
