package org.wearefrank.ladybug.filter;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Arrays;

import org.junit.Test;
import org.junit.Assert;

public class ApplicationMetadataItemHolderTest {
	static class Stub implements ApplicationMetadataItemHolder {
		private @Getter @Setter boolean applicationSet = false;
	}

	private List<String> testMetadataNames = Arrays.asList("application", "xxx");

	@Test
	public void whenTestToolHasNoApplicationThenOmittedFromAView() {
		Stub instance = new Stub();
		Assert.assertEquals(Arrays.asList("xxx"), instance.filterMetadataNames(testMetadataNames));
	}

	@Test
	public void whenTestHasApplicationThenViewsKeepIt() {
		Stub instance = new Stub();
		instance.setApplicationSet(true);
		Assert.assertEquals(Arrays.asList("application", "xxx"), instance.filterMetadataNames(testMetadataNames));
	}
}
