/*
   Copyright 2021-2024 WeAreFrank!

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
package nl.nn.testtool.web.api;

import lombok.Setter;
import nl.nn.testtool.Span;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.web.ApiServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.invoke.MethodHandles;

@Path("/" + ApiServlet.LADYBUG_API_PATH + "/collector")
public class CollectorApi extends ApiBase {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private @Setter @Inject @Autowired TestTool testTool;

    @POST
    @Path("/")
    public Response collectSpans(Span[] trace) {
        testCollector(trace);

        return Response.ok().build();
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response collectSpansJson(Span[] trace) {
        testCollector(trace);

        return Response.ok().build();
    }

    public void testCollector(Span[] trace) {
        try {
            testTool.startpoint(trace[0].getTraceId(), null, trace[0].getLocalEndpoint().get("serviceName").toString(), "Startpoint");
            for (Span span: trace) {
                testTool.infopoint(span.getTraceId(), null, span.getName(), span.toHashmap().toString());
            }
            testTool.endpoint(trace[0].getTraceId(), null, trace[0].getLocalEndpoint().get("serviceName").toString(), "Endpoint");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
