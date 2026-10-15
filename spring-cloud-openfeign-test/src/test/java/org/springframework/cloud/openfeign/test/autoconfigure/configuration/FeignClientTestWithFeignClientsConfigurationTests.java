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

package org.springframework.cloud.openfeign.test.autoconfigure.configuration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.test.autoconfigure.FeignClientTest;
import org.springframework.cloud.openfeign.test.autoconfigure.StubServer;
import org.springframework.cloud.openfeign.test.autoconfigure.application.Greeting;
import org.springframework.cloud.openfeign.test.autoconfigure.application.GreetingClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FeignClientTest @FeignClientTest} with an application that enables its
 * Feign clients in a configuration class.
 *
 * @author Sharang Gupta
 */
@FeignClientTest
class FeignClientTestWithFeignClientsConfigurationTests {

	private static final StubServer server = StubServer.respondingWith("{\"message\":\"Hello Grace\"}");

	@Autowired
	private GreetingClient greetingClient;

	@DynamicPropertySource
	static void greetingsUrl(DynamicPropertyRegistry registry) {
		registry.add("spring.cloud.openfeign.client.config.greetings.url", server::url);
	}

	@AfterAll
	static void stopServer() {
		server.close();
	}

	@Test
	void feignClientsOfTheConfigurationClassAreRegistered() {
		assertThat(this.greetingClient.greet("Grace")).isEqualTo(new Greeting("Hello Grace"));
	}

}
