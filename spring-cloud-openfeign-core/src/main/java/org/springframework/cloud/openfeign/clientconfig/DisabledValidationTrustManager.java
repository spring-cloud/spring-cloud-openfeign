/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.cloud.openfeign.clientconfig;

import java.net.Socket;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;

/**
 * Trusts every certificate, for
 * {@code spring.cloud.openfeign.httpclient.disable-ssl-validation}. Shared by
 * {@link HttpClient5FeignConfiguration} and {@link Http2ClientFeignConfiguration}.
 * <p>
 * It extends {@link X509ExtendedTrustManager} because the JDK
 * {@link java.net.http.HttpClient} delegates hostname verification to an extended trust
 * manager, so this also skips hostname verification there, like the Apache HttpClient 5
 * configuration does with its no-op hostname verifier.
 *
 * @author Rajesh Vasa
 */
final class DisabledValidationTrustManager extends X509ExtendedTrustManager {

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) {
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) {
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return new X509Certificate[0];
	}

}
