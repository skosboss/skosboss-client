package skosboss.client.shacl;

import org.eclipse.rdf4j.model.Model;
import org.topbraid.shacl.validation.ValidationUtil;

public class ShaclValidator {

	private JenaConverter converter;
	
	ShaclValidator(JenaConverter converter) {
		this.converter = converter;
	}

	public Model validate(Model data, Model shapes) {
		return
		converter.toRdf4j(
			ValidationUtil.validateModel(
				converter.toJena(data),
				converter.toJena(shapes),
				false
			)
			.getModel()
		);
	}

	public static ShaclValidator create() {
		return new ShaclValidator(
			new JenaConverter()
		);
	}
}
