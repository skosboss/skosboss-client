package skosboss.client.hydra_model;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

class Rdf {

	private static final ValueFactory f = SimpleValueFactory.getInstance();

	static class SkosApi {
		
		private static final String namespace = "http://skos-api.org/metamodel#";

		private static IRI iri(String localName) {
			return f.createIRI(namespace, localName);
		}

		public static final IRI
		
			uri = iri("uri"),
			inProject = iri("inProject"),
			title = iri("title");
		
	}
	
	static class HydraExt {
		
		private static final String namespace = "http://skos-api.org/hydra-extended#";
		
		private static IRI iri(String localName) {
			return f.createIRI(namespace, localName);
		}
		
		public static final IRI
		
			returnShape = iri("returnShape"),
			deletedDiff = iri("deletedDiff"),
			addedDiff = iri("addedDiff");
		
	}
	
	static class Hydra {
		
		private static final String namespace = "http://www.w3.org/ns/hydra/core#";
		
		private static IRI iri(String localName) {
			return f.createIRI(namespace, localName);
		}
		
		public static final IRI
		
			statusCodes = iri("statusCodes"),
			BasicRepresentation = iri("BasicRepresentation"),
			mapping = iri("mapping"),
			template = iri("template"),
			variableRepresentation = iri("variableRepresentation"),
			title = iri("title"),
			code = iri("code"),
			description = iri("description"),
			method = iri("method"),
			returns = iri("returns"),
			variable = iri("variable"),
			required = iri("required"),
			property = iri("property"),
			readable = iri("readable"),
			writable = iri("writable"),
			entrypoint = iri("entrypoint"),
			supportedProperty = iri("supportedProperty"),
			supportedOperation = iri("supportedOperation"),
			TemplatedLink = iri("TemplatedLink"),
			ApiDocumentation = iri("ApiDocumentation");
		
	}
	
}
