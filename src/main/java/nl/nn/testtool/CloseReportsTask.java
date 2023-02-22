/*
   Copyright 2022-2023 WeAreFrank!

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

import lombok.Setter;

/**
 * An alternative for {@link TestTool#close(String)}, {@link TestTool#setCloseThreads(boolean)} and
 * {@link TestTool#setCloseMessageCapturers(boolean)} that can be scheduled (e.g. by Spring) giving threads and message
 * capturers a chance to finish after the last checkpoint of a report has already finished. For more information see
 * {@link TestTool#close(long, long, boolean, boolean, long, long)}
 * 
 * @see TestTool#close(long, long, boolean, boolean, long, long)
 * @see TestTool#close(String)
 * @see TestTool#setCloseThreads(boolean)
 * @see TestTool#setCloseMessageCapturers(boolean)
 * @author Jaco de Groot
 */
public class CloseReportsTask {
	private @Setter TestTool testTool;
	private @Setter long threadsTime = 60 * 1000;
	private @Setter long messageCapturersTime = 60 * 1000;
	private @Setter boolean waitForMainThreadToFinish = true;
	private @Setter boolean logThreadInfoBeforeClose = false;
	private @Setter long logThreadInfoAfterReportAge = -1;
	private @Setter long logThreadInfoBeforeReportAge = -1;

	public void closeReports() {
		testTool.close(threadsTime, messageCapturersTime, waitForMainThreadToFinish, logThreadInfoBeforeClose,
				logThreadInfoAfterReportAge, logThreadInfoBeforeReportAge);
	}

}
