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

package org.springframework.cloud.openfeign.encoding.proto;

import java.util.stream.Stream;

import feign.RequestTemplate;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.cloud.loadbalancer.support.SimpleObjectProvider;
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.http.converter.StringHttpMessageConverter;

import static feign.Request.HttpMethod.POST;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test {@link SpringEncoder} when protobuf is not in classpath
 *
 * @author ScienJus
 */
@ClassPathExclusions("protobuf-*.jar")
class ProtobufNotInClasspathTest {

	@Test
	void testEncodeWhenProtobufNotInClasspath() {
		ObjectProvider<ClientHttpMessageConvertersCustomizer> factory = mock();
		when(factory.orderedStream())
			.thenReturn(Stream.of(converters -> converters.withStringConverter(new StringHttpMessageConverter())));
		RequestTemplate requestTemplate = new RequestTemplate();
		requestTemplate.method(POST);
		new SpringEncoder(new SimpleObjectProvider<>(new FeignHttpMessageConverters(factory, mock()))).encode("a=b",
				String.class, requestTemplate);
	}

}
