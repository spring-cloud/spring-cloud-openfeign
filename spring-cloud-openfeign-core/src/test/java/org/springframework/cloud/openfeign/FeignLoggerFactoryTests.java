/*
 * Copyright 2016-present the original author or authors.
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

import java.util.List;
import java.util.Map;

import feign.Logger;
import feign.slf4j.Slf4jLogger;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Venil Noronha
 */
class FeignLoggerFactoryTests {

	@Test
	void testDefaultLogger() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SampleConfiguration1.class);
		FeignLoggerFactory loggerFactory = context.getBean(FeignLoggerFactory.class);
		assertThat(loggerFactory).isNotNull();
		Logger logger = loggerFactory.create(Object.class);
		assertThat(logger).isNotNull();
		assertThat(logger instanceof Slf4jLogger).isTrue();
		context.close();
	}

	@Test
	void testCustomLogger() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SampleConfiguration2.class);
		FeignLoggerFactory loggerFactory = context.getBean(FeignLoggerFactory.class);
		assertThat(loggerFactory).isNotNull();
		Logger logger = loggerFactory.create(Object.class);
		assertThat(logger).isNotNull();
		assertThat(logger instanceof LoggerImpl1).isTrue();
		context.close();
	}

	@Test
	void testCustomLoggerFactory() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SampleConfiguration3.class);
		FeignLoggerFactory loggerFactory = context.getBean(FeignLoggerFactory.class);
		assertThat(loggerFactory).isNotNull();
		assertThat(loggerFactory instanceof LoggerFactoryImpl).isTrue();
		Logger logger = loggerFactory.create(Object.class);
		assertThat(logger).isNotNull();
		assertThat(logger instanceof LoggerImpl2).isTrue();
		context.close();
	}

	@Test
	void shouldExcludeSensitiveHeadersCaseInsensitively() {
		FeignClientProperties properties = new FeignClientProperties();
		FeignClientProperties.FeignClientConfiguration clientConfiguration = new FeignClientProperties.FeignClientConfiguration();
		FeignClientProperties.LoggingProperties logging = new FeignClientProperties.LoggingProperties();
		logging.setSensitiveHeaders(List.of("Authorization", "X-API-Key"));
		clientConfiguration.setLogging(logging);
		properties.getConfig().put("inventoryClient", clientConfiguration);

		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources()
			.addFirst(new MapPropertySource("test", Map.of("spring.cloud.openfeign.client.name", "inventoryClient")));

		DefaultFeignLoggerFactory factory = new DefaultFeignLoggerFactory(null, environment, properties);
		Logger logger = factory.create(Object.class);

		assertThat(logger).isInstanceOf(DefaultFeignLoggerFactory.SensitiveHeadersSlf4jLogger.class);

		TestSensitiveHeadersLogger sensitiveLogger = new TestSensitiveHeadersLogger(
				(DefaultFeignLoggerFactory.SensitiveHeadersSlf4jLogger) logger);

		assertThat(sensitiveLogger.shouldLogRequestHeader("Authorization")).isFalse();
		assertThat(sensitiveLogger.shouldLogRequestHeader("authorization")).isFalse();
		assertThat(sensitiveLogger.shouldLogResponseHeader("X-API-Key")).isFalse();
		assertThat(sensitiveLogger.shouldLogResponseHeader("x-api-key")).isFalse();
		assertThat(sensitiveLogger.shouldLogRequestHeader("Content-Type")).isTrue();
	}

	@Test
	void shouldCombineDefaultAndClientSensitiveHeaders() {
		FeignClientProperties properties = new FeignClientProperties();

		FeignClientProperties.FeignClientConfiguration defaultConfiguration = new FeignClientProperties.FeignClientConfiguration();
		FeignClientProperties.LoggingProperties defaultLogging = new FeignClientProperties.LoggingProperties();
		defaultLogging.setSensitiveHeaders(List.of("Authorization"));
		defaultConfiguration.setLogging(defaultLogging);
		properties.getConfig().put("default", defaultConfiguration);

		FeignClientProperties.FeignClientConfiguration clientConfiguration = new FeignClientProperties.FeignClientConfiguration();
		FeignClientProperties.LoggingProperties clientLogging = new FeignClientProperties.LoggingProperties();
		clientLogging.setSensitiveHeaders(List.of("X-API-Key"));
		clientConfiguration.setLogging(clientLogging);
		properties.getConfig().put("inventoryClient", clientConfiguration);

		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources()
			.addFirst(new MapPropertySource("test", Map.of("spring.cloud.openfeign.client.name", "inventoryClient")));

		DefaultFeignLoggerFactory factory = new DefaultFeignLoggerFactory(null, environment, properties);
		Logger logger = factory.create(Object.class);

		assertThat(logger).isInstanceOf(DefaultFeignLoggerFactory.SensitiveHeadersSlf4jLogger.class);

		TestSensitiveHeadersLogger sensitiveLogger = new TestSensitiveHeadersLogger(
				(DefaultFeignLoggerFactory.SensitiveHeadersSlf4jLogger) logger);

		assertThat(sensitiveLogger.shouldLogRequestHeader("Authorization")).isFalse();
		assertThat(sensitiveLogger.shouldLogRequestHeader("X-API-Key")).isFalse();
		assertThat(sensitiveLogger.shouldLogRequestHeader("Content-Type")).isTrue();
	}

	@Test
	void shouldApplyDefaultSensitiveHeadersToAllClients() {
		FeignClientProperties properties = new FeignClientProperties();

		FeignClientProperties.FeignClientConfiguration defaultConfiguration = new FeignClientProperties.FeignClientConfiguration();

		FeignClientProperties.LoggingProperties defaultLogging = new FeignClientProperties.LoggingProperties();

		defaultLogging.setSensitiveHeaders(List.of("Authorization", "Cookie"));
		defaultConfiguration.setLogging(defaultLogging);

		properties.getConfig().put("default", defaultConfiguration);

		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources()
			.addFirst(new MapPropertySource("test", Map.of("spring.cloud.openfeign.client.name", "inventoryClient")));

		DefaultFeignLoggerFactory factory = new DefaultFeignLoggerFactory(null, environment, properties);

		Logger logger = factory.create(Object.class);

		assertThat(logger).isInstanceOf(DefaultFeignLoggerFactory.SensitiveHeadersSlf4jLogger.class);

		TestSensitiveHeadersLogger sensitiveLogger = new TestSensitiveHeadersLogger(
				(DefaultFeignLoggerFactory.SensitiveHeadersSlf4jLogger) logger);

		assertThat(sensitiveLogger.shouldLogRequestHeader("Authorization")).isFalse();
		assertThat(sensitiveLogger.shouldLogResponseHeader("Cookie")).isFalse();
		assertThat(sensitiveLogger.shouldLogRequestHeader("Content-Type")).isTrue();
	}

	@Configuration(proxyBeanMethods = false)
	@Import(FeignClientsConfiguration.class)
	protected static class SampleConfiguration1 {

	}

	@Configuration(proxyBeanMethods = false)
	@Import(FeignClientsConfiguration.class)
	protected static class SampleConfiguration2 {

		@Bean
		public Logger logger() {
			return new LoggerImpl1();
		}

	}

	static class LoggerImpl1 extends Logger {

		@Override
		protected void log(String arg0, String arg1, Object... arg2) {
			// noop
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(FeignClientsConfiguration.class)
	protected static class SampleConfiguration3 {

		@Bean
		public FeignLoggerFactory feignLoggerFactory() {
			return new LoggerFactoryImpl();
		}

	}

	static class LoggerFactoryImpl implements FeignLoggerFactory {

		@Override
		public Logger create(Class<?> type) {
			return new LoggerImpl2();
		}

	}

	static class LoggerImpl2 extends Logger {

		@Override
		protected void log(String arg0, String arg1, Object... arg2) {
			// noop
		}

	}

	static class TestSensitiveHeadersLogger {

		private final DefaultFeignLoggerFactory.SensitiveHeadersSlf4jLogger logger;

		TestSensitiveHeadersLogger(DefaultFeignLoggerFactory.SensitiveHeadersSlf4jLogger logger) {
			this.logger = logger;
		}

		boolean shouldLogRequestHeader(String header) {
			return logger.shouldLogRequestHeader(header);
		}

		boolean shouldLogResponseHeader(String header) {
			return logger.shouldLogResponseHeader(header);
		}

	}

}
