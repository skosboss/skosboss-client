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

import org.apache.commons.lang3.mutable.MutableObject;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;

import skosboss.client.core.Rdf.SkosApi;
import skosboss.client.hydra_model.IriTemplate;
import skosboss.client.hydra_model.ParseDescriptor;
import skosboss.client.hydra_model.SupportedProperty;
import skosboss.client.shacl.ShaclValidator;

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
			.add(SkosApi.uri, "uri")
			.add(SkosApi.inProject, f.createLiteral("project id"))
			.add(SkosApi.title, f.createLiteral("concept scheme title"))
			
			.subject(f.createIRI("urn:concept"))
			.add(RDF.TYPE, SKOS.CONCEPT)
//			.add(SkosApi.uri, "uri")
			.add(SKOS.TOP_CONCEPT_OF, f.createIRI("urn:concept-scheme"))
			
			.build();
		
		System.out.println("desired added diff:");
		printModel(desiredAddedDiff);
		System.out.println("******************");
		
		Function<Model, Cycle> createCycle = m ->
			new Cycle(
				m,
				properties.keySet(),
				properties,
				ShaclValidator.create()
			);
		
		MutableObject<CycleResult> current = new MutableObject<>(
			new CycleResult(
				desiredAddedDiff,
				Optional.empty()
			)
		);
		while (true) {
			Model pre = current.getValue().getDesiredAddedDiff();
			Cycle cycle = createCycle.apply(pre);
			current.setValue(cycle.run());
			if (pre.equals(current.getValue().getDesiredAddedDiff())) {
				System.out.println("########## FINAL REMAINING 'desired added diff':");
				printModel(current.getValue().getDesiredAddedDiff());
				break;
			}
		}
		
		
		// TODO if so, proceed to execute operation according to 'template' below
//		IriTemplate template = properties.get(p);
//		System.out.println("exec template " + template);
		
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
