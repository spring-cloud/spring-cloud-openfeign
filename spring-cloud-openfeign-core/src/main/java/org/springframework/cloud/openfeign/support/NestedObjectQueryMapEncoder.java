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
import java.util.Set;

import feign.QueryMapEncoder;
import feign.codec.EncodeException;

import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

/**
 * A {@link QueryMapEncoder} that sends the nested objects of a query map parameter as
 * nested query parameters, with the property path notation that Spring MVC binds.
 * <p>
 * The query map is produced by the delegate encoder. Then, each value that is not a
 * simple value (see {@link BeanUtils#isSimpleValueType(Class)}) is encoded with the
 * delegate too, and its entries are added with the name of the value as a prefix:
 * <ul>
 * <li>a nested object: {@code range.from=1&range.to=2}</li>
 * <li>the objects of a collection or an array: {@code filters[0].name=status}</li>
 * <li>the entries of a map: {@code attributes[color]=red}</li>
 * </ul>
 * A collection of simple values is kept as is, so it is still sent as repeated query
 * parameters, and an array of simple values is sent the same way.
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
		if (isSimpleValue(value)) {
			queryMap.put(name, value);
		}
		else if (value instanceof Map<?, ?> map) {
			map.forEach((key, entryValue) -> add(queryMap, name + "[" + key + "]", entryValue, path));
		}
		else if (value instanceof Iterable<?> || value.getClass().isArray()) {
			addElements(queryMap, name, value, path);
		}
		else {
			addNestedObject(queryMap, name, value, path);
		}
	}

	private void addElements(Map<String, Object> queryMap, String name, Object value, Set<Object> path) {
		List<Object> elements = toList(value);
		if (elements.stream().allMatch(element -> element == null || isSimpleValue(element))) {
			// Feign sends an Iterable as repeated query parameters
			queryMap.put(name, value instanceof Iterable<?> ? value : elements);
			return;
		}
		for (int index = 0; index < elements.size(); index++) {
			add(queryMap, name + "[" + index + "]", elements.get(index), path);
		}
	}

	private void addNestedObject(Map<String, Object> queryMap, String name, Object value, Set<Object> path) {
		if (!path.add(value)) {
			throw new EncodeException("Cannot encode the query map: an object of type " + value.getClass().getName()
					+ " is nested in itself under '" + name + "'");
		}
		addAll(queryMap, name + ".", delegate.encode(value), path);
		path.remove(value);
	}

	private static boolean isSimpleValue(Object value) {
		return BeanUtils.isSimpleValueType(value.getClass());
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
