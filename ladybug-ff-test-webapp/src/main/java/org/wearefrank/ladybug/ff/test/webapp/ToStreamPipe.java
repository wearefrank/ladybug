package org.wearefrank.ladybug.ff.test.webapp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;

public class ToStreamPipe extends FixedForwardPipe {
    public static enum StreamKind {
        BINARY,
        CHARACTER,
        BINARY_EMPTY,
        CHARACTER_EMPTY,
        BINARY_ALL_BYTES
    }

    private StreamKind streamKind = StreamKind.BINARY;

    public void setStreamKind(StreamKind streamKind) {
        this.streamKind = streamKind;
    }

    public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
        try {
            switch(streamKind) {
                case CHARACTER: {
                    InputStream is = message.asInputStream();
                    String s = readString(is);
                    // Make output different from input
                    s = s + "_suffix";
                    Reader resultReader = new StringReader(s);
                    Message m = new Message(resultReader);
                    return new PipeRunResult(getSuccessForward(), m);
                }
                case BINARY: {
                    InputStream is = message.asInputStream();
                    String s = readString(is);
                    // Make output different from input
                    s = s + "_suffix";
                    InputStream resultIs = string2InputStream(s);
                    Message m = new Message(resultIs);
                    return new PipeRunResult(getSuccessForward(), m);
                }
                case BINARY_EMPTY: {
                    InputStream resultIs = new ByteArrayInputStream(new byte[] {});
                    return new PipeRunResult(getSuccessForward(), resultIs);
                }
                case CHARACTER_EMPTY: {
                    Reader resultReader = new StringReader("");
                    return new PipeRunResult(getSuccessForward(), resultReader);
                }
                case BINARY_ALL_BYTES: {
                    byte[] bytes = new byte[256];
                    for(int i = 0; i < 256; ++i) {
                        bytes[i] = (byte) i;
                    }
                    return new PipeRunResult(getSuccessForward(), new ByteArrayInputStream(bytes));
                }
            }
        }
        catch(IOException e) {
            throw new PipeRunException(this, "IOException", e);
        }
        throw new PipeRunException(this, "Code should be unreachable because all branches of switch return");
    }

    // Copied from https://stackoverflow.com/questions/1891606/read-text-from-inputstream
    private static String readString(InputStream is) throws IOException {
        char[] buf = new char[2048];
        Reader r = new InputStreamReader(is, "UTF-8");
        StringBuilder s = new StringBuilder();
        while (true) {
            int n = r.read(buf);
            if (n < 0)
                break;
            s.append(buf, 0, n);
        }
        return s.toString();
    }

    // Copied from https://stackoverflow.com/questions/782178/how-do-i-convert-a-string-to-an-inputstream-in-java
    private static InputStream string2InputStream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}