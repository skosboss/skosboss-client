package skosboss.client.core;

import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.topbraid.shacl.validation.ValidationUtil;

public class ShaclTest {

	public static void main(String... args) {
		
		Model data = loadModel("test.ttl");
		Model shapes = loadModel("test.sh.ttl");
		
		Resource result = ValidationUtil.validateModel(data, shapes, false);
		result.getModel().write(System.out, "TURTLE");
		
	}
	
	private static Model loadModel(String name) {
		try (InputStream input = ShaclTest.class.getClassLoader().getResourceAsStream(name)) {
			return ModelFactory.createDefaultModel()
				.read(input, "http://none.com/", "TURTLE");
		}
		catch (IOException e) {
			throw new RuntimeException("failed to load model from resource [" + name + "]", e);
		}
	}
}
