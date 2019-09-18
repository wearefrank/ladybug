package nl.nn.testtool;

import org.junit.Test;
import org.junit.Assert;

/**
 * This test was included with the sole purpose of having
 * some tests in the ibis-ladybug project. If real tests
 * are added, this one can be removed.
 *
 * When Martijn made a Jenkins job for this Maven project,
 * he copied from a project that did have unit tests. That
 * Jenkins configuration only works if there are unit tests.
 * This way, the configuration won't need change when real
 * unit tests are introduced.
 *
 * @author martijn
 *
 */
public class DummyTest {
	@Test
	public void testDummy() {
		Assert.assertEquals(1, 1);
	}
}
