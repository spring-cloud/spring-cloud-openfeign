package org.springframework.cloud.openfeign.reactive.utils;

import static org.springframework.cloud.openfeign.reactive.utils.HttpUtils.StatusCodeFamily.*;

public class HttpUtils {

	public static StatusCodeFamily familyOf(final int statusCode) {
		switch (statusCode / 100) {
			case 1:
				return INFORMATIONAL;
			case 2:
				return SUCCESSFUL;
			case 3:
				return REDIRECTION;
			case 4:
				return CLIENT_ERROR;
			case 5:
				return SERVER_ERROR;
			default:
				return OTHER;
		}
	}

	public enum StatusCodeFamily {
		INFORMATIONAL(false),
		SUCCESSFUL(false),
		REDIRECTION(false),
		CLIENT_ERROR(true),
		SERVER_ERROR(true),
		OTHER(false);

		private final boolean error;

		StatusCodeFamily(boolean error) {
			this.error = error;
		}

		public boolean isError() {
			return error;
		}
	}
}
