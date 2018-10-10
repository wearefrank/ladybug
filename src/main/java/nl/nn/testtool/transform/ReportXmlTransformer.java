/*
   Copyright 2018 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.testtool.transform;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import nl.nn.testtool.util.LogUtil;

import org.apache.log4j.Logger;

public class ReportXmlTransformer {
	private Logger log = LogUtil.getLogger(this);
	private String xslt;
	private Transformer transformer = null;
	
	public void setXsltResource(String xsltResource) {
		StringBuffer result = new StringBuffer();
		InputStream stream = getClass().getClassLoader().getResourceAsStream(xsltResource);
		byte[] bytes = new byte[1024];
		int i;
		try {
			i = stream.read(bytes);
			while (i != -1) {
				result.append(new String(bytes, 0, i, "UTF-8"));
				i = stream.read(bytes);
			}
		} catch (UnsupportedEncodingException unsupportedEncodingException) {
			log.error("UnsupportedEncodingException reading xslt", unsupportedEncodingException);
		} catch (IOException ioException) {
			log.error("IOException reading xslt", ioException);
		}
		setXslt(result.toString());
	}

	public String setXslt(String xslt) {
		this.xslt = xslt;
		String error = null;
		TransformerFactory transformerFactory = new net.sf.saxon.TransformerFactoryImpl();
		TransformerFactoryErrorListener transformerFactoryErrorListener = new TransformerFactoryErrorListener();
		transformerFactory.setErrorListener(transformerFactoryErrorListener);
		try {
			transformer = transformerFactory.newTransformer(new StreamSource(new StringReader(xslt)));
		} catch (TransformerConfigurationException e) {
			String message = "Could not create transformer: " + e.getMessageAndLocation() + " " + transformerFactoryErrorListener.getErrorMessages();
			log.error(message);
			error = message;
		}
		return error;
	}
	
	public String getXslt() {
		return xslt;
	}
	
	public String transform(String xml) {
		StreamSource streamSource = new StreamSource(new StringReader(xml));
		StringWriter stringWriter = new StringWriter();
		StreamResult streamResult = new StreamResult(stringWriter);
		try {
			transformer.transform(streamSource, streamResult);
		} catch (TransformerException e) {
			// TODO Foutmelding aan gebruiker geven en dan loggen op debug i.p.v. error
			log.error("Could not transform xml", e);
		}
		xml = stringWriter.toString();
		return xml;
	}
	
}

class TransformerFactoryErrorListener implements ErrorListener {
	private Logger log = LogUtil.getLogger(this);
	String errorMessages;
		
	public void error(TransformerException exception) {
		logAndStoreErrorMessage("TransformerFactoryErrorListener error: " + exception.getMessage());
	}
		
	public void fatalError(TransformerException exception) {
		logAndStoreErrorMessage("TransformerFactoryErrorListener error: " + exception.getMessage());
	}
		
	public void warning(TransformerException exception) {
		logAndStoreErrorMessage("TransformerFactoryErrorListener error: " + exception.getMessage());
	}
		
	public String getErrorMessages() {
		return errorMessages;
	}
		
	private void logAndStoreErrorMessage(String errorMessage) {
		log.error(errorMessage);
		if (errorMessages == null) {
			errorMessages = "[" + errorMessage + "]";
		} else {
			errorMessages = errorMessages + " [" + errorMessage + "]";
		}
	}
}
