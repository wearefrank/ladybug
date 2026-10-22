package org.wearefrank.ladybug.test.junit.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.wearefrank.ladybug.xmldecoder.XMLDecoder;

/**
 * SafeClasses (see TestSafeClasses) only guards the class and method lookups that happen while constructing
 * objects and calling methods. The &lt;int&gt;, &lt;long&gt; and &lt;string&gt; elements never go through that
 * lookup: their handlers (IntElementHandler, LongElementHandler, StringElementHandler) just call
 * Integer.decode()/Long.decode() or use the element text directly, in plain Java code. These tests check that
 * int, Integer, long, Long and String decode correctly in every form XMLEncoder/XMLDecoder's xml format supports
 * for them, including as the component type of an array (an int[], Integer[], ... or String[], all written by
 * XMLEncoder as {@code <array class="...">}), and that the eight classes that box a primitive type plus String
 * can also be constructed directly with {@code <object class="..." method="valueOf">}, while remaining immutable:
 * no other method can be called on them.
 */
public class TestPrimitiveTypes {
	private static class Result {
		Object value;
		final List<Exception> exceptions = new ArrayList<>();

		boolean isBlocked() {
			return isBlockedWith("Unsupported");
		}

		boolean isMethodCallBlocked() {
			return isBlockedWith("Unsupported method call");
		}

		boolean isClassBlocked() {
			return isBlockedWith("Unsupported class");
		}

		private boolean isBlockedWith(String messagePrefix) {
			return exceptions.stream().anyMatch(e -> e instanceof IllegalArgumentException
					&& e.getMessage().startsWith(messagePrefix));
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
	public void intElementSupportsEveryNumberFormatIntegerDecodeAccepts() {
		assertEquals(42, decode("<int>42</int>").value);
		assertEquals(-7, decode("<int>-7</int>").value);
		assertEquals(42, decode("<int>0x2A</int>").value);
		assertEquals(42, decode("<int>#2A</int>").value);
		assertEquals(42, decode("<int>052</int>").value); // octal
		assertEquals(-42, decode("<int>-0x2A</int>").value);
		assertEquals(Integer.class, decode("<int>42</int>").value.getClass());
	}

	@Test
	public void longElementSupportsEveryNumberFormatLongDecodeAccepts() {
		assertEquals(42L, decode("<long>42</long>").value);
		assertEquals(-7L, decode("<long>-7</long>").value);
		assertEquals(42L, decode("<long>0x2A</long>").value);
		assertEquals(42L, decode("<long>#2A</long>").value);
		assertEquals(42L, decode("<long>052</long>").value); // octal
		assertEquals(Long.class, decode("<long>42</long>").value.getClass());
	}

	@Test
	public void stringElementSupportsPlainAndEmptyText() {
		assertEquals("hello", decode("<string>hello</string>").value);
		assertEquals("", decode("<string></string>").value);
		// The value of a nested element is converted with String.valueOf() and appended, per StringElementHandler
		assertEquals("5", decode("<string><int>5</int></string>").value);
	}

	@Test
	public void integerWrittenByEncoderDecodesBack() {
		Result result = decode(encode(Integer.valueOf(42)));
		assertFalse(result.exceptions.toString(), result.isBlocked());
		assertEquals(Integer.valueOf(42), result.value);
	}

	@Test
	public void longWrittenByEncoderDecodesBack() {
		Result result = decode(encode(Long.valueOf(42L)));
		assertFalse(result.exceptions.toString(), result.isBlocked());
		assertEquals(Long.valueOf(42L), result.value);
	}

	@Test
	public void stringWrittenByEncoderDecodesBack() {
		Result result = decode(encode("a string with <angle brackets> & an ampersand"));
		assertFalse(result.exceptions.toString(), result.isBlocked());
		assertEquals("a string with <angle brackets> & an ampersand", result.value);
	}

	@Test
	public void idAndIdrefCanBeUsedToReuseAPreviouslyDecodedIntOrString() {
		List<Exception> exceptions = new ArrayList<>();
		// An element with an id attribute is only stored as a variable, not itself returned by readObject();
		// only the <var idref=".."/> that looks it back up becomes a top-level decoded object. The decoy variable
		// "other", stored after "n" and "s" but before either is looked up, makes sure idref resolves by the
		// specific id given: a lookup that instead returned whatever variable was stored most recently would
		// wrongly return the decoy's value for both <var> elements below.
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><java class=\"java.beans.XMLDecoder\">"
				+ "<int id=\"n\">99</int>"
				+ "<string id=\"s\">reused</string>"
				+ "<int id=\"other\">-1</int>"
				+ "<var idref=\"n\"/>"
				+ "<var idref=\"s\"/>"
				+ "</java>";
		XMLDecoder decoder = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), null, exceptions::add);
		assertEquals(99, decoder.readObject());
		assertEquals("reused", decoder.readObject());
		assertTrue(exceptions.toString(), exceptions.isEmpty());
		// Only the two <var> elements were decoded as top-level objects: the three id'd elements, despite being
		// decoded too, were only stored as variables, not added to the result of readObject().
		try {
			decoder.readObject();
			throw new AssertionError("Expected an ArrayIndexOutOfBoundsException, no third object was written");
		} catch (ArrayIndexOutOfBoundsException expected) {
			// readObject() throws this when the stream has no (more) objects left, per its javadoc
		}
	}

