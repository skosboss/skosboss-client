package skosboss.client.shacl;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableInt;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

class BNodeConversion {

	private static final ValueFactory f = SimpleValueFactory.getInstance();
	
	private MutableInt nextIriId = new MutableInt();
	
	private IRI nextIri(Model model) {
		return
		IntStream.generate(nextIriId::getAndIncrement)
		.mapToObj(id -> f.createIRI("http://temp.com/" + id))
		.filter(iri ->
			!model.contains(iri, null, null) &&
			!model.contains(null, null, iri)
		)
		.findFirst()
		.get();
	}
	
	Model replaceIris(Model model, Map<BNode, IRI> map) {
		
		Map<IRI, BNode> inverse = map.entrySet().stream()
			.collect(Collectors.toMap(e -> e.getValue(), e -> e.getKey()));
		
		return
		replace(
			model,
			r -> {
				if (r instanceof IRI) {
					IRI iri = (IRI) r;
					if (!inverse.containsKey(iri))
						return r;
					return inverse.get(iri);
				}
				return r;
			}
		);
	}
	
	Model replaceBNodes(Model model, Map<BNode, IRI> map) {
		return
		replace(
			model,
			r -> {
				if (r instanceof BNode) {
					BNode bnode = (BNode) r;
					if (!map.containsKey(bnode))
						throw new RuntimeException(
							"encountered bnode [" + bnode + "] has no IRI in provided map");
					return map.get(bnode);
				}
				return r;
			}
		);
	}
	
	private Model replace(
		Model model,
		UnaryOperator<Resource> transform
	) {
		Model result = new LinkedHashModel();
		model.stream().forEach(t -> {
			
			Resource s = t.getSubject();
			Resource newS = transform.apply(s);
			
			IRI p = t.getPredicate();
			
			Value o = t.getObject();
			Value newO;
			if (o instanceof Resource)
				newO = transform.apply((Resource) o);
			else
				newO = o;
			
			result.add(newS, p, newO, t.getContext());
			
		});
		return result;
	}
	
	Model merge(Model... models) {
		LinkedHashModel result = new LinkedHashModel();
		Arrays.asList(models).forEach(result::addAll);
		return result;
	}
	
	Map<BNode, IRI> buildBNodeToIriMap(Model model) {
		
		Supplier<IRI> nextIri = () -> this.nextIri(model);
		
		return
		
		Stream.concat(
			model.subjects().stream(),
			model.objects().stream()
		)
		.filter(v -> v instanceof BNode)
		.map(v -> (BNode) v)
		.distinct()
		
		.collect(
			Collectors.toMap(
				b -> b,
				b -> nextIri.get()
			)
		);
	}

}
