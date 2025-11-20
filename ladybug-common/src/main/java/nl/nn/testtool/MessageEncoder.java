/*
   Copyright 2021-2023, 2025 WeAreFrank!

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

import java.io.Closeable;

import lombok.Getter;
import lombok.Setter;
import nl.nn.testtool.storage.xml.XmlStorage;

/**
 * Implement this interface to implement custom encoding/decoding methods to/from a string representation for message
 * objects passed to checkpoints in a report. The string representation is used to display the message in a GUI and when
 * writing the message to storage making it readable in string based storages like {@link XmlStorage}. When a message in
 * rerun needs to be stubbed the message encoder can return the original object for the string representation.
 * 
 * By default the {@link MessageEncoderImpl} is used by {@link TestTool}. For more specific message objects
 * specific implementation of this interface can be wired to {@code TestTool}.
 * 
 * @author Jaco de Groot
 */
public interface MessageEncoder {

	/**
	 * Encode the message from object to string.
	 * 
	 * @param message  the object to encode to string
	 * @param charset  null or when
	 *                 {@link MessageCapturer#toOutputStream(Object, java.io.OutputStream, java.util.function.Consumer,
	 *                 java.util.function.Consumer)} was called for this message the charset as notified by the
	 *                 application
	 * @return         {@link ToStringResult} containing the string representation of the object, the name of the
	 *                 encoding used (is null when no encoding was applied) and the class name of message
	 */
	public ToStringResult toString(Object message, String charset);

	@Getter
	@Setter
	public class ToStringResult {
		private String string;
		private String encoding;
		private String messageClassName;

		public ToStringResult(String string, String encoding) {
			this.string = string;
			this.encoding = encoding;
		}

		public ToStringResult(String string, String encoding, String messageClassName) {
			this.string = string;
			this.encoding = encoding;
			this.messageClassName = messageClassName;
		}

	}

	/**
	 * Decode the message of a checkpoint from string back to an object. This method is typically used by
	 * {@link Rerunner} implementations to get the initial message of a report and start a rerun.
	 * 
	 * @param checkpoint  the checkpoint holding the string representation and the encoding method used when message was
	 *                    encoded and possible other relevant information to determine the original object type
	 * @return            the message as an Object
	 */
	public Object toObject(Checkpoint checkpoint);

	/**
	 * Decode the message of a checkpoint from string back to an object for stubbing purposes. This method will
	 * generally do the same as {@link #toObject(Checkpoint)} but will give the implementation more information and a
	 * possibility to for example close the message to stub in case it is {@link Closeable}. This method will be called
	 * when a checkpoint needs to be stubbed.
	 * 
	 * @param <T>                 the type of message to stub
	 * @param originalCheckpoint  the checkpoint from the original report that will be used as a stub for the
	 *                            counterpart checkpoint in the report in progress. The original checkpoint holds the
	 *                            string representation and the encoding method used when the original message was
	 *                            encoded and possible other relevant information to determine the original object type.
	 *                            It can be null when the original checkpoint cannot be found (in that case the decision
	 *                            to stub is not based on the original checkpoint but based on a stubbing strategy that
	 *                            stubs certain types of checkpoints). When null the default implementation
	 *                            {@link MessageEncoderImpl} will return the default stub message
	 *                            {@link TestTool#DEFAULT_STUB_MESSAGE}
	 * @param messageToStub       the message in the report in progress that needs to be stubbed
	 * @return                    a stub for the message that needs to be stubbed
	 */
	public <T> T toObject(Checkpoint originalCheckpoint, T messageToStub);

}
