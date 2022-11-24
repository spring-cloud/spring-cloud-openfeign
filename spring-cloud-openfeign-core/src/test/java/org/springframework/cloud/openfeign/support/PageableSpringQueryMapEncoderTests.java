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

import java.util.List;
import java.util.Map;

import feign.QueryMapEncoder;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignClientFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Tests the pagination encoding and sorting.
 *
 * @author Yanming Zhou
 */
@SpringBootTest(classes = SpringEncoderTests.Application.class, webEnvironment = RANDOM_PORT,
		value = { "spring.application.name=springencodertest", "spring.jmx.enabled=false" })
@DirtiesContext
class PageableSpringQueryMapEncoderTests {

	public static final int PAGE = 1;

	public static final int SIZE = 10;

	public static final String SORT_2 = "sort2";

	public static final String SORT_1 = "sort1";

	@Autowired
	private FeignClientFactory context;

	protected String getPageParameter() {
		return "page";
	}

	protected String getSizeParameter() {
		return "size";
	}

	protected String getSortParameter() {
		return "sort";
	}

	@Test
	void testPaginationAndSortingRequest() {
		QueryMapEncoder encoder = this.context.getInstance("foo", QueryMapEncoder.class);
		assertThat(encoder).isNotNull();

		Map<String, Object> map = encoder.encode(createPageAndSortRequest());
		assertThat(map).hasSize(3);
		assertThat((Integer) map.get(getPageParameter())).isEqualTo(PAGE);
		assertThat((Integer) map.get(getSizeParameter())).isEqualTo(SIZE);
		assertThat((List<?>) map.get(getSortParameter())).hasSize(2);
	}

	private Pageable createPageAndSortRequest() {
		return PageRequest.of(PAGE, SIZE, Sort.Direction.ASC, SORT_1, SORT_2);
	}

	@Test
	void testPaginationRequest() {
		QueryMapEncoder encoder = this.context.getInstance("foo", QueryMapEncoder.class);
		assertThat(encoder).isNotNull();

		Map<String, Object> map = encoder.encode(createPageAndRequest());
		assertThat(map).hasSize(2);
		assertThat((Integer) map.get(getPageParameter())).isEqualTo(PAGE);
		assertThat((Integer) map.get(getSizeParameter())).isEqualTo(SIZE);
		assertThat(map).doesNotContainKey(getSortParameter());
	}

	private Pageable createPageAndRequest() {
		return PageRequest.of(PAGE, SIZE);
	}

	@Test
	void testSortingRequest() {
		QueryMapEncoder encoder = this.context.getInstance("foo", QueryMapEncoder.class);
		assertThat(encoder).isNotNull();

		Map<String, Object> map = encoder.encode(createSort());
		assertThat(map).hasSize(1);
		assertThat((List<?>) map.get(getSortParameter())).hasSize(2);
	}

	private Sort createSort() {
		return Sort.by(SORT_1, SORT_2).ascending();
	}

	@Test
	void testUnpagedRequest() {
		QueryMapEncoder encoder = this.context.getInstance("foo", QueryMapEncoder.class);
		assertThat(encoder).isNotNull();

		Map<String, Object> map = encoder.encode(Pageable.unpaged());
		assertThat(map).isEmpty();
	}

}
