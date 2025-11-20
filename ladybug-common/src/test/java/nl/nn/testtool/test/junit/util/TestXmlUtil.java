/*
   Copyright 2022, 2025 WeAreFrank!

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
package nl.nn.testtool.test.junit.util;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.junit.Test;
import org.junit.jupiter.api.function.Executable;

import nl.nn.testtool.util.XmlUtil;

/**
 * @author Jaco de Groot
 */
public class TestXmlUtil {

	@Test
	public void testXpath() throws XPathExpressionException {
		// Plain xml
		XPathExpression xpathExpression = XmlUtil.createXPathExpression("/root/b");
		String value =  xpathExpression.evaluate(XmlUtil.createXmlSourceFromString("<root><a>11</a><b>22</b></root>"));
		assertEquals("22", value);
		// SOAP
		xpathExpression = XmlUtil.createXPathExpression("local-name(/*[local-name()='Envelope']/*[local-name()='Body']/*)");
		value =  xpathExpression.evaluate(XmlUtil.createXmlSourceFromString(
				"<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ns=\"uri:test\">"
				+ "  <soap:Header/>"
				+ "  <soap:Body><Payload/></soap:Body>"
				+ "</soap:Envelope>"));
		assertEquals("Payload", value);
		// Invalid xml
		XPathExpression finalXPathExpression = xpathExpression;
		assertThrows(XPathExpressionException.class, new Executable() {
			public void execute() throws Throwable {
				finalXPathExpression.evaluate("invalid");
			}
		});
	}

}
