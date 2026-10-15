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

package org.springframework.cloud.openfeign.test.autoconfigure;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Response;
import feign.Target;
import feign.codec.ErrorDecoder;
import org.junit.jupiter.api.Test;

import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.cloud.openfeign.CircuitBreakerNameResolver;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.clientconfig.FeignClientConfigurer;
import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FeignClientTypeExcludeFilter}.
 *
 * @author Sharang Gupta
 */
class FeignClientTypeExcludeFilterTests {

	private final MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();

	@Test
	void doesNotExcludeTheComponentsThatCustomizeFeignClients() throws IOException {
		FeignClientTypeExcludeFilter filter = new FeignClientTypeExcludeFilter(WithDefaultFilters.class);

		assertThat(excludes(filter, ExampleFeignClientsConfiguration.class)).isFalse();
		assertThat(excludes(filter, ExampleRequestInterceptor.class)).isFalse();
		assertThat(excludes(filter, ExampleErrorDecoder.class)).isFalse();
		assertThat(excludes(filter, ExampleFallbackFactory.class)).isFalse();
		assertThat(excludes(filter, ExampleCircuitBreakerNameResolver.class)).isFalse();
		assertThat(excludes(filter, ExampleFeignClientConfigurer.class)).isFalse();
		assertThat(excludes(filter, ExampleHttpMessageConverterCustomizer.class)).isFalse();
		assertThat(excludes(filter, ExampleClientHttpMessageConvertersCustomizer.class)).isFalse();
	}

	@Test
	void excludesOtherComponents() throws IOException {
		FeignClientTypeExcludeFilter filter = new FeignClientTypeExcludeFilter(WithDefaultFilters.class);

		assertThat(excludes(filter, ExampleService.class)).isTrue();
	}

	@Test
	void excludesTheComponentsThatCustomizeFeignClientsWithoutDefaultFilters() throws IOException {
		FeignClientTypeExcludeFilter filter = new FeignClientTypeExcludeFilter(WithoutDefaultFilters.class);

		assertThat(excludes(filter, ExampleRequestInterceptor.class)).isTrue();
	}

	private boolean excludes(FeignClientTypeExcludeFilter filter, Class<?> type) throws IOException {
		MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(type.getName());
		return filter.match(metadataReader, this.metadataReaderFactory);
	}

	@FeignClientTest
	static class WithDefaultFilters {

	}

	@FeignClientTest(useDefaultFilters = false)
	static class WithoutDefaultFilters {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableFeignClients
	static class ExampleFeignClientsConfiguration {

	}

	static class ExampleRequestInterceptor implements RequestInterceptor {

		@Override
		public void apply(RequestTemplate template) {
		}

	}

	static class ExampleErrorDecoder implements ErrorDecoder {

		@Override
		public Exception decode(String methodKey, Response response) {
			return new IllegalStateException(methodKey);
		}

	}

	static class ExampleFallbackFactory implements FallbackFactory<Object> {

		@Override
		public Object create(Throwable cause) {
			return new Object();
		}

	}

	static class ExampleCircuitBreakerNameResolver implements CircuitBreakerNameResolver {

		@Override
		public String resolveCircuitBreakerName(String feignClientName, Target<?> target, Method method) {
			return feignClientName;
		}

	}

	static class ExampleFeignClientConfigurer implements FeignClientConfigurer {

	}

	static class ExampleHttpMessageConverterCustomizer implements HttpMessageConverterCustomizer {

		@Override
		public void accept(List<HttpMessageConverter<?>> converters) {
		}

	}

	static class ExampleClientHttpMessageConvertersCustomizer implements ClientHttpMessageConvertersCustomizer {

		@Override
		public void customize(HttpMessageConverters.ClientBuilder builder) {
		}

	}

	@Service
	static class ExampleService {

	}

}
