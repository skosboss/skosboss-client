package skosboss.client.core;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
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
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
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
//		String folder = "D:\\MariaP\\git\\skosboss\\skos-api-metamodel\\demo\\pp\\";
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
		
//		Model desiredAddedDiff = new ModelBuilder()
//			
//			.subject(f.createIRI("http://example.com/scheme/278"))
//			.add(RDF.TYPE, SKOS.CONCEPT_SCHEME)
//			.add(SkosApi.uri, "uri")
//			.add(SkosApi.inProject, f.createLiteral("project id"))
//			.add(SkosApi.title, f.createLiteral("concept scheme title"))
//			
//			.subject(f.createIRI("urn:concept"))
//			.add(RDF.TYPE, SKOS.CONCEPT)
//			.add(SKOS.TOP_CONCEPT_OF, f.createIRI("urn:concept-scheme"))
//			
//			.build();
		
		// add prefLabel to existing concept
//		Model desiredAddedDiff = new ModelBuilder()
//			.subject(f.createIRI("http://xyz"))
//			.add(RDF.TYPE, SKOS.CONCEPT)
//			.add(SKOS.PREF_LABEL, "xyz")
//			.add(SkosApi.uri, "http://xyz")
//			.build();
		
//		Model desiredAddedDiff = new ModelBuilder()
//			.subject(f.createIRI("http://DUMMY"))
//			.add(RDF.TYPE, SKOS.CONCEPT)
//			.add(SKOS.TOP_CONCEPT_OF, f.createIRI("urn:pets"))
//			.add(SkosApi.uri, "urn:whatever")
//			.build();
		
		final String scenario = 
			"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" + 
			"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" + 
			"@prefix owl: <http://www.w3.org/2002/07/owl#> .\n" + 
			"@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n" + 
			"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" + 
			"@prefix hydra: <http://www.w3.org/ns/hydra/core#> .\n" + 
			"@prefix hydra-ext: <http://skos-api.org/hydra-extended#> .\n" + 
			"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" + 
			"@prefix pp: <http://someapivendor.com/api/> .\n" + 
			"@prefix skos-api: <http://skos-api.org/metamodel#> .\n" + 
			"@prefix dmy: <http://dummy/> . \n" + 
			"\n" + 
			"dmy:cs1\n" + 
			"  a skos:ConceptScheme ;\n" + 
			"  skos-api:inProject \"p1-id\" ;\n" + 
			"  skos-api:title \"cs1-title\" ;\n" + 
			"  skos-api:uri \"\" ; \n" + 
			".\n" + 
			"\n" + 
			"dmy:a\n" + 
			"  a skos:Concept ;\n" + 
			"  skos:prefLabel \"a-pLabel\" ;\n" + 
			"  skos:altLabel \"a-aLabel\" ;\n" + 
//			"  skos:narrower dmy:b ;\n" + 
			"  skos:topConceptOf dmy:cs1 ;\n" + 
			"  skos-api:parent dmy:cs1 ;\n" + 
			"  skos-api:uri \"\" ;\n" + 
			".\n" + 
			"\n" + 
			"dmy:b\n" + 
			"  a skos:Concept ;\n" + 
			"  skos:prefLabel \"b-pLabel\" ;\n" + 
			"  skos:broader dmy:a ;\n" + 
			"  skos-api:parent dmy:a ;\n" + 
			"  skos-api:uri \"\" ;\n" + 
			".\n" + 
			"";
		
		
		Model desiredAddedDiff = new LinkedHashModel();
		try {
			desiredAddedDiff = Rio.parse(new StringReader(scenario), "", RDFFormat.TURTLE);
		} catch (RDFParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedRDFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
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
