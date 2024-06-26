package org.springframework.cloud.openfeign.security;
import org.springframework.beans.factory.annotation.Autowired;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;

/**
 * A {@link RequestInterceptor} for Json-Web-Token Feign Requests. Instead of manual
 * implementation by the developers. They can add the parameter as JsonToken.and call this
 * class. We will be overriding the apply method by setting up the token inside the
 * header. so that the token can be passed among different services.
 *
 * @author Harish Babu
 */

public class JwtAccessTokenInterceptor implements RequestInterceptor {

	private static final String HEADER = "Authorization";

	@Autowired
	private HttpServletRequest httpServletRequest;

	public String getToken() {
		return httpServletRequest.getHeader(HEADER);
	}

	@Override
	public void apply(RequestTemplate template) {
		template.header(HEADER, getToken());
	}

}