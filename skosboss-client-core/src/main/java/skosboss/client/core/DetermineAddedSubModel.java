package skosboss.client.core;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import skosboss.client.shacl.ValidationReport;

class DetermineAddedSubModel implements Supplier<Model> {

	private Model desiredAdded;
	private ValidationReport report;
	private IRI targetClass;
	
	DetermineAddedSubModel(
		Model desiredAdded,
		ValidationReport report,
		IRI targetClass
	) {
		this.desiredAdded = desiredAdded;
		this.report = report;
		this.targetClass = targetClass;
	}

	@Override
	public Model get() {

		// we assume ONLY and ALL subjects with an rdf:type predicate
		// are targeted by one or more shacl shapes.
		// so an orphaned/root resource without an rdf:type triple would
		// not be considered.
		
		return
		
		// get all subjects that have an rdf:type triple with
		// the specified target class
		desiredAdded.filter(null, RDF.TYPE, targetClass)
			.subjects().stream()
			
			// process
			.map(this::getForSubject)
			.flatMap(Collection::stream)
			
			// copy statements into result model
			.collect(Collectors.toCollection(
				() -> new LinkedHashModel()
			));
		
	}
	
	private Model getForSubject(Resource subject) {

		Model result = new LinkedHashModel();
		
		// get all predicates used in triples with subject 'subject'
		Set<IRI> predicates = desiredAdded
			.filter(subject, null, null).predicates();
		
		// get all 'result paths' of results that have
		// 'subject' as their focus node
		List<IRI> errorPaths =
			report.getResults().stream()
				.filter(r -> r.getFocusNode().equals(subject))
				.map(r -> r.getResultPath())
				.collect(Collectors.toList());
		
		class CopyPredicateTriplesAndRecurse {
			
			IRI p;
			
			CopyPredicateTriplesAndRecurse(IRI p) {
				this.p = p;
			}
			
			void run() {
				desiredAdded
					.filter(subject, p, null)
					.forEach(s -> {
						result.add(s);
						// TODO recursive step?
//						processObject(s.getObject());
					});
			}
			
			@SuppressWarnings("unused")
			void processObject(Value o) {
				if (o instanceof Resource) {
					Model subModel = getForSubject((Resource) o);
					subModel.forEach(result::add);
				}
			}
		}
		
		// keep only predicates for which no errors exist
		// that have 'resultPath' equal to the predicate.
		predicates.stream()
			.filter(p -> !errorPaths.contains(p))
			
			// copy triples (subject, p, *) to result model
			// and recursively process any object resources.
			.map(p -> new CopyPredicateTriplesAndRecurse(p))
			.forEach(CopyPredicateTriplesAndRecurse::run);
		
		return result;
	}

}
