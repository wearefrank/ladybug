/*
   Copyright 2025 WeAreFrank!

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
package org.wearefrank.ladybug.metadata;

import org.wearefrank.ladybug.util.XmlUtil;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface ExtractionStrategy {
	Optional<String> extract(String message);
}

class RegexExtractionStrategy implements ExtractionStrategy {
	private Pattern pattern;

	public RegexExtractionStrategy(String pattern) {
		setRegexPattern(pattern);
	}

	private void setRegexPattern(String regex) {
		pattern = (regex == null || regex.isEmpty()) ? null : Pattern.compile(regex);
	}

	@Override
	public Optional<String> extract(String message) {
		if (message == null || message.isEmpty() || pattern == null) {
			return Optional.empty();
		}

		Matcher matcher = pattern.matcher(message);
		if (matcher.find()) {
			return Optional.ofNullable(matcher.groupCount() > 0 ? matcher.group(1) : matcher.group(0));
		}
		return Optional.empty();
	}
}

class XpathExtractionStrategy implements ExtractionStrategy {
	private XPathExpression xpathExpression;

	public XpathExtractionStrategy(String xpath) throws XPathExpressionException {
		if (xpath != null) {
			xpathExpression = XmlUtil.createXPathExpression(xpath);
		}
	}

	@Override
	public Optional<String> extract(String message) {
		try {
			return Optional.ofNullable(xpathExpression.evaluate(XmlUtil.createXmlSourceFromString(message)));
		} catch (XPathExpressionException e) {
			return Optional.empty();
		}
	}
}
