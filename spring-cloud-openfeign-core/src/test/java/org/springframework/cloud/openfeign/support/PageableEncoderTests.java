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

import feign.RequestTemplate;
import feign.codec.Encoder;
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
 * @author Charlie Mordant.
 * @author Yanming Zhou
 */
@SpringBootTest(classes = SpringEncoderTests.Application.class, webEnvironment = RANDOM_PORT,
		value = { "spring.application.name=springencodertest", "spring.jmx.enabled=false" })
@DirtiesContext
class PageableEncoderTests {

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
		Encoder encoder = this.context.getInstance("foo", Encoder.class);
		assertThat(encoder).isNotNull();
		RequestTemplate request = new RequestTemplate();
		encoder.encode(createPageAndSortRequest(), null, request);

		// Request queries shall contain three entries
		assertThat(request.queries()).hasSize(3);
		// Request page shall contain page
		assertThat(request.queries().get(getPageParameter())).contains(String.valueOf(PAGE));
		// Request size shall contain size
		assertThat(request.queries().get(getSizeParameter())).contains(String.valueOf(SIZE));
		// Request sort size shall contain sort entries
		assertThat(request.queries().get(getSortParameter())).hasSize(2);
	}

	private Pageable createPageAndSortRequest() {
		return PageRequest.of(PAGE, SIZE, Sort.Direction.ASC, SORT_1, SORT_2);
	}

	@Test
	void testPaginationRequest() {
		Encoder encoder = this.context.getInstance("foo", Encoder.class);
		assertThat(encoder).isNotNull();
		RequestTemplate request = new RequestTemplate();
		encoder.encode(createPageAndRequest(), null, request);
		assertThat(request.queries().size()).isEqualTo(2);
		// Request page shall contain page
		assertThat(request.queries().get(getPageParameter())).contains(String.valueOf(PAGE));
		// Request size shall contain size
		assertThat(request.queries().get(getSizeParameter())).contains(String.valueOf(SIZE));
		// Request sort size shall contain sort entries
		assertThat(request.queries()).doesNotContainKey(getSortParameter());
	}

	private Pageable createPageAndRequest() {
		return PageRequest.of(PAGE, SIZE);
	}

	@Test
	void testSortingRequest() {
		Encoder encoder = this.context.getInstance("foo", Encoder.class);
		assertThat(encoder).isNotNull();
		RequestTemplate request = new RequestTemplate();

		encoder.encode(createSort(), null, request);
		// Request queries shall contain three entries
		assertThat(request.queries().size()).isEqualTo(1);
		// Request sort size shall contain sort entries
		assertThat(request.queries().get(getSortParameter())).hasSize(2);
	}

	private Sort createSort() {
		return Sort.by(SORT_1, SORT_2).ascending();
	}

	@Test
	void testUnpagedRequest() {
		Encoder encoder = this.context.getInstance("foo", Encoder.class);
		assertThat(encoder).isNotNull();
		RequestTemplate request = new RequestTemplate();

		encoder.encode(Pageable.unpaged(), null, request);
		// Request queries shall contain three entries
		assertThat(request.queries()).isEmpty();
	}

}
