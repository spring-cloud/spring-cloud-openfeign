/*
 * Copyright 2013-2015 the original author or authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.cloud.openfeign.support.PageableSpringEncoder;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = EnableFeignClientsSpringDataTests.PlainConfiguration.class)
@DirtiesContext
public class EnableFeignClientsSpringDataTests {

	@Autowired
	private FeignContext feignContext;

	@Test
	public void encoderDefaultCorrect() {

		PageableSpringEncoder.class
				.cast(this.feignContext.getInstance("foo", Encoder.class));
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ PropertyPlaceholderAutoConfiguration.class, ArchaiusAutoConfiguration.class,
			FeignAutoConfiguration.class })
	protected static class PlainConfiguration {

	}

}
