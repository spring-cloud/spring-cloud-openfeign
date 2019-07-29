/*
 * Copyright 2013-2018 the original author or authors.
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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Provides support for encoding spring Pageable via composition.
 *
 * @author Pascal Büttiker
 */
public class PageableSpringEncoder implements Encoder {

	private final Encoder delegate;

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
	 * Creates a new PageableSpringEncoder with the given delegate for fallback. If no
	 * delegate is provided and this encoder cant handle the request, an EncodeException
	 * is thrown.
	 * @param delegate The optional delegate.
	 */
	public PageableSpringEncoder(Encoder delegate) {
		this.delegate = delegate;
	}

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
	public void encode(Object object, Type bodyType, RequestTemplate template)
			throws EncodeException {

		if (supports(object)) {
			if (object instanceof Pageable) {
				Pageable pageable = (Pageable) object;

				if (pageable.isPaged()) {
					template.query(pageParameter, pageable.getPageNumber() + "");
					template.query(sizeParameter, pageable.getPageSize() + "");
				}

				if (pageable.getSort() != null) {
					applySort(template, pageable.getSort());
				}
			}
			else if (object instanceof Sort) {
				Sort sort = (Sort) object;
				applySort(template, sort);
			}
		}
		else {
			if (delegate != null) {
				delegate.encode(object, bodyType, template);
			}
			else {
				throw new EncodeException(
						"PageableSpringEncoder does not support the given object "
								+ object.getClass()
								+ " and no delegate was provided for fallback!");
			}
		}
	}

	private void applySort(RequestTemplate template, Sort sort) {
		Collection<String> existingSorts = template.queries().get("sort");
		List<String> sortQueries = existingSorts != null ? new ArrayList<>(existingSorts)
				: new ArrayList<>();
		if (!sortParameter.equals("sort")) {
			existingSorts = template.queries().get(sortParameter);
			if (existingSorts != null) {
				sortQueries.addAll(existingSorts);
			}
		}
		for (Sort.Order order : sort) {
			sortQueries.add(order.getProperty() + "," + order.getDirection());
		}
		if (!sortQueries.isEmpty()) {
			template.query(sortParameter, sortQueries);
		}
	}

	protected boolean supports(Object object) {
		return object instanceof Pageable || object instanceof Sort;
	}

}
