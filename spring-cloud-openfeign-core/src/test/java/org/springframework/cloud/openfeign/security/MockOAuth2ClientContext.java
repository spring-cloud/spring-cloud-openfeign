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

import java.util.HashMap;

import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

/**
 * Mocks the current client context
 *
 * @author João Pedro Evangelista
 */
public final class MockOAuth2ClientContext implements OAuth2ClientContext {

	private final String value;

	MockOAuth2ClientContext(String value) {
		this.value = value;
	}

	@Override
	public OAuth2AccessToken getAccessToken() {
		return new DefaultOAuth2AccessToken(value);
	}

	@Override
	public void setAccessToken(OAuth2AccessToken accessToken) {

	}

	@Override
	public AccessTokenRequest getAccessTokenRequest() {
		DefaultAccessTokenRequest tokenRequest = new DefaultAccessTokenRequest(new HashMap<String, String[]>());
		tokenRequest.setExistingToken(new DefaultOAuth2AccessToken(value));
		return tokenRequest;
	}

	@Override
	public void setPreservedState(String stateKey, Object preservedState) {

	}

	@Override
	public Object removePreservedState(String stateKey) {
		return null;
	}

}
