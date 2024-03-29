/*
   Copyright 2020, 2022 WeAreFrank!, 2018 Nationale-Nederlanden

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
package nl.nn.testtool.util;

import java.io.File;


public class ExportResult {
	private String errorMessage;
	private File tempFile;
	private String suggestedFilename;

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setTempFile(File tempFile) {
		this.tempFile = tempFile;
	}

	public File getTempFile() {
		return tempFile;
	}

	public void setSuggestedFilename(String suggestedFilename) {
		this.suggestedFilename = suggestedFilename;
	}

	public String getSuggestedFilename() {
		return suggestedFilename;
	}

}
