/*
 * Copyright 2013-2022 the original author or authors.
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
 * @author Yanming Zhou
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
			queryMap.put(sortParameter, sortQueries);
		}
	}

	protected boolean supports(Object object) {
		return object instanceof Pageable || object instanceof Sort;
	}

}
