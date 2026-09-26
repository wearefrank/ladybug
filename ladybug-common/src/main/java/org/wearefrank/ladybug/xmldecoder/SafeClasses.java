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
 * can write. They can be instantiated and only filled, using add() for a {@link Collection}
 * and put() for a {@link Map}. The static factory methods of {@link Collections} that XMLEncoder uses
 * for wrapped collections, like unmodifiableMap(), are allowed too.</li>
 * <li>The Ladybug classes {@link Report} and {@link Checkpoint}. They can be instantiated and filled,
 * using the setters of their persistent bean properties, the properties that XMLEncoder writes.
 * The getters of their collection properties are allowed as well, because XMLEncoder may fill a
 * collection property by calling add() or put() on the result of the getter.</li>
 * </ol>
 * All other method calls and all field access are blocked.
 */
public final class SafeClasses {
	private SafeClasses() {}

	private static final String CONSTRUCTOR = "new";

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
	 * @param methodName  the name of the method, "new" for a constructor
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

	private static boolean isAllowedStaticInvocation(Class<?> clazz, String methodName, Object[] args) {
		String className = clazz.getName();
		if (IMMUTABLE_CLASSES.containsKey(className)) {
			return IMMUTABLE_CLASSES.get(className).equals(methodName) && args.length == 1;
		}
		if (COLLECTION_CLASSES.contains(className) || LADYBUG_CLASSES.containsKey(clazz)) {
			return CONSTRUCTOR.equals(methodName);
		}
		if (clazz == Collections.class) {
			return COLLECTIONS_FACTORY_METHODS.contains(methodName);
		}
		return false;
	}

	private static boolean isAllowedInstanceInvocation(Object target, String methodName, Object[] args) {
		if (target == null) {
			return false;
		}
		LadybugClass ladybugClass = LADYBUG_CLASSES.get(target.getClass());
		if (ladybugClass != null) {
			return ladybugClass.isAllowed(methodName, args);
		}
		if (target instanceof Collection) {
			return "add".equals(methodName) && args.length == 1;
		}
		if (target instanceof Map) {
			return "put".equals(methodName) && args.length == 2;
		}
		if (TIMESTAMP_CLASS.equals(target.getClass().getName())) {
			return "setNanos".equals(methodName) && args.length == 1 && args[0] instanceof Integer;
		}
		// Includes the instances of the other immutable classes
		return false;
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
