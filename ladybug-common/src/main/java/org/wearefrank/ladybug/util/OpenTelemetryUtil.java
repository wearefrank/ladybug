/*
   Copyright 2024, 2025 WeAreFrank!

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
package org.wearefrank.ladybug.util;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.ServiceAttributes;
import org.wearefrank.ladybug.Report;

public class OpenTelemetryUtil {

	/**
	 * Moved code from TestTool.init() to this method to prevent the following when OpenTelemetry is not used:
	 * <p>
	 * <code>
	 * java.lang.IllegalStateException: Failed to introspect Class [nl.nn.testtool.TestTool]
	 * ...
	 * java.lang.ClassNotFoundException: io.opentelemetry.sdk.trace.export.SpanExporter
	 * </code>
	 * </p>
	 * This happens because OpenTelemetry dependencies have scope provided for now.
	 * 
	 * @param openTelemetryEndpoint ...
	 * @return ...
	 */
	public static Tracer getOpenTelemetryTracer(String openTelemetryEndpoint) {
		boolean useZipkin = openTelemetryEndpoint.contains("9411");
		boolean useJaeger = openTelemetryEndpoint.equals("jaeger");
		if (useZipkin || useJaeger) {
			SpanExporter spanExporter;
			if (useZipkin) {
				spanExporter = ZipkinSpanExporter.builder().setEndpoint(openTelemetryEndpoint).build();
			} else {
				spanExporter = OtlpGrpcSpanExporter.builder().build();
			}
			SpanProcessor spanProcessor = BatchSpanProcessor.builder(spanExporter).build();
			Resource resource = Resource.getDefault().toBuilder().put(ServiceAttributes.SERVICE_NAME, "ladybug")
					.put(ServiceAttributes.SERVICE_VERSION, "1.0.0").build();
			SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
						.addSpanProcessor(spanProcessor)
						.setResource(resource)
						.build();
			OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
				.setTracerProvider(sdkTracerProvider)
				.setPropagators(
						ContextPropagators.create(
						TextMapPropagator.composite(
						W3CTraceContextPropagator.getInstance(), W3CBaggagePropagator.getInstance())))
				.buildAndRegisterGlobal();
			return openTelemetry.getTracer(Report.class.getName(), "0.1.0");
		}
		return null;
	}

}
