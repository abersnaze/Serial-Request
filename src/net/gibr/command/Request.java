package net.gibr.command;

public interface Request {
	public byte[] getBytesToSend();
	public ResponseParser getResponseParser();
}
