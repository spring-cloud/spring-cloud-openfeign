/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.openfeign.httpclient;

import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;

/**
 * @author Nguyen Ky Thanh
 */
public interface ApacheAsyncHttpClientConnectionManagerFactory {

	/**
	 * Creates a new {@link AsyncClientConnectionManager}.
	 * @param maxTotalConnections The total number of connections.
	 * @param maxConnectionsPerRoute The total number of connections per route.
	 * @param timeToLive The time a connection is allowed to exist.
	 * @param timeUnit The time unit for the time-to-live value.
	 * @return A new {@link AsyncClientConnectionManager}.
	 */
	AsyncClientConnectionManager newConnectionManager(PoolConcurrencyPolicy poolConcurrencyPolicy,
			int maxTotalConnections, int maxConnectionsPerRoute, long timeToLive, TimeUnit timeUnit);

}
