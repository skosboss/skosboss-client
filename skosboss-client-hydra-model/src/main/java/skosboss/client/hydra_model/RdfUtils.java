package skosboss.client.hydra_model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;

class RdfUtils {

	/**
	 * Creates a new model containing the statements from the source model
	 * that are reachable from the specified root resource.
	 * @param source
	 * @param root
	 * @return
	 */
	Model getResourceTreeModel(Model source, Resource root) {
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
	
}
