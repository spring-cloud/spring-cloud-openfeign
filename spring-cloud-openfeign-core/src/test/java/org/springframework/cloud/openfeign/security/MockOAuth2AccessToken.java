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

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;

/**
 * Mocks the OAuth2 access token
 *
 * @author Mihhail Verhovtsov
 */
public class MockOAuth2AccessToken implements OAuth2AccessToken {

	private String value;

	public MockOAuth2AccessToken(String value) {
		this.value = value;
	}

	@Override
	public Map<String, Object> getAdditionalInformation() {
		return null;
	}

	@Override
	public Set<String> getScope() {
		return null;
	}

	@Override
	public OAuth2RefreshToken getRefreshToken() {
		return null;
	}

	@Override
	public String getTokenType() {
		return null;
	}

	@Override
	public boolean isExpired() {
		return false;
	}

	@Override
	public Date getExpiration() {
		return null;
	}

	@Override
	public int getExpiresIn() {
		return 0;
	}

	@Override
	public String getValue() {
		return value;
	}

}
