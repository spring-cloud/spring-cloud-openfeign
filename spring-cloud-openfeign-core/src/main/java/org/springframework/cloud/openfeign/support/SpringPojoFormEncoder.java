package org.springframework.cloud.openfeign.support;

import feign.form.MultipartFormContentProcessor;
import feign.form.spring.SpringFormEncoder;

import static feign.form.ContentType.MULTIPART;

public class SpringPojoFormEncoder extends SpringFormEncoder {
	public SpringPojoFormEncoder(PojoSerializationWriter pojoSerializationWriter) {
		super();

		MultipartFormContentProcessor processor = (MultipartFormContentProcessor) getContentProcessor(MULTIPART);
		processor.addFirstWriter(pojoSerializationWriter);
	}
}
