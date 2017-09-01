package skosboss.client.core;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

public class ValidationResult {

	private Resource focusNode;
	private String resultMessage;
	private IRI resultPath;
	private IRI resultSeverity;
	private IRI sourceConstraintComponent;
	private Model sourceShape;
	private Value value;

	public ValidationResult(
		Resource focusNode,
		String resultMessage,
		IRI resultPath,
		IRI resultSeverity,
		IRI sourceConstraintComponent,
		Model sourceShape,
		Value value
	) {
		this.focusNode = focusNode;
		this.resultMessage = resultMessage;
		this.resultPath = resultPath;
		this.resultSeverity = resultSeverity;
		this.sourceConstraintComponent = sourceConstraintComponent;
		this.sourceShape = sourceShape;
		this.value = value;
	}

	public Resource getFocusNode() {
		return focusNode;
	}

	public String getResultMessage() {
		return resultMessage;
	}

	public IRI getResultPath() {
		return resultPath;
	}

	public IRI getResultSeverity() {
		return resultSeverity;
	}

	public IRI getSourceConstraintComponent() {
		return sourceConstraintComponent;
	}

	public Model getSourceShape() {
		return sourceShape;
	}

	public Value getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "ValidationResult [focusNode=" + focusNode + ", resultMessage=" + resultMessage + ", resultPath="
			+ resultPath + ", resultSeverity=" + resultSeverity + ", sourceConstraintComponent="
			+ sourceConstraintComponent + ", sourceShape=" + sourceShape + ", value=" + value + "]";
	}

}
