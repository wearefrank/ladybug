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
package org.wearefrank.ladybug;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import io.opentelemetry.proto.trace.v1.Span;
import org.springframework.stereotype.Component;
import org.wearefrank.ladybug.web.common.CollectorApiImpl;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@Component
public class SpanBuffer {
    private final Cache<String, ArrayList<Span>> cache;

    private CollectorApiImpl delegate;

    public SpanBuffer(CollectorApiImpl delegate) {
        this.delegate = delegate;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .scheduler(Scheduler.systemScheduler())
                .removalListener((String traceId, ArrayList<Span> spans, RemovalCause cause) -> {
                    if (spans != null && cause == RemovalCause.EXPIRED) {
                        ArrayList<Span> spansCopy = new ArrayList<>(spans);
                        delegate.processSpans(spansCopy);
                    }
                })
                .build();
    }

    public void addSpan(Span span) {
        String traceId = this.delegate.byteStringToHex(span.getTraceId());

        cache.asMap().compute(traceId, (key, existing) -> {
            ArrayList<Span> updated =
                    existing == null ? new ArrayList<>() : new ArrayList<>(existing);

            updated.add(span);
            return updated;
        });
    }
}