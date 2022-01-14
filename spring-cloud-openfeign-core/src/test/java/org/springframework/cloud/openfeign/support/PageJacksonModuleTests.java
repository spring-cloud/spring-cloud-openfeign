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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PageJacksonModule}.
 *
 * @author Ruben Vervaeke
 * @author Olga Maciaszek-Sharma
 * @author Pedro Mendes
 * @author Nikita Konev
 */
public class PageJacksonModuleTests {

	private static ObjectMapper objectMapper;

	@BeforeAll
	public static void initialize() {
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new PageJacksonModule());
		objectMapper.registerModule(new SortJacksonModule());
	}

	@ParameterizedTest
	@ValueSource(strings = { "totalElements", "total-elements", "total_elements", "totalelements", "TotalElements" })
	public void deserializePage(String totalElements) throws JsonProcessingException {
		// Given
		String pageJson = "{\"content\":[\"A name\"], \"number\":1, \"size\":2, \"" + totalElements + "\": 3}";
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

	@Test
	public void serializeAndDeserializeEmpty() throws JsonProcessingException {
		// Given
		PageImpl<Object> objects = new PageImpl<>(new ArrayList<>());
		String pageJson = objectMapper.writeValueAsString(objects);
		// When
		Page<?> result = objectMapper.readValue(pageJson, Page.class);
		// Then
		assertThat(result).isNotNull();
		assertThat(result.getTotalElements()).isEqualTo(0);
		assertThat(result.getContent()).hasSize(0);
	}

	@Test
	public void serializeAndDeserializeFilledMultiple() throws JsonProcessingException {
		// Given
		ArrayList<Object> pageElements = new ArrayList<>();
		pageElements.add("first element");
		pageElements.add("second element");
		PageImpl<Object> objects = new PageImpl<>(pageElements, PageRequest.of(6, 2), 100);
		assertThat(objects.getContent()).hasSize(2);
		assertThat(objects.getPageable().getPageSize()).isEqualTo(2);

		String pageJson = objectMapper.writeValueAsString(objects);
		// When
		Page<?> result = objectMapper.readValue(pageJson, Page.class);
		// Then
		assertThat(result).isNotNull();
		assertThat(result.getTotalElements()).isEqualTo(100);
		assertThat(result.getContent()).hasSize(2);
		assertThat(result.getContent().get(0)).isEqualTo("first element");
		assertThat(result.getContent().get(1)).isEqualTo("second element");
		assertThat(result.getPageable().getPageSize()).isEqualTo(2);
		assertThat(result.getPageable().getPageNumber()).isEqualTo(6);
	}

	@Test
	public void serializeAndDeserializeEmptyCascade() throws JsonProcessingException {
		// Given
		PageImpl<Object> objects = new PageImpl<>(new ArrayList<>());
		String pageJson = objectMapper.writeValueAsString(objects);
		// When
		Page<?> result = objectMapper.readValue(pageJson, Page.class);
		// Then
		assertThat(result).isNotNull();
		assertThat(result.getTotalElements()).isEqualTo(0);
		assertThat(result.getContent()).hasSize(0);

		String cascadedPageJson = objectMapper.writeValueAsString(result);
		Page<?> cascadedResult = objectMapper.readValue(cascadedPageJson, Page.class);
		assertThat(cascadedResult).isNotNull();
		assertThat(cascadedResult.getTotalElements()).isEqualTo(0);
		assertThat(cascadedResult.getContent()).hasSize(0);
	}

	@Test
	public void serializeAndDeserializeFilledMultipleCascade() throws JsonProcessingException {
		// Given
		ArrayList<Object> pageElements = new ArrayList<>();
		pageElements.add("first element in cascaded serialization");
		pageElements.add("second element in cascaded serialization");
		PageImpl<Object> objects = new PageImpl<>(pageElements, PageRequest.of(6, 2), 100);
		assertThat(objects.getContent()).hasSize(2);
		assertThat(objects.getPageable().getPageSize()).isEqualTo(2);

		String pageJson = objectMapper.writeValueAsString(objects);
		// When
		Page<?> result = objectMapper.readValue(pageJson, Page.class);
		// Then
		assertThat(result).isNotNull();
		assertThat(result.getTotalElements()).isEqualTo(100);
		assertThat(result.getContent()).hasSize(2);
		assertThat(result.getContent().get(0)).isEqualTo("first element in cascaded serialization");
		assertThat(result.getContent().get(1)).isEqualTo("second element in cascaded serialization");
		assertThat(result.getPageable().getPageSize()).isEqualTo(2);
		assertThat(result.getPageable().getPageNumber()).isEqualTo(6);

		String cascadedPageJson = objectMapper.writeValueAsString(result);
		Page<?> cascadedResult = objectMapper.readValue(cascadedPageJson, Page.class);
		// Then
		assertThat(cascadedResult).isNotNull();
		assertThat(cascadedResult.getTotalElements()).isEqualTo(100);
		assertThat(cascadedResult.getContent()).hasSize(2);
		assertThat(cascadedResult.getContent().get(0)).isEqualTo("first element in cascaded serialization");
		assertThat(cascadedResult.getContent().get(1)).isEqualTo("second element in cascaded serialization");
		assertThat(cascadedResult.getPageable().getPageSize()).isEqualTo(2);
		assertThat(cascadedResult.getPageable().getPageNumber()).isEqualTo(6);
	}

}
