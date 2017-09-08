package skosboss.client.core;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class Rdf {

	private static final ValueFactory f = SimpleValueFactory.getInstance();

	public static class SkosApi {
		
		public static final String namespace = "http://skos-api.org/metamodel#";

		private static IRI iri(String localName) {
			return f.createIRI(namespace, localName);
		}

		public static final IRI
		
			parent = iri("parent"),
			uri = iri("uri"),
			inProject = iri("inProject"),
			title = iri("title");
		
	}
	
}
