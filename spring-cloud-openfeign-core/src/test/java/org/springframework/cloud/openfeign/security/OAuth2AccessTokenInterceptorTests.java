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

import java.time.Instant;

import feign.Request.HttpMethod;
import feign.RequestTemplate;
import feign.Target;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link OAuth2AccessTokenInterceptor}.
 *
 * @author Dangzhicairang(小水牛)
 * @author Olga Maciaszek-Sharma
 *
 */
class OAuth2AccessTokenInterceptorTests {

	private final OAuth2AuthorizedClientManager mockOAuth2AuthorizedClientManager = mock(
			OAuth2AuthorizedClientManager.class);

	private OAuth2AccessTokenInterceptor oAuth2AccessTokenInterceptor;

	private RequestTemplate requestTemplate;

	private static final String DEFAULT_CLIENT_REGISTRATION_ID = "feign-client";

	@BeforeEach
	void setUp() {
		requestTemplate = new RequestTemplate().method(HttpMethod.GET);
		Target<?> feignTarget = mock(Target.class);
		when(feignTarget.url()).thenReturn("http://test");
		requestTemplate.feignTarget(feignTarget);
	}

	@Test
	void shouldThrowExceptionWhenNoTokenAcquired() {
		oAuth2AccessTokenInterceptor = new OAuth2AccessTokenInterceptor(mockOAuth2AuthorizedClientManager);
		when(mockOAuth2AuthorizedClientManager.authorize(any())).thenReturn(null);

		assertThatExceptionOfType(IllegalStateException.class)
				.isThrownBy(() -> oAuth2AccessTokenInterceptor.apply(requestTemplate))
				.withMessage("OAuth2 token has not been successfully acquired.");
	}

	@Test
	void shouldAcquireValidToken() {
		oAuth2AccessTokenInterceptor = new OAuth2AccessTokenInterceptor(mockOAuth2AuthorizedClientManager);
		when(mockOAuth2AuthorizedClientManager.authorize(
				argThat((OAuth2AuthorizeRequest request) -> ("test").equals(request.getClientRegistrationId()))))
						.thenReturn(validTokenOAuth2AuthorizedClient());

		oAuth2AccessTokenInterceptor.apply(requestTemplate);

		assertThat(requestTemplate.headers().get("Authorization")).contains("Bearer Valid Token");
	}

	@Test
	void shouldAcquireValidTokenFromServiceId() {
		when(mockOAuth2AuthorizedClientManager.authorize(
				argThat((OAuth2AuthorizeRequest request) -> ("test").equals(request.getClientRegistrationId()))))
						.thenReturn(validTokenOAuth2AuthorizedClient());
		oAuth2AccessTokenInterceptor = new OAuth2AccessTokenInterceptor(mockOAuth2AuthorizedClientManager);

		oAuth2AccessTokenInterceptor.apply(requestTemplate);

		assertThat(requestTemplate.headers().get("Authorization")).contains("Bearer Valid Token");
	}

	@Test
	void shouldAcquireValidTokenFromSpecifiedClientRegistrationId() {
		oAuth2AccessTokenInterceptor = new OAuth2AccessTokenInterceptor(DEFAULT_CLIENT_REGISTRATION_ID,
				mockOAuth2AuthorizedClientManager);
		when(mockOAuth2AuthorizedClientManager
				.authorize(argThat((OAuth2AuthorizeRequest request) -> (DEFAULT_CLIENT_REGISTRATION_ID)
						.equals(request.getClientRegistrationId())))).thenReturn(validTokenOAuth2AuthorizedClient());

		oAuth2AccessTokenInterceptor.apply(requestTemplate);

		assertThat(requestTemplate.headers().get("Authorization")).contains("Bearer Valid Token");
	}

	private OAuth2AccessToken validToken() {
		return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "Valid Token", Instant.now(),
				Instant.now().plusSeconds(60L));
	}

	private OAuth2AuthorizedClient validTokenOAuth2AuthorizedClient() {
		return new OAuth2AuthorizedClient(defaultClientRegistration(), "anonymousUser", validToken());
	}

	private ClientRegistration defaultClientRegistration() {
		return ClientRegistration.withRegistrationId(DEFAULT_CLIENT_REGISTRATION_ID).clientId("clientId")
				.tokenUri("mock token uri").authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS).build();
	}

}
