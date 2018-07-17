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
