/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.testtool.test.junit;

import java.io.IOException;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import junit.framework.TestCase;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.MessageEncoder;
import nl.nn.testtool.MessageEncoderImpl;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.XmlUtil;

/**
 * @author Jaco de Groot
 */
public class TestMessageEncoder extends TestCase {
	public static final String RESOURCE_PATH = "nl/nn/testtool/test/junit/";

	public void testToString() throws SAXException, IOException, ParserConfigurationException, StorageException {
		MessageEncoder messageEncoder = new MessageEncoderImpl();
		String actual;
		Checkpoint checkpoint = new Checkpoint();

		// Test Integer
		actual = messageEncoder.toString(10).getString();
		actual = ReportRelatedTestCase.applyXmlEncoderIgnores(actual);
		ReportRelatedTestCase.assertXml(RESOURCE_PATH, getName(), actual);
		checkpoint.setMessage(actual);
		checkpoint.setEncoding(MessageEncoderImpl.XML_ENCODER);
		assertEquals(10, messageEncoder.toObject(checkpoint));

		// Test Date
		actual = messageEncoder.toString(new Date(0L)).getString();
		assertEquals("1970-01-01 01:00:00.000", actual);
		checkpoint.setMessage(actual);
		checkpoint.setEncoding(MessageEncoderImpl.DATE_ENCODER);
		assertEquals(new Date(0L), messageEncoder.toObject(checkpoint));

		// Test Node
		Node node = XmlUtil.stringToNode("<test/>");
		assertTrue(node instanceof Node);
		assertEquals("test", node.getNodeName());
		actual = messageEncoder.toString(node).getString();
		assertEquals("<test/>", actual);
		checkpoint.setMessage(actual);
		checkpoint.setEncoding(MessageEncoderImpl.DOM_NODE_ENCODER);
		node = (Node)messageEncoder.toObject(checkpoint);
		assertTrue(node instanceof Node);
		assertEquals("test", node.getNodeName());
	}

}
