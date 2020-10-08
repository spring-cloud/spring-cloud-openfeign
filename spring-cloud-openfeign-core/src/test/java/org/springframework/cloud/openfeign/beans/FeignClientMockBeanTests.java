/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.openfeign.beans;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(classes = FeignClientMockBeanTests.Config.class)
public class FeignClientMockBeanTests {

	@MockBean
	private RandomClient randomClient;

	@Autowired
	private TestService testService;

	@Test
	public void randomClientShouldBeMocked() {
		String mockMessage = "Mocked Feign Client";
		when(randomClient.getRandomString()).thenReturn(mockMessage);

		String returnedMessage = testService.testMethod();

		assertThat(returnedMessage).isEqualTo(mockMessage);
	}

	@FeignClient("random-test")
	protected interface RandomClient {

		@GetMapping("/random-test")
		String getRandomString();

	}

	@Configuration
	protected static class Config {

		@Bean
		TestService testService() {
			return new TestService();
		}

	}

}

class TestService {

	@Autowired
	private FeignClientMockBeanTests.RandomClient randomClient;

	public String testMethod() {
		return randomClient.getRandomString();
	}

}
