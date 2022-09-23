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

package org.springframework.cloud.openfeign.security;

import feign.Request;
import feign.RequestTemplate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Dangzhicairang(小水牛)
 */
@SpringBootTest(classes = OAuth2AccessTokenInterceptorAutoconfigureTests.Application.class,
		webEnvironment = RANDOM_PORT,
		value = { "spring.cloud.openfeign.oauth2.enabled=true", "spring.cloud.openfeign.oauth2.specifiedClientIds=feign-client",
				"spring.security.oauth2.client.registration.feign-client.provider=google",
				"spring.security.oauth2.client.registration.feign-client.client-id=feign-client",
				"spring.security.oauth2.client.registration.feign-client.client-name=feign-client-name",
				"spring.security.oauth2.client.registration.feign-client.client-secret=secret",
				"spring.security.oauth2.client.registration.feign-client.authorization-grant-type=client_credentials",
				"spring.security.oauth2.client.registration.feign-client.redirect-uri=feign-client-url",
				"spring.security.oauth2.client.registration.feign-client.scope=openid,feign"
		})
@DirtiesContext
public class OAuth2AccessTokenInterceptorAutoconfigureTests {

	@Autowired
	private ConfigurableApplicationContext applicationContext;

	@Test
	void testAutoconfigure() {
		AssertableApplicationContext assertableContext = AssertableApplicationContext.get(() -> applicationContext);
		assertThat(assertableContext).hasSingleBean(OAuth2AccessTokenInterceptor.class);

		OAuth2AccessTokenInterceptor bean = applicationContext.getBean(OAuth2AccessTokenInterceptor.class);
		RequestTemplate requestTemplate = new RequestTemplate().method(Request.HttpMethod.GET);
		Assertions.assertThatExceptionOfType(IllegalStateException.class)
			.isThrownBy(() -> bean.apply(requestTemplate)).withMessage(
				"No token acquired, which is illegal according to the contract.");

		then(bean).should(times(1)).getToken("feign-client");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	protected static class Application {
	}

}
