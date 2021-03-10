/*
 * Copyright 2013-2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import feign.querymap.BeanQueryMapEncoder;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Provides support for encoding Pageable annotated as
 * {@link org.springframework.cloud.openfeign.SpringQueryMap}.
 *
 * @author Hyeonmin Park
 * @since 2.2.8
 */
public class PageableSpringQueryMapEncoder extends BeanQueryMapEncoder {

	@Override
	public Map<String, Object> encode(Object object) {
		if (supports(object)) {
			Map<String, Object> queryMap = new HashMap<>();

			if (object instanceof Pageable) {
				Pageable pageable = (Pageable) object;

				if (pageable.isPaged()) {
					queryMap.put("page", pageable.getPageNumber());
					queryMap.put("size", pageable.getPageSize());
				}

				if (pageable.getSort() != null) {
					applySort(queryMap, pageable.getSort());
				}
			}
			else if (object instanceof Sort) {
				Sort sort = (Sort) object;
				applySort(queryMap, sort);
			}
			return queryMap;
		}
		else {
			return super.encode(object);
		}
	}

	private void applySort(Map<String, Object> queryMap, Sort sort) {
		List<String> sortQueries = new ArrayList<>();
		for (Sort.Order order : sort) {
			sortQueries.add(order.getProperty() + "%2C" + order.getDirection());
		}
		if (!sortQueries.isEmpty()) {
			queryMap.put("sort", sortQueries);
		}
	}

	protected boolean supports(Object object) {
		return object instanceof Pageable || object instanceof Sort;
	}

}
