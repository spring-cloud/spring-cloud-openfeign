/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.openfeign.support;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import feign.QueryMapEncoder;
import feign.codec.EncodeException;

import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

/**
 * A {@link QueryMapEncoder} that sends the nested objects of a query map parameter as
 * nested query parameters, with the property path notation that Spring MVC binds.
 * <p>
 * The query map is produced by the delegate encoder. Then, each nested object, that is a
 * value that is neither a simple value (see {@link BeanUtils#isSimpleValueType(Class)})
 * nor of a {@code java.*} or {@code javax.*} type, is encoded with the delegate too, and
 * its entries are added with the name of the value as a prefix:
 * <ul>
 * <li>a nested object: {@code range.from=1&range.to=2}</li>
 * <li>the objects of a collection or an array: {@code filters[0].name=status}</li>
 * <li>the entries of a map: {@code attributes[color]=red}</li>
 * </ul>
 * Simple values and the values of other JDK types, such as an {@code Optional} or a
 * {@code Duration}, are kept as is. A collection or an array is encoded with the index of
 * its elements only when none of them is such a value, so a collection that has some,
 * such as a {@code List<String>}, is still sent as repeated query parameters. A map key
 * with a square bracket, and a value that is nested in itself, directly or through other
 * values, are rejected with an {@link EncodeException}.
 *
 * @author Sharang Gupta
 * @since 5.1.0
 * @see org.springframework.cloud.openfeign.FeignClientProperties#isNestedQueryMapEnabled()
 */
public class NestedObjectQueryMapEncoder implements QueryMapEncoder {

	private final QueryMapEncoder delegate;

	public NestedObjectQueryMapEncoder(QueryMapEncoder delegate) {
		Assert.notNull(delegate, "delegate must not be null");
		this.delegate = delegate;
	}

	@Override
	public Map<String, Object> encode(Object object) throws EncodeException {
		if (object == null) {
			return Collections.emptyMap();
		}
		Map<String, Object> queryMap = new LinkedHashMap<>();
		Set<Object> path = Collections.newSetFromMap(new IdentityHashMap<>());
		path.add(object);
		addAll(queryMap, "", delegate.encode(object), path);
		return queryMap;
	}

	private void addAll(Map<String, Object> queryMap, String prefix, Map<String, Object> values, Set<Object> path) {
		values.forEach((name, value) -> add(queryMap, prefix + name, value, path));
	}

	private void add(Map<String, Object> queryMap, String name, Object value, Set<Object> path) {
		if (value == null) {
			return;
		}
		if (isPlainValue(value)) {
			queryMap.put(name, value);
			return;
		}
		// Track the maps, collections, arrays and objects on the current path, so that
		// a value nested in itself is rejected instead of recursing forever
		if (!path.add(value)) {
			throw new EncodeException("Cannot encode the query map: an object of type " + value.getClass().getName()
					+ " is nested in itself under '" + name + "'");
		}
		addNested(queryMap, name, value, path);
		path.remove(value);
	}

	private void addNested(Map<String, Object> queryMap, String name, Object value, Set<Object> path) {
		if (value instanceof Map<?, ?> map) {
			map.forEach((key, entryValue) -> add(queryMap, name + "[" + mapKey(name, key) + "]", entryValue, path));
		}
		else if (value instanceof Iterable<?> || value.getClass().isArray()) {
			addElements(queryMap, name, value, path);
		}
		else {
			addAll(queryMap, name + ".", delegate.encode(value), path);
		}
	}

	private void addElements(Map<String, Object> queryMap, String name, Object value, Set<Object> path) {
		List<Object> elements = toList(value);
		if (!areAllNested(elements)) {
			// A plain value has no property path to index, so keep the elements as Feign
			// sends them: an Iterable as repeated query parameters
			queryMap.put(name, value instanceof Iterable<?> ? value : elements);
			return;
		}
		for (int index = 0; index < elements.size(); index++) {
			add(queryMap, name + "[" + index + "]", elements.get(index), path);
		}
	}

	private static String mapKey(String name, Object key) {
		String mapKey = String.valueOf(key);
		if (mapKey.indexOf('[') >= 0 || mapKey.indexOf(']') >= 0) {
			throw new EncodeException("Cannot encode the query map: the key '" + mapKey + "' of the map under '" + name
					+ "' has a square bracket, which the property path of its entry cannot hold");
		}
		return mapKey;
	}

	private static boolean areAllNested(List<Object> elements) {
		List<Object> presentElements = elements.stream().filter(Objects::nonNull).toList();
		return !presentElements.isEmpty()
				&& presentElements.stream().noneMatch(NestedObjectQueryMapEncoder::isPlainValue);
	}

	private static boolean isPlainValue(Object value) {
		if (value instanceof Map<?, ?> || value instanceof Iterable<?> || value.getClass().isArray()) {
			return false;
		}
		String typeName = value.getClass().getName();
		return BeanUtils.isSimpleValueType(value.getClass()) || typeName.startsWith("java.")
				|| typeName.startsWith("javax.");
	}

	private static List<Object> toList(Object value) {
		List<Object> elements = new ArrayList<>();
		if (value instanceof Iterable<?> iterable) {
			iterable.forEach(elements::add);
		}
		else {
			int length = Array.getLength(value);
			for (int index = 0; index < length; index++) {
				elements.add(Array.get(value, index));
			}
		}
		return elements;
	}

}
