package org.wearefrank.ladybug.ff.test.webapp;

import java.io.IOException;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;

public class TextBlockTestPipe extends FixedForwardPipe {
	// We use digits 0-9, letters a-z and letters A-Z.
	private static final int NUM_SYMBOLS = 10 + 2 * 26;

	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String messageString = null;
		try {
			messageString = message.asString();
		}
		catch(IOException e) {
			throw new PipeRunException(this, "Message should be like: \"<numRows> <numCols>\"", e);
		}
		String[] numberStrings = messageString.split(" ");
		Integer numRows = null;
		Integer numCols = null;
		try {
			numRows = Integer.parseInt(numberStrings[0]);
			numCols = Integer.parseInt(numberStrings[1]);
		} catch(NumberFormatException e) {
			throw new PipeRunException(this, "Message should be like: \"<numRows> <numCols>\"", e);
		}
		return new PipeRunResult(getSuccessForward(), makeTextBlock(numRows, numCols));
	}

	private String makeTextBlock(int rows, int columns) {
		StringBuilder sb = new StringBuilder();
		for (int rowIndex = 0; rowIndex < rows; ++rowIndex) {
			for (int columnIndex = 0; columnIndex < columns; ++columnIndex) {
				if (columnIndex == 0) {
					sb.append(index2char(rowIndex));
				} else {
					sb.append(index2char(columnIndex));
				}
			}
			sb.append('\n');
		}
		return sb.toString();
	}

	private char index2char(int index) {
		index = index % NUM_SYMBOLS;
		if (index < 0) {
			throw new IllegalArgumentException("Expected a nonnegative number");
		}
		if (index < 10) {
			return (char) ('0' + index);
		}
		if (index < 10 + 26) {
			int letterIndex = index - 10;
			return (char) ('a' + letterIndex);
		}
		int letterIndex = index - 10 - 26;
		return (char) ('A' + letterIndex);
	}
}
