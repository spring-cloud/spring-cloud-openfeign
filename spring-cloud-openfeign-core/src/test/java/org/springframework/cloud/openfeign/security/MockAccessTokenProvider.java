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

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.UserApprovalRequiredException;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;

/**
 * Mocks the access token provider
 *
 * @author Mihhail Verhovtsov
 */
public class MockAccessTokenProvider implements AccessTokenProvider {

	private OAuth2AccessToken token;

	public MockAccessTokenProvider(OAuth2AccessToken token) {
		this.token = token;
	}

	@Override
	public OAuth2AccessToken obtainAccessToken(OAuth2ProtectedResourceDetails oAuth2ProtectedResourceDetails,
			AccessTokenRequest accessTokenRequest)
			throws UserRedirectRequiredException, UserApprovalRequiredException, AccessDeniedException {
		return token;
	}

	@Override
	public boolean supportsResource(OAuth2ProtectedResourceDetails oAuth2ProtectedResourceDetails) {
		return true;
	}

	@Override
	public OAuth2AccessToken refreshAccessToken(OAuth2ProtectedResourceDetails oAuth2ProtectedResourceDetails,
			OAuth2RefreshToken oAuth2RefreshToken, AccessTokenRequest accessTokenRequest)
			throws UserRedirectRequiredException {
		return null;
	}

	@Override
	public boolean supportsRefresh(OAuth2ProtectedResourceDetails oAuth2ProtectedResourceDetails) {
		return false;
	}

}
