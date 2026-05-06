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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponseOrBuilder;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.wearefrank.ladybug.TraceTree;
import org.wearefrank.ladybug.web.common.CollectorApiImpl;
import org.wearefrank.ladybug.web.common.Constants;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/" + Constants.LADYBUG_API_PATH + "/v1/traces")
public class TracingApi extends ApiBase {

    @Autowired
    private @Setter CollectorApiImpl delegate;

    @POST
    @Consumes({"application/x-protobuf", "application/json"})
    public Response receiveSpans(@HeaderParam("Content-Type") String contentType, byte[] data)
            throws InvalidProtocolBufferException, SQLException {

        ExportTraceServiceRequest request;

        if (contentType.startsWith("application/x-protobuf")) {
            request = ExportTraceServiceRequest.parseFrom(data);
        } else if (contentType.startsWith("application/json")) {
            String json = new String(data);
            ExportTraceServiceRequest.Builder builder = ExportTraceServiceRequest.newBuilder();
            JsonFormat.parser().merge(json, builder);
            request = builder.build();
        } else {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build();
        }

        for (ResourceSpans resourceSpans : request.getResourceSpansList()) {
            for (ScopeSpans scopeSpans : resourceSpans.getScopeSpansList()) {
                for (Span span : scopeSpans.getSpansList()) {
                    delegate.storeSpan(span);
                }
            }
        }

        ExportTraceServiceResponse response = ExportTraceServiceResponse.newBuilder().build();

        return Response.ok(response.toByteArray()).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllTraces() throws SQLException {

        HashMap<String, ArrayList<Span>> traces = delegate.getAllTraces();

        Map<String, List<String>> result = new HashMap<>();

        for (Map.Entry<String, ArrayList<Span>> entry : traces.entrySet()) {

            List<String> jsonSpans = new ArrayList<>();

            for (Span span : entry.getValue()) {
                try {
                    String json = JsonFormat.printer().print(span);
                    jsonSpans.add(json);
                } catch (Exception e) {
                    jsonSpans.add("{\"error\":\"failed to serialize span\"}");
                }
            }

            result.put(entry.getKey(), jsonSpans);
        }

        return Response.ok(result).build();
    }
}