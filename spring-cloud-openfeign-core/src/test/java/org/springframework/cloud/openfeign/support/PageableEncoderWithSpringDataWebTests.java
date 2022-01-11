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

import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Tests the pagination encoding and sorting.
 *
 * @author Yanming Zhou
 */
@EnableConfigurationProperties(SpringDataWebProperties.class)
@SpringBootTest(classes = SpringEncoderTests.Application.class, webEnvironment = RANDOM_PORT,
		value = { "spring.application.name=springencodertest", "spring.jmx.enabled=false",
				"spring.data.web.pageable.pageParameter=pageNo", "spring.data.web.pageable.sizeParameter=pageSize",
				"spring.data.web.sort.sortParameter=orderBy" })
public class PageableEncoderWithSpringDataWebTests extends PageableEncoderTests {

	@Override
	protected String getPageParameter() {
		return "pageNo";
	}

	@Override
	protected String getSizeParameter() {
		return "pageSize";
	}

	@Override
	protected String getSortParameter() {
		return "orderBy";
	}

}
