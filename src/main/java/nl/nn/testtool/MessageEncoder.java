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

import lombok.Getter;
import lombok.Setter;
import nl.nn.testtool.storage.xml.XmlStorage;

/**
 * Implement this interface to implement custom encoding/decoding methods to/from a string representations for message
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
	 * @return         {@link ToStringResult} containing the string representation of the object, the name of the
	 *                 encoding used (is null when no encoding was applied) and the class name of message
	 */
	ToStringResult toString(Object message);

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
	 * Decode the message from string back to an object for stubbing purposes.
	 * 
	 * @param message   string representation of the message
	 * @param encoding  encoding method used when message was encoded to a string representation
	 * @return          the message as an Object
	 */
	Object toObject(String message, String encoding);

}
