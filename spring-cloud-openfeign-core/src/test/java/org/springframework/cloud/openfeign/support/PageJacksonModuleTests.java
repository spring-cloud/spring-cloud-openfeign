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

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ruben Vervaeke
 */
public class PageJacksonModuleTests {

	private static ObjectMapper objectMapper;

	@BeforeAll
	public static void initialize() {
		objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(Sort.class, new SortJsonComponent.SortSerializer());
		module.addDeserializer(Sort.class, new SortJsonComponent.SortDeserializer());
		objectMapper.registerModules(new PageJacksonModule(), module);
	}

	@Test
	public void deserializePage() throws JsonProcessingException {
		// Given
		String pageJson = "{\"content\":[\"A name\"],\"number\":1,\"size\":2,\"totalElements\":3,\"sort\":[{\"direction\":\"ASC\",\"property\":\"field\",\"ignoreCase\":false,\"nullHandling\":\"NATIVE\",\"descending\":false,\"ascending\":true}]}";
		// When
		Page<?> result = objectMapper.readValue(pageJson, Page.class);
		// Then
		assertThat(result).isNotNull();
		assertThat(result.getTotalElements()).isEqualTo(3);
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getPageable()).isNotNull();
		assertThat(result.getPageable().getPageSize()).isEqualTo(2);
		assertThat(result.getPageable().getPageNumber()).isEqualTo(1);
		assertThat(result.getPageable().getSort()).hasSize(1);
		Optional<Sort.Order> optionalOrder = result.getPageable().getSort().get().findFirst();
		if (optionalOrder.isPresent()) {
			Sort.Order order = optionalOrder.get();
			assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
			assertThat(order.getProperty()).isEqualTo("field");
		}
	}

}
