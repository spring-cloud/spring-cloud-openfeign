/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.cloud.openfeign.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.encoding.HttpEncoding;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.cloud.openfeign.support.FeignUtils.getHeaders;
import static org.springframework.cloud.openfeign.support.FeignUtils.getHttpHeaders;

/**
 * @author Spencer Gibb
 * @author Scien Jus
 * @author Ahmad Mozafarnia
 * @author Aaron Whiteside
 * @author Darren Foong
 * @author Olga Maciaszek-Sharma
 */
@SuppressWarnings("rawtypes")
public class SpringEncoder implements Encoder {

	private static final Log log = LogFactory.getLog(SpringEncoder.class);

	private final SpringFormEncoder springFormEncoder;

	private final ObjectFactory<HttpMessageConverters> messageConverters;

	private final FeignEncoderProperties encoderProperties;

	public SpringEncoder(ObjectFactory<HttpMessageConverters> messageConverters) {
		this(new SpringFormEncoder(), messageConverters);
	}

	public SpringEncoder(SpringFormEncoder springFormEncoder,
			ObjectFactory<HttpMessageConverters> messageConverters) {
		this(springFormEncoder, messageConverters, new FeignEncoderProperties());
	}

	public SpringEncoder(SpringFormEncoder springFormEncoder,
			ObjectFactory<HttpMessageConverters> messageConverters,
			FeignEncoderProperties encoderProperties) {
		this.springFormEncoder = springFormEncoder;
		this.messageConverters = messageConverters;
		this.encoderProperties = encoderProperties;
	}

	@Override
	public void encode(Object requestBody, Type bodyType, RequestTemplate request)
			throws EncodeException {
		// template.body(conversionService.convert(object, String.class));
		if (requestBody != null) {
			Collection<String> contentTypes = request.headers()
					.get(HttpEncoding.CONTENT_TYPE);

			MediaType requestContentType = null;
			if (contentTypes != null && !contentTypes.isEmpty()) {
				String type = contentTypes.iterator().next();
				requestContentType = MediaType.valueOf(type);
			}

			if (isMultipartType(requestContentType)) {
				this.springFormEncoder.encode(requestBody, bodyType, request);
				return;
			}
			else {
				if (bodyType == MultipartFile.class) {
					log.warn(
							"For MultipartFile to be handled correctly, the 'consumes' parameter of @RequestMapping "
									+ "should be specified as MediaType.MULTIPART_FORM_DATA_VALUE");
				}
			}
			encodeWithMessageConverter(requestBody, bodyType, request,
					requestContentType);
		}
	}

	private void encodeWithMessageConverter(Object requestBody, Type bodyType,
			RequestTemplate request, MediaType requestContentType) {
		for (HttpMessageConverter messageConverter : this.messageConverters.getObject()
				.getConverters()) {
			FeignOutputMessage outputMessage;
			try {
				if (messageConverter instanceof GenericHttpMessageConverter) {
					outputMessage = checkAndWrite(requestBody, bodyType,
							requestContentType,
							(GenericHttpMessageConverter) messageConverter, request);
				}
				else {
					outputMessage = checkAndWrite(requestBody, requestContentType,
							messageConverter, request);
				}
			}
			catch (IOException | HttpMessageConversionException ex) {
				throw new EncodeException("Error converting request body", ex);
			}
			if (outputMessage != null) {
				// clear headers
				request.headers(null);
				// converters can modify headers, so update the request
				// with the modified headers
				request.headers(getHeaders(outputMessage.getHeaders()));

				// do not use charset for binary data and protobuf
				Charset charset;

				MediaType contentType = outputMessage.getHeaders().getContentType();
				Charset charsetFromContentType = contentType != null
						? contentType.getCharset() : null;

				if (encoderProperties != null
						&& encoderProperties.isCharsetFromContentType()
						&& charsetFromContentType != null) {
					charset = charsetFromContentType;
				}
				else if (shouldHaveNullCharset(messageConverter, outputMessage)) {
					charset = null;
				}
				else {
					charset = StandardCharsets.UTF_8;
				}
				request.body(outputMessage.getOutputStream().toByteArray(), charset);
				return;
			}
		}
		String message = "Could not write request: no suitable HttpMessageConverter "
				+ "found for request type [" + requestBody.getClass().getName() + "]";
		if (requestContentType != null) {
			message += " and content type [" + requestContentType + "]";
		}
		throw new EncodeException(message);
	}

	private boolean shouldHaveNullCharset(HttpMessageConverter messageConverter,
			FeignOutputMessage outputMessage) {
		return binaryContentType(outputMessage)
				|| messageConverter instanceof ByteArrayHttpMessageConverter
				|| messageConverter instanceof ProtobufHttpMessageConverter
						&& ProtobufHttpMessageConverter.PROTOBUF.isCompatibleWith(
								outputMessage.getHeaders().getContentType());
	}

	@SuppressWarnings("unchecked")
	private FeignOutputMessage checkAndWrite(Object body, MediaType contentType,
			HttpMessageConverter converter, RequestTemplate request) throws IOException {
		if (converter.canWrite(body.getClass(), contentType)) {
			logBeforeWrite(body, contentType, converter);
			FeignOutputMessage outputMessage = new FeignOutputMessage(request);
			converter.write(body, contentType, outputMessage);
			return outputMessage;
		}
		else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private FeignOutputMessage checkAndWrite(Object body, Type genericType,
			MediaType contentType, GenericHttpMessageConverter converter,
			RequestTemplate request) throws IOException {
		if (converter.canWrite(genericType, body.getClass(), contentType)) {
			logBeforeWrite(body, contentType, converter);
			FeignOutputMessage outputMessage = new FeignOutputMessage(request);
			converter.write(body, genericType, contentType, outputMessage);
			return outputMessage;
		}
		else {
			return null;
		}
	}

	private void logBeforeWrite(Object requestBody, MediaType requestContentType,
			HttpMessageConverter messageConverter) {
		if (log.isDebugEnabled()) {
			if (requestContentType != null) {
				log.debug("Writing [" + requestBody + "] as \"" + requestContentType
						+ "\" using [" + messageConverter + "]");
			}
			else {
				log.debug(
						"Writing [" + requestBody + "] using [" + messageConverter + "]");
			}
		}
	}

	private boolean isMultipartType(MediaType requestContentType) {
		return Arrays.asList(MediaType.MULTIPART_FORM_DATA, MediaType.MULTIPART_MIXED,
				MediaType.MULTIPART_RELATED).contains(requestContentType);
	}

	private boolean binaryContentType(FeignOutputMessage outputMessage) {
		MediaType contentType = outputMessage.getHeaders().getContentType();
		return contentType == null || Stream
				.of(MediaType.APPLICATION_CBOR, MediaType.APPLICATION_OCTET_STREAM,
						MediaType.APPLICATION_PDF, MediaType.IMAGE_GIF,
						MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG)
				.anyMatch(mediaType -> mediaType.includes(contentType));
	}

	private final class FeignOutputMessage implements HttpOutputMessage {

		private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		private final HttpHeaders httpHeaders;

		private FeignOutputMessage(RequestTemplate request) {
			this.httpHeaders = getHttpHeaders(request.headers());
		}

		@Override
		public OutputStream getBody() throws IOException {
			return this.outputStream;
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.httpHeaders;
		}

		public ByteArrayOutputStream getOutputStream() {
			return this.outputStream;
		}

	}

}