	@Test
	public void collectionsCanContainIntegerLongAndStringValues() {
		List<Object> list = new ArrayList<>();
		list.add(1);
		list.add(2L);
		list.add("three");
		Result result = decode(encode(list));
		assertFalse(result.exceptions.toString(), result.isBlocked());
		assertEquals(list, result.value);
	}

	// The eight classes that box a primitive type, plus String, are on SafeClasses.ALL with "valueOf" as their
	// only allowed factory method (see SafeClasses.SCALAR_WRAPPER_CLASSES). XMLEncoder itself never writes any of
	// these as an <object>, only as the <boolean>/<byte>/.../<string> shortcut tested above, so this is mainly
	// what makes an array of one of these types possible to create (see the array tests below); being able to
	// construct one directly this way is a side effect of allowing the class at all.
	@Test
	public void scalarWrapperClassesCanBeInstantiatedWithValueOf() {
		assertEquals(Boolean.TRUE, decode("<object class=\"java.lang.Boolean\" method=\"valueOf\"><string>true</string></object>").value);
		assertEquals(Byte.valueOf((byte) 5), decode("<object class=\"java.lang.Byte\" method=\"valueOf\"><string>5</string></object>").value);
		assertEquals(Character.valueOf('a'), decode("<object class=\"java.lang.Character\" method=\"valueOf\"><char>a</char></object>").value);
		assertEquals(Double.valueOf(1.5), decode("<object class=\"java.lang.Double\" method=\"valueOf\"><string>1.5</string></object>").value);
		assertEquals(Float.valueOf(1.5f), decode("<object class=\"java.lang.Float\" method=\"valueOf\"><string>1.5</string></object>").value);
		assertEquals(Integer.valueOf(5), decode("<object class=\"java.lang.Integer\" method=\"valueOf\"><string>5</string></object>").value);
		assertEquals(Long.valueOf(5L), decode("<object class=\"java.lang.Long\" method=\"valueOf\"><string>5</string></object>").value);
		assertEquals(Short.valueOf((short) 5), decode("<object class=\"java.lang.Short\" method=\"valueOf\"><string>5</string></object>").value);
		assertEquals("hello", decode("<object class=\"java.lang.String\" method=\"valueOf\"><string>hello</string></object>").value);
		// The explicit <method> tag form works the same way
		assertEquals(Long.valueOf(5L), decode("<method name=\"valueOf\" class=\"java.lang.Long\"><string>5</string></method>").value);
	}

