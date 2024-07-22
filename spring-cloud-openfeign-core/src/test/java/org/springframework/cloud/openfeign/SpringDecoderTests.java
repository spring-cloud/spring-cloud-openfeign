/*
 * Copyright 2013-2024 the original author or authors.
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

package org.springframework.cloud.openfeign;

import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SpringDecoder}.
 *
 * @author Olga Maciaszek-Sharma
 */
class SpringDecoderTests {

	SpringDecoder decoder;

	@BeforeEach
	void setUp() {
		ObjectFactory<HttpMessageConverters> factory = mock();
		when(factory.getObject()).thenReturn(new HttpMessageConverters());
		decoder = new SpringDecoder(factory);
	}

	// Issue: https://github.com/spring-cloud/spring-cloud-openfeign/issues/972
	@Test
	void shouldNotThrownNPEWhenNoContent() {
		assertThatCode(
				() -> decoder.decode(Response.builder().request(mock(Request.class)).status(200).build(), String.class))
			.doesNotThrowAnyException();
	}

}
