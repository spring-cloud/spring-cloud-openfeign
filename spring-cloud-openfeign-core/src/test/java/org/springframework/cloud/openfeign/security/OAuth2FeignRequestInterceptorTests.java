/*
 * Copyright 2015-2019 the original author or authors.
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author João Pedro Evangelista
 * @author Tim Ysewyn
 */
public class OAuth2FeignRequestInterceptorTests {

	private OAuth2FeignRequestInterceptor oAuth2FeignRequestInterceptor;

	private RequestTemplate requestTemplate;

	@Before
	public void setUp() {
		oAuth2FeignRequestInterceptor = new OAuth2FeignRequestInterceptor(new MockOAuth2ClientContext("Fancy"),
				new BaseOAuth2ProtectedResourceDetails());
		requestTemplate = new RequestTemplate().method(HttpMethod.GET);
	}

	@Test
	public void applyAuthorizationHeader() {
		oAuth2FeignRequestInterceptor.apply(requestTemplate);
		Map<String, Collection<String>> headers = requestTemplate.headers();
		Assert.assertTrue("RequestTemplate must have a Authorization header", headers.containsKey("Authorization"));
		Assert.assertThat("Authorization must have a extract of Fancy", headers.get("Authorization"),
				contains("Bearer Fancy"));
	}

	@Test(expected = OAuth2AccessDeniedException.class)
	public void tryToAcquireToken() {
		oAuth2FeignRequestInterceptor = new OAuth2FeignRequestInterceptor(new DefaultOAuth2ClientContext(),
				new BaseOAuth2ProtectedResourceDetails());
		OAuth2AccessToken oAuth2AccessToken = oAuth2FeignRequestInterceptor.getToken();
		Assert.assertTrue(oAuth2AccessToken.getValue() + " Must be null", oAuth2AccessToken.getValue() == null);
	}

	@Test
	public void configureAccessTokenProvider() {
		OAuth2AccessToken mockedToken = new MockOAuth2AccessToken("MOCKED_TOKEN");
		oAuth2FeignRequestInterceptor.setAccessTokenProvider(new MockAccessTokenProvider(mockedToken));
		Assert.assertEquals("Should return same mocked token instance", mockedToken,
				oAuth2FeignRequestInterceptor.acquireAccessToken());
	}

	@Test
	public void applyAuthorizationHeaderOnlyOnce() {
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
		Assert.assertTrue("RequestTemplate must have a Authorization header", headers.containsKey("Authorization"));
		Assert.assertThat("Authorization must have a extract of Fancy", headers.get("Authorization"), hasSize(1));
		Assert.assertThat("Authorization must have a extract of Fancy", headers.get("Authorization"),
				contains("Bearer Fancy"));
	}

}
