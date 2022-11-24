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

package org.springframework.cloud.openfeign;

import java.util.Map;

import feign.RequestInterceptor;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.encoding.FeignAcceptGzipEncodingAutoConfiguration;
import org.springframework.cloud.openfeign.encoding.FeignAcceptGzipEncodingInterceptor;
import org.springframework.cloud.openfeign.encoding.FeignContentGzipEncodingAutoConfiguration;
import org.springframework.cloud.openfeign.encoding.FeignContentGzipEncodingInterceptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ryan Baxter
 * @author Biju Kunjummen
 * @author Olga Maciaszek-Sharma
 */
class FeignCompressionTests {

	@Test
	void testInterceptors() {
		new ApplicationContextRunner()
				.withPropertyValues("spring.cloud.openfeign.compression.response.enabled=true",
						"spring.cloud.openfeign.compression.request.enabled=true",
						"spring.cloud.openfeign.okhttp.enabled=false")
				.withConfiguration(AutoConfigurations.of(FeignAutoConfiguration.class,
						FeignContentGzipEncodingAutoConfiguration.class,
						FeignAcceptGzipEncodingAutoConfiguration.class))
				.run(context -> {
					FeignClientFactory feignClientFactory = context.getBean(FeignClientFactory.class);
					Map<String, RequestInterceptor> interceptors = feignClientFactory.getInstances("foo",
							RequestInterceptor.class);
					assertThat(interceptors.size()).isEqualTo(2);
					assertThat(interceptors.get("feignAcceptGzipEncodingInterceptor"))
							.isInstanceOf(FeignAcceptGzipEncodingInterceptor.class);
					assertThat(interceptors.get("feignContentGzipEncodingInterceptor"))
							.isInstanceOf(FeignContentGzipEncodingInterceptor.class);
				});
	}

}
