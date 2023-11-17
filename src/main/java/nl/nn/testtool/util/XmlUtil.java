/*
   Copyright 2020-2023 WeAreFrank!, 2018 Nationale-Nederlanden

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

import java.io.ByteArrayInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import lombok.SneakyThrows;
import net.sf.saxon.Configuration;
import net.sf.saxon.xpath.XPathEvaluator;

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
		} catch (SAXException | IOException | ParserConfigurationException e) {
			return false;
		}
		return true;
	}

	public static TransformerFactory getTransformerFactory() {
		return new net.sf.saxon.TransformerFactoryImpl();
	}

	/**
	 * Searches for the given node in a document
	 * @param nodeName The name of the node as a String
	 * @param doc The Document in which to look for the node
	 * @return returns true if the given node exists
	 */
	public static boolean fileHasNode(String nodeName, Document doc) {
		try {
			if (nodeName.contains(":")) {
				//if the given node has a namespace prefix, strip the prefix.
				return getNodesByXPath("//*[local-name()='" + nodeName.substring(nodeName.indexOf(":") + 1) + "']", doc).getLength() != 0;
			}
			return getNodesByXPath("//*[local-name()='" + nodeName + "']", doc).getLength() != 0;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets the nodelist from a document with xPath expression
	 * @param doc document to convert to Nodelist
	 * @param xPathExpression given xPathExpression to search by
	 * @return return the nodelist from xPathExpression
	 * @throws XPathExpressionException if there is an error in the XPath expression
	 */
	public static NodeList getNodesByXPath(String xPathExpression, Document doc) throws XPathExpressionException {
		return (NodeList) createXPathExpression(xPathExpression).evaluate(doc.getDocumentElement(), XPathConstants.NODESET);
	}

	@SneakyThrows
	public static String nodeToString(Node node) {
		Transformer transformer = getTransformerFactory().newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		StringWriter stringWriter = new StringWriter();
		transformer.transform(new DOMSource(node), new StreamResult(stringWriter));
		return stringWriter.toString();
	}

	public static Node stringToNode(String string) throws SAXException, IOException {
		return DocumentUtil.getDocumentBuilder()
				.parse(new ByteArrayInputStream(string.getBytes())).getDocumentElement();
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
