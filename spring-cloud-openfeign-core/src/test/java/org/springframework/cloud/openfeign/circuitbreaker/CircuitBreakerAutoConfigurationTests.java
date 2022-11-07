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

package org.springframework.cloud.openfeign.circuitbreaker;

import feign.Target;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.CircuitBreakerNameResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ryan Baxter
 */
public class CircuitBreakerAutoConfigurationTests {

	@SpringBootTest(classes = CircuitBreakerTests.Application.class,
			webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
			value = { "spring.application.name=springcircuittest", "spring.jmx.enabled=false",
					"spring.cloud.openfeign.circuitbreaker.enabled=true",
					"spring.cloud.openfeign.circuitbreaker.alphanumeric-ids.enabled=false" })
	@Nested
	class DefaultNamingStrategy {

		@Autowired
		CircuitBreakerNameResolver nameResolver;

		@SuppressWarnings("rawtypes")
		@Test
		public void assertDefaultNamingStrategy() throws Exception {
			Target target = mock(Target.class);
			when(target.type()).thenReturn(CircuitBreakerTests.TestClientWithFactory.class);
			assertThat(nameResolver.resolveCircuitBreakerName("foo", target,
					CircuitBreakerTests.TestClientWithFactory.class.getMethod("getHello")))
							.isEqualTo("TestClientWithFactory#getHello()");
		}

	}

	@SpringBootTest(classes = CircuitBreakerTests.Application.class,
			webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
			value = { "spring.application.name=springcircuittest", "spring.jmx.enabled=false",
					"spring.cloud.openfeign.circuitbreaker.enabled=true" })
	@Nested
	class AlphanumericNamingStrategy {

		@Autowired
		CircuitBreakerNameResolver nameResolver;

		@SuppressWarnings("rawtypes")
		@Test
		public void assertAlphanumericNamingStrategy() throws Exception {
			Target target = mock(Target.class);
			when(target.type()).thenReturn(CircuitBreakerTests.TestClientWithFactory.class);
			assertThat(nameResolver.resolveCircuitBreakerName("foo", target,
					CircuitBreakerTests.TestClientWithFactory.class.getMethod("getHello")))
							.isEqualTo("TestClientWithFactorygetHello");
		}

	}

}
