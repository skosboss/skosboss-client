package skosboss.client.hydra_model;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

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

		Resource doc = findApiDocumentation(model);
		
		Resource entryPoint = getEntryPoint(model, doc);
		
		System.out.println(entryPoint);
		
		Set<Resource> classes = determineHydraClasses(model, entryPoint);
		
		System.out.println("hydra classes of entrypoint:\n" + classes);
		
		Set<Resource> hydraProperties =
			classes.stream().flatMap(c ->
				model
					.filter(c, f.createIRI(Hydra.supportedProperty), null).objects().stream()
					.map(o -> (Resource) o)
			)
			.collect(Collectors.toSet());
		
		System.out.println("hydra properties of entry point:");
		System.out.println(hydraProperties);
		
	}
	
	private Set<Resource> determineHydraClasses(Model model, Resource entryPoint) {
		
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

	private Resource getEntryPoint(Model model, Resource doc) {
		return (Resource)
		Models.object(
			model.filter(doc, f.createIRI(Hydra.entrypoint), null)
		)
		.orElseThrow(() ->
			new RuntimeException("resource " + doc + " has no hydra:entrypoint")
		);
	}
	
	private Resource findApiDocumentation(Model model) {
		return
		Models.subject(
			model.filter(null, RDF.TYPE, f.createIRI(Hydra.ApiDocumentation))
		)
		.orElseThrow(() ->
			new RuntimeException("no instance of hydra:ApiDocumentation found")
		);
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
