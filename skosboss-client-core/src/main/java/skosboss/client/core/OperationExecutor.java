package skosboss.client.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class OperationExecutor {
	
	String hostUri;
	OkHttpClient client;
	
	public OperationExecutor(String hostUri, OkHttpClient client) {
		this.hostUri = hostUri;
		this.client = client;
	}
	
	public OperationResult execute(TemplatedOperation operation) throws IOException {
		String iri = operation.getIri();
		RequestBody body = RequestBody.create(null, "");
		Properties props = new Properties();
		props.load(new FileInputStream("config.properties"));
		String credentials = Credentials.basic(props.getProperty("api.user"), props.getProperty("api.pass"));
		Request request = new Request.Builder()
				.url(hostUri + iri)
				.header("Authorization", credentials)
				.post(body)
				.build();
		
		return OperationResult.create(client.newCall(request).execute());
	}

}
