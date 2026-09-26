package org.wearefrank.ladybug.test.junit.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;

import org.junit.Test;

import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.xmldecoder.XMLDecoder;

public class TestSafeClasses {
	private static final String PROPERTY = "ladybug.TestSafeClasses";

	private static class Result {
		Object value;
		final List<Exception> exceptions = new ArrayList<>();

		boolean isBlocked() {
			return exceptions.stream().anyMatch(e -> e instanceof IllegalArgumentException
					&& e.getMessage().startsWith("Unsupported"));
		}
	}

	private static Result decode(String body) {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><java class=\"java.beans.XMLDecoder\">" + body + "</java>";
		Result result = new Result();
		XMLDecoder decoder = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), null, result.exceptions::add);
		try {
			result.value = decoder.readObject();
		} catch (ArrayIndexOutOfBoundsException e) {
			// No object was decoded
		}
		return result;
	}

	@Test
	public void getClassCannotBeUsedToReachOtherClasses() {
		Result result = decode("<object class=\"java.util.HashMap\"><void method=\"getClass\"><void method=\"forName\">"
				+ "<string>java.lang.System</string><void method=\"setProperty\"><string>" + PROPERTY + "</string>"
				+ "<string>reached</string></void></void></void></object>");
		assertTrue(result.isBlocked());
		assertNull(System.getProperty(PROPERTY));
	}

	@Test
	public void immutableClassCanBeInstantiated() {
		Result result = decode("<object class=\"java.math.BigDecimal\"><string>1.50</string></object>");
		assertFalse(result.isBlocked());
		assertEquals(new java.math.BigDecimal("1.50"), result.value);
	}

	@Test
	public void noMethodsCanBeCalledOnImmutableClass() {
		Result result = decode("<object class=\"java.util.Date\"><long>0</long><void method=\"setTime\"><long>5</long></void></object>");
		assertTrue(result.isBlocked());
	}

	@Test
	public void timestampKeepsItsNanos() {
		Timestamp timestamp = new Timestamp(1790343533123L);
		timestamp.setNanos(123456789);
		Result result = decode(encode(timestamp));
		assertFalse(result.exceptions.toString(), result.isBlocked());
		assertEquals(timestamp, result.value);
		assertEquals(123456789, ((Timestamp) result.value).getNanos());
	}


	@Test
	public void zoneOffsetCanOnlyBeCreatedWithOfTotalSeconds() {
		Result result = decode("<object class=\"java.time.ZoneOffset\" method=\"ofTotalSeconds\"><int>7200</int></object>");
		assertFalse(result.exceptions.toString(), result.isBlocked());
		assertEquals(java.time.ZoneOffset.ofHours(2), result.value);
		assertTrue(decode("<object class=\"java.time.ZoneOffset\" method=\"of\"><string>+02:00</string></object>").isBlocked());
	}

	@Test
	public void onlySetNanosCanBeCalledOnTimestamp() {
		assertTrue(decode("<object class=\"java.sql.Timestamp\"><long>0</long><void property=\"time\"><long>5</long></void></object>").isBlocked());
	}

	@Test
	public void immutableClassCanOnlyBeCreatedWithConfiguredFactory() {
		assertTrue(decode("<object class=\"java.util.UUID\" method=\"randomUUID\"/>").isBlocked());
		assertFalse(decode("<object class=\"java.util.UUID\" method=\"fromString\">"
				+ "<string>0f8fad5b-d9cb-469f-a165-70867728950e</string></object>").isBlocked());
	}

	@Test
	public void collectionsCanOnlyBeFilled() {
		Result result = decode("<object class=\"java.util.ArrayList\"><void method=\"add\"><string>a</string></void></object>");
		assertFalse(result.isBlocked());
		assertEquals(List.of("a"), result.value);
		assertTrue(decode("<object class=\"java.util.ArrayList\"><void method=\"clear\"/></object>").isBlocked());
		assertTrue(decode("<object class=\"java.util.HashMap\"><void method=\"remove\"><string>a</string></void></object>").isBlocked());
	}

	@Test
	public void mapImplementationsCanBeDecoded() {
		List<Map<String, Object>> maps = List.of(new HashMap<>(), new LinkedHashMap<>(), new TreeMap<>(), new Hashtable<>(),
				new ConcurrentHashMap<>(), new ConcurrentSkipListMap<>());
		for (Map<String, Object> map: maps) {
			map.put("k", "v");
			Result result = decode(encode(map));
			assertFalse(result.exceptions.toString(), result.isBlocked());
			assertEquals(map.getClass(), result.value.getClass());
			assertEquals("v", ((Map<?, ?>) result.value).get("k"));
		}
		Properties properties = new Properties();
		properties.put("k", "v");
		Result result = decode(encode(properties));
		assertEquals(properties, result.value);
	}

	@Test
	public void collectionImplementationsCanBeDecoded() {
		List<Collection<String>> collections = List.of(new ArrayList<>(), new LinkedList<>(), new ArrayDeque<>(), new Vector<>(),
				new Stack<>(), new HashSet<>(), new LinkedHashSet<>(), new TreeSet<>(), new PriorityQueue<>(),
				new CopyOnWriteArraySet<>(), new ConcurrentLinkedQueue<>(), new ConcurrentLinkedDeque<>(),
				new ConcurrentSkipListSet<>(), new LinkedBlockingQueue<>(), new LinkedBlockingDeque<>(),
				new PriorityBlockingQueue<>(), new LinkedTransferQueue<>());
		for (Collection<String> collection: collections) {
			collection.add("a");
			collection.add("b");
			Result result = decode(encode(collection));
			assertFalse(result.exceptions.toString(), result.isBlocked());
			assertEquals(collection.getClass(), result.value.getClass());
			// Copy because queues do not implement equals()
			assertEquals(new ArrayList<>(collection), new ArrayList<>((Collection<?>) result.value));
		}
	}

	@Test
	public void otherCollectionImplementationsAreBlocked() {
		assertTrue(decode("<object class=\"java.util.concurrent.CopyOnWriteArrayList\"/>").isBlocked());
	}

	@Test
	public void otherMapImplementationsAreBlocked() {
		// java.util.jar.Attributes implements Map but is not listed as safe
		assertTrue(decode("<object class=\"java.util.jar.Attributes\"/>").isBlocked());
	}

	@Test
	public void mapsCanOnlyBeFilled() {
		assertTrue(decode("<object class=\"java.util.TreeMap\"><void method=\"comparator\"/></object>").isBlocked());
		assertTrue(decode("<object class=\"java.util.Properties\"><void method=\"load\"><null/></void></object>").isBlocked());
		assertTrue(decode("<object class=\"java.util.concurrent.ConcurrentHashMap\"><void method=\"clear\"/></object>").isBlocked());
	}

	// Returns the content of the java element written by XMLEncoder
	private static String encode(Object object) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		XMLEncoder encoder = new XMLEncoder(outputStream);
		encoder.writeObject(object);
		encoder.close();
		String xml = outputStream.toString(StandardCharsets.UTF_8);
		return xml.substring(xml.indexOf('>', xml.indexOf("<java")) + 1, xml.lastIndexOf("</java>"));
	}

	@Test
	public void collectionsFactoryMethodsAreAllowed() {
		Result result = decode("<object class=\"java.util.Collections\" method=\"unmodifiableMap\"><object class=\"java.util.HashMap\">"
				+ "<void method=\"put\"><string>k</string><string>v</string></void></object></object>");
		assertFalse(result.isBlocked());
		assertEquals(Map.of("k", "v"), result.value);
		assertTrue(decode("<object class=\"java.util.Collections\" method=\"shuffle\"><object class=\"java.util.ArrayList\"/></object>").isBlocked());
	}

	@Test
	public void reportCanBeFilledWithPersistentProperties() {
		Result result = decode("<object class=\"org.wearefrank.ladybug.Report\" id=\"Report0\">"
				+ "<void property=\"name\"><string>report name</string></void>"
				+ "<void property=\"checkpoints\"><void method=\"add\"><object class=\"org.wearefrank.ladybug.Checkpoint\">"
				+ "<void property=\"level\"><int>2</int></void>"
				+ "<void property=\"message\"><string>message</string></void>"
				+ "<void property=\"report\"><object idref=\"Report0\"/></void>"
				+ "</object></void></void></object>");
		assertFalse(result.exceptions.toString(), result.isBlocked());
		Report report = (Report) result.value;
		assertEquals("report name", report.getName());
		Checkpoint checkpoint = report.getCheckpoints().get(0);
		assertEquals(2, checkpoint.getLevel());
		assertEquals("message", checkpoint.getMessage());
		assertEquals(report, checkpoint.getReport());
	}

	@Test
	public void oldReportClassNamesAreReplaced() {
		Result result = decode("<object class=\"nl.nn.testtool.Report\" id=\"Report0\">"
				+ "<void property=\"name\"><string>old report</string></void>"
				+ "<void property=\"checkpoints\"><void method=\"add\"><object class=\"nl.nn.testtool.Checkpoint\">"
				+ "<void property=\"message\"><string>message</string></void>"
				+ "<void property=\"report\"><object idref=\"Report0\"/></void>"
				+ "</object></void></void></object>");
		assertFalse(result.exceptions.toString(), result.isBlocked());
		Report report = (Report) result.value;
		assertEquals("old report", report.getName());
		Checkpoint checkpoint = report.getCheckpoints().get(0);
		assertEquals("message", checkpoint.getMessage());
		assertEquals(report, checkpoint.getReport());
	}

	@Test
	public void transientPropertiesAndOtherMethodsOfReportAreBlocked() {
		assertTrue(decode("<object class=\"org.wearefrank.ladybug.Report\"><void property=\"testTool\"><null/></void></object>").isBlocked());
		assertTrue(decode("<object class=\"org.wearefrank.ladybug.Report\"><void method=\"toXml\"/></object>").isBlocked());
		assertTrue(decode("<object class=\"org.wearefrank.ladybug.Checkpoint\"><void method=\"getReport\"/></object>").isBlocked());
	}

	@Test
	public void overloadedSetterWithOtherArgumentTypeIsBlocked() {
		// Checkpoint.setMessage(Object) instead of the bean setter Checkpoint.setMessage(String)
		assertTrue(decode("<object class=\"org.wearefrank.ladybug.Checkpoint\"><void property=\"message\">"
				+ "<object class=\"java.util.HashMap\"/></void></object>").isBlocked());
	}

	@Test
	public void fieldAccessIsBlocked() {
		assertTrue(decode("<object class=\"java.util.Collections\" field=\"EMPTY_LIST\"/>").isBlocked());
	}

	@Test
	public void methodAndPropertyElementsAreChecked() {
		assertTrue(decode("<object class=\"java.util.HashMap\"><method name=\"getClass\"/></object>").isBlocked());
		assertTrue(decode("<object class=\"java.util.Date\"><long>0</long><property name=\"time\"><long>5</long></property></object>").isBlocked());
	}
}
