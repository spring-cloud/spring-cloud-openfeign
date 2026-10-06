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

package org.springframework.cloud.openfeign;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.loadbalancer.support.SimpleObjectProvider;
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SpringDecoder}.
 *
 * @author Olga Maciaszek-Sharma
 */
class SpringDecoderTests {

	SpringDecoder decoder;

	@BeforeEach
	void setUp() {
		ObjectProvider<FeignHttpMessageConverters> converters = new SimpleObjectProvider<>(
				new FeignHttpMessageConverters(mock(), mock()));
		decoder = new SpringDecoder(converters);
	}

	// Issue: https://github.com/spring-cloud/spring-cloud-openfeign/issues/972
	@Test
	void shouldNotThrownNPEWhenNoContent() {
		assertThatCode(
				() -> decoder.decode(Response.builder().request(mock(Request.class)).status(200).build(), String.class))
			.doesNotThrowAnyException();
	}

	@Test
	void shouldNotReadBodyForResponseEntityOfVoid() throws Exception {
		Type type = ResolvableType.forClassWithGenerics(ResponseEntity.class, Void.class).getType();

		Object decoded = new ResponseEntityDecoder(decoder).decode(textResponse(), type);

		assertThat(decoded).isInstanceOf(ResponseEntity.class);
		ResponseEntity<?> entity = (ResponseEntity<?>) decoded;
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
		assertThat(entity.getHeaders().getFirst("Location")).isEqualTo("/jobs/1");
		assertThat(entity.getBody()).isNull();
	}

	@Test
	void shouldNotReadBodyForHttpEntityOfVoid() throws Exception {
		Type type = ResolvableType.forClassWithGenerics(HttpEntity.class, Void.class).getType();

		Object decoded = new ResponseEntityDecoder(decoder).decode(textResponse(), type);

		assertThat(decoded).isInstanceOf(ResponseEntity.class);
		assertThat(((HttpEntity<?>) decoded).getBody()).isNull();
	}

	private static Response textResponse() {
		return Response.builder()
			.request(mock(Request.class))
			.status(202)
			.headers(Map.<String, Collection<String>>of("Content-Type", List.of("text/plain"), "Location",
					List.of("/jobs/1")))
			.body("Accepted", StandardCharsets.UTF_8)
			.build();
	}

}
