/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.openfeign.ribbon;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import feign.Request;
import feign.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.util.StreamUtils;

import static feign.Request.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ryan Baxter
 */
@RunWith(MockitoJUnitRunner.class)
public class RibbonResponseStatusCodeExceptionTest {

	@Test
	public void getResponse() throws Exception {
		Map<String, Collection<String>> headers = new HashMap<>();
		List<String> fooValues = new ArrayList<>();
		fooValues.add("bar");
		headers.put("foo", fooValues);
		Request request = Request.create(GET, "https://service.com", new HashMap<>(),
				new byte[] {}, Charset.defaultCharset(), null);
		byte[] body = "foo".getBytes();
		ByteArrayInputStream is = new ByteArrayInputStream(body);
		Response response = Response.builder().status(200).reason("Success")
				.request(request).body(is, body.length).headers(headers).build();
		RibbonResponseStatusCodeException ex = new RibbonResponseStatusCodeException(
				"service", response, body, new URI(request.url()));
		assertThat(ex.getResponse().status()).isEqualTo(200);
		assertThat(ex.getResponse().request()).isEqualTo(request);
		assertThat(ex.getResponse().reason()).isEqualTo("Success");
		assertThat(StreamUtils.copyToString(ex.getResponse().body().asInputStream(),
				Charset.defaultCharset())).isEqualTo("foo");
	}

}
