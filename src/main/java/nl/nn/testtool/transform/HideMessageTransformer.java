/*
   Copyright 2023 WeAreFrank!

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

import java.util.regex.Pattern;

import lombok.Setter;
import nl.nn.testtool.Checkpoint;

/**
 * Hide message of a checkpoint when the name of the checkpoint is equal to a specified string or when it is matching a
 * specified regex
 * 
 * @author Jaco de Groot
 *
 */
public class HideMessageTransformer implements MessageTransformer {
	private @Setter String nameEquals;
	private String nameRegex;
	private Pattern namePattern;
	private @Setter boolean skipeWhitespace = true;

	public void setNameRegex(String nameRegex) {
		this.nameRegex = nameRegex;
		if (nameRegex == null) {
			namePattern = null;
		} else {
			namePattern = Pattern.compile(nameRegex);
		}
	}

	@Override
	public String transform(Checkpoint checkpoint, String message) {
		boolean hide = false;
		String name = checkpoint.getName();
		if (name == null) {
			if (nameEquals == null && nameRegex == null) {
				hide = true;
			}
		} else if (name.equals(nameEquals) || (namePattern != null && namePattern.matcher(name).matches())) {
			hide = true;
		}
		if (hide && message != null) {
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < message.length(); i++) {
				if (Character.isWhitespace(message.charAt(i)) && skipeWhitespace) {
					builder.append(message.charAt(i));
				} else {
					builder.append('*');
				}
			}
			message = builder.toString();
		}
		return message;
	}

}
