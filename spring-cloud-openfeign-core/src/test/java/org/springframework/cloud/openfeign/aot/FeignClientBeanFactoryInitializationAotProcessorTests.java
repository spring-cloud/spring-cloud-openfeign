/*
 * Copyright 2022-2023 the original author or authors.
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

package org.springframework.cloud.openfeign.aot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignClientFactory;
import org.springframework.cloud.openfeign.FeignClientSpecification;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.TypeSpec;
import org.springframework.web.bind.annotation.PostMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.proxies;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.reflection;

/**
 * Tests for {@link FeignClientBeanFactoryInitializationAotProcessor}.
 *
 * @author Olga Maciaszek-Sharma
 */
class FeignClientBeanFactoryInitializationAotProcessorTests {

	private static final String BEAN_DEFINITION_CLASS_NAME = "org.springframework.cloud.openfeign.aot.FeignClientBeanFactoryInitializationAotProcessorTests$TestClient";

	private final TestGenerationContext generationContext = new TestGenerationContext();

	private final FeignClientFactory feignClientFactory = mock(FeignClientFactory.class);

	private final BeanFactoryInitializationCode beanFactoryInitializationCode = new MockBeanFactoryInitializationCode(
			generationContext);

	@BeforeEach
	void setUp() {
		Map<String, FeignClientSpecification> configurations = Map.of("test",
				new FeignClientSpecification("test", TestClient.class.getCanonicalName(), new Class<?>[] {}));
		when(feignClientFactory.getConfigurations()).thenReturn(configurations);

	}

	@Test
	void shouldGenerateBeanInitializationCodeAndRegisterHints() {
		DefaultListableBeanFactory beanFactory = beanFactory();
		GenericApplicationContext context = new GenericApplicationContext(beanFactory);
		FeignClientBeanFactoryInitializationAotProcessor processor = new FeignClientBeanFactoryInitializationAotProcessor(
				context, feignClientFactory);

		BeanFactoryInitializationAotContribution contribution = processor.processAheadOfTime(beanFactory);
		assertThat(contribution).isNotNull();

		verifyContribution((FeignClientBeanFactoryInitializationAotProcessor.AotContribution) contribution);
		contribution.applyTo(generationContext, beanFactoryInitializationCode);

		RuntimeHints hints = generationContext.getRuntimeHints();
		assertThat(reflection().onType(TestReturnType.class)).accepts(hints);
		assertThat(reflection().onType(TestArgType.class)).accepts(hints);
		assertThat(proxies().forInterfaces(TestClient.class)).accepts(hints);
	}

	private static void verifyContribution(
			FeignClientBeanFactoryInitializationAotProcessor.AotContribution contribution) {
		BeanDefinition contributionBeanDefinition = contribution.getFeignClientBeanDefinitions()
			.get(TestClient.class.getCanonicalName());
		assertThat(contributionBeanDefinition.getBeanClassName()).isEqualTo(BEAN_DEFINITION_CLASS_NAME);
		PropertyValues propertyValues = contributionBeanDefinition.getPropertyValues();
		assertThat(propertyValues).isNotEmpty();
		assertThat(((String[]) propertyValues.getPropertyValue("qualifiers").getValue())).containsExactly("test");
		assertThat(propertyValues.getPropertyValue("type").getValue()).isEqualTo(TestClient.class.getCanonicalName());
		assertThat(propertyValues.getPropertyValue("fallback").getValue()).isEqualTo(String.class);
	}

	private DefaultListableBeanFactory beanFactory() {
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition(new RootBeanDefinition(TestClient.class,
				new ConstructorArgumentValues(),
				new MutablePropertyValues(List.of(new PropertyValue("type", TestClient.class.getCanonicalName()),
						new PropertyValue("qualifiers", new String[] { "test" }),
						new PropertyValue("fallback", String.class),
						new PropertyValue("fallbackFactory", Void.class)))));
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition(TestClient.class.getCanonicalName(), beanDefinition);
		return beanFactory;
	}

	@FeignClient
	interface TestClient {

		@PostMapping("/test")
		TestReturnType test(TestArgType arg);

	}

	@SuppressWarnings("NullableProblems")
	static class MockBeanFactoryInitializationCode implements BeanFactoryInitializationCode {

		private static final Consumer<TypeSpec.Builder> emptyTypeCustomizer = type -> {
		};

		private final GeneratedClass generatedClass;

		MockBeanFactoryInitializationCode(GenerationContext generationContext) {
			generatedClass = generationContext.getGeneratedClasses().addForFeature("Test", emptyTypeCustomizer);
		}

		@Override
		public GeneratedMethods getMethods() {
			return generatedClass.getMethods();
		}

		@Override
		public void addInitializer(MethodReference methodReference) {
			new ArrayList<>();
		}

	}

	static class TestReturnType {

		private String test;

		TestReturnType(String test) {
			this.test = test;
		}

		TestReturnType() {
		}

		String getTest() {
			return test;
		}

		void setTest(String test) {
			this.test = test;
		}

	}

	static class TestArgType {

		private String test;

		TestArgType(String test) {
			this.test = test;
		}

		TestArgType() {
		}

		String getTest() {
			return test;
		}

		void setTest(String test) {
			this.test = test;
		}

	}

}
