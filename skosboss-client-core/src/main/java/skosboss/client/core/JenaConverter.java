package skosboss.client.core;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.jena.rdf.model.ModelFactory;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

// TODO use trig to preserve graphs

class JenaConverter {

	org.apache.jena.rdf.model.Model toJena(Model model) {
		return jenaModelFromTurtle(asTurtle(model));
	}
	
	Model toRdf4j(org.apache.jena.rdf.model.Model model) {
		return rdf4jModelfromTurtle(asTurtle(model));
	}

	private String asTurtle(org.apache.jena.rdf.model.Model model) {
		StringWriter writer = new StringWriter();
		model.write(writer, "TURTLE");
		return writer.toString();
	}
	
	private org.apache.jena.rdf.model.Model jenaModelFromTurtle(String ttl) {
		StringReader reader = new StringReader(ttl);
		return ModelFactory.createDefaultModel()
			.read(
				reader,
				"http://none.com/",
				"TURTLE"
			);		
	}
	
	private String asTurtle(Model model) {
		StringWriter writer = new StringWriter();
		Rio.write(model, writer, RDFFormat.TURTLE);
		return writer.toString();
	}
	
	private Model rdf4jModelfromTurtle(String ttl) {
		StringReader reader = new StringReader(ttl);
		try {
			return Rio.parse(
				reader,
				"http://none.com/",
				RDFFormat.TURTLE
			);
		}
		catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
			throw new RuntimeException("error reading rdf4j model from turtle string [" + ttl + "]", e);
		}
	}
	
}
