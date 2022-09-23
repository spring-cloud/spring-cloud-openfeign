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
import java.util.HashMap;

import feign.Request.HttpMethod;
import feign.RequestTemplate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.util.AlternativeJdkIdGenerator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Dangzhicairang(小水牛)
 */
class OAuth2AccessTokenInterceptorTests {

	private OAuth2AccessTokenInterceptor oAuth2AccessTokenInterceptor;

	private RequestTemplate requestTemplate;

	private OAuth2ClientProperties mockOAuth2ClientProperties;

	private static final String DEFAULT_CLIENT_ID = "feign-client";

	@BeforeEach
	void setUp() {

		requestTemplate = new RequestTemplate().method(HttpMethod.GET);

		mockOAuth2ClientProperties = mock(OAuth2ClientProperties.class);
		given(mockOAuth2ClientProperties.getRegistration())
			.willReturn(new HashMap<String, OAuth2ClientProperties.Registration>() {
				{
					put(DEFAULT_CLIENT_ID, mock(OAuth2ClientProperties.Registration.class));
				}
			});

	}

	@Test
	void noTokenAcquired() {

		OAuth2AuthorizedClientService mockOAuth2AuthorizedClientService = mock(OAuth2AuthorizedClientService.class);
		given(mockOAuth2AuthorizedClientService.loadAuthorizedClient(anyString(), anyString())).willReturn(null);

		oAuth2AccessTokenInterceptor = new OAuth2AccessTokenInterceptor(mockOAuth2ClientProperties,
			mockOAuth2AuthorizedClientService, mock(ClientRegistrationRepository.class));

		OAuth2AuthorizedClientManager mockOAuth2AuthorizedClientManager = mock(OAuth2AuthorizedClientManager.class);
		given(mockOAuth2AuthorizedClientManager.authorize(any())).willReturn(null);

		oAuth2AccessTokenInterceptor.setAuthorizedClientManager(mockOAuth2AuthorizedClientManager);

		Assertions.assertThatExceptionOfType(IllegalStateException.class)
			.isThrownBy(() -> oAuth2AccessTokenInterceptor.apply(requestTemplate))
			.withMessage("No token acquired, which is illegal according to the contract.");

	}

	@Test
	void validTokenAcquired() {

		OAuth2AuthorizedClientService mockOAuth2AuthorizedClientService = mock(OAuth2AuthorizedClientService.class);
		given(mockOAuth2AuthorizedClientService.loadAuthorizedClient(anyString(), anyString())).willReturn(null);

		oAuth2AccessTokenInterceptor = new OAuth2AccessTokenInterceptor(mockOAuth2ClientProperties,
			mockOAuth2AuthorizedClientService, mock(ClientRegistrationRepository.class));

		OAuth2AuthorizedClientManager mockOAuth2AuthorizedClientManager = mock(OAuth2AuthorizedClientManager.class);
		given(mockOAuth2AuthorizedClientManager.authorize(any())).willReturn(validTokenOAuth2AuthorizedClient());

		oAuth2AccessTokenInterceptor.setAuthorizedClientManager(mockOAuth2AuthorizedClientManager);

		oAuth2AccessTokenInterceptor.apply(requestTemplate);

		Assertions.assertThat(requestTemplate.headers().get("Authorization"))
			.contains("Bearer Valid Token");
	}

	@Test
	void expireTokenAcquired() {

		OAuth2AuthorizedClientService mockOAuth2AuthorizedClientService = mock(OAuth2AuthorizedClientService.class);
		given(mockOAuth2AuthorizedClientService.loadAuthorizedClient(anyString(), anyString())).willReturn(null);

		oAuth2AccessTokenInterceptor = new OAuth2AccessTokenInterceptor(mockOAuth2ClientProperties,
			mockOAuth2AuthorizedClientService, mock(ClientRegistrationRepository.class));

		OAuth2AuthorizedClientManager mockOAuth2AuthorizedClientManager = mock(OAuth2AuthorizedClientManager.class);
		given(mockOAuth2AuthorizedClientManager.authorize(any())).willReturn(expiredTokenOAuth2AuthorizedClient());

		oAuth2AccessTokenInterceptor.setAuthorizedClientManager(mockOAuth2AuthorizedClientManager);

		Assertions.assertThatExceptionOfType(IllegalStateException.class)
			.isThrownBy(() -> oAuth2AccessTokenInterceptor.apply(requestTemplate))
			.withMessage("No token acquired, which is illegal according to the contract.");
	}

	@Test
	void acquireTokenFromAuthorizedClient() {
		OAuth2AuthorizedClientService mockOAuth2AuthorizedClientService = mock(OAuth2AuthorizedClientService.class);
		given(mockOAuth2AuthorizedClientService.loadAuthorizedClient(anyString(), anyString()))
			.willReturn(validTokenOAuth2AuthorizedClient());

		oAuth2AccessTokenInterceptor = new OAuth2AccessTokenInterceptor(mockOAuth2ClientProperties,
			mockOAuth2AuthorizedClientService, mock(ClientRegistrationRepository.class));

		oAuth2AccessTokenInterceptor.apply(requestTemplate);

		Assertions.assertThat(requestTemplate.headers().get("Authorization"))
			.contains("Bearer Valid Token");
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
		return ClientRegistration.withRegistrationId(new AlternativeJdkIdGenerator().generateId()
				.toString())
			.clientId(DEFAULT_CLIENT_ID).tokenUri("mock token uri")
			.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS).build();
	}

}
