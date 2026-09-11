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

import feign.Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(OutputCaptureExtension.class)
class FeignClientFactoryBeanTests {

	private static final String REPEATED_INITIALIZATION_WARNING = "is being initialized more than once";

	@Test
	void shouldWarnWhenClientIsResolvedTwiceWithinOneApplicationContext(CapturedOutput output) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				SampleConfiguration.class)) {
			FeignClientFactoryBean factoryBean = context.getBean(FeignClientFactoryBean.class);
			factoryBean.getTarget();
			factoryBean.getTarget();
		}

		assertThat(output).contains(REPEATED_INITIALIZATION_WARNING);
	}

	@Test
	void shouldNotWarnWhenSeparateApplicationContextsEachResolveTheClientOnce(CapturedOutput output) {
		try (AnnotationConfigApplicationContext first = new AnnotationConfigApplicationContext(
				SampleConfiguration.class)) {
			first.getBean(FeignClientFactoryBean.class).getTarget();
		}
		try (AnnotationConfigApplicationContext second = new AnnotationConfigApplicationContext(
				SampleConfiguration.class)) {
			second.getBean(FeignClientFactoryBean.class).getTarget();
		}

		assertThat(output).doesNotContain(REPEATED_INITIALIZATION_WARNING);
	}

	@Configuration(proxyBeanMethods = false)
	@Import(FeignClientsConfiguration.class)
	static class SampleConfiguration {

		@Bean
		FeignClientFactory feignClientFactory() {
			return new FeignClientFactory();
		}

		@Bean
		FeignClientProperties feignClientProperties() {
			return new FeignClientProperties();
		}

		@Bean
		Targeter targeter() {
			return new DefaultTargeter();
		}

		@Bean
		Client client() {
			return mock(Client.class);
		}

		@Bean
		FeignClientFactoryBean feignClientFactoryBean() {
			FeignClientFactoryBean factoryBean = new FeignClientFactoryBean();
			factoryBean.setContextId("repeatedclientresolution");
			factoryBean.setName("repeatedclientresolution");
			factoryBean.setType(FeignClientFactoryTests.TestType.class);
			factoryBean.setPath("");
			factoryBean.setUrl("http://some.absolute.url");
			return factoryBean;
		}

	}

}
