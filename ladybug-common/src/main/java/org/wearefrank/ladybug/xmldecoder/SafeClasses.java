/*
   Copyright 2026 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.wearefrank.ladybug.xmldecoder;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;

import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.util.SpecialEncodings;

/**
 * Decides which classes may be used and which methods may be called while decoding a Ladybug report xml.
 * <p>
 * Checking only the classes named in the xml is not enough. The xml can call any public method on the
 * objects it creates, for example getClass(), and through the resulting java.lang.Class object it can
 * reach any other class. Therefore every method call is checked as well. There are three categories of
 * safe classes:
 * <ol>
 * <li>Classes meant to be immutable, like java.math.BigDecimal. They can only be instantiated, using
 * the constructor or static method configured for them. No methods can be called on the instances, except
 * java.sql.Timestamp.setNanos(), because XMLEncoder writes a Timestamp as new Timestamp(long) followed by
 * setNanos(int).</li>
 * <li>Collections: the lists, sets, queues and maps of java.util and java.util.concurrent that XMLEncoder
 * can write. They can be instantiated and their contents can be manipulated, using the methods of
 * {@link Collection} and {@link Map} that only add, remove or replace elements, such as add(), addAll(),
 * remove(), removeAll(), retainAll(), clear(), put(), putAll() and replace(). Methods that would hand back
 * a new object of a class that is not on this allow list, like entrySet() or iterator(), or that take a
 * functional interface argument, like removeIf() or merge(), are deliberately not allowed; see
 * {@link #isAllowedInstanceInvocation}. The static factory methods of {@link Collections} that XMLEncoder uses
 * for wrapped collections, like unmodifiableMap(), are allowed too.</li>
 * <li>The Ladybug classes {@link Report} and {@link Checkpoint}. They can be instantiated and filled,
 * using the setters of their persistent bean properties, the properties that XMLEncoder writes.
 * The getters of their collection properties are allowed as well, because XMLEncoder may fill a
 * collection property by calling add() or put() on the result of the getter.</li>
 * </ol>
 * All other method calls and all field access are blocked.
 * <p>
 * As a further, independent safety net, getClass() is explicitly rejected (see
 * {@link #isAllowedInstanceInvocation}) and no java.lang.Class object for a class outside {@link #ALL} can ever
 * become a value handled by the decoder, no matter how it was produced (see {@link #checkClassLookup}). This
 * defends against reaching arbitrary classes through a Class object, the classic way to defeat a class allow
 * list like this one, even if some other, not yet known, way of obtaining one were ever introduced.
 */
public final class SafeClasses {
	private SafeClasses() {}

	private static final String CONSTRUCTOR = "new";
	// XMLEncoder itself never writes this, but a persistence delegate could plausibly use
	// Class.newInstance()/Constructor.newInstance() instead of Expression's usual "new" marker to describe
	// a plain construction. Treated as equivalent to CONSTRUCTOR everywhere a constructor call is allowed.
	private static final String CONSTRUCTOR_REFLECTIVE = "newInstance";

	// Category 1: maps each class name to the constructor ("new") or static method that creates an instance
	private static final Map<String, String> IMMUTABLE_CLASSES = createImmutableClasses();
	private static final String TIMESTAMP_CLASS = "java.sql.Timestamp";

