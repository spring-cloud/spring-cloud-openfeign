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

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.util.TimeValue;
import org.junit.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Nguyen Ky Thanh
 */
public class DefaultApacheAsyncHttpClientConnectionManagerFactoryTests {

	@Test
	public void newConnectionManager() {
		AsyncClientConnectionManager connectionManager = new DefaultApacheAsyncHttpClientConnectionManagerFactory()
				.newConnectionManager(10, 2, 30, TimeUnit.SECONDS);
		assertThat(connectionManager).isInstanceOf(PoolingAsyncClientConnectionManager.class);
		PoolingAsyncClientConnectionManager poolingAsyncClientConnectionManager = (PoolingAsyncClientConnectionManager) connectionManager;

		assertThat(poolingAsyncClientConnectionManager.getMaxTotal()).isEqualTo(10);
		assertThat(poolingAsyncClientConnectionManager.getDefaultMaxPerRoute()).isEqualTo(2);

		Object pool = getField((connectionManager), "pool");
		then((TimeValue) getField(pool, "timeToLive")).isEqualTo(TimeValue.of(30, TimeUnit.SECONDS));
	}

	@Test
	public void newConnectionManagerWithCustomPoolConcurrencyPolicy() {
		AsyncClientConnectionManager connectionManager = new DefaultApacheAsyncHttpClientConnectionManagerFactory()
				.newConnectionManager(PoolConcurrencyPolicy.LAX, 10, 2, 30, TimeUnit.SECONDS);
		assertThat(connectionManager).isInstanceOf(PoolingAsyncClientConnectionManager.class);
		PoolingAsyncClientConnectionManager poolingAsyncClientConnectionManager = (PoolingAsyncClientConnectionManager) connectionManager;

		assertThat(poolingAsyncClientConnectionManager.getMaxTotal()).isEqualTo(0);

		assertThat(poolingAsyncClientConnectionManager.getDefaultMaxPerRoute()).isEqualTo(2);

		Object pool = getField((connectionManager), "pool");
		then((TimeValue) getField(pool, "timeToLive")).isEqualTo(TimeValue.of(30, TimeUnit.SECONDS));
	}

	@SuppressWarnings("unchecked")
	protected <T> T getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		if (field == null) {
			throw new IllegalArgumentException("Can not find field " + name + " in " + target.getClass());
		}
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return (T) value;
	}

}
