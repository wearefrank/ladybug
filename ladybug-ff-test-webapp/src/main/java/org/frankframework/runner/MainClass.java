package org.frankframework.runner;

import java.io.IOException;

public class MainClass {

	public static void main(String[] args) throws IOException {
		FrankApplication app = new FrankApplication();
		app.run(args);
	}

}
