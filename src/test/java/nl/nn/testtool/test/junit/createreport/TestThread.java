/*
   Copyright 2022 WeAreFrank!

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

package nl.nn.testtool.test.junit.createreport;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import lombok.Getter;
import lombok.Setter;
import nl.nn.testtool.TestTool;

class TestThread extends Thread {
	@Setter TestTool testTool;
	@Setter String correlationId;
	@Setter int nrOfTests = 1;
	@Setter boolean keepReportOpenWithMessageCapturer = false;
	@Getter Throwable throwable;

	@Override
	public void run() {
		try {
			for (int i = 0; i < nrOfTests; i++) {
				testTool.startpoint(correlationId, null, getName(), "startmessage1");
				if (keepReportOpenWithMessageCapturer) {
					OutputStream outputStreamOriginalMessage = new ByteArrayOutputStream();
					OutputStream outputStreamMessage = testTool.inputpoint(correlationId, null, "outputstream",
							outputStreamOriginalMessage);
					outputStreamMessage.write("Hello OutputStream World!".getBytes());
				}
				testTool.endpoint(correlationId, null, getName(), "endmessage1");
			}
		} catch (Throwable t) {
			throwable = t;
		}
	}

}