	// Category 2. Only well-known classes of which the constructors and add() or put() have no side effects.
	// Allowing any implementation of Map or Collection would not be safe: some libraries have implementations
	// that execute code while they are filled, like LazyMap and TransformedMap of Apache Commons Collections.
	// Left out on purpose: CopyOnWriteArrayList, because XMLEncoder writes it without its elements, and
	// ArrayBlockingQueue, because XMLEncoder cannot write it (it has no no-arg constructor).
	private static final Set<String> COLLECTION_CLASSES = Set.of(
			ArrayList.class.getName(),
			LinkedList.class.getName(),
			ArrayDeque.class.getName(),
			Vector.class.getName(),
			Stack.class.getName(),
			HashSet.class.getName(),
			LinkedHashSet.class.getName(),
			TreeSet.class.getName(),
			PriorityQueue.class.getName(),
			CopyOnWriteArraySet.class.getName(),
			ConcurrentLinkedQueue.class.getName(),
			ConcurrentLinkedDeque.class.getName(),
			ConcurrentSkipListSet.class.getName(),
			LinkedBlockingQueue.class.getName(),
			LinkedBlockingDeque.class.getName(),
			PriorityBlockingQueue.class.getName(),
			LinkedTransferQueue.class.getName(),
			HashMap.class.getName(),
			LinkedHashMap.class.getName(),
			TreeMap.class.getName(),
			Hashtable.class.getName(),
			Properties.class.getName(),
			ConcurrentHashMap.class.getName(),
			ConcurrentSkipListMap.class.getName());
	private static final Set<String> COLLECTIONS_FACTORY_METHODS = Set.of(
			"emptyList", "emptySet", "emptyMap",
			"singletonList", "singleton", "singletonMap",
			"unmodifiableList", "unmodifiableSet", "unmodifiableMap", "unmodifiableCollection",
			"synchronizedList", "synchronizedSet", "synchronizedMap", "synchronizedCollection");

	// Category 3
	private static final Map<Class<?>, LadybugClass> LADYBUG_CLASSES = Map.of(
			Report.class, new LadybugClass(Report.class),
			Checkpoint.class, new LadybugClass(Checkpoint.class));

	/**
	 * The names of all classes that may be used in a Ladybug report xml.
	 */
	public static final List<String> ALL = createAll();

	/**
	 * Only for unit tests that check the safety of method invocations. When true, every class may be looked up
	 * and instantiated, not only the classes in {@link #ALL}, so a test can prove that dangerous methods of an
	 * arbitrary class are blocked by the method checks alone. All other method invocations are still checked as
	 * usual. Package-private and without a setter on purpose: it can only be written by test code in this
	 * package (see SafeClassesTestAccess in src/test), so it is always false in production.
	 */
	static boolean challengeMethodInvocations = false;

	private static Map<String, String> createImmutableClasses() {
		Map<String, String> result = new LinkedHashMap<>();
		// XMLEncoder writes new Date(long)
		result.put("java.util.Date", CONSTRUCTOR);
		result.put(TIMESTAMP_CLASS, CONSTRUCTOR);
		// java.sql.Date and java.sql.Time are not supported on purpose, see the comment at MessageEncoderImpl.TIMESTAMP_ENCODER
		result.put("java.time.ZoneOffset", SpecialEncodings.ZONE_OFFSET_FACTORY);
		for (String className: SpecialEncodings.SPECIALLY_ENCODED_CLASSES) {
			result.put(className, SpecialEncodings.getFactoryName(className));
		}
		return result;
	}

	private static List<String> createAll() {
		List<String> result = new ArrayList<>(IMMUTABLE_CLASSES.keySet());
		result.addAll(COLLECTION_CLASSES);
		result.add(Collections.class.getName());
		LADYBUG_CLASSES.keySet().forEach(clazz -> result.add(clazz.getName()));
		return List.copyOf(result);
	}

	/**
	 * Throws an exception unless the decoder is allowed to call the given method.
	 *
	 * @param target      the object to call the method on, or the class for a static method or constructor
	 * @param methodName  the name of the method, "new" or "newInstance" for a constructor
	 * @param args        the arguments of the call
	 */
	public static void checkInvocation(Object target, String methodName, Object[] args) {
		boolean allowed;
		if (target instanceof Class<?> clazz) {
			allowed = isAllowedStaticInvocation(clazz, methodName, args);
		} else {
			allowed = isAllowedInstanceInvocation(target, methodName, args);
		}
		if (!allowed) {
			throw new IllegalArgumentException(String.format(
					"Unsupported method call while parsing Ladybug report xml: [%s.%s] with %d argument(s)",
					target == null ? null : (target instanceof Class<?> clazz ? clazz.getName() : target.getClass().getName()),
					methodName, args.length));
		}
	}