	// Unlike java.util.Date and java.sql.Timestamp, the scalar wrapper classes are not configured with the "new"
	// constructor marker, only with "valueOf" (see SafeClasses.createImmutableClasses()), so the plain,
	// constructor-style notation without an explicit method="valueOf" remains blocked.
	@Test
	public void scalarWrapperClassesCannotBeInstantiatedWithPlainNew() {
		assertTrue(decode("<object class=\"java.lang.Integer\"><int>5</int></object>").isMethodCallBlocked());
		assertTrue(decode("<object class=\"java.lang.String\"><string>hello</string></object>").isMethodCallBlocked());
	}

	// Calling anything other than the configured "valueOf" factory remains blocked, including Integer/Long's own
	// decode(), which the IntElementHandler/LongElementHandler javadocs call equivalent to <int>/<long>. That
	// costs no real functionality: XMLEncoder never writes this form, it always writes <int>/<long> directly.
	@Test
	public void onlyValueOfCanBeCalledAsAFactoryMethodOnScalarWrapperClasses() {
		assertTrue(decode("<method name=\"decode\" class=\"java.lang.Integer\"><string>5</string></method>").isMethodCallBlocked());
		assertTrue(decode("<object class=\"java.lang.Long\" method=\"decode\"><string>5</string></object>").isMethodCallBlocked());
	}

	// The scalar wrapper classes stay immutable: once constructed, no instance method can be called on them,
	// same as the other immutable classes in SafeClasses (java.math.BigDecimal, java.util.UUID, ...).
	@Test
	public void noInstanceMethodCanBeCalledOnAScalarWrapperInstance() {
		assertTrue(decode("<object class=\"java.lang.Integer\" method=\"valueOf\"><int>5</int>"
				+ "<void method=\"intValue\"/></object>").isMethodCallBlocked());
		assertTrue(decode("<object class=\"java.lang.String\" method=\"valueOf\"><string>hi</string>"
				+ "<void method=\"length\"/></object>").isMethodCallBlocked());
	}

	// XMLEncoder writes an array as <array class="..." length="N">, naming the component type, for example
	// <array class="int"> for an int[], <array class="java.lang.Integer"> for an Integer[] and
	// <array class="java.lang.String"> for a String[]. Creating that array needs the component type to be looked
	// up, like any class, and setting its elements needs the set()/get() access that ArrayElementHandler's
	// javadoc documents for arrays (see SafeClasses.isAllowedArrayInvocation()).
	@Test
	public void arraysOfPrimitivesBoxedTypesAndStringCanBeDecoded() {
		assertArrayEquals(new int[] {1, 2, 3}, (int[]) decode(encode(new int[] {1, 2, 3})).value);
		assertArrayEquals(new long[] {1L, 2L}, (long[]) decode(encode(new long[] {1L, 2L})).value);
		assertArrayEquals(new boolean[] {true, false}, (boolean[]) decode(encode(new boolean[] {true, false})).value);
		assertArrayEquals(new Integer[] {1, 2}, (Integer[]) decode(encode(new Integer[] {1, 2})).value);
		assertArrayEquals(new Long[] {1L, 2L}, (Long[]) decode(encode(new Long[] {1L, 2L})).value);
		assertArrayEquals(new String[] {"a", "b"}, (String[]) decode(encode(new String[] {"a", "b"})).value);
	}

	// Besides the length-prefixed, index-addressed form XMLEncoder itself writes (tested above), the array
	// element format also allows listing the values directly as the array's length, without any index.
	@Test
	public void arrayElementsCanAlsoBeListedDirectlyWithoutIndices() {
		Result result = decode("<array class=\"int\">" + "<int>123</int><int>456</int></array>");
		assertFalse(result.exceptions.toString(), result.isBlocked());
		assertArrayEquals(new int[] {123, 456}, (int[]) result.value);
	}
}

