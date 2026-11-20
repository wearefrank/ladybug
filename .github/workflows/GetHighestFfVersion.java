import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Prints the highest version of frankframework-webapp that is available in
 * Maven Central or in the Frank!Framework Nexus releases repository.
 *
 * <p>Plain releases (k.l.m, e.g. 10.3.0) are published to Maven Central.
 * Nightly builds (k.l.m-yyyyMMdd.HHmmss, e.g. 10.3.0-20261010.120000) are
 * published to the Nexus releases repository. Both are read, because the
 * highest version can be in either of them.
 *
 * <p>Versions are ordered by k, l and m numerically. For equal k.l.m, the
 * plain release is higher than every nightly build, because those builds are
 * made before the release. Nightly builds with equal k.l.m are ordered by
 * their timestamp. Note that this differs from Maven's own ordering, which
 * puts 10.3.0-&lt;timestamp&gt; above 10.3.0.
 *
 * <p>Any version with another format makes this program fail, so that a
 * change in the versioning scheme of the Frank!Framework is noticed and the
 * ordering above can be adapted. Old versions with another format that are
 * known not to matter are listed in {@link #KNOWN_EXCEPTIONS}; they are
 * ignored.
 *
 * <p>The &lt;release&gt; element of maven-metadata.xml is not used, because
 * it holds the most recently deployed version, not the highest one. When a
 * nightly build of an older branch (e.g. 9.4.x) is deployed after the newest
 * 10.x build, &lt;release&gt; points to that older version. Instead, all
 * &lt;version&gt; elements are compared.
 *
 * <p>Usage, from the root of the ladybug repository (Java 21 or higher):
 * <pre>
 * java .github/workflows/GetHighestFfVersion.java [&lt;metadata-url&gt; ...]
 * </pre>
 * Without arguments, the metadata of Maven Central and of the Nexus releases
 * repository is read. A metadata URL can also be a file: URL, which is
 * useful for testing.
 *
 * <p>The highest version is printed to stdout. Errors are printed to stderr
 * and make the program exit with status 1.
 */
public class GetHighestFfVersion {
	private static final List<String> DEFAULT_METADATA_URLS = List.of(
			"https://repo.maven.apache.org/maven2/org/frankframework/frankframework-webapp/maven-metadata.xml",
			"https://nexus.frankframework.org/content/repositories/releases/org/frankframework/frankframework-webapp/maven-metadata.xml");

	/** Exact versions with an unexpected format that are ignored. */
	private static final Set<String> KNOWN_EXCEPTIONS = Set.of(
			"8.1.0-RC1");

	public static void main(String[] args) throws Exception {
		List<String> metadataUrls = args.length > 0 ? List.of(args) : DEFAULT_METADATA_URLS;
		List<Version> versions = new ArrayList<>();
		List<String> unknown = new ArrayList<>();
		for (String metadataUrl : metadataUrls) {
			for (String text : readVersions(metadataUrl)) {
				Version version = Version.parse(text);
				if (version != null) {
					versions.add(version);
				} else if (!KNOWN_EXCEPTIONS.contains(text)) {
					unknown.add(text);
				}
			}
		}
		if (!unknown.isEmpty()) {
			System.err.println("Frank!Framework versions with unknown format found in metadata at: " + metadataUrls);
			unknown.forEach(System.err::println);
			System.err.println("Adapt GetHighestFfVersion.java to support this format, or add the versions to KNOWN_EXCEPTIONS.");
			System.exit(1);
		}
		if (versions.isEmpty()) {
			System.err.println("Could not determine highest version of Frank!Framework from metadata at: " + metadataUrls);
			System.exit(1);
		}
		Version highest = versions.stream().max(Comparator.naturalOrder()).orElseThrow();
		System.out.println(highest.text());
	}

	/** Returns the text of all &lt;version&gt; elements in a maven-metadata.xml. */
	private static List<String> readVersions(String metadataUrl) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// The metadata comes from the internet, so do not process DTDs (prevents XXE attacks).
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document;
		try (InputStream in = URI.create(metadataUrl).toURL().openStream()) {
			document = builder.parse(in);
		}
		NodeList nodes = document.getElementsByTagName("version");
		List<String> result = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++) {
			result.add(nodes.item(i).getTextContent().trim());
		}
		return result;
	}

	/**
	 * A version k.l.m (a release, timestamp is null) or k.l.m-yyyyMMdd.HHmmss
	 * (a nightly build).
	 */
	record Version(int k, int l, int m, String timestamp, String text) implements Comparable<Version> {
		/** Returns null when the text does not have one of the expected formats. */
		static Version parse(String text) {
			String[] parts = text.split("-");
			if (parts.length > 2) {
				return null;
			}
			String[] numbers = parts[0].split("\\.");
			if (numbers.length != 3 || !allDigits(numbers[0]) || !allDigits(numbers[1]) || !allDigits(numbers[2])) {
				return null;
			}
			String timestamp = null;
			if (parts.length == 2) {
				timestamp = parts[1];
				String[] dateAndTime = timestamp.split("\\.");
				if (dateAndTime.length != 2
						|| dateAndTime[0].length() != 8 || !allDigits(dateAndTime[0])
						|| dateAndTime[1].length() != 6 || !allDigits(dateAndTime[1])) {
					return null;
				}
			}
			return new Version(Integer.parseInt(numbers[0]), Integer.parseInt(numbers[1]),
					Integer.parseInt(numbers[2]), timestamp, text);
		}

		private static boolean allDigits(String s) {
			return s.matches("[0-9]+");
		}

		boolean isRelease() {
			return timestamp == null;
		}

		@Override
		public int compareTo(Version other) {
			return Comparator.comparingInt(Version::k)
					.thenComparingInt(Version::l)
					.thenComparingInt(Version::m)
					// A release is higher than the nightly builds with the same k.l.m.
					.thenComparing(Version::isRelease)
					// Timestamps have a fixed length, so comparing them as text is comparing them in time.
					.thenComparing(Version::timestamp, Comparator.nullsLast(Comparator.naturalOrder()))
					.compare(this, other);
		}
	}
}
