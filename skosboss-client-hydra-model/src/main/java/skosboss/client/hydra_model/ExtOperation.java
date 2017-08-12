package skosboss.client.hydra_model;

import java.util.Set;

public class ExtOperation extends Operation {

	private Shape returnShape;
	private Shape addedDiff;
	private Shape deletedDiff;
	
	public ExtOperation(
		Operation operation,
		String method,
		Set<StatusCode> statusCodes,
		HydraClass returns,
		Shape returnShape,
		Shape addedDiff,
		Shape deletedDiff
	) {
		super(operation, method, statusCodes, returns);
		this.returnShape = returnShape;
		this.addedDiff = addedDiff;
		this.deletedDiff = deletedDiff;
	}

	public Shape getReturnShape() {
		return returnShape;
	}

	public Shape getAddedDiff() {
		return addedDiff;
	}

	public Shape getDeletedDiff() {
		return deletedDiff;
	}

	@Override
	public String toString() {
		return "ExtOperation [returnShape=" + returnShape + ", addedDiff=" + addedDiff + ", deletedDiff=" + deletedDiff
			+ "]";
	}

}
