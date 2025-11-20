/*
   Copyright 2020-2025 WeAreFrank!, 2018-2019 Nationale-Nederlanden

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
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Setter;
import nl.nn.testtool.util.XmlUtil;

@Singleton
public class ReportXmlTransformer {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private @Setter @Inject @Autowired String xsltResource;
	private String xslt;
	private Transformer transformer;
	private String createTransformerError;
	private Exception createTransformerException;

	@PostConstruct
	public void init() {
		StringBuilder result = new StringBuilder();
		InputStream stream = getClass().getClassLoader().getResourceAsStream(xsltResource);
		if (stream == null) {
			createTransformerError = "Could not find xslt resource: " + xsltResource;
		} else {
			byte[] bytes = new byte[1024];
			int i;
			try {
				i = stream.read(bytes);
				while (i != -1) {
					result.append(new String(bytes, 0, i, "UTF-8"));
					i = stream.read(bytes);
				}
			} catch (UnsupportedEncodingException unsupportedEncodingException) {
				createTransformerError = "UnsupportedEncodingException reading xslt";
				createTransformerException = unsupportedEncodingException;
				log.debug(createTransformerError, createTransformerException);
			} catch (IOException ioException) {
				createTransformerError = "IOException reading xslt";
				createTransformerException = ioException;
				log.debug(createTransformerError, createTransformerException);
			}
		}
		if (createTransformerError == null) {
			setXslt(result.toString());
		}
	}

	public void setXslt(String xslt) {
		this.xslt = xslt;
		TransformerFactory transformerFactory = XmlUtil.getTransformerFactory();
		TransformerFactoryErrorListener transformerFactoryErrorListener = new TransformerFactoryErrorListener();
		transformerFactory.setErrorListener(transformerFactoryErrorListener);
		try {
			transformer = transformerFactory.newTransformer(new StreamSource(new StringReader(xslt)));
		} catch (TransformerConfigurationException e) {
			createTransformerError = "Could not create transformer: " + e.getMessageAndLocation() + " " + transformerFactoryErrorListener.getErrorMessages();
			createTransformerException = e;
			log.debug(createTransformerError, createTransformerException);
		}
	}

	public String updateXslt(String xslt) {
		createTransformerError = null;
		createTransformerException = null;
		setXslt(xslt);
		if (createTransformerError == null) {
			return null;
		} else {
			return createTransformerError;
		}
	}

	public String getXslt() {
		return xslt;
	}

	public String transform(String xml) {
		StringWriter stringWriter = new StringWriter();
		if (createTransformerError != null) {
			printException(createTransformerError, createTransformerException, stringWriter);
			stringWriter.write("\n");
			printFirstXmlCharacters(xml, stringWriter);
		} else {
			StreamSource streamSource = new StreamSource(new StringReader(xml));
			StreamResult streamResult = new StreamResult(stringWriter);
			try {
				transformer.transform(streamSource, streamResult);
			} catch (TransformerException e) {
				String message = "Could not transform report xml";
				log.debug(message, e);
				printException(message, e, stringWriter);
				stringWriter.write("\n");
				printFirstXmlCharacters(xml, stringWriter);
			}
		}
		return stringWriter.toString();
	}

	private void printException(String message, Exception e, StringWriter stringWriter) {
		stringWriter.write(message);
		if (e != null) {
			stringWriter.write(": " + e.getMessage());
			stringWriter.write("\n\n");
			stringWriter.write("Stacktrace:\n");
			PrintWriter printWriter = new PrintWriter(stringWriter);
			e.printStackTrace(printWriter);
			printWriter.close();
		}
	}

	private void printFirstXmlCharacters(String xml, StringWriter stringWriter) {
		int i = 10000;
		if (xml.length() < i) {
			i = xml.length();
		}
		stringWriter.write("First " + i + " characters of xml to transform:\n" + xml.substring(0, i));
	}

}

class TransformerFactoryErrorListener implements ErrorListener {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
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
