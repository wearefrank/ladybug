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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.function.Consumer;

/**
 * Default implementation of {@link MessageCapturer} used by {@link TestTool} that will wrap message objects that are an
 * instance of {@link Writer} or {@link OutputStream} in a proxy object that listens for characters or bytes in the
 * message object stream and then write those characters or bytes to the writer or output stream received through the
 * methods of this interface while also writing the characters or bytes to the original message object stream.
 * 
 * @author Jaco de Groot
 *
 */
public class MessageCapturerImpl implements MessageCapturer {

	@Override
	public StreamingType getStreamingType(Object message) {
		if (message instanceof Writer) {
			return StreamingType.CHARACTER_STREAM;
		} else if (message instanceof OutputStream) {
			return StreamingType.BYTE_STREAM;
		} else {
			return StreamingType.NONE;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T toWriter(T message, Writer writer) {
		return (T) new BufferedWriter((Writer)message) {

			@Override
			public void write(String str, int off, int len) throws IOException {
				writer.write(str, off, len);
				super.write(str, off, len);
			}

			@Override
			public void write(int c) throws IOException {
				writer.write(c);
				super.write(c);
			}

			@Override
			public void write(char[] cbuf, int off, int len) throws IOException {
				writer.write(cbuf, off, len);
				super.write(cbuf, off, len);
			}
			

			@Override
			public void close() throws IOException {
				writer.close();
				super.close();
			}

		};
	}
	@SuppressWarnings("unchecked")
	@Override
	public <T> T toOutputStream(T message, OutputStream outputStream, Consumer<String> charsetNotifier) {
		return (T) new BufferedOutputStream((OutputStream)message) {

				@Override
				public synchronized void write(int b) throws IOException {
					outputStream.write(b);
					super.write(b);
				}

				@Override
				public synchronized void write(byte[] b, int off, int len) throws IOException {
					outputStream.write(b, off, len);
					super.write(b, off, len);
				}

				@Override
				public void close() throws IOException {
					outputStream.close();
					super.close();
				}

		};
	}

}
