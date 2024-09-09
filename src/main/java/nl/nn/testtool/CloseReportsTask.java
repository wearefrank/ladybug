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

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private @Setter TestTool testTool;
	private @Setter long threadsTime = 60 * 1000;
	private @Setter long messageCapturersTime = 60 * 1000;
	private @Setter boolean waitForMainThreadToFinish = true;
	private @Setter boolean logThreadInfoBeforeClose = false;
	// Log thread info is disabled when minimum age and maximum age is the same
	private @Setter long logThreadInfoMinReportAge = 5 * 60 * 1000;
	private @Setter long logThreadInfoMaxReportAge = 5 * 60 * 1000;

	public void closeReports() {
		// Check whether initialized as it seems to be possible for this method to be called by the scheduler before
		// initialization:
		//    https://github.com/wearefrank/ladybug/issues/257
		//    https://github.com/wearefrank/ladybug/issues/293
		// It doesn't reproduce in the test webapp when adding a delay in TestTool.init(). In that case it works as
		// expected and this method isn't called before TestTool.init() is finished and setTestTool() on this class has
		// been called.
		if (testTool != null) {
			testTool.close(threadsTime, messageCapturersTime, waitForMainThreadToFinish, logThreadInfoBeforeClose,
					logThreadInfoMinReportAge, logThreadInfoMaxReportAge);
		} else {
			log.debug("Skip execution because setTestTool() hasn't been called (yet)");
		}
	}

}
