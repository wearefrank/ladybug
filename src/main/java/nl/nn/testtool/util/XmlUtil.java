package nl.nn.testtool.util;

import java.io.StringReader;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

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

