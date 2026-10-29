import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Finds the dependencies of ladybug-ff-test-webapp that differ from what the Frank!Framework would
 * give the same webapp outside the Ladybug project. ladybug-ff-test-webapp inherits ladybug-parent,
 * whose dependencyManagement can override the Frank!Framework BOM.
 *
 * <p>Called by check-ff-dependencies.sh, which runs Maven. Commands:
 * <ul>
 * <li>{@code baseline-pom <webapp pom> <webapp tree> <output pom>}: writes a copy of the webapp pom
 * without the parent. Direct dependencies without a version get the version the webapp resolves,
 * except Frank!Framework artifacts, which get their version from the Frank!Framework BOM.</li>
 * <li>{@code compare <webapp tree> <baseline tree> <allowed differences>}: reports differences in
 * version or scope. Exits with 1 when a difference is not in the allowed differences file.</li>
 * </ul>
 * Trees are files written by {@code mvn dependency:tree -DoutputFile=...}.
 */
public class CheckFfDependencies {
	private static final String FF_GROUP_ID = "org.frankframework";
	// Indentation followed by groupId:artifactId:type[:classifier]:version[:scope], possibly followed by " (optional)"
	private static final Pattern TREE_LINE = Pattern.compile("^([| +\\\\-]*)([^ ]+)( \\(optional\\))?$");
	private static final int TREE_INDENT = 3;

	public static void main(String[] args) throws Exception {
		if (args.length == 4 && args[0].equals("baseline-pom")) {
			writeBaselinePom(Path.of(args[1]), Path.of(args[2]), Path.of(args[3]));
		} else if (args.length == 4 && args[0].equals("compare")) {
			System.exit(compare(Path.of(args[1]), Path.of(args[2]), Path.of(args[3])));
		} else {
			System.err.println("Usage: java CheckFfDependencies.java baseline-pom <webapp pom> <webapp tree> <output pom>");
			System.err.println("       java CheckFfDependencies.java compare <webapp tree> <baseline tree> <allowed differences>");
			System.exit(2);
		}
	}

	record Dependency(String key, String version, String scope, int depth) {}

	static Map<String, Dependency> readTree(Path tree) throws IOException {
		Map<String, Dependency> result = new LinkedHashMap<>();
		List<String> lines = Files.readAllLines(tree, StandardCharsets.UTF_8);
		// The first line is the project itself
		for (String line : lines.subList(1, lines.size())) {
			if (line.isBlank()) {
				continue;
			}
			Matcher matcher = TREE_LINE.matcher(line);
			if (!matcher.matches()) {
				throw new IllegalStateException("Unexpected line in " + tree + ": " + line);
			}
			String[] parts = matcher.group(2).split(":");
			if (parts.length != 5 && parts.length != 6) {
				throw new IllegalStateException("Unexpected artifact in " + tree + ": " + line);
			}
			// groupId:artifactId:type for 5 parts, groupId:artifactId:type:classifier for 6 parts
			String key = String.join(":", List.of(parts).subList(0, parts.length - 2));
			int depth = matcher.group(1).length() / TREE_INDENT;
			result.put(key, new Dependency(key, parts[parts.length - 2], parts[parts.length - 1], depth));
		}
		return result;
	}

