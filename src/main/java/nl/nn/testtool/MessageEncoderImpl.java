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
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
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
	public static final String XML_ENCODER = "XMLEncoder";
	public static final String UTF8_ENCODER = "UTF-8";
	public static final String CHARSET_ENCODER_PREFIX = "CHARSET-";
	public static final String BASE64_ENCODER = "Base64";
	public static final String TO_STRING_ENCODER = "toString()";
	public static final String DOM_NODE_ENCODER = "XmlUtil.nodeToString()";
	// Don't use static final SimpleDateFormat, see SimpleDateFormat javadoc: It is recommended to create separate format instances for each thread.
	public static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	public static final String DATE_ENCODER = "SimpleDateFormat(\"" + DATE_PATTERN + "\")";
	public static final String WAITING_FOR_STREAM_MESSAGE = "Waiting for stream to be read, captured and closed...";

	@Override
	@SneakyThrows(UnsupportedEncodingException.class)
	public ToStringResult toString(Object message, String charset) {
		ToStringResult toStringResult;
		if (message == null) {
			toStringResult = new ToStringResult(null, null);
		} else if (message instanceof String) {
			toStringResult = new ToStringResult((String)message, null);
		} else {
			if (message instanceof byte[]) {
				String encoding;
				if (charset == null) {
					charset = "UTF-8";
					encoding = UTF8_ENCODER;
				} else {
					encoding = CHARSET_ENCODER_PREFIX + charset;
				}
				CharsetDecoder charsetDecoder = Charset.forName(charset).newDecoder();
				try {
					CharBuffer charBuffer = charsetDecoder.decode(ByteBuffer.wrap((byte[])message));
					toStringResult = new ToStringResult(charBuffer.toString(), encoding);
				} catch (CharacterCodingException e) {
					toStringResult = new ToStringResult(java.util.Base64.getEncoder().encodeToString((byte[])message),
							BASE64_ENCODER);
				}
			} else if (message instanceof Writer || message instanceof OutputStream) {
				toStringResult = new ToStringResult(WAITING_FOR_STREAM_MESSAGE, null);
			} else if (message instanceof Node) {
				Node node = (Node)message;
				toStringResult = new ToStringResult(XmlUtil.nodeToString(node), DOM_NODE_ENCODER);
			} else if (message instanceof Date) {
				toStringResult = new ToStringResult(new SimpleDateFormat(DATE_PATTERN).format((Date)message),
						DATE_ENCODER);
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
	public Object toObject(Checkpoint checkpoint) {
		// Can be null when called from toObject(Checkpoint originalCheckpoint, T messageToStub), see javadoc on param
		// originalCheckpoin in MessageEncoder
		if (checkpoint == null || checkpoint.getMessage() == null) {
			return null;
		} else {
			String message = checkpoint.getMessage();
			String encoding = checkpoint.getEncoding();
			if (encoding == null) {
				if (checkpoint.getStreaming() == null) {
					return message;
				} else {
					return new StringReader(message);
				}
			} else if (encoding.equals(UTF8_ENCODER) || encoding.startsWith(CHARSET_ENCODER_PREFIX)) {
				if (encoding.startsWith(CHARSET_ENCODER_PREFIX)) {
					encoding = encoding.substring(CHARSET_ENCODER_PREFIX.length());
				}
				CharsetEncoder charsetEncoder = Charset.forName("UTF-8").newEncoder();
				ByteBuffer byteBuffer = charsetEncoder.encode(CharBuffer.wrap(message));
				byte[] bytes = new byte[byteBuffer.remaining()];
				byteBuffer.get(bytes);
				if (checkpoint.getStreaming() == null) {
					return bytes;
				} else {
					return new ByteArrayInputStream(bytes);
				}
			} else if (encoding.equals(BASE64_ENCODER)) {
				byte[] bytes = java.util.Base64.getDecoder().decode(message);
				if (checkpoint.getStreaming() == null) {
					return bytes;
				} else {
					return new ByteArrayInputStream(bytes);
				}
			} else if (encoding.equals(DOM_NODE_ENCODER)) {
				return XmlUtil.stringToNode(message);
			} else if (encoding.equals(DATE_ENCODER)) {
				return new SimpleDateFormat(DATE_PATTERN).parse(message);
			} else if (encoding.equals(XML_ENCODER)) {
				ByteArrayInputStream byteArrayInputStream = null;
				byteArrayInputStream = new ByteArrayInputStream(message.getBytes("UTF-8"));
				XMLDecoder xmlDecoder = new XMLDecoder(byteArrayInputStream);
				return xmlDecoder.readObject();
			} else {
				return message;
			}
		}
	}

	@Override
	@SneakyThrows
	@SuppressWarnings("unchecked")
	public <T> T toObject(Checkpoint originalCheckpoint, T messageToStub) {
		// In case a stream is stubbed the replaced stream needs to be closed as the system under test will read and
		// close the stub which would leave the replaced stream unclosed
		if (messageToStub instanceof AutoCloseable) {
			((AutoCloseable)messageToStub).close();
		}
		return (T) toObject(originalCheckpoint);
	}

}
