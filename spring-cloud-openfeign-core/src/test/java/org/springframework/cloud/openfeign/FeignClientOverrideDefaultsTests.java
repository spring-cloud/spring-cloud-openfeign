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
import java.util.concurrent.TimeUnit;

import feign.Capability;
import feign.Contract;
import feign.ExceptionPropagationPolicy;
import feign.Logger;
import feign.QueryMapEncoder;
import feign.Request;
import feign.RequestInterceptor;
import feign.RequestLine;
import feign.Retryer;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.micrometer.MicrometerCapability;
import feign.micrometer.MicrometerObservationCapability;
import feign.optionals.OptionalDecoder;
import feign.querymap.BeanQueryMapEncoder;
import feign.querymap.FieldQueryMapEncoder;
import feign.slf4j.Slf4jLogger;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.support.PageableSpringEncoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.GetMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author Spencer Gibb
 * @author Jonatan Ivanov
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(classes = FeignClientOverrideDefaultsTests.TestConfiguration.class)
@DirtiesContext
class FeignClientOverrideDefaultsTests {

	@Autowired
	private FeignClientFactory context;

	@Autowired
	private FooClient foo;

	@Autowired
	private BarClient bar;

	@Test
	void clientsAvailable() {
		assertThat(foo).isNotNull();
		assertThat(bar).isNotNull();
	}

	@Test
	void overrideDecoder() {
		Decoder.Default.class.cast(context.getInstance("foo", Decoder.class));
		OptionalDecoder.class.cast(context.getInstance("bar", Decoder.class));
	}

	@Test
	void overrideEncoder() {
		Encoder.Default.class.cast(context.getInstance("foo", Encoder.class));
		PageableSpringEncoder.class.cast(context.getInstance("bar", Encoder.class));
	}

	@Test
	void overrideLogger() {
		Logger.JavaLogger.class.cast(context.getInstance("foo", Logger.class));
		Slf4jLogger.class.cast(context.getInstance("bar", Logger.class));
	}

	@Test
	void overrideContract() {
		Contract.Default.class.cast(context.getInstance("foo", Contract.class));
		SpringMvcContract.class.cast(context.getInstance("bar", Contract.class));
	}

	@Test
	void overrideLoggerLevel() {
		assertThat(context.getInstance("foo", Logger.Level.class)).isNull();
		assertThat(context.getInstance("bar", Logger.Level.class)).isEqualTo(Logger.Level.HEADERS);
	}

	@Test
	void overrideRetryer() {
		assertThat(context.getInstance("foo", Retryer.class)).isEqualTo(Retryer.NEVER_RETRY);
		Retryer.Default.class.cast(context.getInstance("bar", Retryer.class));
	}

	@Test
	void overrideErrorDecoder() {
		assertThat(context.getInstance("foo", ErrorDecoder.class)).isNull();
		ErrorDecoder.Default.class.cast(context.getInstance("bar", ErrorDecoder.class));
	}

	@Test
	void overrideRequestOptions() {
		assertThat(context.getInstance("foo", Request.Options.class)).isNull();
		Request.Options options = context.getInstance("bar", Request.Options.class);
		assertThat(options.connectTimeoutMillis()).isEqualTo(1);
		assertThat(options.readTimeoutMillis()).isEqualTo(1);
		assertThat(options.isFollowRedirects()).isFalse();
	}

	@Test
	void overrideQueryMapEncoder() {
		assertThatCode(() -> {
			FieldQueryMapEncoder.class.cast(context.getInstance("foo", QueryMapEncoder.class));
			BeanQueryMapEncoder.class.cast(context.getInstance("bar", QueryMapEncoder.class));
		}).doesNotThrowAnyException();
	}

	@Test
	void addRequestInterceptor() {
		assertThat(context.getInstances("foo", RequestInterceptor.class).size()).isEqualTo(1);
		assertThat(context.getInstances("bar", RequestInterceptor.class).size()).isEqualTo(2);
	}

	@Test
	void exceptionPropagationPolicy() {
		assertThat(context.getInstances("foo", ExceptionPropagationPolicy.class)).isEmpty();
		assertThat(context.getInstances("bar", ExceptionPropagationPolicy.class))
				.containsValues(ExceptionPropagationPolicy.UNWRAP);
	}

