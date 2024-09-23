/*
   Copyright 2021-2023 WeAreFrank!

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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
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
	public static final String THROWABLE_ENCODER = "printStackTrace()";
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
			if (message instanceof Boolean) {
				toStringResult = new ToStringResult(Boolean.toString((Boolean) message), null, Boolean.class.getName());
			}
			else if (message instanceof byte[]) {
				String encoding;
				if (charset == null) {
					charset = "UTF-8";
					encoding = UTF8_ENCODER;
				} else {
					encoding = CHARSET_ENCODER_PREFIX + charset;
				}
				CharsetDecoder charsetDecoder = Charset.forName(charset).newDecoder();
				try {
					// This will throw an exception were new String(((byte[])message), "UTF-8") would use the
					// replacement character instead of throwing an exception. See https://en.wikipedia.org/wiki/UTF-8
					// also (search for replacement character)
					CharBuffer charBuffer = charsetDecoder.decode(ByteBuffer.wrap((byte[])message));
					toStringResult = new ToStringResult(charBuffer.toString(), encoding);
				} catch (CharacterCodingException e) {
					toStringResult = new ToStringResult(java.util.Base64.getEncoder().encodeToString((byte[])message),
							BASE64_ENCODER);
				}
			} else if (message instanceof Reader || message instanceof InputStream
					|| message instanceof Writer || message instanceof OutputStream) {
				// See comment at the top of CHeckpoint.setMessage(T message)
				toStringResult = new ToStringResult(WAITING_FOR_STREAM_MESSAGE, null);
			} else if (message instanceof Throwable) {
				StringWriter stringWriter = new StringWriter();
				((Throwable)message).printStackTrace(new PrintWriter(stringWriter));
				toStringResult = new ToStringResult(stringWriter.toString(), THROWABLE_ENCODER);
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
		return toObject(checkpoint, null);
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
		// Can be null when called from toObject(Checkpoint originalCheckpoint, T messageToStub), see javadoc on param
		// originalCheckpoint in MessageEncoder
		if (originalCheckpoint == null) {
			return (T)TestTool.DEFAULT_STUB_MESSAGE;
		} else if (originalCheckpoint.getMessage() == null) {
			return null;
		} else {
			String message = originalCheckpoint.getMessage();
			String encoding = originalCheckpoint.getEncoding();
			String messageClassName = originalCheckpoint.getMessageClassName();
			if (messageClassName.equals(Boolean.class.getName())) {
				Object rawResult = Boolean.valueOf(message);
				return (T) rawResult;
			}
			if (encoding == null) {
				if (originalCheckpoint.getStreaming() == null) {
					return (T)message;
				} else {
					if (messageToStub instanceof Writer) {
						((Writer)messageToStub).write(message);
						return messageToStub;
					} else {
						return (T)new StringReader(message);
					}
				}
			} else if (encoding.equals(UTF8_ENCODER) || encoding.startsWith(CHARSET_ENCODER_PREFIX)) {
				if (encoding.startsWith(CHARSET_ENCODER_PREFIX)) {
					encoding = encoding.substring(CHARSET_ENCODER_PREFIX.length());
				}
				CharsetEncoder charsetEncoder = Charset.forName(encoding).newEncoder();
				ByteBuffer byteBuffer = charsetEncoder.encode(CharBuffer.wrap(message));
				byte[] bytes = new byte[byteBuffer.remaining()];
				byteBuffer.get(bytes);
				if (originalCheckpoint.getStreaming() == null) {
					return (T)bytes;
				} else {
					if (messageToStub instanceof OutputStream) {
						((OutputStream)messageToStub).write(bytes);
						return messageToStub;
					} else {
						return (T)new ByteArrayInputStream(bytes);
					}
				}
			} else if (encoding.equals(BASE64_ENCODER)) {
				byte[] bytes = java.util.Base64.getDecoder().decode(message);
				if (originalCheckpoint.getStreaming() == null) {
					return (T)bytes;
				} else {
					return (T)new ByteArrayInputStream(bytes);
				}
			} else if (encoding.equals(DOM_NODE_ENCODER)) {
				return (T)XmlUtil.stringToNode(message);
			} else if (encoding.equals(DATE_ENCODER)) {
				return (T)new SimpleDateFormat(DATE_PATTERN).parse(message);
			} else if (encoding.equals(XML_ENCODER)) {
				ByteArrayInputStream byteArrayInputStream = null;
				byteArrayInputStream = new ByteArrayInputStream(message.getBytes("UTF-8"));
				XMLDecoder xmlDecoder = new XMLDecoder(byteArrayInputStream);
				return (T)xmlDecoder.readObject();
			} else {
				return (T)message;
			}
		}
	}

}
