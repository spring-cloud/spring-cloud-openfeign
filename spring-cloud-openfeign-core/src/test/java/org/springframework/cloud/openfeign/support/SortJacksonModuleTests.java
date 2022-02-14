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

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Can Bezmen
 */
@ExtendWith(MockitoExtension.class)
class SortJacksonModuleTests {

	@Spy
	private ObjectMapper objectMapper;

	@BeforeEach
	public void setup() {
		objectMapper.registerModules(new PageJacksonModule());
		objectMapper.registerModule(new SortJacksonModule());
	}

	@Test
	public void deserializePage() throws JsonProcessingException {
		// Given
		String pageJson = "{\"content\":[\"A name\"],\"number\":1,\"size\":2,\"totalElements\":3,\"sort\":[{\"direction\":\"ASC\",\"property\":\"field\",\"ignoreCase\":false,\"nullHandling\":\"NATIVE\",\"descending\":false,\"ascending\":true}]}";
		// When
		Page<?> result = objectMapper.readValue(pageJson, Page.class);
		// Then
		assertThat(result, notNullValue());
		assertThat(result, hasProperty("totalElements", is(3L)));
		assertThat(result.getContent(), hasSize(1));
		assertThat(result.getPageable(), notNullValue());
		assertThat(result.getPageable().getPageNumber(), is(1));
		assertThat(result.getPageable().getPageSize(), is(2));
		assertThat(result.getPageable().getSort(), notNullValue());
		result.getPageable().getSort();
		Optional<Sort.Order> optionalOrder = result.getPageable().getSort().get().findFirst();
		if (optionalOrder.isPresent()) {
			Sort.Order order = optionalOrder.get();
			assertThat(order, hasProperty("property", is("field")));
			assertThat(order, hasProperty("direction", is(Sort.Direction.ASC)));
		}
	}

	@Test
	public void serializePage() throws IOException {
		// Given
		Sort sort = Sort.by(Sort.Order.by("fieldName"));
		// When
		String result = objectMapper.writeValueAsString(sort);
		// Then
		assertThat(result, containsString("\"direction\":\"ASC\""));
		assertThat(result, containsString("\"property\":\"fieldName\""));
	}

}
