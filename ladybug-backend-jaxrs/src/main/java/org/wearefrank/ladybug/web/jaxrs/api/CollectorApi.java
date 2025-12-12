/*
   Copyright 2021-2025 WeAreFrank!

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

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Setter;
import org.wearefrank.ladybug.Span;
import org.springframework.beans.factory.annotation.Autowired;

import org.wearefrank.ladybug.web.common.CollectorApiImpl;
import org.wearefrank.ladybug.web.common.Constants;

@Path("/" + Constants.LADYBUG_API_PATH + "/collector")
public class CollectorApi extends ApiBase {
    @Autowired
    private @Setter CollectorApiImpl delegate;

    @POST
    @Path("/")
    public Response collectSpans(Span[] trace) {
        delegate.processSpans(trace);
        return Response.ok().build();
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response collectSpansJson(Span[] trace) {
        delegate.processSpans(trace);
        return Response.ok().build();
    }
}
