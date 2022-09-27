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
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.util.AlternativeJdkIdGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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

	private final OAuth2AuthorizedClientService mockOAuth2AuthorizedClientService = mock(
			OAuth2AuthorizedClientService.class);

	private final OAuth2AuthorizedClientManager mockOAuth2AuthorizedClientManager = mock(
			OAuth2AuthorizedClientManager.class);

	private OAuth2AccessTokenInterceptor oAuth2AccessTokenInterceptor;

	private RequestTemplate requestTemplate;

	private static final String DEFAULT_CLIENT_ID = "feign-client";

	@BeforeEach
	void setUp() {
		requestTemplate = new RequestTemplate().method(HttpMethod.GET);
		Target<?> feignTarget = mock(Target.class);
		when(feignTarget.url()).thenReturn("http://test");
		requestTemplate.feignTarget(feignTarget);
	}

	@Test
	void shouldThrowExceptionWhenNoTokenAcquired() {
		when(mockOAuth2AuthorizedClientService.loadAuthorizedClient(anyString(), anyString())).thenReturn(null);
		oAuth2AccessTokenInterceptor = new OAuth2AccessTokenInterceptor(mockOAuth2AuthorizedClientService,
				mock(ClientRegistrationRepository.class));
		when(mockOAuth2AuthorizedClientManager.authorize(any())).thenReturn(null);
		oAuth2AccessTokenInterceptor.setAuthorizedClientManager(mockOAuth2AuthorizedClientManager);

		assertThatExceptionOfType(IllegalStateException.class)
				.isThrownBy(() -> oAuth2AccessTokenInterceptor.apply(requestTemplate))
				.withMessage("OAuth2 token has not been successfully acquired.");
	}

	@Test
	void shouldAcquireValidToken() {
		when(mockOAuth2AuthorizedClientService.loadAuthorizedClient(anyString(), anyString())).thenReturn(null);
		oAuth2AccessTokenInterceptor = new OAuth2AccessTokenInterceptor(mockOAuth2AuthorizedClientService,
				mock(ClientRegistrationRepository.class));
		when(mockOAuth2AuthorizedClientManager.authorize(
				argThat((OAuth2AuthorizeRequest request) -> ("test").equals(request.getClientRegistrationId()))))
						.thenReturn(validTokenOAuth2AuthorizedClient());
		oAuth2AccessTokenInterceptor.setAuthorizedClientManager(mockOAuth2AuthorizedClientManager);

		oAuth2AccessTokenInterceptor.apply(requestTemplate);

		assertThat(requestTemplate.headers().get("Authorization")).contains("Bearer Valid Token");
	}

	@Test
	void shouldThrowExceptionWhenExpiredTokenAcquired() {
		when(mockOAuth2AuthorizedClientService.loadAuthorizedClient(anyString(), anyString())).thenReturn(null);
		oAuth2AccessTokenInterceptor = new OAuth2AccessTokenInterceptor(mockOAuth2AuthorizedClientService,
				mock(ClientRegistrationRepository.class));
		when(mockOAuth2AuthorizedClientManager.authorize(any())).thenReturn(expiredTokenOAuth2AuthorizedClient());
		oAuth2AccessTokenInterceptor.setAuthorizedClientManager(mockOAuth2AuthorizedClientManager);

		assertThatExceptionOfType(IllegalStateException.class)
				.isThrownBy(() -> oAuth2AccessTokenInterceptor.apply(requestTemplate))
				.withMessage("OAuth2 token has not been successfully acquired.");
	}

	@Test
	void shouldAcquireTokenFromAuthorizedClient() {
		when(mockOAuth2AuthorizedClientService.loadAuthorizedClient(eq("test"), anyString()))
				.thenReturn(validTokenOAuth2AuthorizedClient());
		oAuth2AccessTokenInterceptor = new OAuth2AccessTokenInterceptor(mockOAuth2AuthorizedClientService,
				mock(ClientRegistrationRepository.class));

		oAuth2AccessTokenInterceptor.apply(requestTemplate);

		assertThat(requestTemplate.headers().get("Authorization")).contains("Bearer Valid Token");
	}

	@Test
	void shouldAcquireValidTokenFromSpecifiedClientId() {
		when(mockOAuth2AuthorizedClientService.loadAuthorizedClient(eq("testId"), anyString()))
				.thenReturn(validTokenOAuth2AuthorizedClient());
		oAuth2AccessTokenInterceptor = new OAuth2AccessTokenInterceptor("testId", mockOAuth2AuthorizedClientService,
				mock(ClientRegistrationRepository.class));

		oAuth2AccessTokenInterceptor.apply(requestTemplate);

		assertThat(requestTemplate.headers().get("Authorization")).contains("Bearer Valid Token");
	}

	private OAuth2AccessToken validToken() {
		return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "Valid Token", Instant.now(),
				Instant.now().plusSeconds(60L));
	}

	private OAuth2AccessToken expiredToken() {
		return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "Expired Token",
				Instant.now().minusSeconds(61L), Instant.now().minusSeconds(60L));
	}

	private OAuth2AuthorizedClient validTokenOAuth2AuthorizedClient() {
		return new OAuth2AuthorizedClient(defaultClientRegistration(), "anonymousUser", validToken());
	}

	private OAuth2AuthorizedClient expiredTokenOAuth2AuthorizedClient() {
		return new OAuth2AuthorizedClient(defaultClientRegistration(), "anonymousUser", expiredToken());
	}

	private ClientRegistration defaultClientRegistration() {
		return ClientRegistration.withRegistrationId(new AlternativeJdkIdGenerator().generateId().toString())
				.clientId(DEFAULT_CLIENT_ID).tokenUri("mock token uri")
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS).build();
	}

}
