package org.wearefrank.ladybug.web.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class FrontendRolesResolverTest {
	@Test
	public void when_user_has_one_frontend_role_then_one_role_returned() {
		FrontendRolesResolver instance = nonCumulativeInstance();
		Predicate<List<String>> userInRole = roles -> roles.contains("SomeBackendTesterRole");
		assertEquals(Arrays.asList("tester"), instance.getFrontendRoles(userInRole));
		assertEquals(Arrays.asList("tester"), instance.getFrontendRoles("SomeBackendTesterRole"));
		userInRole = roles -> roles.contains("SomeObserverRole");
		assertEquals(Arrays.asList("observer"), instance.getFrontendRoles(userInRole));
		assertEquals(Arrays.asList("observer"), instance.getFrontendRoles("SomeObserverRole"));
	}

	@Test
	public void when_user_has_multiple_fronten_roles_then_all_returned() {
		FrontendRolesResolver instance = cumulativeInstance();
		Predicate<List<String>> userInRole = roles -> roles.contains("SomeBackendTesterRole");
		List<String> actual = new ArrayList<>(instance.getFrontendRoles(userInRole));
		Collections.sort(actual);
		assertEquals(Arrays.asList("admin", "observer", "tester"), actual);
		actual = new ArrayList<>(instance.getFrontendRoles("SomeBackendTesterRole"));
		Collections.sort(actual);
		assertEquals(Arrays.asList("admin", "observer", "tester"), actual);
		userInRole = roles -> roles.contains("SomeObserverRole");
		assertEquals(Arrays.asList("observer"), instance.getFrontendRoles(userInRole));
		assertEquals(Arrays.asList("observer"), instance.getFrontendRoles("SomeObserverRole"));
	}

	private FrontendRolesResolver nonCumulativeInstance() {
		FrontendRolesResolver instance = new FrontendRolesResolver();
		// Can be anything, depending on the Spring configuration XML on the classpath.
		instance.testerRoles = Arrays.asList("SomeBackendTesterRole");
		instance.dataAdminRoles = Arrays.asList("SomeDataAdminRole");
		instance.observerRoles = Arrays.asList("SomeObserverRole");
		return instance;
	}

	private FrontendRolesResolver cumulativeInstance() {
		FrontendRolesResolver instance = new FrontendRolesResolver();
		// Can be anything, depending on the Spring configuration XML on the classpath.
		instance.testerRoles = Arrays.asList("SomeBackendTesterRole");
		instance.dataAdminRoles = Arrays.asList("SomeDataAdminRole", "SomeBackendTesterRole");
		instance.observerRoles = Arrays.asList("SomeObserverRole", "SomeDataAdminRole", "SomeBackendTesterRole");
		return instance;
	}
}
