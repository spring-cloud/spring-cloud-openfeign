/*
 * Copyright 2013-2019 the original author or authors.
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

import feign.hystrix.FallbackFactory;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import org.springframework.cloud.openfeign.testclients.TestClient;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sven Döring
 */
public class FeignClientBuilderTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private FeignClientBuilder feignClientBuilder;

	private ApplicationContext applicationContext;

	private static Object getDefaultValueFromFeignClientAnnotation(
			final String methodName) {
		final Method method = ReflectionUtils.findMethod(FeignClient.class, methodName);
		return method.getDefaultValue();
	}

	private static void assertFactoryBeanField(final FeignClientBuilder.Builder builder,
			final String fieldName, final Object expectedValue) {
		final Field factoryBeanField = ReflectionUtils
				.findField(FeignClientBuilder.Builder.class, "feignClientFactoryBean");
		ReflectionUtils.makeAccessible(factoryBeanField);
		final FeignClientFactoryBean factoryBean = (FeignClientFactoryBean) ReflectionUtils
				.getField(factoryBeanField, builder);

		final Field field = ReflectionUtils.findField(FeignClientFactoryBean.class,
				fieldName);
		ReflectionUtils.makeAccessible(field);
		final Object value = ReflectionUtils.getField(field, factoryBean);
		assertThat(value).as("Expected value for the field '" + fieldName + "':")
				.isEqualTo(expectedValue);
	}

	@Before
	public void setUp() {
		this.applicationContext = Mockito.mock(ApplicationContext.class);
		this.feignClientBuilder = new FeignClientBuilder(this.applicationContext);
	}

	@Test
	public void safetyCheckForNewFieldsOnTheFeignClientAnnotation() {
		final List<String> methodNames = new ArrayList();
		for (final Method method : FeignClient.class.getMethods()) {
			methodNames.add(method.getName());
		}
		methodNames.removeAll(
				Arrays.asList("annotationType", "value", "serviceId", "qualifier",
						"configuration", "primary", "equals", "hashCode", "toString"));
		Collections.sort(methodNames);
		// If this safety check fails the Builder has to be updated.
		// (1) Either a field was removed from the FeignClient annotation and so it has to
		// be removed
		// on this builder class.
		// (2) Or a new field was added and the builder class has to be extended with this
		// new field.
		assertThat(methodNames).containsExactly("contextId", "decode404", "fallback",
				"fallbackFactory", "name", "path", "url");
	}

	@Test
	public void forType_preinitializedBuilder() {
		// when:
		final FeignClientBuilder.Builder builder = this.feignClientBuilder
				.forType(TestFeignClient.class, "TestClient");

		// then:
		assertFactoryBeanField(builder, "applicationContext", this.applicationContext);
		assertFactoryBeanField(builder, "type", TestFeignClient.class);
		assertFactoryBeanField(builder, "name", "TestClient");
		assertFactoryBeanField(builder, "contextId", "TestClient");

		// and:
		assertFactoryBeanField(builder, "url",
				getDefaultValueFromFeignClientAnnotation("url"));
		assertFactoryBeanField(builder, "path",
				getDefaultValueFromFeignClientAnnotation("path"));
		assertFactoryBeanField(builder, "decode404",
				getDefaultValueFromFeignClientAnnotation("decode404"));
		assertFactoryBeanField(builder, "fallback",
				getDefaultValueFromFeignClientAnnotation("fallback"));
		assertFactoryBeanField(builder, "fallbackFactory",
				getDefaultValueFromFeignClientAnnotation("fallbackFactory"));
	}

	@Test
	public void forType_allFieldsSetOnBuilder() {
		// when:
		final FeignClientBuilder.Builder builder = this.feignClientBuilder
				.forType(TestFeignClient.class, "TestClient").decode404(true)
				.fallback(TestFeignClientFallback.class)
				.fallbackFactory(TestFeignClientFallbackFactory.class).path("Path/")
				.url("Url/").contextId("TestContext");

		// then:
		assertFactoryBeanField(builder, "applicationContext", this.applicationContext);
		assertFactoryBeanField(builder, "type", TestFeignClient.class);
		assertFactoryBeanField(builder, "name", "TestClient");
		assertFactoryBeanField(builder, "contextId", "TestContext");

		// and:
		assertFactoryBeanField(builder, "url", "http://Url/");
		assertFactoryBeanField(builder, "path", "/Path");
		assertFactoryBeanField(builder, "decode404", true);
		assertFactoryBeanField(builder, "fallback", TestFeignClientFallback.class);
		assertFactoryBeanField(builder, "fallbackFactory",
				TestFeignClientFallbackFactory.class);
	}

	@Test
	public void forType_build() {
		// given:
		Mockito.when(this.applicationContext.getBean(FeignContext.class))
				.thenThrow(new ClosedFileSystemException()); // throw an unusual exception
																// in the
																// FeignClientFactoryBean
		final FeignClientBuilder.Builder builder = this.feignClientBuilder
				.forType(TestClient.class, "TestClient");

		// expect: 'the build will fail right after calling build() with the mocked
		// unusual exception'
		this.thrown.expect(Matchers.isA(ClosedFileSystemException.class));
		builder.build();
	}

	private interface TestFeignClient {

	}

	private class TestFeignClientFallback implements TestFeignClient {

	}

	private class TestFeignClientFallbackFactory
			implements FallbackFactory<TestFeignClient> {

		@Override
		public TestFeignClientFallback create(Throwable throwable) {
			return new TestFeignClientFallback();
		}

	}

}
