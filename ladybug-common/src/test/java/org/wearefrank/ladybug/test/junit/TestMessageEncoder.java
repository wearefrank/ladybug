/*
   Copyright 2021, 2025 WeAreFrank!

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
package org.wearefrank.ladybug.test.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.w3c.dom.Node;
import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.MessageEncoderImpl;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.util.XmlUtil;
import org.xml.sax.SAXException;

/**
 * @author Jaco de Groot
 */
public class TestMessageEncoder {
	public static final String RESOURCE_PATH = "org/wearefrank/ladybug/test/junit/";

	@Rule
	public TestName name = new TestName();

	@Test
	public void testToString() throws SAXException, IOException, ParserConfigurationException, StorageException {
		TestTool testTool = new TestTool();
		Report report = new Report();
		report.setTestTool(testTool);
		Checkpoint checkpoint = new Checkpoint();
		checkpoint.setReport(report);
		String actual;

		// Test Integer
		actual = testTool.getMessageEncoder().toString(10, null).getString();
		actual = ReportRelatedTestCase.applyXmlEncoderIgnores(actual);
		ReportRelatedTestCase.assertXml(RESOURCE_PATH, name.getMethodName(), actual);
		checkpoint.setMessage(actual);
		checkpoint.setEncoding(MessageEncoderImpl.XML_ENCODER);
		assertEquals(10, checkpoint.getMessageAsObject());
		assertEquals(new Integer(10), checkpoint.getMessageAsObject(new Integer(1)));

		// Test Date
		Date date = new Date(0L);
		actual = testTool.getMessageEncoder().toString(date, null).getString();
		String offset = new SimpleDateFormat("Z").format(date);
		assertEquals("1970-01-01T" + offset.substring(1, 3) + ":" + offset.substring(3, 5) + ":00.000" + offset, actual);
		checkpoint.setMessage(actual);
		checkpoint.setEncoding(MessageEncoderImpl.DATE_ENCODER);
		assertEquals(new Date(0L), checkpoint.getMessageAsObject());
		assertEquals(new Date(0L), checkpoint.getMessageAsObject(new Date(10L)));

		// Test Node
		Node node = XmlUtil.stringToNode("<test/>");
		assertTrue(node instanceof Node);
		assertEquals("test", node.getNodeName());
		actual = testTool.getMessageEncoder().toString(node, null).getString();
		assertEquals("<test/>", actual);
		checkpoint.setMessage(actual);
		checkpoint.setEncoding(MessageEncoderImpl.DOM_NODE_ENCODER);
		node = (Node) checkpoint.getMessageAsObject();
		assertTrue(node instanceof Node);
		assertEquals("test", node.getNodeName());
		node = checkpoint.getMessageAsObject(XmlUtil.stringToNode("<test2/>"));
		assertTrue(node instanceof Node);
		assertEquals("test", node.getNodeName());
	}

}
