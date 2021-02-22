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

import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;

/**
 * @author Nguyen Ky Thanh
 */
public class DefaultApacheAsyncHttpClientConnectionManagerFactory
		implements ApacheAsyncHttpClientConnectionManagerFactory {

	public AsyncClientConnectionManager newConnectionManager(int maxTotalConnections, int maxConnectionsPerRoute,
			long timeToLive, TimeUnit timeUnit) {
		return newConnectionManager(PoolConcurrencyPolicy.STRICT, maxTotalConnections, maxConnectionsPerRoute,
				timeToLive, timeUnit);
	}

	@Override
	public AsyncClientConnectionManager newConnectionManager(PoolConcurrencyPolicy poolConcurrencyPolicy,
			int maxTotalConnections, int maxConnectionsPerRoute, long timeToLive, TimeUnit timeUnit) {
		return PoolingAsyncClientConnectionManagerBuilder.create().setMaxConnTotal(maxTotalConnections)
				.setMaxConnPerRoute(maxConnectionsPerRoute)
				.setTlsStrategy(ClientTlsStrategyBuilder.create().setSslContext(SSLContexts.createSystemDefault())
						.setTlsVersions(TLS.V_1_3, TLS.V_1_2).build())
				.setPoolConcurrencyPolicy(poolConcurrencyPolicy).setConnPoolPolicy(PoolReusePolicy.LIFO)
				.setConnectionTimeToLive(TimeValue.of(timeToLive, timeUnit)).build();
	}

}
