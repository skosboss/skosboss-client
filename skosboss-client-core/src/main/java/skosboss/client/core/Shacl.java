package skosboss.client.core;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

class Shacl {

	private static final ValueFactory f = SimpleValueFactory.getInstance();

	private static final String namespace = "http://www.w3.org/ns/shacl#";
	
	private static IRI iri(String localName) {
		return f.createIRI(namespace, localName);
	}
	
	public static final IRI
	
		ValidationReport = iri("ValidationReport"),
		conforms = iri("conforms"),
		result = iri("result"),
		
		ValidationResult = iri("ValidationResult"),
		focusNode = iri("focusNode"),
		resultMessage = iri("resultMessage"),
		resultPath = iri("resultPath"),
		resultSeverity = iri("resultSeverity"),
		sourceConstraintComponent = iri("sourceConstraintComponent"),
		sourceShape = iri("sourceShape"),
		value = iri("value");
	
}
