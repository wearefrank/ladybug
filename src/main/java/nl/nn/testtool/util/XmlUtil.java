/*
   Copyright 2018 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import lombok.SneakyThrows;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;

public class XmlUtil {
	private static final net.sf.saxon.sxpath.XPathEvaluator evaluator = 
			new net.sf.saxon.sxpath.XPathEvaluator();
	static {
		// Prevent error messages being printed to system out (for example when
		// source xml is invalid).
		evaluator.getConfiguration().setErrorListener(new XmlUtilErrorListener());
	}

	public static class XPathEvaluator {
		net.sf.saxon.sxpath.XPathExpression expression;

		private XPathEvaluator(String xpath) throws XPathException {
			expression = evaluator.createExpression(xpath);
		}

		public String evaluate(String xml) throws XPathException {
			Source source = createXmlSourceFromString(xml);
			return evaluate(source);
		}

		public String evaluate(Source xml) throws XPathException {
			Object o = expression.evaluateSingle(xml);
			if (o == null) {
				return null;
			} else if (o instanceof Item) {
				return ((Item)o).getStringValue();
			} else {
				return o.toString();
			}
		}
	}

	public static XPathEvaluator createXPathEvaluator(String xpath) throws XPathException {
		return new XPathEvaluator(xpath);
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

	@SneakyThrows
	public static String nodeToString(Node node) {
		Transformer transformer = getTransformerFactory().newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		StringWriter stringWriter = new StringWriter();
		transformer.transform(new DOMSource(node), new StreamResult(stringWriter));
		return stringWriter.toString();
	}
}

class XmlUtilErrorListener implements ErrorListener {

	public void warning(TransformerException exception)
			throws TransformerException {
	}

	public void error(TransformerException exception)
			throws TransformerException {
	}

	public void fatalError(TransformerException exception)
			throws TransformerException {
	}
	
}

