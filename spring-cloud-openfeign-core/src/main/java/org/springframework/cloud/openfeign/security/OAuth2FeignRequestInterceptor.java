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

import java.util.Arrays;

import feign.RequestInterceptor;
import feign.RequestTemplate;

import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.http.AccessTokenRequiredException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenProviderChain;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.implicit.ImplicitAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

/**
 * Pre-defined custom RequestInterceptor for Feign Requests. It uses the
 * {@link OAuth2ClientContext OAuth2ClientContext} provided from the environment and
 * construct a new header on the request before it is made by Feign.
 *
 * @author Joao Pedro Evangelista
 * @author Tim Ysewyn
 * @since 3.0.0
 */
public class OAuth2FeignRequestInterceptor implements RequestInterceptor {

	/**
	 * The name of the token.
	 */
	public static final String BEARER = "Bearer";

	/**
	 * The name of the header.
	 */
	public static final String AUTHORIZATION = "Authorization";

	private final OAuth2ClientContext oAuth2ClientContext;

	private final OAuth2ProtectedResourceDetails resource;

	private final String tokenType;

	private final String header;

	private AccessTokenProvider accessTokenProvider = new AccessTokenProviderChain(Arrays.<AccessTokenProvider>asList(
			new AuthorizationCodeAccessTokenProvider(), new ImplicitAccessTokenProvider(),
			new ResourceOwnerPasswordAccessTokenProvider(), new ClientCredentialsAccessTokenProvider()));

	/**
	 * Default constructor which uses the provided OAuth2ClientContext and Bearer tokens
	 * within Authorization header.
	 * @param oAuth2ClientContext provided context
	 * @param resource type of resource to be accessed
	 */
	public OAuth2FeignRequestInterceptor(OAuth2ClientContext oAuth2ClientContext,
			OAuth2ProtectedResourceDetails resource) {
		this(oAuth2ClientContext, resource, BEARER, AUTHORIZATION);
	}

	/**
	 * Fully customizable constructor for changing token type and header name, in cases of
	 * Bearer and Authorization is not the default such as "bearer", "authorization".
	 * @param oAuth2ClientContext current oAuth2 Context
	 * @param resource type of resource to be accessed
	 * @param tokenType type of token e.g. "token", "Bearer"
	 * @param header name of the header e.g. "Authorization", "authorization"
	 */
	public OAuth2FeignRequestInterceptor(OAuth2ClientContext oAuth2ClientContext,
			OAuth2ProtectedResourceDetails resource, String tokenType, String header) {
		this.oAuth2ClientContext = oAuth2ClientContext;
		this.resource = resource;
		this.tokenType = tokenType;
		this.header = header;
	}

	/**
	 * Create a template with the header of provided name and extracted extract.
	 *
	 * @see RequestInterceptor#apply(RequestTemplate)
	 */
	@Override
	public void apply(RequestTemplate template) {
		template.header(header); // Clears out the header, no "clear" method available.
		template.header(header, extract(tokenType));
	}

	/**
	 * Extracts the token extract id the access token exists or returning an empty extract
	 * if there is no one on the context it may occasionally causes Unauthorized response
	 * since the token extract is empty.
	 * @param tokenType type name of token
	 * @return token value from context if it exists otherwise empty String
	 */
	protected String extract(String tokenType) {
		OAuth2AccessToken accessToken = getToken();
		return String.format("%s %s", tokenType, accessToken.getValue());
	}

	/**
	 * Extract the access token within the request or try to acquire a new one by
	 * delegating it to {@link #acquireAccessToken()}.
	 * @return valid token
	 */
	public OAuth2AccessToken getToken() {

		OAuth2AccessToken accessToken = oAuth2ClientContext.getAccessToken();
		if (accessToken == null || accessToken.isExpired()) {
			try {
				accessToken = acquireAccessToken();
			}
			catch (UserRedirectRequiredException e) {
				oAuth2ClientContext.setAccessToken(null);
				String stateKey = e.getStateKey();
				if (stateKey != null) {
					Object stateToPreserve = e.getStateToPreserve();
					if (stateToPreserve == null) {
						stateToPreserve = "NONE";
					}
					oAuth2ClientContext.setPreservedState(stateKey, stateToPreserve);
				}
				throw e;
			}
		}
		return accessToken;
	}

	/**
	 * Try to acquire the token using a access token provider.
	 * @return valid access token
	 * @throws UserRedirectRequiredException in case the user needs to be redirected to an
	 * approval page or login page
	 */
	protected OAuth2AccessToken acquireAccessToken() throws UserRedirectRequiredException {
		AccessTokenRequest tokenRequest = oAuth2ClientContext.getAccessTokenRequest();
		if (tokenRequest == null) {
			throw new AccessTokenRequiredException(
					"Cannot find valid context on request for resource '" + resource.getId() + "'.", resource);
		}
		String stateKey = tokenRequest.getStateKey();
		if (stateKey != null) {
			tokenRequest.setPreservedState(oAuth2ClientContext.removePreservedState(stateKey));
		}
		OAuth2AccessToken existingToken = oAuth2ClientContext.getAccessToken();
		if (existingToken != null) {
			oAuth2ClientContext.setAccessToken(existingToken);
		}
		OAuth2AccessToken obtainableAccessToken;
		obtainableAccessToken = accessTokenProvider.obtainAccessToken(resource, tokenRequest);
		if (obtainableAccessToken == null || obtainableAccessToken.getValue() == null) {
			throw new IllegalStateException(
					" Access token provider returned a null token, which is illegal according to the contract.");
		}
		oAuth2ClientContext.setAccessToken(obtainableAccessToken);
		return obtainableAccessToken;
	}

	public void setAccessTokenProvider(AccessTokenProvider accessTokenProvider) {
		this.accessTokenProvider = accessTokenProvider;
	}

}
