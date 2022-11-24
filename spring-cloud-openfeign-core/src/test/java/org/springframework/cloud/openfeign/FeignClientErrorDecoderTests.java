/*
 * Copyright 2020-2022 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

import feign.Contract;
import feign.InvocationHandlerFactory;
import feign.RequestLine;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael Cramer
 * @author Jonatan Ivanov
 */
@SpringBootTest(classes = FeignClientErrorDecoderTests.TestConfiguration.class)
@DirtiesContext
public class FeignClientErrorDecoderTests {

	@Autowired
	private FeignClientFactory context;

	@Autowired
	private FooClient foo;

	@Autowired
	private BarClient bar;

	@Test
	public void clientsAvailable() {
		assertThat(this.foo).isNotNull();
		assertThat(this.bar).isNotNull();
	}

	@Test
	public void errorDecoderInConfiguration() {
		assertThat(this.context.getInstance("foo", ErrorDecoder.class)).isInstanceOf(ErrorDecoder.Default.class);
		assertThat(this.context.getInstance("bar", ErrorDecoder.class)).isNull();
	}

	@Test
	@DisabledForJreRange(min = JRE.JAVA_16)
	public void useConfiguredErrorDecoderWhenAlsoErrorDecoderFactoryIsAvailable() {
		Object errorDecoder = getErrorDecoderFromClient(this.foo);
		assertThat(errorDecoder).isInstanceOf(ErrorDecoder.Default.class);
	}

	@Test
	@DisabledForJreRange(min = JRE.JAVA_16)
	public void useErrorDecoderFromErrorDecoderFactory() {
		Object errorDecoder = getErrorDecoderFromClient(this.bar);
		assertThat(errorDecoder).isInstanceOf(ErrorDecoderImpl.class);
	}

	@SuppressWarnings({ "unchecked", "ConstantConditions" })
	private Object getErrorDecoderFromClient(final Object client) {
		Object invocationHandlerLambda = ReflectionTestUtils.getField(client, "h");
		Object invocationHandler = ReflectionTestUtils.getField(invocationHandlerLambda, "arg$2");
		Map<Method, InvocationHandlerFactory.MethodHandler> dispatch = (Map<Method, InvocationHandlerFactory.MethodHandler>) ReflectionTestUtils
				.getField(invocationHandler, "dispatch");
		Method key = new ArrayList<>(dispatch.keySet()).get(0);
		return ReflectionTestUtils.getField(ReflectionTestUtils.getField(dispatch.get(key), "asyncResponseHandler"),
				"errorDecoder");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableFeignClients(clients = { FooClient.class, BarClient.class })
	@EnableAutoConfiguration
	protected static class TestConfiguration {

	}

	@FeignClient(name = "foo", url = "http://foo", configuration = FooConfiguration.class)
	interface FooClient {

		@RequestLine("GET /")
		String get();

	}

	public static class FooConfiguration {

		@Bean
		Contract feignContract() {
			return new Contract.Default();
		}

		@Bean
		FeignErrorDecoderFactory errorDecoderFactory() {
			return new FeignErrorDecoderFactoryImpl();
		}

		@Bean
		ErrorDecoder feignErrorDecoder() {
			return new ErrorDecoder.Default();
		}

	}

	@FeignClient(name = "bar", url = "http://bar", configuration = BarConfiguration.class)
	interface BarClient {

		@GetMapping("/")
		String get();

	}

	public static class BarConfiguration {

		@Bean
		Contract feignContract() {
			return new SpringMvcContract();
		}

		@Bean
		FeignErrorDecoderFactory errorDecoderFactory() {
			return new FeignErrorDecoderFactoryImpl();
		}

	}

	public static class FeignErrorDecoderFactoryImpl implements FeignErrorDecoderFactory {

		@Override
		public ErrorDecoder create(final Class<?> type) {
			return new ErrorDecoderImpl();
		}

	}

	public static class ErrorDecoderImpl implements ErrorDecoder {

		@Override
		public Exception decode(final String methodKey, final Response response) {
			return null;
		}

	}

}
