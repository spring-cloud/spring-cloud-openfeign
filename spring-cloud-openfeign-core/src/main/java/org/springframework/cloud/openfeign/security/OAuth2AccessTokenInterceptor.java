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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import feign.RequestInterceptor;
import feign.RequestTemplate;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.util.StringUtils;

/**
 * RequestInterceptor for OAuth2 Feign Requests. By default, It uses the
 * {@link AuthorizedClientServiceOAuth2AuthorizedClientManager } to get
 * {@link OAuth2AuthorizedClient } that hold an {@link OAuth2AccessToken }. Use the
 * Client(s) from properties if not specific the field
 * {@link OAuth2AccessTokenInterceptor#specifiedClientIds}
 *
 * @author Dangzhicairang(小水牛)
 * @since 4.0.0
 */
public class OAuth2AccessTokenInterceptor implements RequestInterceptor {

	/**
	 * The name of the token.
	 */
	public static final String BEARER = "Bearer";

	/**
	 * The name of the header.
	 */
	public static final String AUTHORIZATION = "Authorization";

	private final String tokenType;

	private final String header;

	private final List<String> specifiedClientIds;

	private final OAuth2ClientProperties oAuth2ClientProperties;

	private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

	private OAuth2AuthorizedClientManager authorizedClientManager;

	public void setAuthorizedClientManager(OAuth2AuthorizedClientManager authorizedClientManager) {
		this.authorizedClientManager = authorizedClientManager;
	}

	private static final Authentication ANONYMOUS_AUTHENTICATION = new AnonymousAuthenticationToken("anonymous",
		"anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

	public OAuth2AccessTokenInterceptor(OAuth2ClientProperties oAuth2ClientProperties,
		OAuth2AuthorizedClientService oAuth2AuthorizedClientService,
		ClientRegistrationRepository clientRegistrationRepository) {
		this(new ArrayList<>(), oAuth2ClientProperties, oAuth2AuthorizedClientService, clientRegistrationRepository);
	}

	public OAuth2AccessTokenInterceptor(List<String> specifiedClientIds, OAuth2ClientProperties oAuth2ClientProperties,
		OAuth2AuthorizedClientService oAuth2AuthorizedClientService,
		ClientRegistrationRepository clientRegistrationRepository) {
		this(BEARER, AUTHORIZATION, specifiedClientIds, oAuth2ClientProperties, oAuth2AuthorizedClientService,
			clientRegistrationRepository);
	}

	public OAuth2AccessTokenInterceptor(String tokenType, String header, List<String> specifiedClientIds,
		OAuth2ClientProperties oAuth2ClientProperties, OAuth2AuthorizedClientService oAuth2AuthorizedClientService,
		ClientRegistrationRepository clientRegistrationRepository) {
		this.tokenType = tokenType;
		this.header = header;
		this.specifiedClientIds = specifiedClientIds;
		this.oAuth2ClientProperties = oAuth2ClientProperties;
		this.oAuth2AuthorizedClientService = oAuth2AuthorizedClientService;
		this.authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
			clientRegistrationRepository, this.oAuth2AuthorizedClientService);
	}

	@Override
	public void apply(RequestTemplate template) {
		template.header(header);
		template.header(header, extract(tokenType));
	}

	protected String extract(String tokenType) {
		OAuth2AccessToken accessToken = getToken();
		return String.format("%s %s", tokenType, accessToken.getTokenValue());
	}

	public OAuth2AccessToken getToken() {

		// if specific, try to use them to get token.
		for (String clientId : this.specifiedClientIds) {
			OAuth2AccessToken token = this.getToken(clientId);
			if (token != null) {
				return token;
			}
		}

		// use clients from properties by default
		for (String clientId : Optional.ofNullable(this.oAuth2ClientProperties)
			.map(OAuth2ClientProperties::getRegistration).map(Map::keySet)
			.orElse(new HashSet<>())) {
			OAuth2AccessToken token = this.getToken(clientId);
			if (token != null) {
				return token;
			}
		}

		throw new IllegalStateException("No token acquired, which is illegal according to the contract.");
	}

	protected OAuth2AccessToken getToken(String clientId) {

		if (!StringUtils.hasText(clientId)) {
			return null;
		}

		Authentication principal = SecurityContextHolder.getContext().getAuthentication();
		if (principal == null) {
			principal = ANONYMOUS_AUTHENTICATION;
		}

		// already exist
		OAuth2AuthorizedClient oAuth2AuthorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient(clientId,
			principal.getName());
		if (oAuth2AuthorizedClient != null) {
			OAuth2AccessToken accessToken = oAuth2AuthorizedClient.getAccessToken();
			if (accessToken != null && this.noExpire(accessToken)) {
				return accessToken;
			}
		}

		OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(clientId)
			.principal(principal).build();
		OAuth2AuthorizedClient authorize = this.authorizedClientManager.authorize(authorizeRequest);
		return Optional.ofNullable(authorize).map(OAuth2AuthorizedClient::getAccessToken)
			.filter(this::noExpire)
			.orElse(null);
	}

	protected boolean noExpire(OAuth2AccessToken token) {
		return Optional.ofNullable(token).map(OAuth2AccessToken::getExpiresAt)
			.map(expire -> expire.isAfter(Instant.now())).orElse(false);
	}

}
