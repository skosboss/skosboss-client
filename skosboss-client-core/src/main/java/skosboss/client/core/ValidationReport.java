package skosboss.client.core;

import java.util.Set;

public class ValidationReport {

	private boolean conforms;
	private Set<ValidationResult> results;
	
	public ValidationReport(
		boolean conforms,
		Set<ValidationResult> results
	) {
		this.conforms = conforms;
		this.results = results;
	}

	public boolean getConforms() {
		return conforms;
	}

	public Set<ValidationResult> getResults() {
		return results;
	}

	@Override
	public String toString() {
		return "ValidationReport [conforms=" + conforms + ", results=" + results + "]";
	}

}
