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
