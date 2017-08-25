package skosboss.client.hydra_model;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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

import skosboss.client.hydra_model.Rdf.SkosApi;

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
			new ParseDescriptor(model).run();

		
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
			
			System.out.println("???");
			
			Property property = p.getProperty();
			System.out.println("\tis templated link: " + (property instanceof TemplatedLink));
			if (!(property instanceof TemplatedLink))
				return false;
			
			TemplatedLink link = (TemplatedLink) property;
			Operation operation = link.getSupportedOperation();
			System.out.println("\thas ExtOperation: " + (operation instanceof ExtOperation));
			if (!(operation instanceof ExtOperation))
				return false;
			
			ExtOperation extOperation = (ExtOperation) operation;
			Shape addedDiff = extOperation.getAddedDiff();
			
			if (addedDiff == null) {
				System.out.println("op has NO added diff");
				return false;
			}
			
			System.out.println("OPERATION ADDED DIFF:");
			
			Model isolatedAddedDiff = getResourceTreeModel(addedDiff.getModel(), addedDiff.getResource());
			
			printModel(isolatedAddedDiff);
			
			// TODO check if 'addedDiff' shape matches 'desiredAddedDiff' graph
			
			// TODO if so, proceed to execute operation according to 'template' below
			IriTemplate template = properties.get(p);
			
			
			return true;
		})
		.forEach(x -> {});
		
	}
	
	private Model getResourceTreeModel(Model source, Resource root) {
		Model model = new LinkedHashModel();
		getResourceTreeModel(source, root, model, Collections.emptyList());
		return model;
	}
	
	private void getResourceTreeModel(Model source, Resource root, Model result, List<Resource> visited) {
		
		if (visited.contains(root)) return;
		
		List<Resource> newVisited =
			Stream.concat(
				visited.stream(),
				Arrays.asList(root).stream()
			)
			.collect(Collectors.toList());
		
		source.filter(root, null, null).forEach(s -> {
			result.add(s);
			Value o = s.getObject();
			if (o instanceof Resource)
				getResourceTreeModel(source, (Resource) o, result, newVisited);
		});
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
