/*
   Copyright 2020-2022 WeAreFrank!, 2018 Nationale-Nederlanden

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

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.XmlWriter;
import org.w3c.dom.Node;
import org.xml.sax.*;
import org.xml.sax.ext.LexicalHandler;
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

	private static Map<String, TransformerPool> pools = new HashMap<>();
//
//	public static String transform(Source src, String xslt) {
//		TransformerFactory transformerFactory = TransformerFactory.newInstance();
//
//		URL xsltUrl = XmlUtils.class.getResource(xslt);
//		Transformer transformer;
//		try (InputStream fileInputStream = xsltUrl.openStream()) {
//			transformer = transformerFactory.newTransformer(new StreamSource(fileInputStream, xslt));
//			StringWriter sw = new StringWriter();
//			StreamResult result = new StreamResult(sw);
//			transformer.transform(src, result);
//			return sw.toString();
//		} catch (Exception e) {
//			System.err.println("Failed to transform source xml [" + src.getSystemId() + "] using styleSheet [" + xslt + "]: " + e);
//		}
//		return null;
//	}
//
	public static String transform(InputSource src, String xslt) throws IOException, SAXException, ConfigurationException, TransformerConfigurationException {
		TransformerPool tp = pools.get(xslt);
		if (tp == null) {
			try {
				tp = TransformerPool.configureTransformer(null, null, null, xslt, null, true, null);
				pools.put(xslt, tp);
			} catch (ConfigurationException e) {
				throw new RuntimeException("Error occured trying to instantiate TransformerPool for xslt [" + xslt + "]", e);
			}
		}
		XmlWriter writer = new XmlWriter();
		ContentHandler handler = tp.getTransformerFilter(null, writer, false, false);
		XmlUtils.parseXml(src, handler, null);
		return writer.toString();
	}
//
//	public static String nodeContentsToString(Node node) throws TransformerException {
//		StringBuilder result = new StringBuilder();
//		NodeList list = node.getChildNodes();
//		for (int i = 0; i < list.getLength(); i++) {
//			Node child = list.item(i);
//			result.append(nodeToString(child, false));
//		}
//		return result.toString();
//	}
//
//	public static String nodeToString(Node node, boolean useIndentation) throws TransformerException {
//		Transformer transformer = TransformerFactory.newInstance().newTransformer();
//		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
//		if (useIndentation) {
//			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//		}
//		StringWriter sw = new StringWriter();
//		transformer.transform(new DOMSource(node), new StreamResult(sw));
//		return sw.toString();
//	}
//
//	public static boolean isWellFormed(String input) {
//		XmlValidatorContentHandler xmlHandler = new XmlValidatorContentHandler(null, null, true);
////		XmlValidatorContentHandler xmlHandler = new XmlValidatorContentHandler();
//		XmlValidatorErrorHandler xmlValidatorErrorHandler = new XmlValidatorErrorHandler(xmlHandler, "Is not well formed");
//		xmlHandler.setXmlValidatorErrorHandler(xmlValidatorErrorHandler);
//		try {
//			// set ErrorHandler to prevent message in System.err: [Fatal Error] :-1:-1: Premature end of file.
//			parseXml(new InputSource(new StringReader(input)), xmlHandler, xmlValidatorErrorHandler);
//		} catch (Exception e) {
//			return false;
//		}
//		return true;
//	}
//
//	public static void parseXml(InputSource inputSource, ContentHandler handler, ErrorHandler errorHandler) throws IOException, SAXException {
//		XMLReader xmlReader;
//		try {
//			xmlReader = getXMLReader(handler);
//			if (errorHandler != null) {
//				xmlReader.setErrorHandler(errorHandler);
//			}
//		} catch (ParserConfigurationException e) {
//			throw new SaxException("Cannot configure parser",e);
//		}
//		xmlReader.parse(inputSource);
//	}

	private static XMLReader getXMLReader(ContentHandler handler) throws ParserConfigurationException, SAXException {
		XMLReader xmlReader = getXMLReader(true);
		xmlReader.setContentHandler(handler);
		if (handler instanceof LexicalHandler) {
			xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
		}
		if (handler instanceof ErrorHandler) {
			xmlReader.setErrorHandler((ErrorHandler)handler);
		}
		return xmlReader;
	}

	private static XMLReader getXMLReader(boolean namespaceAware) throws ParserConfigurationException, SAXException {
		SAXParserFactory factory = getSAXParserFactory(namespaceAware);
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		XMLReader xmlReader = factory.newSAXParser().getXMLReader();
		xmlReader.setEntityResolver(new NonResolvingExternalEntityResolver());
		return xmlReader;
	}

	public static SAXParserFactory getSAXParserFactory(boolean namespaceAware) {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(namespaceAware);
		return factory;
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
		Transformer transformer = getTransformerFactory().newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		StringWriter stringWriter = new StringWriter();
		transformer.transform(new DOMSource(node), new StreamResult(stringWriter));
		return stringWriter.toString();
	}

	public static Node stringToNode(String string) throws SAXException, IOException, ParserConfigurationException {
		return getDocumentBuilderFactory().newDocumentBuilder()
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