	static void writeBaselinePom(Path webappPom, Path webappTree, Path outputPom) throws Exception {
		Map<String, Dependency> resolved = readTree(webappTree);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		Document document = factory.newDocumentBuilder().parse(webappPom.toFile());
		Element project = document.getDocumentElement();

		Element parent = child(project, "parent");
		if (parent != null) {
			project.removeChild(parent);
		}
		// Own coordinates, so that nothing refers to properties of the removed parent
		setChildText(project, "groupId", "org.wearefrank");
		setChildText(project, "artifactId", text(project, "artifactId") + "-ff-baseline");
		setChildText(project, "version", "0");

		Element dependencies = child(project, "dependencies");
		for (Element dependency : children(dependencies, "dependency")) {
			String groupId = text(dependency, "groupId");
			if (child(dependency, "version") != null || FF_GROUP_ID.equals(groupId)) {
				continue;
			}
			String prefix = groupId + ":" + text(dependency, "artifactId") + ":";
			Dependency direct = resolved.values().stream()
					.filter(d -> d.depth() == 1 && d.key().startsWith(prefix))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("Direct dependency " + prefix + " not found in " + webappTree));
			Element version = document.createElement("version");
			version.setTextContent(direct.version());
			dependency.insertBefore(version, child(dependency, "artifactId").getNextSibling());
		}

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		Files.createDirectories(outputPom.toAbsolutePath().getParent());
		transformer.transform(new DOMSource(document), new StreamResult(outputPom.toFile()));
	}

	static int compare(Path webappTree, Path baselineTree, Path allowedFile) throws IOException {
		Map<String, Dependency> webapp = readTree(webappTree);
		Map<String, Dependency> baseline = readTree(baselineTree);
		Map<String, String> allowed = readAllowed(allowedFile);

		List<String[]> rows = new ArrayList<>();
		rows.add(new String[] {"ARTIFACT", "WEBAPP", "FRANK!FRAMEWORK", "ALLOWED"});
		TreeSet<String> usedAllowed = new TreeSet<>();
		int notAllowed = 0;
		TreeSet<String> keys = new TreeSet<>(webapp.keySet());
		keys.addAll(baseline.keySet());
		for (String key : keys) {
			String webappValue = describe(webapp.get(key));
			String baselineValue = describe(baseline.get(key));
			if (webappValue.equals(baselineValue)) {
				continue;
			}
			String allowedBy = allowed.keySet().stream().filter(a -> key.startsWith(a + ":")).findFirst().orElse(null);
			if (allowedBy == null) {
				notAllowed++;
			} else {
				usedAllowed.add(allowedBy);
			}
			rows.add(new String[] {key, webappValue, baselineValue, allowedBy == null ? "NO" : "yes"});
		}

		if (rows.size() == 1) {
			System.out.println("ladybug-ff-test-webapp has the same dependencies as the Frank!Framework gives it outside the Ladybug project");
		} else {
			System.out.println("Dependencies of ladybug-ff-test-webapp that differ from what the Frank!Framework gives it outside the Ladybug project:");
			System.out.println();
			printTable(rows);
		}
		for (String unused : allowed.keySet()) {
			if (!usedAllowed.contains(unused)) {
				System.out.println();
				System.out.println("Note: " + unused + " is in " + allowedFile.getFileName() + " but no longer differs, it can be removed");
			}
		}
		if (notAllowed > 0) {
			System.out.println();
			System.out.println(notAllowed + " difference(s) not allowed. Align the version or scope in the root pom.xml of Ladybug,");
			System.out.println("or add groupId:artifactId with a reason to " + allowedFile);
			return 1;
		}
		return 0;
	}

	/** Lines with groupId:artifactId, optionally followed by # and a reason */
	static Map<String, String> readAllowed(Path allowedFile) throws IOException {
		Map<String, String> result = new LinkedHashMap<>();
		for (String line : Files.readAllLines(allowedFile, StandardCharsets.UTF_8)) {
			int hash = line.indexOf('#');
			String entry = (hash < 0 ? line : line.substring(0, hash)).trim();
			if (!entry.isEmpty()) {
				result.put(entry, hash < 0 ? "" : line.substring(hash + 1).trim());
			}
		}
		return result;
	}

	static String describe(Dependency dependency) {
		return dependency == null ? "-" : dependency.version() + " (" + dependency.scope() + ")";
	}

	static void printTable(List<String[]> rows) {
		int[] widths = new int[rows.get(0).length];
		for (String[] row : rows) {
			for (int i = 0; i < row.length; i++) {
				widths[i] = Math.max(widths[i], row[i].length());
			}
		}
		for (String[] row : rows) {
			StringBuilder line = new StringBuilder();
			for (int i = 0; i < row.length; i++) {
				line.append(String.format("%-" + (widths[i] + 2) + "s", row[i]));
			}
			System.out.println(line.toString().stripTrailing());
		}
	}

	static Element child(Element parent, String name) {
		List<Element> children = children(parent, name);
		return children.isEmpty() ? null : children.get(0);
	}

	static List<Element> children(Element parent, String name) {
		List<Element> result = new ArrayList<>();
		if (parent != null) {
			for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
				if (node instanceof Element element && element.getTagName().equals(name)) {
					result.add(element);
				}
			}
		}
		return result;
	}

	static String text(Element parent, String name) {
		Element child = child(parent, name);
		return child == null ? null : child.getTextContent().trim();
	}

	static void setChildText(Element parent, String name, String value) {
		Element child = child(parent, name);
		if (child == null) {
			child = parent.getOwnerDocument().createElement(name);
			parent.insertBefore(child, child(parent, "artifactId"));
		}
		child.setTextContent(value);
	}
}
