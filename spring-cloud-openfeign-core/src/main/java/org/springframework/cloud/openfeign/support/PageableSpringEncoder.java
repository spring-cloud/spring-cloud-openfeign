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
	 * Creates a new PageableSpringEncoder with the given delegate for fallback. If no
	 * delegate is provided and this encoder cant handle the request, an EncodeException
	 * is thrown.
	 * @param delegate The optional delegate.
	 */
	public PageableSpringEncoder(Encoder delegate) {
		this.delegate = delegate;
	}

	@Override
	public void encode(Object object, Type bodyType, RequestTemplate template)
			throws EncodeException {

		if (supports(object)) {
			if (object instanceof Pageable) {
				Pageable pageable = (Pageable) object;
				template.query("page", pageable.getPageNumber() + "");
				template.query("size", pageable.getPageSize() + "");
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
		for (Sort.Order order : sort) {
			sortQueries.add(order.getProperty() + "," + order.getDirection());
		}
		if (!sortQueries.isEmpty()) {
			template.query("sort", sortQueries);
		}
	}

	protected boolean supports(Object object) {
		return object instanceof Pageable || object instanceof Sort;
	}

}
