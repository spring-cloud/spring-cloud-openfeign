/*
 * Copyright 2013-2021 the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import feign.Contract;
import feign.ExceptionPropagationPolicy;
import feign.Logger;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Eko Kurniawan Khannedy
 * @author Ilia Ilinykh
 * @author Ram Anaswara
 * @author Olga Maciaszek-Sharma
 */
@ConfigurationProperties("feign.client")
public class FeignClientProperties {

	private boolean defaultToProperties = true;

	private String defaultConfig = "default";

	private Map<String, FeignClientConfiguration> config = new HashMap<>();

	/**
	 * Feign clients do not encode slash `/` characters by default. To change this
	 * behavior, set the `decodeSlash` to `false`.
	 */
	private boolean decodeSlash = true;

	public boolean isDefaultToProperties() {
		return defaultToProperties;
	}

	public void setDefaultToProperties(boolean defaultToProperties) {
		this.defaultToProperties = defaultToProperties;
	}

	public String getDefaultConfig() {
		return defaultConfig;
	}

	public void setDefaultConfig(String defaultConfig) {
		this.defaultConfig = defaultConfig;
	}

	public Map<String, FeignClientConfiguration> getConfig() {
		return config;
	}

	public void setConfig(Map<String, FeignClientConfiguration> config) {
		this.config = config;
	}

	public boolean isDecodeSlash() {
		return decodeSlash;
	}

	public void setDecodeSlash(boolean decodeSlash) {
		this.decodeSlash = decodeSlash;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		FeignClientProperties that = (FeignClientProperties) o;
		return defaultToProperties == that.defaultToProperties
				&& Objects.equals(defaultConfig, that.defaultConfig)
				&& Objects.equals(config, that.config)
				&& Objects.equals(decodeSlash, that.decodeSlash);
	}

	@Override
	public int hashCode() {
		return Objects.hash(defaultToProperties, defaultConfig, config, decodeSlash);
	}

	/**
	 * Feign client configuration.
	 */
	public static class FeignClientConfiguration {

		private Logger.Level loggerLevel;

		private Integer connectTimeout;

		private Integer readTimeout;

		private Class<Retryer> retryer;

		private Class<ErrorDecoder> errorDecoder;

		private List<Class<RequestInterceptor>> requestInterceptors;

		private Map<String, Collection<String>> defaultRequestHeaders;

		private Map<String, Collection<String>> defaultQueryParameters;

		private Boolean decode404;

		private Class<Decoder> decoder;

		private Class<Encoder> encoder;

		private Class<Contract> contract;

		private ExceptionPropagationPolicy exceptionPropagationPolicy;

		private Boolean followRedirects;

		public Logger.Level getLoggerLevel() {
			return loggerLevel;
		}

		public void setLoggerLevel(Logger.Level loggerLevel) {
			this.loggerLevel = loggerLevel;
		}

		public Integer getConnectTimeout() {
			return connectTimeout;
		}

		public void setConnectTimeout(Integer connectTimeout) {
			this.connectTimeout = connectTimeout;
		}

		public Integer getReadTimeout() {
			return readTimeout;
		}

		public void setReadTimeout(Integer readTimeout) {
			this.readTimeout = readTimeout;
		}

		public Class<Retryer> getRetryer() {
			return retryer;
		}

		public void setRetryer(Class<Retryer> retryer) {
			this.retryer = retryer;
		}

		public Class<ErrorDecoder> getErrorDecoder() {
			return errorDecoder;
		}

		public void setErrorDecoder(Class<ErrorDecoder> errorDecoder) {
			this.errorDecoder = errorDecoder;
		}

		public List<Class<RequestInterceptor>> getRequestInterceptors() {
			return requestInterceptors;
		}

		public void setRequestInterceptors(
				List<Class<RequestInterceptor>> requestInterceptors) {
			this.requestInterceptors = requestInterceptors;
		}

		public Map<String, Collection<String>> getDefaultRequestHeaders() {
			return defaultRequestHeaders;
		}

		public void setDefaultRequestHeaders(
				Map<String, Collection<String>> defaultRequestHeaders) {
			this.defaultRequestHeaders = defaultRequestHeaders;
		}

		public Map<String, Collection<String>> getDefaultQueryParameters() {
			return defaultQueryParameters;
		}

		public void setDefaultQueryParameters(
				Map<String, Collection<String>> defaultQueryParameters) {
			this.defaultQueryParameters = defaultQueryParameters;
		}

		public Boolean getDecode404() {
			return decode404;
		}

		public void setDecode404(Boolean decode404) {
			this.decode404 = decode404;
		}

		public Class<Decoder> getDecoder() {
			return decoder;
		}

		public void setDecoder(Class<Decoder> decoder) {
			this.decoder = decoder;
		}

		public Class<Encoder> getEncoder() {
			return encoder;
		}

		public void setEncoder(Class<Encoder> encoder) {
			this.encoder = encoder;
		}

		public Class<Contract> getContract() {
			return contract;
		}

		public void setContract(Class<Contract> contract) {
			this.contract = contract;
		}

		public ExceptionPropagationPolicy getExceptionPropagationPolicy() {
			return exceptionPropagationPolicy;
		}

		public void setExceptionPropagationPolicy(
				ExceptionPropagationPolicy exceptionPropagationPolicy) {
			this.exceptionPropagationPolicy = exceptionPropagationPolicy;
		}

		public Boolean isFollowRedirects() {
			return followRedirects;
		}

		public void setFollowRedirects(Boolean followRedirects) {
			this.followRedirects = followRedirects;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			FeignClientConfiguration that = (FeignClientConfiguration) o;
			return loggerLevel == that.loggerLevel
					&& Objects.equals(connectTimeout, that.connectTimeout)
					&& Objects.equals(readTimeout, that.readTimeout)
					&& Objects.equals(retryer, that.retryer)
					&& Objects.equals(errorDecoder, that.errorDecoder)
					&& Objects.equals(requestInterceptors, that.requestInterceptors)
					&& Objects.equals(decode404, that.decode404)
					&& Objects.equals(encoder, that.encoder)
					&& Objects.equals(decoder, that.decoder)
					&& Objects.equals(contract, that.contract)
					&& Objects.equals(exceptionPropagationPolicy,
							that.exceptionPropagationPolicy)
					&& Objects.equals(defaultRequestHeaders, that.defaultRequestHeaders)
					&& Objects.equals(defaultQueryParameters, that.defaultQueryParameters)
					&& Objects.equals(followRedirects, that.followRedirects);
		}

		@Override
		public int hashCode() {
			return Objects.hash(loggerLevel, connectTimeout, readTimeout, retryer,
					errorDecoder, requestInterceptors, decode404, encoder, decoder,
					contract, exceptionPropagationPolicy, defaultQueryParameters,
					defaultRequestHeaders, followRedirects);
		}

	}

}
