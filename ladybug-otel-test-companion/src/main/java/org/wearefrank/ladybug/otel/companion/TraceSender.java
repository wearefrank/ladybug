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
package org.wearefrank.ladybug.otel.companion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.internal.otlp.traces.TraceRequestMarshaler;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Sends a trace with a parent span and two child spans to Ladybug.
 */
@Component
@Slf4j
public class TraceSender implements CommandLineRunner {
	private static final AttributeKey<String> SERVICE_NAME = AttributeKey.stringKey("service.name");
	private static final String ROOT_SPAN_NAME = "Otel root";
	private static final String HELLO_WORLD_SPAN_NAME = "Otel here, hello world!";
	private static final String GOODBYE_WORLD_SPAN_NAME = "Otel here, goodbye world";

	/**
	 * The OTLP protocols that are supported, see OTEL_EXPORTER_OTLP_PROTOCOL in the OpenTelemetry specification.
	 */
	public enum Protocol {
		HTTP_PROTOBUF("application/x-protobuf"),
		HTTP_JSON("application/json");

		private final String contentType;

		Protocol(String contentType) {
			this.contentType = contentType;
		}
	}

	/**
	 * How the spans of the trace are divided over HTTP requests.
	 */
	public enum Batching {
		/** All spans in one request */
		SINGLE_REQUEST,
		/** First a request with only the hello world span, then a request with the other spans */
		HELLO_WORLD_SEPARATE
	}

	@Value("${spring.application.name}")
	private String serviceName;

	@Value("${ladybug.otel.endpoint}")
	private String endpoint;

	@Value("${ladybug.otel.protocol}")
	private Protocol protocol;

	@Value("${ladybug.otel.batching}")
	private Batching batching;

	@Value("${ladybug.otel.timeout-seconds:30}")
	private long timeoutSeconds;

	@Override
	public void run(String... args) throws IOException, InterruptedException {
		List<SpanData> finishedSpans = Collections.synchronizedList(new ArrayList<>());
		Resource resource = Resource.getDefault().merge(Resource.create(Attributes.of(SERVICE_NAME, serviceName)));
		try (SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
				.setResource(resource)
				.addSpanProcessor(new CollectingSpanProcessor(finishedSpans))
				.build()) {
			createTrace(tracerProvider.get(getClass().getName()));
		}
		// Divide the spans over requests ourselves instead of using a BatchSpanProcessor, so that the requests made are
		// deterministic
		List<List<SpanData>> batches = switch (batching) {
			case SINGLE_REQUEST -> List.of(finishedSpans);
			case HELLO_WORLD_SEPARATE -> {
				Map<Boolean, List<SpanData>> isHelloWorld = finishedSpans.stream()
						.collect(Collectors.partitioningBy(span -> HELLO_WORLD_SPAN_NAME.equals(span.getName())));
				yield List.of(isHelloWorld.get(true), isHelloWorld.get(false));
			}
		};
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds)).build();
		for (List<SpanData> batch : batches) {
			send(httpClient, batch);
		}
		log.info("Trace sent successfully in {} request(s)", batches.size());
	}

	private void createTrace(Tracer tracer) {
		Span root = tracer.spanBuilder(ROOT_SPAN_NAME).startSpan();
		try (Scope ignored = root.makeCurrent()) {
			tracer.spanBuilder(HELLO_WORLD_SPAN_NAME).setParent(Context.current()).startSpan().end();
			tracer.spanBuilder(GOODBYE_WORLD_SPAN_NAME).setParent(Context.current()).startSpan().end();
		} finally {
			root.end();
		}
	}

	private void send(HttpClient httpClient, List<SpanData> spans) throws IOException, InterruptedException {
		// The public OtlpHttpSpanExporter only supports protobuf, so serialize with the marshaler it uses internally,
		// which also supports the OTLP JSON encoding
		TraceRequestMarshaler marshaler = TraceRequestMarshaler.create(spans);
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		if (protocol == Protocol.HTTP_JSON) {
			marshaler.writeJsonTo(body);
		} else {
			marshaler.writeBinaryTo(body);
		}
		HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
				.timeout(Duration.ofSeconds(timeoutSeconds))
				.header("Content-Type", protocol.contentType)
				.POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
				.build();
		List<String> spanNames = spans.stream().map(SpanData::getName).toList();
		log.info("Sending spans {} to [{}] as {}", spanNames, endpoint, protocol.contentType);
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() / 100 != 2) {
			throw new IllegalStateException("Failed to send spans " + spanNames + " to [" + endpoint + "], got HTTP status "
					+ response.statusCode() + " with body: " + response.body());
		}
	}

	private static class CollectingSpanProcessor implements SpanProcessor {
		private final List<SpanData> finishedSpans;

		CollectingSpanProcessor(List<SpanData> finishedSpans) {
			this.finishedSpans = finishedSpans;
		}

		@Override
		public void onStart(Context parentContext, ReadWriteSpan span) {
		}

		@Override
		public boolean isStartRequired() {
			return false;
		}

		@Override
		public void onEnd(ReadableSpan span) {
			finishedSpans.add(span.toSpanData());
		}

		@Override
		public boolean isEndRequired() {
			return true;
		}
	}
}
