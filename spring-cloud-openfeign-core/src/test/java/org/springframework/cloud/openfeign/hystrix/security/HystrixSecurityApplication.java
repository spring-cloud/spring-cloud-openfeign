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

package org.springframework.cloud.openfeign.hystrix.security;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.hystrix.security.app.CustomConcurrenyStrategy;
import org.springframework.cloud.openfeign.hystrix.security.app.ProxyUsernameController;
import org.springframework.cloud.openfeign.hystrix.security.app.TestInterceptor;
import org.springframework.cloud.openfeign.hystrix.security.app.UsernameClient;
import org.springframework.cloud.openfeign.hystrix.security.app.UsernameController;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Daniel Lavoie
 */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EnableFeignClients(clients = UsernameClient.class)
@Import(NoSecurityConfiguration.class)
public class HystrixSecurityApplication {

	@Bean
	public CustomConcurrenyStrategy customConcurrenyStrategy() {
		return new CustomConcurrenyStrategy();
	}

	@Bean
	public TestInterceptor testInterceptor() {
		return new TestInterceptor();
	}

	@Bean
	public ProxyUsernameController proxyUsernameController() {
		return new ProxyUsernameController();
	}

	@Bean
	public UsernameController usernameController() {
		return new UsernameController();
	}

}
