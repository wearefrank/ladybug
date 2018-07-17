package nl.nn.testtool;

import java.security.Principal;
import java.util.List;

public interface SecurityContext {

	public Principal getUserPrincipal();

	public boolean isUserInRoles(List<String> roles);

}
