/*
 * Copyright 2020-2022 the original author or authors.
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

import feign.Response;
import feign.codec.ErrorDecoder;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael Cramer
 */
class FeignErrorDecoderFactoryTests {

	@Test
	void testNoDefaultFactory() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SampleConfiguration1.class);
		String[] beanNamesForType = context.getBeanNamesForType(FeignErrorDecoderFactory.class);
		assertThat(beanNamesForType).isEmpty();
		context.close();
	}

	@Test
	void testCustomErrorDecoderFactory() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SampleConfiguration2.class);
		FeignErrorDecoderFactory errorDecoderFactory = context.getBean(FeignErrorDecoderFactory.class);
		assertThat(errorDecoderFactory).isNotNull();
		ErrorDecoder errorDecoder = errorDecoderFactory.create(Object.class);
		assertThat(errorDecoder).isNotNull();
		assertThat(errorDecoder instanceof ErrorDecoderImpl).isTrue();
		context.close();
	}

	@Test
	void testCustomErrorDecoderFactoryNotOverwritingErrorDecoder() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SampleConfiguration3.class);
		FeignErrorDecoderFactory errorDecoderFactory = context.getBean(FeignErrorDecoderFactory.class);
		assertThat(errorDecoderFactory).isNotNull();
		ErrorDecoder errorDecoderFromFactory = errorDecoderFactory.create(Object.class);
		assertThat(errorDecoderFromFactory).isNotNull();
		assertThat(errorDecoderFromFactory instanceof ErrorDecoderImpl).isTrue();
		ErrorDecoder errorDecoder = context.getBean(ErrorDecoder.class);
		assertThat(errorDecoder).isNotNull();
		assertThat(errorDecoder instanceof ErrorDecoder.Default).isTrue();
		context.close();
	}

	@Configuration
	@Import(FeignClientsConfiguration.class)
	protected static class SampleConfiguration1 {

	}

	@Configuration
	@Import(FeignClientsConfiguration.class)
	protected static class SampleConfiguration2 {

		@Bean
		public FeignErrorDecoderFactory errorDecoderFactory() {
			return new ErrorDecoderFactoryImpl();
		}

	}

	@Configuration
	@Import(FeignClientsConfiguration.class)
	protected static class SampleConfiguration3 {

		@Bean
		public FeignErrorDecoderFactory errorDecoderFactory() {
			return new ErrorDecoderFactoryImpl();
		}

		@Bean
		public ErrorDecoder errorDecoder() {
			return new ErrorDecoder.Default();
		}

	}

	static class ErrorDecoderFactoryImpl implements FeignErrorDecoderFactory {

		@Override
		public ErrorDecoder create(final Class<?> type) {
			return new ErrorDecoderImpl();
		}

	}

	static class ErrorDecoderImpl implements ErrorDecoder {

		@Override
		public Exception decode(final String methodKey, final Response response) {
			return null;
		}

	}

}
