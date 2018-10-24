package org.sea9.android.secret.core;

class AsyncTaskResponse {
	private int status;
	private String message;
	private String errors;

	AsyncTaskResponse() {
		status = -1;
		message = null;
		errors = null;
	}

	final int getStatus() { return status; }
	final AsyncTaskResponse setStatus(int status) {
		this.status = status;
		return this;
	}

	final String getMessage() { return message; }
	final AsyncTaskResponse setMessage(String message) {
		this.message = message;
		return this;
	}

	final String getErrors() { return errors; }
	final AsyncTaskResponse setErrors(String error) {
		if (errors == null) {
			errors = error;
		} else {
			errors += '\n' + error;
		}
		return this;
	}
}