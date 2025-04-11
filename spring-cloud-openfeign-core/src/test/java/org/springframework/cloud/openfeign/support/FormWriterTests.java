/*
 * Copyright 2013-2025 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Wu Daifu
 * @author Olga Maciaszek-Sharma
 */
class FormWriterTests {

	@Test
	void shouldCorrectlyResolveIfApplicableForCollection() {
		MockFormWriter formWriter = new MockFormWriter();
		Object object = new Object();
		assertThat(formWriter.isApplicable(object)).isFalse();
		object = new Object[] { new Object(), new Object() };
		assertThat(formWriter.isApplicable(object)).isFalse();
		object = new UserPojo();
		assertThat(formWriter.isApplicable(object)).isTrue();
		object = new UserPojo[] { new UserPojo(), new UserPojo() };
		assertThat(formWriter.isApplicable(object)).isTrue();
		object = new byte[] { '1', '2' };
		assertThat(formWriter.isApplicable(object)).isFalse();
	}

	static class MockFormWriter extends AbstractFormWriter {

		@Override
		protected MediaType getContentType() {
			return null;
		}

		@Override
		protected String writeAsString(Object object) {
			return null;
		}

	}

	static class UserPojo {

	}

}
