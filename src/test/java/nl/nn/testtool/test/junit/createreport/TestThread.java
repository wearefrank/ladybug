package nl.nn.testtool.test.junit.createreport;

import lombok.Getter;
import lombok.Setter;
import nl.nn.testtool.TestTool;

class TestThread extends Thread {
	@Setter TestTool testTool;
	@Setter String correlationId;
	@Getter Throwable throwable;

	@Override
	public void run() {
		try {
			testTool.startpoint(correlationId, null, getName(), "startmessage1");
			testTool.endpoint(correlationId, null, getName(), "endmessage1");
		} catch (Throwable t) {
			throwable = t;
		}
	}

}