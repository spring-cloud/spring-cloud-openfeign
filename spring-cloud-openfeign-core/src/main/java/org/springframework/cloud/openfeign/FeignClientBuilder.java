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

package org.springframework.cloud.openfeign;

import feign.hystrix.FallbackFactory;

import org.springframework.context.ApplicationContext;

/**
 * A builder for creating Feign clients without using the {@link FeignClient} annotation.
 * <p>
 * This builder builds the Feign client exactly like it would be created by using the
 * {@link FeignClient} annotation.
 *
 * @author Sven Döring
 */
public class FeignClientBuilder {

	private final ApplicationContext applicationContext;

	public FeignClientBuilder(final ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public <T> Builder<T> forType(final Class<T> type, final String name) {
		return new Builder<>(this.applicationContext, type, name);
	}

	/**
	 * Builder of feign targets.
	 *
	 * @param <T> type of target
	 */
	public static final class Builder<T> {

		private FeignClientFactoryBean feignClientFactoryBean;

		private Builder(final ApplicationContext applicationContext, final Class<T> type,
				final String name) {
			this.feignClientFactoryBean = new FeignClientFactoryBean();

			this.feignClientFactoryBean.setApplicationContext(applicationContext);
			this.feignClientFactoryBean.setType(type);
			this.feignClientFactoryBean.setName(FeignClientsRegistrar.getName(name));
			this.feignClientFactoryBean.setContextId(FeignClientsRegistrar.getName(name));
			// preset default values - these values resemble the default values on the
			// FeignClient annotation
			this.url("").path("").decode404(false);
		}

		public Builder<T> url(final String url) {
			this.feignClientFactoryBean.setUrl(FeignClientsRegistrar.getUrl(url));
			return this;
		}

		public Builder<T> contextId(final String contextId) {
			this.feignClientFactoryBean.setContextId(contextId);
			return this;
		}

		public Builder<T> path(final String path) {
			this.feignClientFactoryBean.setPath(FeignClientsRegistrar.getPath(path));
			return this;
		}

		public Builder<T> decode404(final boolean decode404) {
			this.feignClientFactoryBean.setDecode404(decode404);
			return this;
		}

		public Builder<T> fallback(final Class<? extends T> fallback) {
			FeignClientsRegistrar.validateFallback(fallback);
			this.feignClientFactoryBean.setFallback(fallback);
			return this;
		}

		public Builder<T> fallbackFactory(
				final Class<? extends FallbackFactory<? extends T>> fallbackFactory) {
			FeignClientsRegistrar.validateFallbackFactory(fallbackFactory);
			this.feignClientFactoryBean.setFallbackFactory(fallbackFactory);
			return this;
		}

		/**
		 * @return the created Feign client
		 */
		public T build() {
			return this.feignClientFactoryBean.getTarget();
		}

	}

}
