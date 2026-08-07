/*
 * Copyright 2013-present the original author or authors.
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

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Java Record support in {@link PageableSpringQueryMapEncoder}.
 *
 * @author Joo (Seongho)
 */
class RecordQueryMapEncoderTests {

	private final PageableSpringQueryMapEncoder encoder = new PageableSpringQueryMapEncoder();

	@Test
	void testSimpleRecordEncoding() {
		// TC1: Record Only
		SimpleRecord record = new SimpleRecord("hello", 1);

		Map<String, Object> result = encoder.encode(record);

		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
		assertThat(result.get("keyword")).isEqualTo("hello");
		assertThat(result.get("page")).isEqualTo(1);
	}

	@Test
	void testEmptyRecordEncoding() {
		// TC4: Empty Record
		EmptyRecord record = new EmptyRecord();

		Map<String, Object> result = encoder.encode(record);

		assertThat(result).isNotNull();
		assertThat(result).isEmpty();
	}

	@Test
	void testRecordWithNullField() {
		// TC5: Record with null field
		RecordWithNull record = new RecordWithNull(null);

		Map<String, Object> result = encoder.encode(record);

		assertThat(result).isNotNull();
		// FieldQueryMapEncoder excludes null fields
		assertThat(result).isEmpty();
	}

	@Test
	void testNullInput() {
		// TC6: Null input
		Map<String, Object> result = encoder.encode(null);

		assertThat(result).isNotNull();
		assertThat(result).isEmpty();
	}

	@Test
	void testPageableEncodingNoConflict() {
		// TC2 & TC7: Pageable encoding - ensure no serialVersionUID conflict
		Pageable pageable = PageRequest.of(2, 20, Sort.by("name").ascending());

		Map<String, Object> result = encoder.encode(pageable);

		assertThat(result).isNotNull();
		assertThat(result).containsEntry("page", 2);
		assertThat(result).containsEntry("size", 20);
		assertThat(result).containsKey("sort");
		// Most important: no IllegalStateException with "Duplicate key serialVersionUID"
	}

	@Test
	void testSortEncoding() {
		// Sort encoding test
		Sort sort = Sort.by("name", "age").ascending();

		Map<String, Object> result = encoder.encode(sort);

		assertThat(result).isNotNull();
		assertThat(result).containsKey("sort");
	}

	@Test
	void testPojoFallback() {
		// TC3: Bean/POJO fallback to BeanQueryMapEncoder
		SimplePojo pojo = new SimplePojo("John", 30);

		Map<String, Object> result = encoder.encode(pojo);

		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
		assertThat(result.get("name")).isEqualTo("John");
		assertThat(result.get("age")).isEqualTo(30);
	}

	// Test Records (local definitions)
	record SimpleRecord(String keyword, int page) {
	}

	record EmptyRecord() {
	}

	record RecordWithNull(String value) {
	}

	// Test POJO for fallback testing
	public static class SimplePojo {

		private final String name;

		private final int age;

		SimplePojo(String name, int age) {
			this.name = name;
			this.age = age;
		}

		public String getName() {
			return name;
		}

		public int getAge() {
			return age;
		}

	}

}