	/**
	 * Returns the exception to throw on field access, because reading or writing fields is never needed to
	 * decode a Ladybug report xml. The caller throws it, so the compiler can check that nothing happens after it.
	 */
	public static IllegalArgumentException fieldAccessRejected(String fieldName) {
		return new IllegalArgumentException(String.format(
				"Unsupported field access while parsing Ladybug report xml: [%s]", fieldName));
	}

	/**
	 * Throws an exception if the given class is not in {@link #ALL}, the classes that may be used in a Ladybug
	 * report xml. Called from {@code ValueObjectImpl.create(Object)}, the single place every value the decoder
	 * hands back passes through, whether it ends up bound to a variable, used as a method argument or returned
	 * as the final decoded object. A {@link Class} object for a class outside {@link #ALL} must never reach any
	 * of those places: once obtained, it becomes the target of further method calls (see
	 * {@link #isAllowedStaticInvocation}), and from there, classes not on this allow list could otherwise be
	 * reached, for example through {@code Class.forName(String)}. Blocking it centrally here, rather than only
	 * at each known call site that could produce one, such as {@code getClass()} (see
	 * {@link #isAllowedInstanceInvocation}) or the {@code class} attribute (see {@code DocumentHandler.findClass}),
	 * defends against any current or future way of obtaining a Class instance.
	 *
	 * @param clazz the class to check, never {@code null}
	 */
	public static void checkClassLookup(Class<?> clazz) {
		if (!isAllowedClassName(clazz.getName())) {
			throw new IllegalArgumentException(String.format(
					"Unsupported class while parsing Ladybug report xml: [%s]", clazz.getName()));
		}
	}

	/**
	 * Returns whether the class with the given name may be used in a Ladybug report xml, see {@link #ALL}.
	 */
	static boolean isAllowedClassName(String className) {
		return challengeMethodInvocations || ALL.contains(className);
	}

	private static boolean isAllowedStaticInvocation(Class<?> clazz, String methodName, Object[] args) {
		if (challengeMethodInvocations && isConstructorMethodName(methodName)) {
			return true;
		}
		String className = clazz.getName();
		if (IMMUTABLE_CLASSES.containsKey(className)) {
			String factoryMethod = IMMUTABLE_CLASSES.get(className);
			boolean matches = CONSTRUCTOR.equals(factoryMethod)
					? isConstructorMethodName(methodName)
					: factoryMethod.equals(methodName);
			return matches && args.length == 1;
		}
		if (COLLECTION_CLASSES.contains(className) || LADYBUG_CLASSES.containsKey(clazz)) {
			return isConstructorMethodName(methodName);
		}
		if (clazz == Collections.class) {
			return COLLECTIONS_FACTORY_METHODS.contains(methodName);
		}
		return false;
	}

	// "new" is the marker Expression uses for an ordinary constructor call; "newInstance" is accepted as an
	// equivalent, in case a persistence delegate ever describes the same plain construction that way.
	private static boolean isConstructorMethodName(String methodName) {
		return CONSTRUCTOR.equals(methodName) || CONSTRUCTOR_REFLECTIVE.equals(methodName);
	}

