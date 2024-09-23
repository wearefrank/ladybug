package nl.nn.testtool;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class MessageEncoderTest {
    @Test
    public void encode_and_decode_boolean_true() {
        MessageEncoder instance = new MessageEncoderImpl();
        MessageEncoder.ToStringResult encoded = instance.toString(true, null);
        assertEquals("true", encoded.getString());
        assertEquals("java.lang.Boolean", encoded.getMessageClassName());
        Checkpoint checkpoint = new Checkpoint();
        checkpoint.setMessage(encoded.getString());
        checkpoint.setEncoding(encoded.getEncoding());
        checkpoint.setMessageClassName(encoded.getMessageClassName());
        Object back = instance.toObject(checkpoint);
        if(! (back instanceof Boolean)) {
            fail("Expected to get back Boolean");
        }
        assertTrue((Boolean) back);
    }

    @Test
    public void encode_decode_boolean_false() {
        MessageEncoder instance = new MessageEncoderImpl();
        MessageEncoder.ToStringResult encoded = instance.toString(false, null);
        assertEquals("false", encoded.getString());
        assertEquals("java.lang.Boolean", encoded.getMessageClassName());
        Checkpoint checkpoint = new Checkpoint();
        checkpoint.setMessage(encoded.getString());
        checkpoint.setEncoding(encoded.getEncoding());
        checkpoint.setMessageClassName(encoded.getMessageClassName());
        Object back = instance.toObject(checkpoint);
        if(! (back instanceof Boolean)) {
            fail("Expected to get back Boolean");
        }
        assertFalse((Boolean) back);
    }
}
