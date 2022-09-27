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

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Target;

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
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link RequestInterceptor} for OAuth2 Feign Requests. By default, it uses the
 * {@link AuthorizedClientServiceOAuth2AuthorizedClientManager } to get
 * {@link OAuth2AuthorizedClient } that holds an {@link OAuth2AccessToken }. If the user
 * has specified an OAuth2 {@code clientId} using the
 * {@code spring.cloud.openfeign.oauth2.clientId} property, it will be used to retrieve
 * the token. If the token is not retrieved or the {@code clientId} has not been
 * specified, the {@code serviceId} retrieved from the {@code url} host segment will be
 * used. This approach is convenient for load-balanced Feign clients. For
 * non-load-balanced ones, the property-based {@code clientId} is a suitable approach.
 *
 * @author Dangzhicairang(小水牛)
 * @author Olga Maciaszek-Sharma
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

	private final String clientId;

	private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

	private OAuth2AuthorizedClientManager authorizedClientManager;

	public void setAuthorizedClientManager(OAuth2AuthorizedClientManager authorizedClientManager) {
		this.authorizedClientManager = authorizedClientManager;
	}

	private static final Authentication ANONYMOUS_AUTHENTICATION = new AnonymousAuthenticationToken("anonymous",
			"anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

	public OAuth2AccessTokenInterceptor(OAuth2AuthorizedClientService oAuth2AuthorizedClientService,
			ClientRegistrationRepository clientRegistrationRepository) {
		this(null, oAuth2AuthorizedClientService, clientRegistrationRepository);
	}

	public OAuth2AccessTokenInterceptor(String clientId, OAuth2AuthorizedClientService oAuth2AuthorizedClientService,
			ClientRegistrationRepository clientRegistrationRepository) {
		this(BEARER, AUTHORIZATION, clientId, oAuth2AuthorizedClientService, clientRegistrationRepository);
	}

	public OAuth2AccessTokenInterceptor(String tokenType, String header, String clientId,
			OAuth2AuthorizedClientService oAuth2AuthorizedClientService,
			ClientRegistrationRepository clientRegistrationRepository) {
		this.tokenType = tokenType;
		this.header = header;
		this.clientId = clientId;
		this.oAuth2AuthorizedClientService = oAuth2AuthorizedClientService;
		this.authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
				clientRegistrationRepository, this.oAuth2AuthorizedClientService);
	}

	@Override
	public void apply(RequestTemplate template) {
		OAuth2AccessToken token = getToken(template);
		String extractedToken = String.format("%s %s", tokenType, token.getTokenValue());
		template.header(header);
		template.header(header, extractedToken);
	}

	public OAuth2AccessToken getToken(RequestTemplate template) {
		// If specified, try to use them to get token.
		if (StringUtils.hasText(clientId)) {
			OAuth2AccessToken token = getToken(clientId);
			if (token != null) {
				return token;
			}
		}

		// If not specified use host (synonymous with serviceId for load-balanced
		// requests; non-load-balanced requests should use the method above).
		OAuth2AccessToken token = getToken(getServiceId(template));
		if (token != null) {
			return token;
		}
		throw new IllegalStateException("OAuth2 token has not been successfully acquired.");
	}

	protected OAuth2AccessToken getToken(String clientId) {
		if (!StringUtils.hasText(clientId)) {
			return null;
		}

		Authentication principal = SecurityContextHolder.getContext().getAuthentication();
		if (principal == null) {
			principal = ANONYMOUS_AUTHENTICATION;
		}

		// Already exist
		OAuth2AuthorizedClient oAuth2AuthorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient(clientId,
				principal.getName());
		if (oAuth2AuthorizedClient != null) {
			OAuth2AccessToken accessToken = oAuth2AuthorizedClient.getAccessToken();
			if (accessToken != null && notExpired(accessToken)) {
				return accessToken;
			}
		}

		OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(clientId)
				.principal(principal).build();
		OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
		return Optional.ofNullable(authorizedClient).map(OAuth2AuthorizedClient::getAccessToken)
				.filter(this::notExpired).orElse(null);
	}

	protected boolean notExpired(OAuth2AccessToken token) {
		return Optional.ofNullable(token).map(OAuth2AccessToken::getExpiresAt)
				.map(expire -> expire.isAfter(Instant.now())).orElse(false);
	}

	private static String getServiceId(RequestTemplate template) {
		Target<?> feignTarget = template.feignTarget();
		Assert.notNull(feignTarget, "feignTarget may not be null");
		String url = feignTarget.url();
		Assert.hasLength(url, "url may not be empty");
		final URI originalUri = URI.create(url);
		return originalUri.getHost();
	}

}
