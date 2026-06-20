package org.wearefrank.ladybug.filter;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Arrays;

import org.junit.Test;
import org.junit.Assert;

public class HostAndApplicationHolderTest {
	static class Stub implements HostAndApplicationHolder {
		private @Getter @Setter boolean hostSet = false;
		private @Getter @Setter boolean applicationSet = false;
	}

	private List<String> testMetadataNames = Arrays.asList("host", "application", "xxx");

	@Test
	public void whenTestToolHasNoHostAndNoApplicationThenTheyAreOmittedFromAView() {
		Stub instance = new Stub();
		Assert.assertEquals(Arrays.asList("xxx"), instance.filterMetadataNames(testMetadataNames));
	}

	@Test
	public void whenTestToolHasOnlyHostThenViewsKeepHostAndOmitApplication() {
		Stub instance = new Stub();
		instance.setHostSet(true);
		Assert.assertEquals(Arrays.asList("host", "xxx"), instance.filterMetadataNames(testMetadataNames));
	}

	@Test
	public void whenTestHasHostAndApplicationThenViewsKeepBoth() {
		Stub instance = new Stub();
		instance.setHostSet(true);
		instance.setApplicationSet(true);
		Assert.assertEquals(Arrays.asList("host", "application", "xxx"), instance.filterMetadataNames(testMetadataNames));
	}
}
