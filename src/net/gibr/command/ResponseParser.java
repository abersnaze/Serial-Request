package net.gibr.command;

public abstract class ResponseParser {
	public byte[] buffer;
	public byte bytesReceived = 0;

	public ResponseParser(int bufferSize) {
		buffer = new byte[bufferSize];
	}

	/**
	 * @return true if the response is expecting more bytes before it is complete.
	 */
	public final boolean needsMore() {
		return bytesReceived < buffer.length;
	}

	/**
	 * As bytes are received from the serial port they are pushed into the response object to be parsed.
	 * 
	 * @param value
	 */
	public final void pushByteReceived(byte value) {
		if (!needsMore())
			throw new ResponseException();
		synchronized (this) {
			buffer[bytesReceived++] = value;
			if (!needsMore()) {
				notifyAll();
			}
		}
	}

	protected final byte[] getResponseBytes() {
		byte[] copy = new byte[bytesReceived];
		System.arraycopy(buffer, 0, copy, 0, bytesReceived);
		return copy;
	}

	/**
	 * Once the response has all of the bytes that it needs it can determine if the response was received correctly and produce a response object.
	 * 
	 * @return
	 */
	public abstract Response getResponse();

	/**
	 * Pause this thread of execution until this response has been returned and parsed or until the timeout.
	 * 
	 * @param timeout
	 * @return
	 */
	public final boolean waitForCompletion(int timeout) {
		if (needsMore()) {
			synchronized (this) {
				if (needsMore()) {
					try {
						wait(timeout);
					} catch (InterruptedException e) {
						return true;
					}
				}
			}
			return !needsMore();
		}
		return true;
	}
}
