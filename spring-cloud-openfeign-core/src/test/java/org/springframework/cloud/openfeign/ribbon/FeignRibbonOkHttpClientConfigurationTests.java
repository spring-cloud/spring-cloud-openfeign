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

package org.springframework.cloud.openfeign.ribbon;

import java.lang.reflect.Field;

import javax.net.ssl.HostnameVerifier;

import okhttp3.OkHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.commons.httpclient.OkHttpClientFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
		classes = FeignRibbonOkHttpClientConfigurationTests.FeignRibbonOkHttpClientConfigurationTestsApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "debug=true", "feign.httpclient.disableSslValidation=true",
				"feign.okhttp.enabled=true", "feign.httpclient.enabled=false" })
@DirtiesContext
public class FeignRibbonOkHttpClientConfigurationTests {

	@Autowired
	OkHttpClient httpClient;

	@Test
	public void disableSslTest() throws Exception {
		HostnameVerifier hostnameVerifier = (HostnameVerifier) this
				.getField(this.httpClient, "hostnameVerifier");
		assertThat(
				OkHttpClientFactory.TrustAllHostnames.class.isInstance(hostnameVerifier))
						.isTrue();
	}

	protected <T> Object getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return value;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	static class FeignRibbonOkHttpClientConfigurationTestsApplication {

		public static void main(String[] args) {
			new SpringApplicationBuilder(FeignRibbonClientRetryTests.Application.class)
					.run(args);
		}

	}

}
