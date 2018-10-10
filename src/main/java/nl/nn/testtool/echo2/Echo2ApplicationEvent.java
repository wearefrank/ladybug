/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.testtool.echo2;

import org.springframework.context.ApplicationEvent;

public class Echo2ApplicationEvent extends ApplicationEvent {
	private String command;
	private Object customObject;

	public Echo2ApplicationEvent(Object source) {
		super(source);
	}

	public void setCommand(String command) {
		this.command = command;
	}
	
	public String getCommand() {
		return command;
	}

	public void setCustomObject(Object customObject) {
		this.customObject = customObject;
	}
	
	public Object getCustomObject() {
		return customObject;
	}
}
