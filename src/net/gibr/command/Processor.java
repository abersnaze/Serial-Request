package net.gibr.command;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Processor {
	private class Transaction {
		public Transaction(Request req, Callback callback, int timeout) {
			this.req = req;
			this.resParser = req.getResponseParser();
			this.callback = callback;
			this.timeout = timeout;
		}
		final Request req;
		final ResponseParser resParser;
		final Callback callback;
		final int timeout;
	}

	public static final int DEFAULT_TIMEOUT = 2000;

	private final SerialPort port;
	private final OutputStream out;
	private final InputStream in;
	private final ConcurrentLinkedQueue<Transaction> queue = new ConcurrentLinkedQueue<Transaction>();
	private Transaction active = null;

	private boolean stopWriter = false;
	private final Thread writerThread = new Thread() {
		@Override
		public void run() {
			// check to see if the run loop should stop.
			while (!stopWriter) {
				while (queue.isEmpty()) {
					synchronized (queue) {
						try {
							if (queue.isEmpty()) {
								// block until someone puts a transaction on the queue.
								queue.wait();
							}
						} catch (InterruptedException e) {
							// do nothing
						}
					}

					// the thread could be unblocked to signal the close of the processor
					if (stopWriter)
						// TODO we may also have to flush the queue if there are pending transactions.
						return;
				}

				// get the next transaction
				Transaction trans = queue.remove();
				assert trans != null : "the transaction queue was not empty but did not return a transaction";

				// set up the parser before the possibility of receiving a response.
				active = trans;

				// send the message
				try {
					out.write(trans.req.getBytesToSend());
					out.flush();
					System.err.println("wrote message");
				} catch (IOException e) {
					// TODO send error response and remove from the queue.
					e.printStackTrace();
				}

				// wait for the completion
				trans.resParser.waitForCompletion(trans.timeout);

				active = null;
			}
		};
	};

	private SerialPortEventListener eventListener = new SerialPortEventListener() {
		@Override
		public void serialEvent(SerialPortEvent event) {
			if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
				Transaction trans = active;

				// receiving bytes but there is nowhere to put them.
				if (trans == null)
					return;

				try {
					while (in.available() > 0) {
						int value = in.read();
						System.err.format("read %02X\n", value);
						trans.resParser.pushByteReceived((byte) value);
						if (!trans.resParser.needsMore()) {
							if (trans.callback != null) {
								trans.callback.requestComplete(trans.req, trans.resParser.getResponse());
							}
						}
					}
				} catch (IOException e) {
					return;
				}
			}
		}
	};

	public Processor(SerialPort port) throws TooManyListenersException, IOException {
		this.port = port;
		port.addEventListener(eventListener);
		port.notifyOnDataAvailable(true);
		out = port.getOutputStream();
		in = port.getInputStream();
		writerThread.start();
	}

	public Response execute(Request req) throws IOException {
		Transaction trans = new Transaction(req, null, DEFAULT_TIMEOUT);
		synchronized (queue) {
			queue.add(trans);
			queue.notify();
		}
		trans.resParser.waitForCompletion(DEFAULT_TIMEOUT);
		return trans.resParser.getResponse();
	}

	public void close() throws IOException {
		stopWriter = true;
		synchronized (queue) {
			queue.notifyAll();
		}
		while (writerThread.isAlive()) {
		}
		in.close();
		out.close();
		port.removeEventListener();
	}
}
