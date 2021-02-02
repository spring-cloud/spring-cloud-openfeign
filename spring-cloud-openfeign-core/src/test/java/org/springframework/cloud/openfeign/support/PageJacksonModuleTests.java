/*
 * Copyright 2013-2020 the original author or authors.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.data.domain.Page;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PageJacksonModule}.
 *
 * @author Ruben Vervaeke
 * @author Olga Maciaszek-Sharma
 */
public class PageJacksonModuleTests {

	private static ObjectMapper objectMapper;

	@BeforeAll
	public static void initialize() {
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new PageJacksonModule());
	}

	@ParameterizedTest
	@ValueSource(strings = { "totalElements", "total-elements", "total_elements",
			"totalelements", "TotalElements" })
	public void deserializePage(String totalElements) throws JsonProcessingException {
		// Given
		String pageJson = "{\"content\":[\"A name\"], \"number\":1, \"size\":2, \""
				+ totalElements + "\": 3}";
		// When
		Page<?> result = objectMapper.readValue(pageJson, Page.class);
		// Then
		assertThat(result).isNotNull();
		assertThat(result.getTotalElements()).isEqualTo(3);
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getPageable()).isNotNull();
		assertThat(result.getPageable().getPageSize()).isEqualTo(2);
		assertThat(result.getPageable().getPageNumber()).isEqualTo(1);
	}

}
