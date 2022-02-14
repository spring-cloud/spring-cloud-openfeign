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

import java.util.Collection;
import java.util.Map;

import feign.Request.HttpMethod;
import feign.RequestTemplate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author João Pedro Evangelista
 * @author Tim Ysewyn
 * @author Szymon Linowski
 */
class OAuth2FeignRequestInterceptorTests {

	private OAuth2FeignRequestInterceptor oAuth2FeignRequestInterceptor;

	private RequestTemplate requestTemplate;

	@BeforeEach
	void setUp() {
		oAuth2FeignRequestInterceptor = new OAuth2FeignRequestInterceptor(new MockOAuth2ClientContext("Fancy"),
				new BaseOAuth2ProtectedResourceDetails());
		requestTemplate = new RequestTemplate().method(HttpMethod.GET);
	}

	@Test
	void applyAuthorizationHeader() {
		oAuth2FeignRequestInterceptor.apply(requestTemplate);
		Map<String, Collection<String>> headers = requestTemplate.headers();

		assertThat(headers.containsKey("Authorization")).describedAs("RequestTemplate must have a Authorization header")
				.isTrue();
		Assertions.assertThat(headers.get("Authorization")).describedAs("Authorization must have a extract of Fancy")
				.contains("Bearer Fancy");
	}

	@Test
	void tryToAcquireToken() {
		oAuth2FeignRequestInterceptor = new OAuth2FeignRequestInterceptor(new DefaultOAuth2ClientContext(),
				new BaseOAuth2ProtectedResourceDetails());

		Assertions.assertThatExceptionOfType(OAuth2AccessDeniedException.class)
				.isThrownBy(() -> oAuth2FeignRequestInterceptor.getToken()).withMessage(
						"Unable to obtain a new access token for resource 'null'. The provider manager is not configured to support it.");
	}

	@Test
	void configureAccessTokenProvider() {
		OAuth2AccessToken mockedToken = new MockOAuth2AccessToken("MOCKED_TOKEN");
		oAuth2FeignRequestInterceptor.setAccessTokenProvider(new MockAccessTokenProvider(mockedToken));

		assertThat(oAuth2FeignRequestInterceptor.acquireAccessToken())
				.describedAs("Should return same mocked token instance").isEqualTo(mockedToken);
	}

	@Test
	void applyAuthorizationHeaderOnlyOnce() {
		OAuth2ClientContext oAuth2ClientContext = mock(OAuth2ClientContext.class);
		when(oAuth2ClientContext.getAccessToken()).thenReturn(new MockOAuth2AccessToken("MOCKED_TOKEN"));

		OAuth2FeignRequestInterceptor oAuth2FeignRequestInterceptor = new OAuth2FeignRequestInterceptor(
				oAuth2ClientContext, new BaseOAuth2ProtectedResourceDetails());

		oAuth2FeignRequestInterceptor.apply(requestTemplate);

		// First idempotent call failed, retry mechanism kicks in, and token has expired
		// in the meantime

		OAuth2AccessToken expiredAccessToken = mock(OAuth2AccessToken.class);
		when(expiredAccessToken.isExpired()).thenReturn(true);
		when(oAuth2ClientContext.getAccessToken()).thenReturn(expiredAccessToken);
		AccessTokenRequest accessTokenRequest = mock(AccessTokenRequest.class);
		when(oAuth2ClientContext.getAccessTokenRequest()).thenReturn(accessTokenRequest);
		OAuth2AccessToken newToken = new MockOAuth2AccessToken("Fancy");
		oAuth2FeignRequestInterceptor.setAccessTokenProvider(new MockAccessTokenProvider(newToken));

		oAuth2FeignRequestInterceptor.apply(requestTemplate);

		Map<String, Collection<String>> headers = requestTemplate.headers();
		assertThat(headers.containsKey("Authorization")).describedAs("RequestTemplate must have a Authorization header")
				.isTrue();
		assertThat(headers.get("Authorization")).describedAs("Authorization must have a extract of Fancy").hasSize(1);
		assertThat(headers.get("Authorization")).describedAs("Authorization must have a extract of Fancy")
				.contains("Bearer Fancy");
	}

}
