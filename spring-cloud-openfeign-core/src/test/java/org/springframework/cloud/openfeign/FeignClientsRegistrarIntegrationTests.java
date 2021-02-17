/*
 * Copyright 2013-2021 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link FeignClientsRegistrar}.
 *
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(
		classes = FeignClientsRegistrarIntegrationTests.QualifiersTestConfig.class)
class FeignClientsRegistrarIntegrationTests {

	@Autowired
	ConfigurableApplicationContext context;

	@Test
	void shouldUseQualifiersIfPresent() {
		assertThat(context.getBean("qualifier1")).isNotNull();
		assertThat(context.getBean("qualifier2")).isNotNull();
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> context.getBean("qualifier3"));
	}

	@Test
	void shouldUseQualifierIfQualifiersArrayNotPresent() {
		assertThat(context.getBean("qualifier4")).isNotNull();
	}

	@Test
	void shouldUseDefaultQualifierWhenNonePresent() {
		assertThat(context.getBean("noQualifiersFeignClient")).isNotNull();
	}

	@Test
	void shouldUseQualifierWhenEmptyQualifiers() {
		assertThat(context.getBean("test1")).isNotNull();
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> context.getBean("emptyQualifiersFeignClient"));
	}

	@Test
	void shouldUseQualifierWhenWhitespaceQualifiers() {
		assertThat(context.getBean("test2")).isNotNull();
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> context.getBean("whitespaceQualifiersFeignClient"));
	}

	@Test
	void shouldUseDefaultQualifierWhenEmptyQualifiers() {
		assertThat(context.getBean("emptyQualifiersNoQualifierFeignClient")).isNotNull();
	}

	@Test
	void shouldUseDefaultQualifierWhenWhitespaceQualifiers() {
		assertThat(context.getBean("whitespaceQualifiersNoQualifierFeignClient"))
				.isNotNull();
	}

	@FeignClient(name = "qualifiersClient", qualifiers = { "qualifier1", "qualifier2" },
			qualifier = "qualifier3")
	protected interface QualifiersClient {

	}

	@FeignClient(name = "qualifierClient", qualifier = "qualifier4")
	protected interface QualifierClient {

	}

	@FeignClient(name = "noQualifiers")
	protected interface NoQualifiersClient {

	}

	@FeignClient(name = "emptyQualifiers", qualifier = "test1", qualifiers = {})
	protected interface EmptyQualifiersClient {

	}

	@FeignClient(name = "whitespaceQualifiers", qualifier = "test2", qualifiers = { " " })
	protected interface WhitespaceQualifiersClient {

	}

	@FeignClient(name = "emptyQualifiersNoQualifier", qualifiers = {})
	protected interface EmptyQualifiersNoQualifierClient {

	}

	@FeignClient(name = "whitespaceQualifiersNoQualifier", qualifiers = { " " })
	protected interface WhitespaceQualifiersNoQualifierClient {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@Import(NoSecurityConfiguration.class)
	@EnableFeignClients(clients = { QualifiersClient.class, QualifierClient.class,
			NoQualifiersClient.class, EmptyQualifiersClient.class,
			WhitespaceQualifiersClient.class, EmptyQualifiersNoQualifierClient.class,
			WhitespaceQualifiersNoQualifierClient.class })
	protected static class QualifiersTestConfig {

	}

}
