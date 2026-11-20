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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import feign.Param;
import feign.codec.EncodeException;
import feign.querymap.BeanQueryMapEncoder;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.util.ReflectionUtils;

/**
 * Provides support for encoding Pageable annotated as
 * {@link org.springframework.cloud.openfeign.SpringQueryMap}, and also serves as the
 * default {@code QueryMapEncoder} used to resolve {@code @RequestHeader HttpHeaders}
 * header-map parameters (see {@code RequestHeaderParameterProcessor}). Since Spring
 * Framework 7, {@link HttpHeaders} no longer implements {@code MultiValueMap}, so without
 * this special case it falls through to the generic bean-property reflection in
 * {@link BeanQueryMapEncoder}, which drops the real header values and injects unrelated
 * getter-derived entries instead.
 *
 * @author Hyeonmin Park
 * @author Yanming Zhou
 * @author Gokalp Kuscu
 * @author Rajesh Vasa
 * @since 2.2.8
 */
public class PageableSpringQueryMapEncoder extends BeanQueryMapEncoder {

	/**
	 * Page index parameter name.
	 */
	private String pageParameter = "page";

	/**
	 * Page size parameter name.
	 */
	private String sizeParameter = "size";

	/**
	 * Sort parameter name.
	 */
	private String sortParameter = "sort";

	/**
	 * Sort ignoreCase parameter name.
	 */
	private final String ignoreCase = "ignorecase";

	public void setPageParameter(String pageParameter) {
		this.pageParameter = pageParameter;
	}

	public void setSizeParameter(String sizeParameter) {
		this.sizeParameter = sizeParameter;
	}

	public void setSortParameter(String sortParameter) {
		this.sortParameter = sortParameter;
	}

	@Override
	public Map<String, Object> encode(Object object) {
		if (supports(object)) {
			Map<String, Object> queryMap = new HashMap<>();

			if (object instanceof Pageable pageable) {

				if (pageable.isPaged()) {
					queryMap.put(pageParameter, pageable.getPageNumber());
					queryMap.put(sizeParameter, pageable.getPageSize());
				}

				if (pageable.getSort() != null) {
					applySort(queryMap, pageable.getSort());
				}
			}
			else if (object instanceof Sort sort) {
				applySort(queryMap, sort);
			}
			else if (object instanceof HttpHeaders httpHeaders) {
				httpHeaders.forEach(queryMap::put);
			}
			return queryMap;
		}
		else {
			Map<String, Object> queryMap = super.encode(object);
			return (object != null) ? applyFieldParamAliases(object.getClass(), queryMap) : queryMap;
		}
	}

	/**
	 * {@link BeanQueryMapEncoder} only honors {@link Param} when it is declared on a
	 * getter. Since {@link SpringFieldQueryMapEncoder}, the default when Spring Data is
	 * not on the classpath, honors {@link Param} declared on a field, also honor it here
	 * so that the query parameter names do not depend on whether Spring Data is present.
	 * A {@link Param} declared on the getter takes precedence, and a {@link Param} with
	 * an empty value keeps the property name.
	 * <p>
	 * The result is built as a new map from the encoded entries, so it does not depend on
	 * the property iteration order. If two entries end up with the same name, for example
	 * an alias that is also the name of another property, an {@link EncodeException} is
	 * thrown instead of one value silently replacing the other.
	 */
	private static Map<String, Object> applyFieldParamAliases(Class<?> type, Map<String, Object> encoded) {
		Map<String, String> aliases = fieldParamAliases(type);
		Map<String, Object> queryMap = new HashMap<>();
		for (Map.Entry<String, Object> entry : encoded.entrySet()) {
			String name = aliases.getOrDefault(entry.getKey(), entry.getKey());
			if (queryMap.containsKey(name)) {
				throw new EncodeException(
						"Duplicate query parameter name '" + name + "' when encoding " + type.getName());
			}
			queryMap.put(name, entry.getValue());
		}
		return queryMap;
	}

	/**
	 * Returns the {@link Param} names declared on the fields of the given type, keyed by
	 * property name, for the properties whose getter has no {@link Param}.
	 */
	private static Map<String, String> fieldParamAliases(Class<?> type) {
		Map<String, String> aliases = new HashMap<>();
		try {
			for (PropertyDescriptor descriptor : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
				Method readMethod = descriptor.getReadMethod();
				if (readMethod == null || readMethod.isAnnotationPresent(Param.class)) {
					continue;
				}
				String propertyName = descriptor.getName();
				Field field = ReflectionUtils.findField(type, propertyName);
				Param alias = (field != null) ? field.getAnnotation(Param.class) : null;
				if (alias != null && !alias.value().isEmpty()) {
					aliases.put(propertyName, alias.value());
				}
			}
		}
		catch (IntrospectionException ex) {
			throw new EncodeException("Failure encoding object into query map", ex);
		}
		return aliases;
	}

	private void applySort(Map<String, Object> queryMap, Sort sort) {
		List<String> sortQueries = new ArrayList<>();
		for (Sort.Order order : sort) {
			String sortQuery = order.getProperty() + "%2C" + order.getDirection();
			if (order.isIgnoreCase()) {
				sortQuery += "%2C" + ignoreCase;
			}
			sortQueries.add(sortQuery);
		}
		if (!sortQueries.isEmpty()) {
			queryMap.put(sortParameter, sortQueries);
		}
	}

	protected boolean supports(Object object) {
		return object instanceof Pageable || object instanceof Sort || object instanceof HttpHeaders;
	}

}
