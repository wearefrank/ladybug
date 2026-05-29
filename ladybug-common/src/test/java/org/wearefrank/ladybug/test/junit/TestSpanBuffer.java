/*
   Copyright 2026 WeAreFrank!

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
package org.wearefrank.ladybug.test.junit;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.trace.v1.Span;
import org.junit.Test;
import org.wearefrank.ladybug.SpanBuffer;
import org.wearefrank.ladybug.web.common.TracingApiImpl;

import java.util.ArrayList;

import static org.mockito.Mockito.*;

public class TestSpanBuffer {

    @Test
    public void testAddSingleSpan() {
        TracingApiImpl delegate = mock(TracingApiImpl.class);

        when(delegate.byteStringToHex(any()))
                .thenReturn("trace1");

        SpanBuffer spanBuffer = new SpanBuffer(delegate);

        Span span = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace1"))
                .setSpanId(ByteString.copyFromUtf8("span1"))
                .setName("span")
                .build();

        spanBuffer.addSpan(span);

        verify(delegate, times(1))
                .byteStringToHex(span.getTraceId());
    }

    @Test
    public void testAddMultipleSpansSameTrace() {
        TracingApiImpl delegate = mock(TracingApiImpl.class);

        when(delegate.byteStringToHex(any()))
                .thenReturn("trace1");

        SpanBuffer spanBuffer = new SpanBuffer(delegate);

        Span span1 = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace1"))
                .setSpanId(ByteString.copyFromUtf8("span1"))
                .setName("span1")
                .build();

        Span span2 = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace1"))
                .setSpanId(ByteString.copyFromUtf8("span2"))
                .setName("span2")
                .build();

        spanBuffer.addSpan(span1);
        spanBuffer.addSpan(span2);

        verify(delegate, times(2))
                .byteStringToHex(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExpirationProcessesSpans() throws Exception {
        TracingApiImpl delegate = mock(TracingApiImpl.class);

        when(delegate.byteStringToHex(any()))
                .thenReturn("trace1");

        SpanBuffer spanBuffer = new SpanBuffer(delegate);

        Span span1 = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace1"))
                .setSpanId(ByteString.copyFromUtf8("span1"))
                .setName("span1")
                .build();

        Span span2 = Span.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("trace1"))
                .setSpanId(ByteString.copyFromUtf8("span2"))
                .setName("span2")
                .build();

        spanBuffer.addSpan(span1);
        spanBuffer.addSpan(span2);

        Thread.sleep(31000);

        verify(delegate, timeout(5000).times(1))
                .processSpans(any(ArrayList.class));
    }
}