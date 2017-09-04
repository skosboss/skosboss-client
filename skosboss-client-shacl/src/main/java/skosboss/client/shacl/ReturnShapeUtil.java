package skosboss.client.shacl;

import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.jena.ext.com.google.common.collect.Iterators;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

public class ReturnShapeUtil {
	
	private static int counter = 0;
	
	private static final ValueFactory f = SimpleValueFactory.getInstance();
	
	// TODO move this elsewhere
		private static class SkosApi {
			
			static IRI iri(String value) {
				return f.createIRI(NAMESPACE + value);
			}
			
			static final String NAMESPACE = "http://skos-api.org/metamodel#";
			
			static IRI uri = iri("uri");
			
		}
	
	public static Model asModel(String turtle) {
		try {
			return Rio.parse(new StringReader(turtle), "", RDFFormat.TURTLE);
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
		return new LinkedHashModel();
	}
	
	public static Model predictReturnShapeEffect(Model model, Model returnShape, Resource target) {
		// Determine returned concept uri
		IRI subject = f.createIRI("http://someuri/"+counter);
		
		// If target is specified, process target, and add triples
		if (target != null) {
			return processTargetAndAddTriples(model, returnShape, target, subject);
		} else {
		// If target unspecified, add triples
			return addTriples(subject, returnShape);
		}
	}
	
	private static Optional<Value> determineRdfType(Model returnShape) {
		return Models.object(returnShape.filter(null, Shacl.targetClass, null));
	}

	
	private static Set<IRI> getPredicates(Model shape) {
		// normal paths
		return shape.stream()
			.filter(s -> s.getPredicate().equals(Shacl.path) && 
					!(s.getObject() instanceof BNode))
			.map(s -> (IRI) s.getObject())
			.collect(Collectors.toSet());
		
		// TODO: inverse paths, if necessary
	}
	
	private static Set<IRI> getPredicatesExceptSkosApiUri(Model shape) {
		// normal paths
		return shape.stream()
			.filter(s -> s.getPredicate().equals(Shacl.path) && 
					!(s.getObject() instanceof BNode))
			.filter(s -> !s.getObject().equals(SkosApi.uri))
			.filter(s -> !s.getObject().equals(RDF.TYPE))
			.map(s -> (IRI) s.getObject())
			.collect(Collectors.toSet());
		
		// TODO: inverse paths, if necessary
	}
	
	private static Model processTargetAndAddTriples(Model model, Model returnShape, Resource target, IRI subject) {
		Set<IRI> predicates = getPredicates(returnShape);
		if (predicates.contains(SkosApi.uri)) {
			return addTriplesAndSubstituteTarget(model, returnShape, target, subject);
		} else {
			return addTriples(subject, returnShape);
		}
		
	}
	
	private static Model addTriplesAndSubstituteTarget(Model model, Model returnShape, Resource target, IRI subject) {
		Model result = new LinkedHashModel();
		
		result.addAll( 
			model.stream()
				.map( s -> {
					if (s.getSubject().equals(target)) {
						return f.createStatement(subject, s.getPredicate(), s.getObject(), s.getContext());
					} else if (s.getObject().equals(target)) {
						return f.createStatement(s.getSubject(), s.getPredicate(), subject, s.getContext());
					}
					return s;
				})
				.collect(Collectors.toCollection(LinkedHashModel::new))
		);
		
		result.add(subject, SkosApi.uri, f.createLiteral(subject.stringValue()));
		result.remove(subject, SkosApi.uri, f.createLiteral(""));
		
		return result;
	}
	
	private static Model addTriples(IRI subject, Model returnShape) {
		ModelBuilder builder = new ModelBuilder();
		builder.subject(subject)
		.add(RDF.TYPE, determineRdfType(returnShape).get());
		if (!returnShape.filter(null, null, SkosApi.uri).isEmpty()) {
			builder.add(SkosApi.uri, f.createLiteral(subject.stringValue()));
		}
		getPredicatesExceptSkosApiUri(returnShape).forEach( p -> builder.add(p, determineDummyObj(p, returnShape)));
		return builder.build();
	}
	
	private static Value determineDummyObj(IRI predicate, Model resultShape) {
		return getNodeKind(predicate, resultShape)
			.map(nk -> {
				if (nk.equals(Shacl.IRI)) {
					return f.createIRI(predicate.stringValue() + "_iri");
				} else if (nk.equals(Shacl.BlankNode)) {
					return f.createBNode();
				} else {
					return f.createLiteral(predicate.stringValue() + "_value");
				}
			})
			.orElse(f.createLiteral(predicate.stringValue() + "_value"));
	}
	
	private static Optional<IRI> getNodeKind(IRI predicate, Model returnShape) {
		Resource propertyShape = Iterators.getOnlyElement(returnShape.filter(null, Shacl.path, predicate).iterator()).getSubject();
		return Optional.of((IRI) Iterators.getOnlyElement(returnShape.filter(propertyShape, Shacl.nodeKind, null).iterator()).getObject());
	}
	
//	public static void main(String... args) {
//		Model returnShape = asModel(CREATE_CS_RETURN_SHAPE);
//		Model model = asModel(SCENARIO);
//		Model result = predictReturnShapeEffect(model, returnShape, f.createIRI("http://dummy/cs1"));
////		Model result = predictReturnShapeEffect(new LinkedHashModel(), returnShape, null);
//		
//		result.forEach(System.out::println);
//		
//	}
//	
//	public static final String SCENARIO = 
//		"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" + 
//		"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" + 
//		"@prefix owl: <http://www.w3.org/2002/07/owl#> .\n" + 
//		"@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n" + 
//		"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" + 
//		"@prefix hydra: <http://www.w3.org/ns/hydra/core#> .\n" + 
//		"@prefix hydra-ext: <http://skos-api.org/hydra-extended#> .\n" + 
//		"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" + 
//		"@prefix pp: <http://someapivendor.com/api/> .\n" + 
//		"@prefix skos-api: <http://skos-api.org/metamodel#> .\n" + 
//		"@prefix dmy: <http://dummy/> . \n" + 
//		"\n" + 
//		"dmy:cs1\n" + 
//		"  a skos:ConceptScheme ;\n" + 
//		"  skos-api:inProject \"p1-id\" ;\n" + 
//		"  skos-api:title \"cs1-title\" ;\n" + 
//		"  skos-api:uri \"\" ; \n" + 
//		".\n" + 
//		"\n" + 
//		"dmy:a\n" + 
//		"  a skos:Concept ;\n" + 
//		"  skos:prefLabel \"a-pLabel\" ;\n" + 
//		"  skos:altLabel \"a-aLabel\" ;\n" + 
//		"  skos:narrower dmy:b ;\n" + 
//		"  skos:topConceptOf dmy:cs1 ;\n" + 
//		"  skos-api:parent dmy:cs1 ;\n" + 
//		"  skos-api:uri \"\" ;\n" + 
//		".\n" + 
//		"\n" + 
//		"dmy:b\n" + 
//		"  a skos:Concept ;\n" + 
//		"  skos:prefLabel \"b-pLabel\" ;\n" + 
//		"  skos:broader dmy:a ;\n" + 
//		"  skos-api:parent dmy:a ;\n" + 
//		"  skos-api:uri \"\" ;\n" + 
//		".\n";
//
//	public static final String CREATE_CS_RETURN_SHAPE = 
//		"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\r\n" + 
//		"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\r\n" + 
//		"@prefix owl: <http://www.w3.org/2002/07/owl#> .\r\n" + 
//		"@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\r\n" + 
//		"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\r\n" + 
//		"@prefix hydra: <http://www.w3.org/ns/hydra/core#> .\r\n" + 
//		"@prefix hydra-ext: <http://skos-api.org/hydra-extended#> .\r\n" + 
//		"@prefix sh: <http://www.w3.org/ns/shacl#> .\r\n" + 
//		"@prefix pp: <http://someapivendor.com/api/> .\r\n" + 
//		"@prefix skos-api: <http://skos-api.org/metamodel#> .\r\n" + 
//		"\r\n" + 
//		"[]\r\n" + 
//		"  a sh:NodeShape ;\r\n" + 
//		"  sh:targetClass skos:ConceptScheme ;\r\n" + 
//		"  sh:property\r\n" + 
//		"    [\r\n" + 
//		"      sh:path rdf:type ;\r\n" + 
//		"      sh:minCount 1 ;\r\n" + 
//		"      sh:maxCount 1 ;\r\n" + 
//		"      sh:nodeKind sh:IRI ;\r\n" + 
//		"      sh:hasValue skos:ConceptScheme ;\r\n" + 
//		"    ] ,\r\n" + 
//		"    [\r\n" + 
//		"      sh:path skos-api:uri ;\r\n" + 
//		"      sh:minCount 1 ;\r\n" + 
//		"      sh:maxCount 1 ;\r\n" + 
//		"      sh:nodeKind sh:Literal ;\r\n" + 
//		"    ] ;\r\n" + 
//		"  sh:closed true .";
}
