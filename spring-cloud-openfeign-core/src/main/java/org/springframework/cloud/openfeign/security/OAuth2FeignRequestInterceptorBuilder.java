/*
 * Copyright 2015-2022 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenProviderChain;
import org.springframework.security.oauth2.client.token.OAuth2AccessTokenSupport;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.implicit.ImplicitAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;

/**
 * Allows to customize pre-defined {@link OAuth2FeignRequestInterceptor} using configurer
 * beans of class {@link OAuth2FeignRequestInterceptorConfigurer}. Each configurer
 * instance can add {@link AccessTokenProvider} new {@link ClientHttpRequestInterceptor}
 * instances.
 *
 * @author Wojciech Mąka
 * @since 3.1.1
 */
public class OAuth2FeignRequestInterceptorBuilder {

	private AccessTokenProvider accessTokenProvider;

	private final List<ClientHttpRequestInterceptor> accessTokenProviderInterceptors = new ArrayList<>();

	public OAuth2FeignRequestInterceptorBuilder() {
		accessTokenProvider = new AccessTokenProviderChain(Arrays.<AccessTokenProvider>asList(
				new AuthorizationCodeAccessTokenProvider(), new ImplicitAccessTokenProvider(),
				new ResourceOwnerPasswordAccessTokenProvider(), new ClientCredentialsAccessTokenProvider()));
	}

	public OAuth2FeignRequestInterceptorBuilder withAccessTokenProviderInterceptors(
			ClientHttpRequestInterceptor... interceptors) {
		accessTokenProviderInterceptors.addAll(Arrays.asList(interceptors));
		return this;
	}

	OAuth2FeignRequestInterceptor build(OAuth2ClientContext oAuth2ClientContext,
			OAuth2ProtectedResourceDetails resource) {
		if (OAuth2AccessTokenSupport.class.isAssignableFrom(accessTokenProvider.getClass())) {
			((OAuth2AccessTokenSupport) accessTokenProvider).setInterceptors(accessTokenProviderInterceptors);
		}
		final OAuth2FeignRequestInterceptor feignRequestInterceptor = new OAuth2FeignRequestInterceptor(
				oAuth2ClientContext, resource);
		feignRequestInterceptor.setAccessTokenProvider(accessTokenProvider);
		return feignRequestInterceptor;
	}

	public static OAuth2FeignRequestInterceptor buildWithConfigurers(OAuth2ClientContext oAuth2ClientContext,
			OAuth2ProtectedResourceDetails resource, List<OAuth2FeignRequestInterceptorConfigurer> buildConfigurers) {
		final OAuth2FeignRequestInterceptorBuilder builder = new OAuth2FeignRequestInterceptorBuilder();
		for (OAuth2FeignRequestInterceptorConfigurer configurer : buildConfigurers) {
			configurer.customize(builder);
		}
		return builder.build(oAuth2ClientContext, resource);
	}

}
