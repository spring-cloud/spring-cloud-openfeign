package org.springframework.cloud.openfeign.support;

import feign.codec.EncodeException;
import feign.form.multipart.AbstractWriter;
import feign.form.multipart.Output;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Iterator;

import static feign.form.ContentProcessor.CRLF;
import static feign.form.util.PojoUtil.isUserPojo;

public abstract class PojoSerializationWriter extends AbstractWriter {
	@Override
	public boolean isApplicable(Object object) {
		return !(object instanceof MultipartFile) && !(object instanceof MultipartFile[])
			&& (isUserPojoCollection(object) || isUserPojo(object));
	}

	@Override
	public void write (Output output, String key, Object object) throws EncodeException {
		try {
			String string = new StringBuilder()
				.append("Content-Disposition: form-data; name=\"").append(key).append('"')
				.append(CRLF)
				.append("Content-Type: ").append(getContentType())
				.append("; charset=").append(output.getCharset().name())
				.append(CRLF)
				.append(CRLF)
				.append(serialize(object))
				.toString();

			output.write(string);
		} catch (IOException e) {
			throw new EncodeException(e.getMessage());
		}
	}

	protected abstract MediaType getContentType();

	protected abstract String serialize(Object object) throws IOException;

	private boolean isUserPojoCollection(Object object) {
		// TODO Refactor!
		if (object.getClass().isArray()) {
			Object[] array = (Object[]) object;

			return array.length > 1 && isUserPojo(array[0]);
		}

		if (!(object instanceof Iterable)) {
			return false;
		}

		Iterable<?> iterable = (Iterable<?>) object;
		Iterator<?> iterator = iterable.iterator();

		if (iterator.hasNext()) {
			Object next = iterator.next();

			return !(next instanceof MultipartFile) && isUserPojo(next);
		} else {
			return false;
		}
	}
}
