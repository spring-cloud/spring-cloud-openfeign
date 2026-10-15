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

package org.springframework.cloud.openfeign.test.autoconfigure.application;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cloud.openfeign.test.autoconfigure.AutoConfigureFeign;
import org.springframework.cloud.openfeign.test.autoconfigure.StubServer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigureFeign @AutoConfigureFeign} in a
 * {@link WebMvcTest @WebMvcTest}.
 *
 * @author Sharang Gupta
 */
@WebMvcTest(GreetingController.class)
@AutoConfigureFeign
class AutoConfigureFeignTests {

	private static final StubServer server = StubServer.respondingWith("{\"message\":\"Hello Ada\"}");

	@Autowired
	private MockMvcTester mvc;

	@DynamicPropertySource
	static void greetingsUrl(DynamicPropertyRegistry registry) {
		registry.add("spring.cloud.openfeign.client.config.greetings.url", server::url);
	}

	@AfterAll
	static void stopServer() {
		server.close();
	}

	@Test
	void controllerUsesTheFeignClient() {
		assertThat(this.mvc.get().uri("/hello/Ada")).hasStatusOk().hasBodyTextEqualTo("Hello Ada");
	}

}
