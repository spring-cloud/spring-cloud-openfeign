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
import java.util.Optional;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Target;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link RequestInterceptor} for OAuth2 Feign Requests. By default, it uses the
 * {@link OAuth2AuthorizedClientManager } to get {@link OAuth2AuthorizedClient } that
 * holds an {@link OAuth2AccessToken }. If the user has specified an OAuth2
 * {@code clientRegistrationId} using the
 * {@code spring.cloud.openfeign.oauth2.clientRegistrationId} property, it will be used to
 * retrieve the token. If the token is not retrieved or the {@code clientRegistrationId}
 * has not been specified, the {@code serviceId} retrieved from the {@code url} host
 * segment will be used. This approach is convenient for load-balanced Feign clients. For
 * non-load-balanced ones, the property-based {@code clientRegistrationId} is a suitable
 * approach.
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

	private final String clientRegistrationId;

	private final OAuth2AuthorizedClientManager authorizedClientManager;

	private static final Authentication ANONYMOUS_AUTHENTICATION = new AnonymousAuthenticationToken("anonymous",
			"anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

	public OAuth2AccessTokenInterceptor(OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager) {
		this(null, oAuth2AuthorizedClientManager);
	}

	public OAuth2AccessTokenInterceptor(String clientRegistrationId,
			OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager) {
		this(BEARER, AUTHORIZATION, clientRegistrationId, oAuth2AuthorizedClientManager);
	}

	public OAuth2AccessTokenInterceptor(String tokenType, String header, String clientRegistrationId,
			OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager) {
		this.tokenType = tokenType;
		this.header = header;
		this.clientRegistrationId = clientRegistrationId;
		this.authorizedClientManager = oAuth2AuthorizedClientManager;
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
		if (StringUtils.hasText(clientRegistrationId)) {
			OAuth2AccessToken token = getToken(clientRegistrationId);
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

	protected OAuth2AccessToken getToken(String clientRegistrationId) {
		if (!StringUtils.hasText(clientRegistrationId)) {
			return null;
		}

		Authentication principal = SecurityContextHolder.getContext().getAuthentication();
		if (principal == null) {
			principal = ANONYMOUS_AUTHENTICATION;
		}

		OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(clientRegistrationId)
				.principal(principal).build();
		OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
		return Optional.ofNullable(authorizedClient).map(OAuth2AuthorizedClient::getAccessToken).orElse(null);
	}

	private static String getServiceId(RequestTemplate template) {
		Target<?> feignTarget = template.feignTarget();
		Assert.notNull(feignTarget, "FeignTarget may not be null.");
		String url = feignTarget.url();
		Assert.hasLength(url, "Url may not be empty.");
		final URI originalUri = URI.create(url);
		return originalUri.getHost();
	}

}
