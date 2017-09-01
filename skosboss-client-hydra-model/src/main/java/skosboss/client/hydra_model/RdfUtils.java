package skosboss.client.hydra_model;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;

public class RdfUtils {

	/**
	 * Creates a new model containing the statements from the source model
	 * that are reachable from the specified root resource.
	 * @param source
	 * @param root
	 * @return
	 */
	public Model getResourceTreeModel(Model source, Resource root) {
		Model model = new LinkedHashModel();
		getResourceTreeModel(source, root, model, Collections.emptyList());
		return model;
	}
	
	private void getResourceTreeModel(Model source, Resource root, Model result, List<Resource> visited) {
		
		if (visited.contains(root)) return;
		
		List<Resource> newVisited =
			Stream.concat(
				visited.stream(),
				Arrays.asList(root).stream()
			)
			.collect(Collectors.toList());
		
		source.filter(root, null, null).forEach(s -> {
			result.add(s);
			Value o = s.getObject();
			if (o instanceof Resource)
				getResourceTreeModel(source, (Resource) o, result, newVisited);
		});
	}
	
	private String asTurtle(Model model) {
		StringWriter writer = new StringWriter();
		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.PRETTY_PRINT, true);
		Rio.write(model, writer, RDFFormat.TURTLE, config);
		return writer.toString();
	}
	
	@SuppressWarnings("unused")
	private void printModel(Model model) {
		System.out.println(asTurtle(model));
	}
	
}
