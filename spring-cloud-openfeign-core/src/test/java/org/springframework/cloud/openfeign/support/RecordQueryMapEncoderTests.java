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

import java.util.Collections;
import java.util.Map;

import feign.QueryMapEncoder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RecordQueryMapEncoder}.
 *
 * @author Joo
 */
class RecordQueryMapEncoderTests {

	private final RecordQueryMapEncoder encoder = new RecordQueryMapEncoder();

	@Test
	void shouldEncodeSimpleRecord() {
		// given
		SimpleRecord record = new SimpleRecord("hello", 1);

		// when
		Map<String, Object> result = this.encoder.encode(record);

		// then
		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
		assertThat(result.get("keyword")).isEqualTo("hello");
		assertThat(result.get("page")).isEqualTo(1);
	}

	@Test
	void shouldEncodeEmptyRecord() {
		// given
		EmptyRecord record = new EmptyRecord();

		// when
		Map<String, Object> result = this.encoder.encode(record);

		// then
		assertThat(result).isNotNull();
		assertThat(result).isEmpty();
	}

	@Test
	void shouldReturnEmptyMapForNullInput() {
		// when
		Map<String, Object> result = this.encoder.encode(null);

		// then
		assertThat(result).isNotNull();
		assertThat(result).isEmpty();
	}

	@Test
	void shouldDelegateToBeanEncoderForPojo() {
		// given
		SimplePojo pojo = new SimplePojo();
		pojo.setName("test");

		// when
		Map<String, Object> result = this.encoder.encode(pojo);

		// then
		assertThat(result).isNotNull();
		assertThat(result).containsEntry("name", "test");
	}

	@Test
	void shouldUseProvidedDelegates() {
		// given
		QueryMapEncoder mockRecordEncoder = mock(QueryMapEncoder.class);
		QueryMapEncoder mockBeanEncoder = mock(QueryMapEncoder.class);
		RecordQueryMapEncoder customEncoder = new RecordQueryMapEncoder(mockRecordEncoder, mockBeanEncoder);

		SimpleRecord record = new SimpleRecord("test", 1);
		when(mockRecordEncoder.encode(record)).thenReturn(Collections.emptyMap());

		// when
		customEncoder.encode(record);

		// then
		verify(mockRecordEncoder).encode(record);
	}

	@Test
	void shouldUseProvidedBeanDelegate() {
		// given
		QueryMapEncoder mockRecordEncoder = mock(QueryMapEncoder.class);
		QueryMapEncoder mockBeanEncoder = mock(QueryMapEncoder.class);
		RecordQueryMapEncoder customEncoder = new RecordQueryMapEncoder(mockRecordEncoder, mockBeanEncoder);

		SimplePojo pojo = new SimplePojo();
		when(mockBeanEncoder.encode(pojo)).thenReturn(Collections.emptyMap());

		// when
		customEncoder.encode(pojo);

		// then
		verify(mockBeanEncoder).encode(pojo);
	}

	// Test Records (local definitions)
	record SimpleRecord(String keyword, int page) {
	}

	record EmptyRecord() {
	}

	record RecordWithNullField(String value) {
	}

	// Test POJO for fallback testing
	public static class SimplePojo {

		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
