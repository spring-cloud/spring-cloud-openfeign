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
import java.util.List;

import feign.Feign;
import feign.Logger;
import feign.RequestInterceptor;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.clientconfig.FeignClientConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author matt king
 */
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FeignClientUsingConfigurerTest.Application.class, value = {
		"feign.client.config.default.loggerLevel=full",
		"feign.client.config.default.requestInterceptors[0]=org.springframework.cloud.openfeign.FeignClientUsingPropertiesTests.FooRequestInterceptor",
		"feign.client.config.default.requestInterceptors[1]=org.springframework.cloud.openfeign.FeignClientUsingPropertiesTests.BarRequestInterceptor" })
public class FeignClientUsingConfigurerTest {

	private static final String BEAN_NAME_PREFIX = "org.springframework.cloud.openfeign.FeignClientUsingConfigurerTest$";

	@Autowired
	private ConfigurableListableBeanFactory beanFactory;

	@Autowired
	private FeignContext context;

	@Test
	public void testFeignClient() {
		FeignClientFactoryBean factoryBean = (FeignClientFactoryBean) beanFactory
				.getBeanDefinition(BEAN_NAME_PREFIX + "TestFeignClient")
				.getAttribute("feignClientsRegistrarFactoryBean");
		Feign.Builder builder = factoryBean.feign(context);

		List<RequestInterceptor> interceptors = (List) getBuilderValue(builder,
				"requestInterceptors");
		assertThat(interceptors.size()).as("interceptors not set").isEqualTo(3);
		assertThat(getBuilderValue(builder, "logLevel")).as("log level not set")
				.isEqualTo(Logger.Level.FULL);
	}

	private Object getBuilderValue(Feign.Builder builder, String member) {
		Field builderField = ReflectionUtils.findField(Feign.Builder.class, member);
		ReflectionUtils.makeAccessible(builderField);

		return ReflectionUtils.getField(builderField, builder);
	}

	@Test
	public void testNoInheritFeignClient() {
		FeignClientFactoryBean factoryBean = (FeignClientFactoryBean) beanFactory
				.getBeanDefinition(BEAN_NAME_PREFIX + "NoInheritFeignClient")
				.getAttribute("feignClientsRegistrarFactoryBean");
		Feign.Builder builder = factoryBean.feign(context);

		List<RequestInterceptor> interceptors = (List) getBuilderValue(builder,
				"requestInterceptors");

		assertThat(interceptors).as("interceptors not set").isEmpty();
		assertThat(factoryBean.isInheritParentContext())
				.as("is inheriting from parent configuration").isFalse();
	}

	@Test
	public void testNoInheritFeignClient_ignoreProperties() {
		FeignClientFactoryBean factoryBean = (FeignClientFactoryBean) beanFactory
				.getBeanDefinition(BEAN_NAME_PREFIX + "NoInheritFeignClient")
				.getAttribute("feignClientsRegistrarFactoryBean");
		Feign.Builder builder = factoryBean.feign(context);

		assertThat(getBuilderValue(builder, "logLevel")).as("log level not set")
				.isEqualTo(Logger.Level.HEADERS);
	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	@EnableFeignClients(clients = { TestFeignClient.class, NoInheritFeignClient.class })
	protected static class Application {

		@Bean
		public RequestInterceptor requestInterceptor() {
			return requestTemplate -> {
			};
		}

	}

	public static class NoInheritConfiguration {

		@Bean
		public Logger.Level logLevel() {
			return Logger.Level.HEADERS;
		}

		@Bean
		public FeignClientConfigurer feignClientConfigurer() {
			return new FeignClientConfigurer() {

				@Override
				public boolean inheritParentConfiguration() {
					return false;
				}
			};

		}

	}

	@FeignClient("testFeignClient")
	interface TestFeignClient {

	}

	@FeignClient(name = "noInheritFeignClient",
			configuration = NoInheritConfiguration.class)
	interface NoInheritFeignClient {

	}

}
