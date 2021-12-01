/*
 * Copyright 2015-2021 the original author or authors.
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

package org.springframework.cloud.openfeign.security;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.token.AccessTokenProviderChain;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Static methods used for in-tests assertion.
 *
 * @author Wojciech Mąka
 * @since 3.1.1
 */
public final class OAuth2AccessTokenProviderReflectionUtils {

	private OAuth2AccessTokenProviderReflectionUtils() {
	}

	public static AccessTokenProviderChain getAccessTokenProvider(OAuth2FeignRequestInterceptor interceptor) {
		Field accessTokenProvider = ReflectionUtils.findField(OAuth2FeignRequestInterceptor.class,
				"accessTokenProvider");
		ReflectionUtils.makeAccessible(Objects.requireNonNull(accessTokenProvider));
		return (AccessTokenProviderChain) ReflectionUtils.getField(accessTokenProvider, interceptor);
	}

	@SuppressWarnings("unchecked")
	public static List<ClientHttpRequestInterceptor> getAccessTokenProviderInterceptors(
			OAuth2FeignRequestInterceptor interceptor) {
		AccessTokenProviderChain accessTokenProviderChain = getAccessTokenProvider(interceptor);
		Field interceptorsField = ReflectionUtils.findField(AccessTokenProviderChain.class, "interceptors");
		ReflectionUtils.makeAccessible(Objects.requireNonNull(interceptorsField));
		return (List<ClientHttpRequestInterceptor>) ReflectionUtils.getField(interceptorsField,
				accessTokenProviderChain);
	}

	@SuppressWarnings("unchecked")
	public static <T extends ClientHttpRequestInterceptor> T getAccessTokenProviderInterceptor(
			OAuth2FeignRequestInterceptor interceptor, Class<T> clazz) {
		List<ClientHttpRequestInterceptor> interceptors = getAccessTokenProviderInterceptors(interceptor);
		if (!CollectionUtils.isEmpty(interceptors)) {
			for (ClientHttpRequestInterceptor accessTokenInterceptor : interceptors) {
				if (clazz.isAssignableFrom(accessTokenInterceptor.getClass())) {
					return (T) accessTokenInterceptor;
				}
			}
		}
		return null;
	}

}
