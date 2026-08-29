/*
   Copyright 2020-2021, 2025-2026 WeAreFrank!

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
package org.wearefrank.ladybug;

import org.wearefrank.ladybug.run.ReportRunner;

/**
 * @author Jaco de Groot
 */
public interface Rerunner {
	/**
	 * Rerun a previous generated report. This method should at least trigger
	 * the same first checkpoint as has been triggered when the original report
	 * was created.
	 *  
	 * @param correlationId   the correlationId to be used so the report of the rerun will have this correlationId and
	 *                        can be linked to the original report so stub strategy can be copied and stubbing can be
	 *                        applied
	 * @param originalReport  the original report that should be rerun
	 * @param securityContext can be used to do implementation specific security checks. For example, Tibet2 connects to
	 *                        LDAP to check the role for the principal name:
	 *                        https://github.com/frankframework/frankframework/blob/v7.9.8/ladybug/src/main/java/nl/nn/ibistesttool/tibet2/Debugger.java#L75
	 * @param reportRunner    ...
	 * @return                an error message when an error occurred 
	 */
	public String rerun(String correlationId, Report originalReport, SecurityContext securityContext,
			ReportRunner reportRunner);

}
