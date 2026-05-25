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
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.wearefrank.ladybug.SpanBuffer;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.web.common.TracingApiImpl;
import org.wearefrank.ladybug.web.common.Constants;

@Path("/" + Constants.LADYBUG_API_PATH + "/traces")
public class TracingApi extends ApiBase {
    private SpanBuffer spanBuffer;

    @Autowired
    private @Setter TracingApiImpl delegate;

    @PostConstruct
    public void init() {
        spanBuffer = new SpanBuffer(delegate);
    }

    @POST
    @Consumes({"application/x-protobuf", "application/json"})
    public Response receiveTrace(@HeaderParam("Content-Type") String contentType, byte[] data) throws InvalidProtocolBufferException, StorageException {
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

        for (ResourceSpans resourceSpans : request.getResourceSpansList()) {
            for (ScopeSpans scopeSpans : resourceSpans.getScopeSpansList()) {
                for (Span span : scopeSpans.getSpansList()) {
                    spanBuffer.addSpan(span);
                }
            }
        }

        return Response.ok().build();
    }
}