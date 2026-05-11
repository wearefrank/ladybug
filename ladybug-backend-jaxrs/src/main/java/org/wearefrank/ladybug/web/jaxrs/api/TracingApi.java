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
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.web.common.TracingApiImpl;
import org.wearefrank.ladybug.web.common.Constants;

import java.sql.SQLException;
import java.util.ArrayList;

@Path("/" + Constants.LADYBUG_API_PATH + "/v1/traces")
public class TracingApi extends ApiBase {

    @Autowired
    private @Setter TracingApiImpl delegate;

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
    public Response getTraceReports(@QueryParam("amount") Integer amount) throws SQLException {

        ArrayList<Report> traceReports = delegate.getTraceReports();

        if (amount != null && traceReports.size() > amount) {
            return Response.ok(traceReports.subList(0, amount)).build();
        }

        return Response.ok(traceReports).build();
    }

    @GET
    @Path("/count")
    public Response getTraceCount() throws SQLException {
        int count = delegate.getTraceCount();

        return Response.ok(count).build();
    }
}