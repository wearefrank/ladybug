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
package org.wearefrank.ladybug;

import java.io.OutputStream;
import java.io.Writer;
import java.util.function.Consumer;

/**
 * Implement this interface to stream specific messages to a report.
 * <p>
 * When the same streaming message object is passed on to several checkpoints the message capturer will only be called
 * once for this message. All the checkpoints will receive the result of the stream when the stream is closed.
 * <p>
 * By default the {@link MessageCapturerImpl} is used by {@link TestTool}. For more specific situations like a custom
 * message object that can be streamed a specific implementation of this interface can be wired to {@code TestTool}.
 *
 * @author Jaco de Groot
 */
public interface MessageCapturer {
	enum StreamingType {
		NONE(null),
		CHARACTER_STREAM("Character"),
		BYTE_STREAM("Byte");

		String name;

		StreamingType(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/**
	 * Get {@link StreamingType} of message. The {@link StreamingType} returned will determine whether
	 * {@link #toWriter(Object, Writer, Consumer)} or {@link #toOutputStream(Object, OutputStream, Consumer, Consumer)}
	 * will be called (or none of them).
	 *
	 * @param message the message for which to return the {@link StreamingType}
	 * @return the {@link StreamingType} for the specified message
	 */
	public StreamingType getStreamingType(Object message);

	/**
	 * This method will be called for messages of {@link StreamingType} {@link StreamingType#CHARACTER_STREAM} to write
	 * a representation of the message to the specified writer. This can be done asynchronous in which case Ladybug
	 * will continue to add checkpoints and when the report has finished will wait for the writer to close before
	 * closing and storing the report. The characters written to this writer will be passed as a string to
	 * {@link Checkpoint#setMessage(String)}.
	 *
	 * @param <T>               type of message
	 * @param message           the checkpoint message object
	 * @param writer            write the data of the checkpoint message object to this writer
	 * @param exceptionNotifier enables application to notify Ladybug of an exception being thrown during processing of
	 *                          the stream
	 * @return the message itself, or a wrapper around it that will be passed back to the caller of
	 * 		the checkpoint method. When later the caller reads characters from the wrapper or
	 * 		writes characters to the wrapper these characters can be captured by the wrapper and
	 * 		copied to writer
	 */
	public <T> T toWriter(T message, Writer writer, Consumer<Throwable> exceptionNotifier);

	/**
	 * This method will be called for messages of {@link StreamingType} {@link StreamingType#BYTE_STREAM} to write a
	 * representation of the message to the specified output stream. This can be done asynchronous in which case Ladybug
	 * will continue to add checkpoints and when the report has finished will wait for the output stream to close before
	 * closing and storing the report. The bytes written to this writer will be passed as a byte array to
	 * {@link Checkpoint#setMessage(Object)} using {@link MessageEncoder#toString(Object, String)} with the charset that
	 * the application optionally notified the Ladybug of.
	 *
	 * @param <T>               type of message
	 * @param message           the checkpoint message object
	 * @param outputStream      write the data of the checkpoint message object to this output stream
	 * @param charsetNotifier   enables application to notify Ladybug of the charset that probably is the most proper
	 *                          to render the outputStream to characters
	 * @param exceptionNotifier enables application to notify Ladybug of an exception being thrown during processing of
	 *                          the stream
	 * @return the message itself, or a wrapper around it that will be passed back to the caller of
	 * 		the checkpoint method. When later the caller reads bytes from the wrapper or
	 * 		writes bytes to the wrapper these bytes can be captured by the wrapper and
	 * 		copied to outputStream
	 */
	public <T> T toOutputStream(T message, OutputStream outputStream, Consumer<String> charsetNotifier,
								Consumer<Throwable> exceptionNotifier);

}
