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

import feign.Param;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that {@link PageableSpringQueryMapEncoder} honors {@link Param} on fields the
 * same way {@link SpringFieldQueryMapEncoder} does.
 *
 * @author Rajesh Vasa
 */
class PageableSpringQueryMapEncoderParamTests {

	private final PageableSpringQueryMapEncoder encoder = new PageableSpringQueryMapEncoder();

	@Test
	void honorsParamDeclaredOnField() {
		Map<String, Object> queryMap = encoder.encode(new FieldParamCriteria("value1", "value2"));

		assertThat(queryMap).containsOnly(Map.entry("dummy-param1", "value1"), Map.entry("plainParam", "value2"));
	}

	@Test
	void encodesSameNamesAsSpringFieldQueryMapEncoder() {
		FieldParamCriteria criteria = new FieldParamCriteria("value1", "value2");

		assertThat(encoder.encode(criteria)).isEqualTo(new SpringFieldQueryMapEncoder().encode(criteria));
	}

	@Test
	void honorsParamDeclaredOnInheritedField() {
		Map<String, Object> queryMap = encoder.encode(new ChildCriteria("value1", "value2", "value3"));

		assertThat(queryMap).containsOnly(Map.entry("dummy-param1", "value1"), Map.entry("plainParam", "value2"),
				Map.entry("child-param", "value3"));
	}

	@Test
	void paramOnGetterTakesPrecedenceOverParamOnField() {
		Map<String, Object> queryMap = encoder.encode(new GetterParamCriteria("value1"));

		assertThat(queryMap).containsOnly(Map.entry("getter-name", "value1"));
	}

	@Test
	void skipsNullValues() {
		Map<String, Object> queryMap = encoder.encode(new FieldParamCriteria(null, "value2"));

		assertThat(queryMap).containsOnly(Map.entry("plainParam", "value2"));
	}

	// BeanQueryMapEncoder invokes getters reflectively without making them accessible,
	// so query map beans must be public, as they are in real applications.
	public static class FieldParamCriteria {

		@Param("dummy-param1")
		private String dummyParam1;

		private String plainParam;

		FieldParamCriteria(String dummyParam1, String plainParam) {
			this.dummyParam1 = dummyParam1;
			this.plainParam = plainParam;
		}

		public String getDummyParam1() {
			return dummyParam1;
		}

		public String getPlainParam() {
			return plainParam;
		}

	}

	public static class ChildCriteria extends FieldParamCriteria {

		@Param("child-param")
		private String childParam;

		ChildCriteria(String dummyParam1, String plainParam, String childParam) {
			super(dummyParam1, plainParam);
			this.childParam = childParam;
		}

		public String getChildParam() {
			return childParam;
		}

	}

	public static class GetterParamCriteria {

		@Param("field-name")
		private String value;

		GetterParamCriteria(String value) {
			this.value = value;
		}

		@Param("getter-name")
		public String getValue() {
			return value;
		}

	}

}
