package skosboss.client.hydra_model;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.eclipse.rdf4j.model.Model;
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
			
			
			// TODO check if 'addedDiff' shape matches 'desiredAddedDiff' graph
			
			// TODO if so, proceed to execute operation according to 'template' below
			IriTemplate template = properties.get(p);
			
			
			return true;
		});
		
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