	private static boolean isAllowedInstanceInvocation(Object target, String methodName, Object[] args) {
		if (target == null) {
			return false;
		}
		// Explicitly block getClass(), instead of relying on it not being listed as allowed below: getClass()
		// is the classic way to reach a java.lang.Class object, and from there any other class, without ever
		// going through DocumentHandler.findClass(). See also ValueObjectImpl.create(), which blocks any
		// Class object from becoming a value of the decoder, in case some other method call would ever return one.
		if ("getClass".equals(methodName)) {
			return false;
		}
		LadybugClass ladybugClass = LADYBUG_CLASSES.get(target.getClass());
		if (ladybugClass != null) {
			return ladybugClass.isAllowed(methodName, args);
		}
		if (target instanceof Collection) {
			return isAllowedCollectionInvocation(methodName, args);
		}
		if (target instanceof Map) {
			return isAllowedMapInvocation(methodName, args);
		}
		if (TIMESTAMP_CLASS.equals(target.getClass().getName())) {
			return "setNanos".equals(methodName) && args.length == 1 && args[0] instanceof Integer;
		}
		// Includes the instances of the other immutable classes
		return false;
	}

	// Only methods that add, remove or replace elements. All of them only take elements, or a Collection that
	// is itself already on this allow list, as arguments, and return void, a boolean or an element that was
	// already in the collection. Methods that hand back a new object, like entrySet() or iterator(), whose
	// concrete class is not on this allow list, or that take a functional interface argument, like removeIf(),
	// are deliberately not allowed.
	private static boolean isAllowedCollectionInvocation(String methodName, Object[] args) {
		switch (methodName) {
			case "add", "remove":
				return args.length == 1;
			case "addAll", "removeAll", "retainAll":
				return args.length == 1 && args[0] instanceof Collection;
			case "clear":
				return args.length == 0;
			default:
				return false;
		}
	}

	// Same idea as isAllowedCollectionInvocation(), for the methods of Map.
	private static boolean isAllowedMapInvocation(String methodName, Object[] args) {
		switch (methodName) {
			case "put", "putIfAbsent":
				return args.length == 2;
			case "putAll":
				return args.length == 1 && args[0] instanceof Map;
			case "remove":
				return args.length == 1 || args.length == 2;
			case "replace":
				return args.length == 2 || args.length == 3;
			case "clear":
				return args.length == 0;
			default:
				return false;
		}
	}

	private static class LadybugClass {
		private final Map<String, Class<?>> setterParameterTypes = new HashMap<>();
		private final Set<String> collectionGetters = new HashSet<>();

		LadybugClass(Class<?> clazz) {
			try {
				for (PropertyDescriptor property: Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
					Method getter = property.getReadMethod();
					Method setter = property.getWriteMethod();
					// XMLEncoder only writes properties that have a getter and a setter and are not transient
					if (getter == null || setter == null || Boolean.TRUE.equals(property.getValue("transient"))) {
						continue;
					}
					setterParameterTypes.put(setter.getName(), setter.getParameterTypes()[0]);
					Class<?> type = property.getPropertyType();
					if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
						collectionGetters.add(getter.getName());
					}
				}
			} catch (IntrospectionException e) {
				throw new IllegalStateException("Could not introspect " + clazz.getName(), e);
			}
		}

		boolean isAllowed(String methodName, Object[] args) {
			if (args.length == 0) {
				return collectionGetters.contains(methodName);
			}
			if (args.length == 1 && setterParameterTypes.containsKey(methodName)) {
				// Prevents that an overloaded method with the same name is chosen, like Checkpoint.setMessage(Object)
				return isAssignable(setterParameterTypes.get(methodName), args[0]);
			}
			return false;
		}
	}

	private static boolean isAssignable(Class<?> type, Object value) {
		if (value == null) {
			return !type.isPrimitive();
		}
		return box(type).isInstance(value);
	}

	private static Class<?> box(Class<?> type) {
		if (!type.isPrimitive()) {
			return type;
		}
		if (type == int.class) return Integer.class;
		if (type == long.class) return Long.class;
		if (type == boolean.class) return Boolean.class;
		if (type == double.class) return Double.class;
		if (type == float.class) return Float.class;
		if (type == short.class) return Short.class;
		if (type == byte.class) return Byte.class;
		return Character.class;
	}
}
