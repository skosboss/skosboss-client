package skosboss.client.hydra_model;

public class SupportedProperty {

	private Property property;
	private boolean readable;
	private boolean writable;
	private String title;
	
	public SupportedProperty(
		Property property,
		boolean readable,
		boolean writable,
		String title
	) {
		this.property = property;
		this.readable = readable;
		this.writable = writable;
		this.title = title;
	}

	public Property getProperty() {
		return property;
	}

	public boolean isReadable() {
		return readable;
	}

	public boolean isWritable() {
		return writable;
	}
	
	public String getTitle() {
		return title;
	}

	@Override
	public String toString() {
		return "SupportedProperty [property=" + property + ", readable=" + readable + ", writable=" + writable
			+ ", title=" + title + "]";
	}

}
