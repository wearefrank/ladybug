package org.wearefrank.ladybug.ff.test.webapp;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;
import org.springframework.http.MediaType;

public class PutMediaTypeInSessionPipe extends FixedForwardPipe {
	private String sessionKey = "mediaType";

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		// Store the MediaType instance itself (not a String) so Ladybug traces it as the raw
		// checkpoint message, reproducing https://github.com/wearefrank/ladybug/issues/514.
		session.put(sessionKey, MediaType.APPLICATION_JSON);
		return new PipeRunResult(getSuccessForward(), message);
	}

	public void setSessionKey(String sessionKey) {
		this.sessionKey = sessionKey;
	}

	public String getSessionKey() {
		return sessionKey;
	}
}
