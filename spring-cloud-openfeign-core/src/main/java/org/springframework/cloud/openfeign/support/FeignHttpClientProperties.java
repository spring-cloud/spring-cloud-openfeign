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

package org.springframework.cloud.openfeign.support;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import feign.okhttp.OkHttpClient;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Ryan Baxter
 * @author Nguyen Ky Thanh
 * @author Olga Maciaszek-Sharma
 */
@ConfigurationProperties(prefix = "spring.cloud.openfeign.httpclient")
public class FeignHttpClientProperties {

	/**
	 * Default value for disabling SSL validation.
	 */
	public static final boolean DEFAULT_DISABLE_SSL_VALIDATION = false;

	/**
	 * Default value for max number od connections.
	 */
	public static final int DEFAULT_MAX_CONNECTIONS = 200;

	/**
	 * Default value for max number od connections per route.
	 */
	public static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 50;

	/**
	 * Default value for time to live.
	 */
	public static final long DEFAULT_TIME_TO_LIVE = 900L;

	/**
	 * Default time to live unit.
	 */
	public static final TimeUnit DEFAULT_TIME_TO_LIVE_UNIT = TimeUnit.SECONDS;

	/**
	 * Default value for following redirects.
	 */
	public static final boolean DEFAULT_FOLLOW_REDIRECTS = true;

	/**
	 * Default value for connection timeout.
	 */
	public static final int DEFAULT_CONNECTION_TIMEOUT = 2000;

	/**
	 * Default value for connection timer repeat.
	 */
	public static final int DEFAULT_CONNECTION_TIMER_REPEAT = 3000;

	private boolean disableSslValidation = DEFAULT_DISABLE_SSL_VALIDATION;

	private int maxConnections = DEFAULT_MAX_CONNECTIONS;

	private int maxConnectionsPerRoute = DEFAULT_MAX_CONNECTIONS_PER_ROUTE;

	private long timeToLive = DEFAULT_TIME_TO_LIVE;

	private TimeUnit timeToLiveUnit = DEFAULT_TIME_TO_LIVE_UNIT;

	private boolean followRedirects = DEFAULT_FOLLOW_REDIRECTS;

	private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

	private int connectionTimerRepeat = DEFAULT_CONNECTION_TIMER_REPEAT;

	/**
	 * Apache HttpClient5 additional properties.
	 */
	private Hc5Properties hc5 = new Hc5Properties();

	/**
	 * Additional {@link OkHttpClient}-specific properties.
	 */
	private OkHttp okHttp = new OkHttp();

	public int getConnectionTimerRepeat() {
		return connectionTimerRepeat;
	}

	public void setConnectionTimerRepeat(int connectionTimerRepeat) {
		this.connectionTimerRepeat = connectionTimerRepeat;
	}

	public boolean isDisableSslValidation() {
		return disableSslValidation;
	}

	public void setDisableSslValidation(boolean disableSslValidation) {
		this.disableSslValidation = disableSslValidation;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	public int getMaxConnectionsPerRoute() {
		return maxConnectionsPerRoute;
	}

	public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
		this.maxConnectionsPerRoute = maxConnectionsPerRoute;
	}

	public long getTimeToLive() {
		return timeToLive;
	}

	public void setTimeToLive(long timeToLive) {
		this.timeToLive = timeToLive;
	}

	public TimeUnit getTimeToLiveUnit() {
		return timeToLiveUnit;
	}

	public void setTimeToLiveUnit(TimeUnit timeToLiveUnit) {
		this.timeToLiveUnit = timeToLiveUnit;
	}

	public boolean isFollowRedirects() {
		return followRedirects;
	}

	public void setFollowRedirects(boolean followRedirects) {
		this.followRedirects = followRedirects;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public Hc5Properties getHc5() {
		return hc5;
	}

	public void setHc5(Hc5Properties hc5) {
		this.hc5 = hc5;
	}

	public OkHttp getOkHttp() {
		return okHttp;
	}

	public void setOkHttp(OkHttp okHttp) {
		this.okHttp = okHttp;
	}

	public static class Hc5Properties {

		/**
		 * Default value for pool concurrency policy.
		 */
		public static final PoolConcurrencyPolicy DEFAULT_POOL_CONCURRENCY_POLICY = PoolConcurrencyPolicy.STRICT;

		/**
		 * Default value for pool reuse policy.
		 */
		public static final PoolReusePolicy DEFAULT_POOL_REUSE_POLICY = PoolReusePolicy.FIFO;

		/**
		 * Default value for socket timeout.
		 */
		public static final int DEFAULT_SOCKET_TIMEOUT = 5;

		/**
		 * Default value for socket timeout unit.
		 */
		public static final TimeUnit DEFAULT_SOCKET_TIMEOUT_UNIT = TimeUnit.SECONDS;

		/**
		 * Pool concurrency policies.
		 */
		private PoolConcurrencyPolicy poolConcurrencyPolicy = DEFAULT_POOL_CONCURRENCY_POLICY;

		/**
		 * Pool connection re-use policies.
		 */
		private PoolReusePolicy poolReusePolicy = DEFAULT_POOL_REUSE_POLICY;

		/**
		 * Default value for socket timeout.
		 */
		private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;

		/**
		 * Default value for socket timeout unit.
		 */
		private TimeUnit socketTimeoutUnit = DEFAULT_SOCKET_TIMEOUT_UNIT;

		public PoolConcurrencyPolicy getPoolConcurrencyPolicy() {
			return poolConcurrencyPolicy;
		}

		public void setPoolConcurrencyPolicy(PoolConcurrencyPolicy poolConcurrencyPolicy) {
			this.poolConcurrencyPolicy = poolConcurrencyPolicy;
		}

		public PoolReusePolicy getPoolReusePolicy() {
			return poolReusePolicy;
		}

		public void setPoolReusePolicy(PoolReusePolicy poolReusePolicy) {
			this.poolReusePolicy = poolReusePolicy;
		}

		public TimeUnit getSocketTimeoutUnit() {
			return socketTimeoutUnit;
		}

		public void setSocketTimeoutUnit(TimeUnit socketTimeoutUnit) {
			this.socketTimeoutUnit = socketTimeoutUnit;
		}

		public int getSocketTimeout() {
			return socketTimeout;
		}

		public void setSocketTimeout(int socketTimeout) {
			this.socketTimeout = socketTimeout;
		}

		/**
		 * Enumeration of pool concurrency policies.
		 */
		public enum PoolConcurrencyPolicy {

			/**
			 * Higher concurrency but with lax connection max limit guarantees.
			 */
			LAX,

			/**
			 * Strict connection max limit guarantees.
			 */
			STRICT

		}

		/**
		 * Enumeration of pooled connection re-use policies.
		 */
		public enum PoolReusePolicy {

			/**
			 * Re-use as few connections as possible making it possible for connections to
			 * become idle and expire.
			 */
			LIFO,

			/**
			 * Re-use all connections equally preventing them from becoming idle and
			 * expiring.
			 */
			FIFO

		}

	}

	/**
	 * {@link OkHttpClient}-specific properties.
	 */
	public static class OkHttp {

		/**
		 * {@link OkHttpClient} read timeout; defaults to 60 seconds.
		 */
		private Duration readTimeout = Duration.ofSeconds(60);

		public Duration getReadTimeout() {
			return readTimeout;
		}

		public void setReadTimeout(Duration readTimeout) {
			this.readTimeout = readTimeout;
		}

	}

}
