/*
 * Copyright 2021-2022 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import feign.Capability;
import feign.Contract;
import feign.ExceptionPropagationPolicy;
import feign.Logger;
import feign.QueryMapEncoder;
import feign.RequestInterceptor;
import feign.ResponseInterceptor;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.openfeign.test.EqualsAndHashCodeAssert.assertEqualsAndHashCodeConsistency;
import static org.springframework.cloud.openfeign.test.EqualsAndHashCodeAssert.assertEqualsConsistency;
import static org.springframework.cloud.openfeign.test.EqualsAndHashCodeAssert.assertEqualsReflexivity;
import static org.springframework.cloud.openfeign.test.EqualsAndHashCodeAssert.assertEqualsSymmetricity;
import static org.springframework.cloud.openfeign.test.EqualsAndHashCodeAssert.assertEqualsTransitivity;
import static org.springframework.cloud.openfeign.test.EqualsAndHashCodeAssert.assertHashCodeConsistency;

/**
 * @author Jonatan Ivanov
 * @author Hyeonmin Park
 * @author Olga Maciaszek-Sharma
 */
class FeignClientConfigurationTests {

	@Test
	void shouldDefaultToValuesWhenFieldsNotSet() {
		FeignClientProperties.FeignClientConfiguration config = new FeignClientProperties.FeignClientConfiguration();

		assertThat(config.getLoggerLevel()).isNull();
		assertThat(config.getConnectTimeout()).isNull();
		assertThat(config.getReadTimeout()).isNull();
		assertThat(config.getRetryer()).isNull();
		assertThat(config.getErrorDecoder()).isNull();
		assertThat(config.getRequestInterceptors()).isNull();
		assertThat(config.getResponseInterceptor()).isNull();
		assertThat(config.getDefaultRequestHeaders()).isNull();
		assertThat(config.getDefaultQueryParameters()).isNull();
		assertThat(config.getDismiss404()).isNull();
		assertThat(config.getDecoder()).isNull();
		assertThat(config.getEncoder()).isNull();
		assertThat(config.getContract()).isNull();
		assertThat(config.getExceptionPropagationPolicy()).isNull();
		assertThat(config.getCapabilities()).isNull();
		assertThat(config.getQueryMapEncoder()).isNull();
		assertThat(config.getMicrometer()).isNull();
	}

	@Test
	void shouldReturnValuesWhenSet() {
		FeignClientProperties.FeignClientConfiguration config = new FeignClientProperties.FeignClientConfiguration();
		config.setLoggerLevel(Logger.Level.FULL);
		config.setConnectTimeout(21);
		config.setReadTimeout(42);
		config.setRetryer(Retryer.class);
		config.setErrorDecoder(ErrorDecoder.class);
		List<Class<RequestInterceptor>> requestInterceptors = Lists.list(RequestInterceptor.class);
		config.setRequestInterceptors(requestInterceptors);
		Class<ResponseInterceptor> responseInterceptor = ResponseInterceptor.class;
		config.setResponseInterceptor(responseInterceptor);
		Map<String, Collection<String>> defaultRequestHeaders = Maps.newHashMap("default", Collections.emptyList());
		config.setDefaultRequestHeaders(defaultRequestHeaders);
		Map<String, Collection<String>> defaultQueryParameters = Maps.newHashMap("default", Collections.emptyList());
		config.setDefaultQueryParameters(defaultQueryParameters);
		config.setDismiss404(true);
		config.setDecoder(Decoder.class);
		config.setEncoder(Encoder.class);
		config.setContract(Contract.class);
		config.setExceptionPropagationPolicy(ExceptionPropagationPolicy.UNWRAP);
		List<Class<Capability>> capabilities = Lists.list(Capability.class);
		config.setCapabilities(capabilities);
		config.setQueryMapEncoder(QueryMapEncoder.class);
		FeignClientProperties.MicrometerProperties micrometer = new FeignClientProperties.MicrometerProperties();
		config.setMicrometer(micrometer);

		assertThat(config.getLoggerLevel()).isSameAs(Logger.Level.FULL);
		assertThat(config.getConnectTimeout()).isEqualTo(21);
		assertThat(config.getReadTimeout()).isEqualTo(42);
		assertThat(config.getRetryer()).isSameAs(Retryer.class);
		assertThat(config.getErrorDecoder()).isSameAs(ErrorDecoder.class);
		assertThat(config.getRequestInterceptors()).isSameAs(requestInterceptors);
		assertThat(config.getResponseInterceptor()).isSameAs(responseInterceptor);
		assertThat(config.getDefaultRequestHeaders()).isSameAs(defaultRequestHeaders);
		assertThat(config.getDefaultQueryParameters()).isSameAs(defaultQueryParameters);
		assertThat(config.getDismiss404()).isTrue();
		assertThat(config.getDecoder()).isSameAs(Decoder.class);
		assertThat(config.getEncoder()).isSameAs(Encoder.class);
		assertThat(config.getContract()).isSameAs(Contract.class);
		assertThat(config.getExceptionPropagationPolicy()).isSameAs(ExceptionPropagationPolicy.UNWRAP);
		assertThat(config.getCapabilities()).isSameAs(capabilities);
		assertThat(config.getQueryMapEncoder()).isSameAs(QueryMapEncoder.class);
		assertThat(config.getMicrometer()).isSameAs(micrometer);
	}

	/**
	 * Sanity-checks equals and hashCode contracts but does not check every variation of
	 * the fields.
	 */
	@Test
	void shouldHaveSomewhatValidEqualsAndHashCode() {
		FeignClientProperties.FeignClientConfiguration configOne = new FeignClientProperties.FeignClientConfiguration();
		FeignClientProperties.FeignClientConfiguration configTwo = new FeignClientProperties.FeignClientConfiguration();
		FeignClientProperties.FeignClientConfiguration configThree = new FeignClientProperties.FeignClientConfiguration();
		FeignClientProperties.FeignClientConfiguration differentConfig = new FeignClientProperties.FeignClientConfiguration();
		differentConfig.setDismiss404(true);

		assertEqualsReflexivity(configOne);

		assertEqualsSymmetricity(configOne, configTwo);
		assertEqualsSymmetricity(configOne, differentConfig);
		assertEqualsSymmetricity(configOne, 42);

		assertEqualsTransitivity(configOne, configTwo, configThree);

		assertEqualsConsistency(configOne, configTwo);
		assertEqualsConsistency(configOne, differentConfig);
		assertEqualsConsistency(configOne, 42);
		assertEqualsConsistency(configOne, null);

		assertHashCodeConsistency(configOne);
		assertEqualsAndHashCodeConsistency(configOne, configTwo);
	}

}
