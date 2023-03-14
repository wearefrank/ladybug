/*
   Copyright 2020, 2022, 2023 WeAreFrank!, 2018 Nationale-Nederlanden

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
package nl.nn.testtool.metadata;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.MetadataFieldExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.util.XmlUtil;

/**
 * @author Jaco de Groot
 */
public class XpathMetadataFieldExtractor extends DefaultValueMetadataFieldExtractor {
	private static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	protected String xpath;
	protected XPathExpression xpathExpression;
	protected String extractFrom = "first";
	private MetadataFieldExtractor delegate = null;

	public void setXpath(String xpath) throws XPathExpressionException {
		this.xpath = xpath;
		if (xpath == null) {
			xpathExpression = null;
		} else {
			xpathExpression = XmlUtil.createXPathExpression(xpath);
		}
	}

	public void setExtractFrom(String extractFrom) {
		this.extractFrom = extractFrom;
	}

	/**
	 * If there was no abort then calculate the status from the delegate MetadataFieldextractor.
	 */
	public void setDelegate(MetadataFieldExtractor delegate) {
		this.delegate = delegate;
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
					value = xpathExpression.evaluate(XmlUtil.createXmlSourceFromString(message));
					if(StringUtils.isBlank(value) && (delegate != null)) {
						value = (String) delegate.extractMetadata(report);
					}
					if(StringUtils.isBlank(value) && (defaultValue != null)) {
						value = defaultValue;
					}
				} catch (XPathExpressionException e) {
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
