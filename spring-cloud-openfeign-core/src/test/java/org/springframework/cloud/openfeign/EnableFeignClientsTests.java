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

import feign.Contract;
import feign.Feign;
import feign.Logger;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.optionals.OptionalDecoder;
import feign.slf4j.Slf4jLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Spencer Gibb
 */
@ClassPathExclusions({ "spring-data-commons-*.jar" })
class EnableFeignClientsTests {

	private ConfigurableApplicationContext context;

	@BeforeEach
	void setUp() {
		context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.properties("debug=true", "spring.cloud.openfeign.httpclient.enabled=false")
				.sources(EnableFeignClientsTests.PlainConfiguration.class).run();
	}

	@AfterEach
	void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	void decoderDefaultCorrect() {
		OptionalDecoder.class.cast(this.context.getBeansOfType(Decoder.class).get(0));
	}

	@Test
	void encoderDefaultCorrect() {
		SpringEncoder.class.cast(this.context.getBeansOfType(Encoder.class).get(0));
	}

	@Test
	void loggerDefaultCorrect() {
		Slf4jLogger.class.cast(this.context.getBeansOfType(Logger.class).get(0));
	}

	@Test
	void contractDefaultCorrect() {
		SpringMvcContract.class.cast(this.context.getBeansOfType(Contract.class).get(0));
	}

	@Test
	void builderDefaultCorrect() {
		Feign.Builder.class.cast(this.context.getBeansOfType(Feign.Builder.class).get(0));
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ FeignAutoConfiguration.class })
	protected static class PlainConfiguration {

	}

}