	@Test
	void shouldOverrideMicrometerCapabilities() {
		// override micrometerCapability
		assertThat(context.getInstance("foo", MicrometerCapability.class))
				.isExactlyInstanceOf(TestMicrometerCapability.class);
		assertThat(context.getInstance("foo", MicrometerObservationCapability.class))
				.isExactlyInstanceOf(MicrometerObservationCapability.class);
		Map<String, Capability> fooCapabilities = context.getInstances("foo", Capability.class);
		assertThat(fooCapabilities).hasSize(2);
		assertThat(fooCapabilities.get("micrometerCapability")).isExactlyInstanceOf(TestMicrometerCapability.class);
		assertThat(fooCapabilities.get("micrometerObservationCapability"))
				.isExactlyInstanceOf(MicrometerObservationCapability.class);

		// override micrometerObservationCapability
		assertThat(context.getInstance("bar", MicrometerObservationCapability.class))
				.isExactlyInstanceOf(TestMicrometerObservationCapability.class);
		Map<String, Capability> barCapabilities = context.getInstances("bar", Capability.class);
		assertThat(barCapabilities).hasSize(1);
		assertThat(barCapabilities.get("micrometerCapability")).isNull();
		assertThat(barCapabilities.get("micrometerObservationCapability"))
				.isExactlyInstanceOf(TestMicrometerObservationCapability.class);

		// override both + an extra capability
		assertThat(context.getInstance("baz", MicrometerCapability.class))
				.isExactlyInstanceOf(TestMicrometerCapability.class);
		assertThat(context.getInstance("baz", MicrometerObservationCapability.class))
				.isExactlyInstanceOf(TestMicrometerObservationCapability.class);
		Map<String, Capability> bazCapabilities = context.getInstances("baz", Capability.class);
		assertThat(bazCapabilities).hasSize(3);
		assertThat(bazCapabilities.get("micrometerCapability")).isExactlyInstanceOf(TestMicrometerCapability.class);
		assertThat(bazCapabilities.get("micrometerObservationCapability"))
				.isExactlyInstanceOf(TestMicrometerObservationCapability.class);
		assertThat(bazCapabilities.get("noOpCapability")).isExactlyInstanceOf(NoOpCapability.class);
	}

	@FeignClient(name = "foo", url = "https://foo", configuration = FooConfiguration.class)
	interface FooClient {

		@RequestLine("GET /")
		String get();

	}

	@FeignClient(name = "bar", url = "https://bar", configuration = BarConfiguration.class)
	interface BarClient {

		@GetMapping("/")
		String get();

	}

	@FeignClient(name = "baz", url = "https://baz", configuration = BazConfiguration.class)
	interface BazClient {

		@GetMapping("/baz")
		String get();

	}

	@Configuration(proxyBeanMethods = false)
	@EnableFeignClients(clients = { FooClient.class, BarClient.class, BazClient.class })
	@EnableAutoConfiguration
	protected static class TestConfiguration {

		@Bean
		RequestInterceptor defaultRequestInterceptor() {
			return template -> {
			};
		}

	}

	static class FooConfiguration {

		@Bean
		Decoder feignDecoder() {
			return new Decoder.Default();
		}

		@Bean
		Encoder feignEncoder() {
			return new Encoder.Default();
		}

		@Bean
		Logger feignLogger() {
			return new Logger.JavaLogger(FooConfiguration.class);
		}

		@Bean
		Contract feignContract() {
			return new Contract.Default();
		}

		@Bean
		QueryMapEncoder queryMapEncoder() {
			return new FieldQueryMapEncoder();
		}

		@Bean
		MicrometerCapability micrometerCapability() {
			return new TestMicrometerCapability();
		}

	}

	static class BarConfiguration {

		@Bean
		Logger.Level feignLevel() {
			return Logger.Level.HEADERS;
		}

		@Bean
		Retryer feignRetryer() {
			return new Retryer.Default();
		}

		@Bean
		ErrorDecoder feignErrorDecoder() {
			return new ErrorDecoder.Default();
		}

		@Bean
		Request.Options feignRequestOptions() {
			return new Request.Options(1, TimeUnit.MILLISECONDS, 1, TimeUnit.MILLISECONDS, false);
		}

		@Bean
		RequestInterceptor feignRequestInterceptor() {
			return new BasicAuthRequestInterceptor("user", "pass");
		}

		@Bean
		QueryMapEncoder queryMapEncoder() {
			return new BeanQueryMapEncoder();
		}

		@Bean
		ExceptionPropagationPolicy exceptionPropagationPolicy() {
			return ExceptionPropagationPolicy.UNWRAP;
		}

		@Bean
		MicrometerObservationCapability micrometerObservationCapability() {
			return new TestMicrometerObservationCapability();
		}

	}

	static class BazConfiguration {

		@Bean
		MicrometerObservationCapability micrometerObservationCapability() {
			return new TestMicrometerObservationCapability();
		}

		@Bean
		MicrometerCapability micrometerCapability() {
			return new TestMicrometerCapability();
		}

		@Bean
		Capability noOpCapability() {
			return new NoOpCapability();
		}

	}

	private static class TestMicrometerObservationCapability extends feign.micrometer.MicrometerObservationCapability {

		TestMicrometerObservationCapability() {
			super(null);
		}

	}

	private static class TestMicrometerCapability extends feign.micrometer.MicrometerCapability {

	}

	private static class NoOpCapability implements Capability {

	}

}
