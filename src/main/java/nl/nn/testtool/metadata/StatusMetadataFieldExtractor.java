/*
   Copyright 2020-2024 WeAreFrank!

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
package nl.nn.testtool.metadata;

import java.util.List;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.CheckpointType;
import nl.nn.testtool.MetadataFieldExtractor;
import nl.nn.testtool.Report;

/**
 * @author Jaco de Groot
 */
public class StatusMetadataFieldExtractor extends DefaultValueMetadataFieldExtractor {
	private MetadataFieldExtractor delegate = null;
	private String errorLabel = "Error";
	private String successLabel = "Success";
	private int maxLength = 0;

	public StatusMetadataFieldExtractor() {
		name = "status";
		label = "Status";
	}

	/**
	 * If there was no abort then calculate the status from the delegate MetadataFieldextractor.
	 * 
	 * @param delegate ...
	 */
	public void setDelegate(MetadataFieldExtractor delegate) {
		this.delegate = delegate;
	}

	public void setErrorLabel(String errorLabel) {
		this.errorLabel = errorLabel;
	}

	public void setSuccessLabel(String successLabel) {
		this.successLabel = successLabel;
	}

	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	public Object extractMetadata(Report report) {
		String status = successLabel;
		List<Checkpoint> checkpoints = report.getCheckpoints();
		if (checkpoints.size() > 0) {
			Checkpoint lastCheckpoint = (Checkpoint)checkpoints.get(checkpoints.size() - 1);
			if (lastCheckpoint.getType() == CheckpointType.ABORTPOINT.toInt()) {
				status = errorLabel;
			} else if(delegate != null) {
				status = (String) delegate.extractMetadata(report);
			}
		}
		if(maxLength > 0){
			if(status != null){
				if(status.length() > maxLength){
					status = status.substring(0, maxLength);
				}
			}
		}
		return status;
	}
}
