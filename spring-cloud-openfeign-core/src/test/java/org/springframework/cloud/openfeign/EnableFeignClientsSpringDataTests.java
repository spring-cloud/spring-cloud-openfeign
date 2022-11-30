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

import feign.codec.Encoder;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.support.PageableSpringEncoder;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

/**
 * @author Spencer Gibb
 */
@SpringBootTest(classes = EnableFeignClientsSpringDataTests.PlainConfiguration.class)
@DirtiesContext
class EnableFeignClientsSpringDataTests {

	@Autowired
	private FeignClientFactory feignClientFactory;

	@Test
	void encoderDefaultCorrect() {
		PageableSpringEncoder.class.cast(this.feignClientFactory.getInstance("foo", Encoder.class));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	protected static class PlainConfiguration {

	}

}
