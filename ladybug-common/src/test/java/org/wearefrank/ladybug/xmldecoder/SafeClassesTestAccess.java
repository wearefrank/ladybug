package org.wearefrank.ladybug.xmldecoder;

/**
 * Gives unit tests write access to {@link SafeClasses#challengeMethodInvocations}. Lives in src/test, so the flag
 * cannot be changed by production code.
 */
public final class SafeClassesTestAccess {
	private SafeClassesTestAccess() {}

	public static void setChallengeMethodInvocations(boolean challengeMethodInvocations) {
		SafeClasses.challengeMethodInvocations = challengeMethodInvocations;
	}
}
