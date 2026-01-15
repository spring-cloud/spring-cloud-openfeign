/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.cloud.openfeign;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Feign OAuth2 support.
 *
 * @author jaehun lee
 */
@ConfigurationProperties(prefix = "spring.cloud.openfeign.oauth2")
public class FeignOAuth2Properties {

	/**
	 * Enables feign interceptor for managing oauth2 access token.
	 */
	private boolean enabled = false;

	/**
	 * Client registration id to be used to retrieve the OAuth2 access token. If not
	 * specified, the {@code serviceId} retrieved from the {@code url} host segment will
	 * be used. This is convenient for load-balanced Feign clients. For non-load-balanced
	 * clients, specifying the {@code clientRegistrationId} is recommended.
	 */
	private String clientRegistrationId = "";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getClientRegistrationId() {
		return clientRegistrationId;
	}

	public void setClientRegistrationId(String clientRegistrationId) {
		this.clientRegistrationId = clientRegistrationId;
	}

}
