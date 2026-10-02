/*
   Copyright 2021, 2025 WeAreFrank!

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
package org.wearefrank.ladybug.test.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.MessageEncoder.ToStringResult;
import org.wearefrank.ladybug.MessageEncoderImpl;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.util.XmlUtil;

/**
 * @author Jaco de Groot
 */
public class TestMessageEncoder {
	public static final String RESOURCE_PATH = "org/wearefrank/ladybug/test/junit/";

	@Rule
	public TestName name = new TestName();

	@Test
	public void testToString() throws SAXException, IOException, ParserConfigurationException, StorageException {
		TestTool testTool = new TestTool();
		Report report = new Report();
		report.setTestTool(testTool);
		Checkpoint checkpoint = new Checkpoint();
		checkpoint.setReport(report);
		String actual;

		// Test Integer
		actual = testTool.getMessageEncoder().toString(10, null).getString();
		actual = ReportRelatedTestCase.applyXmlEncoderIgnores(actual);
		ReportRelatedTestCase.assertXml(RESOURCE_PATH, name.getMethodName(), actual);
		checkpoint.setMessage(actual);
		checkpoint.setEncoding(MessageEncoderImpl.XML_ENCODER);
		assertEquals(10, checkpoint.getMessageAsObject());
		assertEquals(new Integer(10), checkpoint.getMessageAsObject(new Integer(1)));

		// Test Date
		Date date = new Date(0L);
		actual = testTool.getMessageEncoder().toString(date, null).getString();
		String offset = new SimpleDateFormat("Z").format(date);
		assertEquals("1970-01-01T" + offset.substring(1, 3) + ":" + offset.substring(3, 5) + ":00.000" + offset, actual);
		checkpoint.setMessage(actual);
		checkpoint.setEncoding(MessageEncoderImpl.DATE_ENCODER);
		assertEquals(new Date(0L), checkpoint.getMessageAsObject());
		assertEquals(new Date(0L), checkpoint.getMessageAsObject(new Date(10L)));

		// MediaType
		MediaType mediaType = MediaType.valueOf("application/json");
		actual = testTool.getMessageEncoder().toString(mediaType, null).getString();
		checkpoint.setMessage(actual);
		checkpoint.setEncoding("org.springframework.http.MediaType.toString()");
		Object roundTrip = checkpoint.getMessageAsObject();
		assertEquals("application/json", roundTrip.toString());
		assertTrue(roundTrip instanceof MediaType);

		// MimeType
		MimeType mimeType = MimeType.valueOf("FakeMime/type");
		actual = testTool.getMessageEncoder().toString(mimeType, null).getString();
		checkpoint.setMessage(actual);
		checkpoint.setEncoding("org.springframework.util.MimeType.toString()");
		roundTrip = checkpoint.getMessageAsObject();
		assertEquals("FakeMime/type", roundTrip.toString());
		assertTrue(roundTrip instanceof MimeType);

		// Test Node
		Node node = XmlUtil.stringToNode("<test/>");
		assertTrue(node instanceof Node);
		assertEquals("test", node.getNodeName());
		actual = testTool.getMessageEncoder().toString(node, null).getString();
		assertEquals("<test/>", actual);
		checkpoint.setMessage(actual);
		checkpoint.setEncoding(MessageEncoderImpl.DOM_NODE_ENCODER);
		node = (Node)checkpoint.getMessageAsObject();
		assertTrue(node instanceof Node);
		assertEquals("test", node.getNodeName());
		node = checkpoint.getMessageAsObject(XmlUtil.stringToNode("<test2/>"));
		assertTrue(node instanceof Node);
		assertEquals("test", node.getNodeName());
	}


	@Test
	public void testSpeciallyEncodedClassesWithOtherFactoryThanValueOf() {
		TestTool testTool = new TestTool();
		Report report = new Report();
		report.setTestTool(testTool);
		Checkpoint checkpoint = new Checkpoint();
		checkpoint.setReport(report);
		List<Object> values = List.of(
				Instant.parse("2026-09-25T13:38:53.123Z"),
				LocalDate.parse("2026-09-25"),
				LocalDateTime.parse("2026-09-25T15:38:53.123"),
				LocalTime.parse("15:38:53.123456789"),
				OffsetDateTime.parse("2026-09-25T15:38:53.123+02:00"),
				OffsetTime.parse("09:00+02:00"),
				ZonedDateTime.parse("2026-09-25T15:38:53.123+02:00[Europe/Amsterdam]"),
				Duration.parse("PT1H2M3S"),
				Period.parse("P1Y2M3D"),
				Year.of(2026),
				YearMonth.of(2026, 9),
				MonthDay.of(2, 29),
				UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e"),
				new BigDecimal("1234.5600"),
				new BigDecimal("1E+3"),
				new BigInteger("123456789012345678901234567890"));
		for (Object value: values) {
			ToStringResult toStringResult = testTool.getMessageEncoder().toString(value, null);
			assertEquals(value.toString(), toStringResult.getString());
			assertEquals(value.getClass().getName() + ".toString()", toStringResult.getEncoding());
			checkpoint.setMessage(toStringResult.getString());
			checkpoint.setEncoding(toStringResult.getEncoding());
			assertEquals(value, checkpoint.getMessageAsObject());
		}
	}

	@Test
	public void testTimestampRoundTrip() {
		TestTool testTool = new TestTool();
		Report report = new Report();
		report.setTestTool(testTool);
		Checkpoint checkpoint = new Checkpoint();
		checkpoint.setReport(report);
		Timestamp timestamp = new Timestamp(1790343533123L);
		timestamp.setNanos(123456789);
		ToStringResult toStringResult = testTool.getMessageEncoder().toString(timestamp, null);
		assertEquals("2026-09-25T13:38:53.123456789Z", toStringResult.getString());
		assertEquals(MessageEncoderImpl.TIMESTAMP_ENCODER, toStringResult.getEncoding());
		checkpoint.setMessage(toStringResult.getString());
		checkpoint.setEncoding(toStringResult.getEncoding());
		Object roundTrip = checkpoint.getMessageAsObject();
		assertTrue(roundTrip instanceof Timestamp);
		assertEquals(timestamp, roundTrip);
		assertEquals(123456789, ((Timestamp)roundTrip).getNanos());
	}


	@Test
	public void testZoneOffsetRoundTrip() {
		TestTool testTool = new TestTool();
		Report report = new Report();
		report.setTestTool(testTool);
		Checkpoint checkpoint = new Checkpoint();
		checkpoint.setReport(report);
		for (ZoneOffset zoneOffset: List.of(ZoneOffset.ofHours(2), ZoneOffset.UTC, ZoneOffset.ofHoursMinutesSeconds(-5, -30, -15))) {
			ToStringResult toStringResult = testTool.getMessageEncoder().toString(zoneOffset, null);
			assertEquals(zoneOffset.toString(), toStringResult.getString());
			assertEquals(MessageEncoderImpl.ZONE_OFFSET_ENCODER, toStringResult.getEncoding());
			checkpoint.setMessage(toStringResult.getString());
			checkpoint.setEncoding(toStringResult.getEncoding());
			assertEquals(zoneOffset, checkpoint.getMessageAsObject());
		}
	}
}
