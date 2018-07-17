 package nl.nn.testtool.metadata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.saxon.trans.XPathException;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.util.LogUtil;
import nl.nn.testtool.util.XmlUtil;

import org.apache.log4j.Logger;

/**
 * @author Jaco de Groot
 */
public class XpathMetadataFieldExtractor extends DefaultValueMetadataFieldExtractor {
	private static Logger log = LogUtil.getLogger(XpathMetadataFieldExtractor.class);
	protected String xpath;
	protected XmlUtil.XPathEvaluator xpathEvaluator;
	protected String extractFrom = "first";

	public void setXpath(String xpath) throws XPathException {
		this.xpath = xpath;
		if (xpath == null) {
			xpathEvaluator = null;
		} else {
			xpathEvaluator = XmlUtil.createXPathEvaluator(xpath);
		}
	}

	public void setExtractFrom(String extractFrom) {
		this.extractFrom = extractFrom;
	}

	public Object extractMetadata(Report report) {
		String value = null;
		List extractFromList = null;
		if (extractFrom.equals("all")) {
			extractFromList = report.getCheckpoints();
		} else {
			extractFromList = new ArrayList();
			if (report.getCheckpoints().size() > 0) {
				if (extractFrom.equals("first")) {
					extractFromList.add(report.getCheckpoints().get(0));
				} else if (extractFrom.equals("last")) {
					extractFromList.add(report.getCheckpoints().get(report.getCheckpoints().size() - 1));
				}
			}
		}
		Iterator iterator = extractFromList.iterator();
		while (value == null && iterator.hasNext()) {
			String message = ((Checkpoint)iterator.next()).getMessage();
			if (message != null) {
				try { 
					value = xpathEvaluator.evaluate(message);
				} catch (XPathException e) {
					log.debug("The message probably isn't in XML format", e);
				}
			}
		}
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}

}
