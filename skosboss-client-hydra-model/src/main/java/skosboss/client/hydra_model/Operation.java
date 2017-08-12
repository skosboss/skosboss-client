package skosboss.client.hydra_model;

import java.util.Set;

public class Operation extends HydraResource {

	private String method;
	private Set<StatusCode> statusCodes;
	private HydraClass returns;
	
	public Operation(
		Operation operation,
		String method,
		Set<StatusCode> statusCodes,
		HydraClass returns
	) {
		super(operation);
		this.method = method;
		this.statusCodes = statusCodes;
		this.returns = returns;
	}

	public String getMethod() {
		return method;
	}

	public Set<StatusCode> getStatusCodes() {
		return statusCodes;
	}

	public HydraClass getReturns() {
		return returns;
	}

	@Override
	public String toString() {
		return "Operation [method=" + method + ", statusCodes=" + statusCodes + ", returns=" + returns + "]";
	}

}
