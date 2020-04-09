/*
 * Copyright 2013-2020 the original author or authors.
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

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Predicate;

import feign.codec.EncodeException;
import feign.form.multipart.AbstractWriter;
import feign.form.multipart.Output;
import feign.form.util.PojoUtil;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import static feign.form.ContentProcessor.CRLF;
import static feign.form.util.PojoUtil.isUserPojo;

/**
 * @author Darren Foong
 */
public abstract class AbstractFormWriter extends AbstractWriter {

	@Override
	public boolean isApplicable(Object object) {
		return !isTypeOrCollection(object, o -> o instanceof MultipartFile)
				&& isTypeOrCollection(object, PojoUtil::isUserPojo);
	}

	@Override
	public void write(Output output, String key, Object object) throws EncodeException {
		try {
			String string = new StringBuilder()
					.append("Content-Disposition: form-data; name=\"").append(key)
					.append('"').append(CRLF).append("Content-Type: ")
					.append(getContentType()).append("; charset=")
					.append(output.getCharset().name()).append(CRLF).append(CRLF)
					.append(writeAsString(object)).toString();

			output.write(string);
		}
		catch (IOException e) {
			throw new EncodeException(e.getMessage());
		}
	}

	protected abstract MediaType getContentType();

	protected abstract String writeAsString(Object object) throws IOException;

	private boolean isTypeOrCollection(Object object, Predicate<Object> isType) {
		if (object.getClass().isArray()) {
			Object[] array = (Object[]) object;

			return array.length > 1 && isType.test(array[0]);
		}
		else if (object instanceof Iterable) {
			Iterable<?> iterable = (Iterable<?>) object;
			Iterator<?> iterator = iterable.iterator();

			return iterator.hasNext() && isType.test(iterator.next());
		}
		else {
			return isType.test(object);
		}
	}

}
