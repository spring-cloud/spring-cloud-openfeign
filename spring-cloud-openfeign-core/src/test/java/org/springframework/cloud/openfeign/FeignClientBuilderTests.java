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

package org.springframework.cloud.openfeign;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.ClosedFileSystemException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import feign.Feign;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.cloud.openfeign.testclients.TestClient;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Sven Döring
 * @author Sam Kruglov
 * @author Szymon Linowski
 */
class FeignClientBuilderTests {

	private FeignClientBuilder feignClientBuilder;

	private ApplicationContext applicationContext;

	private static Object getDefaultValueFromFeignClientAnnotation(final String methodName) {
		final Method method = ReflectionUtils.findMethod(FeignClient.class, methodName);
		return method.getDefaultValue();
	}

	private static void assertFactoryBeanField(final FeignClientBuilder.Builder builder, final String fieldName,
			final Object expectedValue) {
		final Object value = getFactoryBeanField(builder, fieldName);
		assertThat(value).as("Expected value for the field '" + fieldName + "':").isEqualTo(expectedValue);
	}

	@SuppressWarnings("unchecked")
	private static <T> T getFactoryBeanField(final FeignClientBuilder.Builder builder, final String fieldName) {
		final Field factoryBeanField = ReflectionUtils.findField(FeignClientBuilder.Builder.class,
				"feignClientFactoryBean");
		ReflectionUtils.makeAccessible(factoryBeanField);
		final FeignClientFactoryBean factoryBean = (FeignClientFactoryBean) ReflectionUtils.getField(factoryBeanField,
				builder);

		final Field field = ReflectionUtils.findField(FeignClientFactoryBean.class, fieldName);
		ReflectionUtils.makeAccessible(field);
		return (T) ReflectionUtils.getField(field, factoryBean);
	}

	@BeforeEach
	void setUp() {
		this.applicationContext = Mockito.mock(ApplicationContext.class);
		this.feignClientBuilder = new FeignClientBuilder(this.applicationContext);
	}

	@Test
	void safetyCheckForNewFieldsOnTheFeignClientAnnotation() {
		final List<String> methodNames = new ArrayList<>();
		for (final Method method : FeignClient.class.getMethods()) {
			methodNames.add(method.getName());
		}
		methodNames.removeAll(Arrays.asList("annotationType", "value", "serviceId", "qualifier", "qualifiers",
				"configuration", "primary", "equals", "hashCode", "toString"));
		Collections.sort(methodNames);
		// If this safety check fails the Builder has to be updated.
		// (1) Either a field was removed from the FeignClient annotation and so it has to
		// be removed
		// on this builder class.
		// (2) Or a new field was added and the builder class has to be extended with this
		// new field.
		assertThat(methodNames).containsExactly("contextId", "dismiss404", "fallback", "fallbackFactory", "name",
				"path", "url");
	}

	@Test
	void forType_preinitializedBuilder() {
		// when:
		final FeignClientBuilder.Builder builder = this.feignClientBuilder.forType(TestFeignClient.class, "TestClient");

		// then:
		assertFactoryBeanField(builder, "applicationContext", this.applicationContext);
		assertFactoryBeanField(builder, "type", TestFeignClient.class);
		assertFactoryBeanField(builder, "name", "TestClient");
		assertFactoryBeanField(builder, "contextId", "TestClient");

		// and:
		assertFactoryBeanField(builder, "url", getDefaultValueFromFeignClientAnnotation("url"));
		assertFactoryBeanField(builder, "path", getDefaultValueFromFeignClientAnnotation("path"));
		assertFactoryBeanField(builder, "dismiss404", getDefaultValueFromFeignClientAnnotation("dismiss404"));
		assertFactoryBeanField(builder, "fallback", getDefaultValueFromFeignClientAnnotation("fallback"));
		assertFactoryBeanField(builder, "fallbackFactory", getDefaultValueFromFeignClientAnnotation("fallbackFactory"));
	}

	@Test
	void forType_allFieldsSetOnBuilder() {
		// when:
		final FeignClientBuilder.Builder builder = this.feignClientBuilder.forType(TestFeignClient.class, "TestClient")
				.dismiss404(true).url("Url/").path("/Path").contextId("TestContext");

		// then:
		assertFactoryBeanField(builder, "applicationContext", this.applicationContext);
		assertFactoryBeanField(builder, "type", TestFeignClient.class);
		assertFactoryBeanField(builder, "name", "TestClient");
		assertFactoryBeanField(builder, "contextId", "TestContext");

		// and:
		assertFactoryBeanField(builder, "url", "http://Url/");
		assertFactoryBeanField(builder, "path", "/Path");
		assertFactoryBeanField(builder, "dismiss404", true);

	}

	@Test
	void forType_clientFactoryBeanProvided() {
		// when:
		final FeignClientBuilder.Builder builder = this.feignClientBuilder
				.forType(TestFeignClient.class, new FeignClientFactoryBean(), "TestClient").dismiss404(true)
				.path("Path/").url("Url/").contextId("TestContext").customize(Feign.Builder::doNotCloseAfterDecode);

		// then:
		assertFactoryBeanField(builder, "applicationContext", this.applicationContext);
		assertFactoryBeanField(builder, "type", TestFeignClient.class);
		assertFactoryBeanField(builder, "name", "TestClient");
		assertFactoryBeanField(builder, "contextId", "TestContext");

		// and:
		assertFactoryBeanField(builder, "url", "http://Url/");
		assertFactoryBeanField(builder, "path", "/Path");
		assertFactoryBeanField(builder, "dismiss404", true);
		List<FeignBuilderCustomizer> additionalCustomizers = getFactoryBeanField(builder, "additionalCustomizers");
		assertThat(additionalCustomizers).hasSize(1);
	}

	@Test
	void forType_build() {
		// given:
		Mockito.when(this.applicationContext.getBean(FeignClientFactory.class))
				.thenThrow(new ClosedFileSystemException()); // throw
		// an
		// unusual
		// exception
		// in
		// the
		// FeignClientFactoryBean
		final FeignClientBuilder.Builder builder = this.feignClientBuilder.forType(TestClient.class, "TestClient");
		// expect: 'the build will fail right after calling build() with the mocked
		// unusual exception'
		assertThatExceptionOfType(ClosedFileSystemException.class).isThrownBy(builder::build);
	}

	private interface TestFeignClient {

	}

}
