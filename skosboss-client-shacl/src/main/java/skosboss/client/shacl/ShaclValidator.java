package skosboss.client.shacl;

import java.util.Map;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.topbraid.shacl.validation.ValidationUtil;

public class ShaclValidator {

	private JenaConverter converter;
	private BNodeConversion bnodes;
	
	ShaclValidator(
		JenaConverter converter,
		BNodeConversion bnodes
	) {
		this.converter = converter;
		this.bnodes = bnodes;
	}

	public Model validate(Model data, Model shapes) {
		
		// TODO WARNING:if 'data' and 'shapes' contain bnodes with
		// the same ids, these become the same bnode in 'merged'.
		
		Model merged = bnodes.merge(data, shapes);
		Map<BNode, IRI> map = bnodes.buildBNodeToIriMap(merged);
		
		Model result =
			converter.toRdf4j(
				ValidationUtil.validateModel(
					converter.toJena(data),
					converter.toJena(shapes),
					false
				)
				.getModel()
			);
		
		return bnodes.replaceIris(result, map);
	}

	public static ShaclValidator create() {
		return new ShaclValidator(
			new JenaConverter(),
			new BNodeConversion()
		);
	}
}
