package skosboss.client.core;

import java.io.IOException;
import java.io.StringReader;
import okhttp3.Response;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

public class OperationResult {

	private Model body;
	private Response response;
	
	public OperationResult(Model body, Response response) {
		this.body = body;
		this.response = response;
	}
	
	public static OperationResult create(Response response) {
		Model responseBody = new LinkedHashModel();
		if (response.isSuccessful()) {
			try {
				responseBody = Rio.parse(new StringReader(response.body().string()), "", RDFFormat.TURTLE);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
		return new OperationResult(responseBody, response);
	}
	

	public Model getBody() {
		return body;
	}

	public void setBody(Model body) {
		this.body = body;
	}

	public Response getResponse() {
		return response;
	}

	public void setResponse(Response response) {
		this.response = response;
	}
}
