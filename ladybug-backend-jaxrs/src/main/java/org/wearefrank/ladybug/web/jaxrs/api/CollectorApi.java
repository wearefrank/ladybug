/*
   Copyright 2021-2026 WeAreFrank!

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
package org.wearefrank.ladybug.web.jaxrs.api;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.wearefrank.ladybug.SpanBuffer;
import org.wearefrank.ladybug.web.common.CollectorApiImpl;
import org.wearefrank.ladybug.web.common.Constants;
import java.util.*;

@Path("/" + Constants.LADYBUG_API_PATH + "/collector")
public class CollectorApi extends ApiBase {
    private SpanBuffer spanBuffer;

    @Autowired
    private @Setter CollectorApiImpl delegate;

    @PostConstruct
    public void init() {
        spanBuffer = new SpanBuffer(delegate);
    }

    @POST
    @Consumes({"application/x-protobuf", "application/json"})
    public Response receiveTrace(@HeaderParam("Content-Type") String contentType, byte[] data)
            throws InvalidProtocolBufferException {

        ExportTraceServiceRequest request;

        if (contentType.startsWith("application/x-protobuf")) {
            request = ExportTraceServiceRequest.parseFrom(data);
        } else if (contentType.startsWith("application/json")) {
            String json = new String(data);
            ExportTraceServiceRequest.Builder builder = ExportTraceServiceRequest.newBuilder();
            JsonFormat.parser().merge(json, builder);
            request = builder.build();
        } else {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity("Unsupported Content-Type: " + contentType)
                    .build();
        }

        ArrayList<Span> unorderedSpans = new ArrayList<>();

        for (ResourceSpans resourceSpans : request.getResourceSpansList()) {
            for (ScopeSpans scopeSpans : resourceSpans.getScopeSpansList()) {
                for (Span span : scopeSpans.getSpansList()) {
                    unorderedSpans.add(span);
                }
            }
        }

        orderInTraces(unorderedSpans);

        return Response.ok().build();
    }

    public void orderInTraces(ArrayList<Span> unorderedSpans) {
        List<Span> roots = new ArrayList<>();

        for (Span span : unorderedSpans) {
            if (isRoot(span, unorderedSpans)) {
                roots.add(span);
            }
        }

        roots.sort(Comparator.comparingLong(Span::getStartTimeUnixNano));

        for (Span root : roots) {
            ArrayList<Span> trace = new ArrayList<>();
            traverse(root, unorderedSpans, trace);
            spanBuffer.addTrace(trace);
        }
    }

    private void traverse(Span parent, ArrayList<Span> unorderedSpans, ArrayList<Span> trace) {
        trace.add(parent);

        String parentId = delegate.byteStringToHex(parent.getSpanId());

        List<Span> children = new ArrayList<>();
        for (Span span : unorderedSpans) {
            String childParentId = delegate.byteStringToHex(span.getParentSpanId());
            if (childParentId.equals(parentId)) {
                children.add(span);
            }
        }

        children.sort(Comparator.comparingLong(Span::getStartTimeUnixNano));

        for (Span child : children) {
            traverse(child, unorderedSpans, trace);
        }
    }

    private boolean isRoot(Span span, List<Span> unordered) {
        String parentId = delegate.byteStringToHex(span.getParentSpanId());

        if (parentId.isEmpty()) {
            return true;
        }

        for (Span s : unordered) {
            if (delegate.byteStringToHex(s.getSpanId()).equals(parentId)) {
                return false;
            }
        }

        return true;
    }
}