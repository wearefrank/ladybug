/*
   Copyright 2023, 2025 WeAreFrank!

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
package nl.nn.testtool.transform;

import java.util.List;

import nl.nn.testtool.Checkpoint;

/**
 * Delegate message transformation to a list of message transformers to make it possible to have multiple message
 * transformers linked together and exposed as one message transformer. The
 * {@link MessageTransformer#transform(Checkpoint, String)} method of every {@link MessageTransformer} in a specified
 * list of {@link MessageTransformer}'s will be called.
 * 
 * @author Jaco de Groot
 *
 */
public class LinkingMessageTransformer implements MessageTransformer {
	private List<MessageTransformer> messageTransformers;

	public void setMessageTransformers(List<MessageTransformer> messageTransformers) {
		this.messageTransformers = messageTransformers;
	}

	@Override
	public String transform(Checkpoint checkpoint, String message) {
		if (messageTransformers != null) {
			for (MessageTransformer messageTransformer : messageTransformers) {
				message = messageTransformer.transform(checkpoint, message);
			}
		}
		return message;
	}

}
