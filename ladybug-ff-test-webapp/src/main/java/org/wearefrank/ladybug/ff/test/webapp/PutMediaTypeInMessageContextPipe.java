package org.wearefrank.ladybug.ff.test.webapp;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;
import org.springframework.http.MediaType;

public class PutMediaTypeInMessageContextPipe extends FixedForwardPipe {
	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		// Unlike PutMediaTypeInSessionPipe, this attaches the MediaType to the message's own
		// context (as Metadata.MimeType) instead of a session variable. Ladybug stores a
		// checkpoint's messageContext map as-is (Checkpoint.setMessageContext()), without
		// converting its values to a String the way it does for the checkpoint's own message.
		// So the MediaType instance survives as a live object all the way into the stored
		// report, and is still there when the report is later downloaded, actually reproducing
		// https://github.com/wearefrank/ladybug/issues/514 (unlike PutMediaTypeInSessionPipe,
		// whose captured MediaType is flattened to a String immediately at checkpoint-creation
		// time and therefore never reaches the report's XMLEncoder-based export).
		message.getContext().withMimeType(MediaType.APPLICATION_JSON);
		return new PipeRunResult(getSuccessForward(), message);
	}
}
