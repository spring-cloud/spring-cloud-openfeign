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

import java.util.LinkedHashMap;
import java.util.Map;

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
	 * Client registration id to be used as the default when retrieving the OAuth2 access
	 * token. A client-specific registration id, when configured, takes precedence. If no
	 * client-specific registration id is configured and the token cannot be retrieved
	 * using this default registration id, the {@code serviceId} retrieved from the
	 * {@code url} host segment will be used. This is convenient for load-balanced Feign
	 * clients. For non-load-balanced clients, specifying the {@code clientRegistrationId}
	 * is recommended.
	 */
	private String clientRegistrationId = "";

	/**
	 * Map of Feign client names to OAuth2 client registration ids. The registration id
	 * associated with the Feign client name is used to retrieve the OAuth2 access token.
	 * If the token cannot be retrieved using this registration id, no fallback is
	 * attempted. If no client-specific registration id is configured, the default
	 * {@code clientRegistrationId} is used. If no token can be retrieved using the
	 * default registration id, the {@code serviceId} retrieved from the {@code url} host
	 * segment is used as a fallback.
	 */
	private Map<String, String> clientRegistrationIds = new LinkedHashMap<>();

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

	public Map<String, String> getClientRegistrationIds() {
		return clientRegistrationIds;
	}

	public void setClientRegistrationIds(Map<String, String> clientRegistrationIds) {
		this.clientRegistrationIds = clientRegistrationIds != null ? clientRegistrationIds : new LinkedHashMap<>();
	}

}
