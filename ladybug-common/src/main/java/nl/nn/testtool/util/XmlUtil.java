/*
   Copyright 2020-2022, 2024, 2025 WeAreFrank!, 2018 Nationale-Nederlanden

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
package nl.nn.testtool.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import lombok.SneakyThrows;
import net.sf.saxon.Configuration;
import net.sf.saxon.xpath.XPathEvaluator;
import nl.nn.testtool.storage.StorageException;

public class XmlUtil {
	private static final XPathEvaluator xpathEvaluator = new XPathEvaluator();
	static {
		// Prevent error messages being printed to system err (for example when source xml is invalid).
		Configuration configuration = xpathEvaluator.getConfiguration();
		DummyOutputStream dummyOutputStream = new DummyOutputStream(configuration.getStandardErrorOutput());
		configuration.setStandardErrorOutput(new PrintStream(dummyOutputStream));
	}

	public static XPathExpression createXPathExpression(String xpath) throws XPathExpressionException {
		return xpathEvaluator.compile(xpath);
	}

	public static Source createXmlSourceFromString(String xml) {
		StringReader stringReader = new StringReader(xml);
		return new StreamSource(stringReader);
	}

	public static boolean isXml(String xml) {
		InputSource inputSource = new InputSource(new StringReader(xml));
		SAXParserFactory factory = new org.apache.xerces.jaxp.SAXParserFactoryImpl();
		try {
			factory.newSAXParser().parse(inputSource, new DefaultHandler());
		} catch (SAXException e) {
			return false;
		} catch (IOException e) {
			return false;
		} catch (ParserConfigurationException e) {
			return false;
		}
		return true;
	}

	public static boolean isJavaBeansXml(File file) throws StorageException {
		String line;
		try (FileReader fileReader = new FileReader(file);
				BufferedReader bufferedReader = new BufferedReader(fileReader)) {
			line = bufferedReader.readLine();
			if (line != null) {
				String firstLine = line;
				line = bufferedReader.readLine();
				if (line != null) {
					if (firstLine.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
							&& line.startsWith("<java version=\"")) {
						return true;
					}
				}
			}
		} catch (IOException e) {
			throw new StorageException("IOException while trying to determine whether '" + file.getName() +
					"' is a JavaBeans XML file", e);
		}
		return false;
	}

	public static TransformerFactory getTransformerFactory() {
		return new net.sf.saxon.TransformerFactoryImpl();
	}

	public static DocumentBuilderFactory getDocumentBuilderFactory() {
		// Deprecated
		// return new net.sf.saxon.dom.DocumentBuilderFactoryImpl();
		// Xerces
		// return new org.apache.xerces.jaxp.DocumentBuilderFactoryImpl();
		return DocumentBuilderFactory.newInstance();
	}

	@SneakyThrows
	public static String nodeToString(Node node) {
		if (node.getNodeType() == Node.CDATA_SECTION_NODE || node.getNodeType() == Node.TEXT_NODE) {
			return node.getNodeValue();
		} else {
			Transformer transformer = getTransformerFactory().newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter stringWriter = new StringWriter();
			transformer.transform(new DOMSource(node), new StreamResult(stringWriter));
			return stringWriter.toString();
		}
	}

	public static Node stringToNode(String string) throws SAXException, IOException, ParserConfigurationException {
		return getDocumentBuilderFactory().newDocumentBuilder()
				.parse(new ByteArrayInputStream(string.getBytes())).getDocumentElement();
	}

	public static Node fileToNode(File file) throws SAXException, IOException, ParserConfigurationException {
		return getDocumentBuilderFactory().newDocumentBuilder()
				.parse(new FileInputStream(file)).getDocumentElement();
	}
}

class DummyOutputStream extends FilterOutputStream {

	public DummyOutputStream(OutputStream out) {
		super(out);
	}

	@Override
	public void write(byte[] arg0) throws IOException {
	}

	@Override
	public void write(byte[] arg0, int arg1, int arg2) throws IOException {
	}

	@Override
	public void write(int arg0) throws IOException {
	}

}
