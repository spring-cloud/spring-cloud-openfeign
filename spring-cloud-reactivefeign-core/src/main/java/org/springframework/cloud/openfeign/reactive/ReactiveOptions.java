/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.openfeign.reactive;

/**
 * @author Sergii Karpenko
 */
public class ReactiveOptions {

	private final Integer connectTimeoutMillis;
	private final Integer readTimeoutMillis;
	private final Boolean tryUseCompression;

	private ReactiveOptions(Integer connectTimeoutMillis, Integer readTimeoutMillis,
			Boolean tryUseCompression) {
		this.connectTimeoutMillis = connectTimeoutMillis;
		this.readTimeoutMillis = readTimeoutMillis;
		this.tryUseCompression = tryUseCompression;
	}

	public Integer getConnectTimeoutMillis() {
		return connectTimeoutMillis;
	}

	public Integer getReadTimeoutMillis() {
		return readTimeoutMillis;
	}

	public Boolean isTryUseCompression() {
		return tryUseCompression;
	}

	public boolean isEmpty() {
		return connectTimeoutMillis == null && readTimeoutMillis == null
				&& tryUseCompression == null;
	}

	public static class Builder {
		private Integer connectTimeoutMillis;
		private Integer readTimeoutMillis;
		private Boolean tryUseCompression;

		public Builder() {
		}

		public Builder setConnectTimeoutMillis(int connectTimeoutMillis) {
			this.connectTimeoutMillis = connectTimeoutMillis;
			return this;
		}

		public Builder setReadTimeoutMillis(int readTimeoutMillis) {
			this.readTimeoutMillis = readTimeoutMillis;
			return this;
		}

		public Builder setTryUseCompression(boolean tryUseCompression) {
			this.tryUseCompression = tryUseCompression;
			return this;
		}

		public ReactiveOptions build() {
			return new ReactiveOptions(connectTimeoutMillis, readTimeoutMillis,
					tryUseCompression);
		}
	}
}
