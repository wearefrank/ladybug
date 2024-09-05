/*
   Copyright 2024 WeAreFrank!

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
package nl.nn.testtool.test.junit.util;

import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;

public class Span implements io.opentelemetry.api.trace.Span {

	@Override
	public <T> io.opentelemetry.api.trace.Span setAttribute(AttributeKey<T> key, T value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public io.opentelemetry.api.trace.Span addEvent(String name, Attributes attributes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public io.opentelemetry.api.trace.Span addEvent(String name, Attributes attributes, long timestamp, TimeUnit unit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public io.opentelemetry.api.trace.Span setStatus(StatusCode statusCode, String description) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public io.opentelemetry.api.trace.Span recordException(Throwable exception, Attributes additionalAttributes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public io.opentelemetry.api.trace.Span updateName(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void end() {
		// TODO Auto-generated method stub

	}

	@Override
	public void end(long timestamp, TimeUnit unit) {
		// TODO Auto-generated method stub

	}

	@Override
	public SpanContext getSpanContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isRecording() {
		// TODO Auto-generated method stub
		return false;
	}

}
