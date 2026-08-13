package org.wearefrank.ladybug.test.junit;

import org.junit.Assert;
import org.junit.Test;
import org.wearefrank.ladybug.TestTool;

import java.util.regex.Pattern;

public class TestToolTest {
	@Test
	public void testDefaultValueOfHostIsTheIpAddresss() {
		TestTool testTool = new TestTool();
		String regexOfIpAddress = "^\\d+\\.\\d+\\.\\d+\\.\\d+$";
		String actual = testTool.getHost();
		Assert.assertTrue(Pattern.matches(regexOfIpAddress, actual));
	}
}
