package skosboss.client.core;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Models;

import skosboss.client.hydra_model.RdfParseUtils;
import skosboss.client.hydra_model.RdfUtils;

class ParseShaclResult implements Supplier<ValidationReport> {

	private Model model;
	private RdfParseUtils parseUtils;
	private RdfUtils utils;

	static ParseShaclResult create(Model model) {
		return new ParseShaclResult(
			model,
			new RdfParseUtils(model),
			new RdfUtils()
		);
	}
	
	ParseShaclResult(
		Model model,
		RdfParseUtils parseUtils,
		RdfUtils utils
	) {
		this.model = model;
		this.parseUtils = parseUtils;
		this.utils = utils;
	}
	
	public ValidationReport get() {
		
		Resource report = getValidationReportResource();
		
		boolean conforms = parseUtils
			.getBooleanProperty(report, Shacl.conforms)
			.orElseThrow(() ->
				new RuntimeException("no shacl:conforms predicate present"));
		
		Set<ValidationResult> results = 
			Models.objectResources(
				model.filter(report, Shacl.result, null)
			)
			.stream()
			.map(this::parseResult)
			.collect(Collectors.toSet());

		return new ValidationReport(
			conforms,
			results
		);
	}
	
	private ValidationResult parseResult(Resource resource) {
		return new ValidationResult(
			
			// focusNode
			parseUtils.getResourceProperty(resource, Shacl.focusNode).orElse(null),
			
			// resultMessage
			parseUtils.getStringProperty(resource, Shacl.resultMessage).orElse(""),
			
			// resultPath
			parseUtils.getIriProperty(resource, Shacl.resultPath).orElse(null),
			
			// resultSeverity
			parseUtils.getIriProperty(resource, Shacl.resultSeverity).orElse(null), // TODO enum?
			
			// sourceConstraintComponent
			parseUtils.getIriProperty(resource, Shacl.sourceConstraintComponent).orElse(null), // TODO enum?
			
			// sourceShape
			parseUtils.getResourceProperty(resource, Shacl.sourceShape)
				.map(r -> utils.getResourceTreeModel(model, r))
				.orElse(null),
			
			// value
			parseUtils.getProperty(resource, Shacl.value).orElse(null)
			
		);
	}
	
	private Resource getValidationReportResource() {
		return parseUtils
			.getSubjectOfType(Shacl.ValidationReport)
			.orElseThrow(() ->
				new RuntimeException("no shacl:ValidationReport resource present")
			);
	}
	
}
