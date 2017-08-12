package skosboss.client.hydra_model;

public class StatusCode {

	private int code;
	private String description;

	public StatusCode(int code, String description) {
		this.code = code;
		this.description = description;
	}

	public int getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return "StatusCode [code=" + code + ", description=" + description + "]";
	}

}
