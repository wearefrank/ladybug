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
package nl.nn.testtool;

import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.w3c.dom.Node;

import lombok.SneakyThrows;
import nl.nn.testtool.util.XmlUtil;
import nl.nn.xmldecoder.XMLDecoder;

/**
 * Default implementation of {@link MessageEncoder} used by {@link TestTool} that provides a basic set of
 * encoding/decoding methods.
 * 
 * @author Jaco de Groot
 */
public class MessageEncoderImpl implements MessageEncoder {
	private static final String XML_ENCODER = "XMLEncoder";
	private static final String BASE64_ENCODER = "Base64";
	private static final String TO_STRING_ENCODER = "toString()";
	private static final String DOM_NODE_ENCODER = "XmlUtil.nodeToString()";
	// Don't use static final SimpleDateFormat, see SimpleDateFormat javadoc: It is recommended to create separate format instances for each thread.
	private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
	private static final String DATE_ENCODER = "SimpleDateFormat(\"" + DATE_PATTERN + "\")";

	@Override
	@SneakyThrows(UnsupportedEncodingException.class)
	public ToStringResult toString(Object message) {
		ToStringResult toStringResult;
		if (message == null) {
			toStringResult = new ToStringResult(null, null);
		} else {
			if (message instanceof String) {
				toStringResult = new ToStringResult((String)message, null);
			} else if (message instanceof Writer) {
				toStringResult = new ToStringResult(
						"Implement MessageCapturer or use MessageCapturerImpl to get the content of this writer: "
						+ message.getClass().getTypeName(), null);
			} else if (message instanceof OutputStream) {
				toStringResult = new ToStringResult(
						"Implement MessageCapturer or use MessageCapturerImpl to get the content of this output stream: "
						+ message.getClass().getTypeName(), null);
			} else if (message instanceof Node) {
				Node node = (Node)message;
				toStringResult = new ToStringResult(XmlUtil.nodeToString(node), DOM_NODE_ENCODER);
			} else if (message instanceof Date) {
				toStringResult = new ToStringResult(new SimpleDateFormat(DATE_PATTERN).format((Date)message), DATE_ENCODER);
			} else if (message instanceof byte[]) {
				toStringResult = new ToStringResult(java.util.Base64.getEncoder().encodeToString((byte[])message), BASE64_ENCODER);
			} else {
				String xml = null;
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				XMLEncoder encoder = new XMLEncoder(byteArrayOutputStream);
				XMLEncoderExceptionListener exceptionListener = new XMLEncoderExceptionListener();
				encoder.setExceptionListener(exceptionListener);
				encoder.writeObject(message);
				encoder.close();
				xml = byteArrayOutputStream.toString("UTF-8");
				if (exceptionListener.isExceptionThrown()) {
					// Object doesn't seem to be a bean
					toStringResult = new ToStringResult(message.toString(), TO_STRING_ENCODER);
				} else {
					toStringResult = new ToStringResult(xml, XML_ENCODER);
				}
			}
			toStringResult.setMessageClassName(message.getClass().getTypeName());
		}
		return toStringResult;
	}

	@Override
	@SneakyThrows
	public Object toObject(String message, String encoding) {
		if (encoding == BASE64_ENCODER) {
			return java.util.Base64.getDecoder().decode(message);
		} else if (encoding == XML_ENCODER) {
			ByteArrayInputStream byteArrayInputStream = null;
			byteArrayInputStream = new ByteArrayInputStream(message.getBytes("UTF-8"));
			XMLDecoder xmlDecoder = new XMLDecoder(byteArrayInputStream);
			return xmlDecoder.readObject();
		} else if (encoding == DATE_ENCODER) {
			return new SimpleDateFormat(DATE_PATTERN).parse(message);
		} else {
			return message;
		}
	}

}
