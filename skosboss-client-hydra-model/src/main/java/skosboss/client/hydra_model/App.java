package skosboss.client.hydra_model;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

public class App implements Runnable {

	public static void main(String... args) {
		new App().run();
	}

	@Override
	public void run() {

		String folder = "D:\\development\\skos-api-metamodel\\demo\\pp\\";
		Model model = merge(
			load(folder + "pp.ttl"),
			load(folder + "EntryPoint.ttl")
		);

		Map<SupportedProperty, IriTemplate> properties =
			new ParseDescriptor(model).run();
		
		
	}
	
	
	private Model merge(Model a, Model b) {
		Model model = new LinkedHashModel();
		model.addAll(a);
		model.addAll(b);
		return model;
	}

	private Model load(String path) {
		try (Reader reader = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
			return Rio.parse(reader, "http://none.com/", RDFFormat.TURTLE);
		}
		catch (IOException e) {
			throw new RuntimeException("error loading rdf model from " + path, e);
		}
	}

}
