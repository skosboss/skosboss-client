package skosboss.client.hydra_model;

import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;

public class RdfParseUtils {

	private Model model;

	public RdfParseUtils(Model model) {
		this.model = model;
	}
	
	public Optional<Value> getProperty(
		Resource subject,
		IRI predicate
	) {
		return Models.object(
			model.filter(subject, predicate, null)
		);
	}
	
	public Optional<Boolean> getBooleanProperty(
		Resource subject,
		IRI predicate
	) {
		return getProperty(subject, predicate)
			.filter(v -> v instanceof Literal)
			.map(v -> ((Literal) v).booleanValue());
	}
	
	public Optional<IRI> getIriProperty(
		Resource subject,
		IRI predicate
	) {
		return getProperty(subject, predicate)
			.filter(v -> v instanceof IRI)
			.map(v -> (IRI) v);
	}
	
	public Optional<Resource> getResourceProperty(
		Resource subject,
		IRI predicate
	) {
		return getProperty(subject, predicate)
			.filter(v -> v instanceof Resource)
			.map(v -> (Resource) v);
	}

	public Optional<String> getStringProperty(
		Resource subject,
		IRI predicate
	) {
		return getProperty(subject, predicate)
			.filter(v -> v instanceof Literal)
			.map(v -> v.stringValue());
	}
	
	public Optional<Integer> getIntegerProperty(
		Resource subject,
		IRI predicate
	) {
		return getProperty(subject, predicate)
			.filter(v -> v instanceof Literal)
			.map(v -> ((Literal) v).intValue());
	}
	
	public Optional<Resource> getSubjectOfType(IRI type) {
		return Models.subject(
			model.filter(null, RDF.TYPE, type)
		);
	}
}
