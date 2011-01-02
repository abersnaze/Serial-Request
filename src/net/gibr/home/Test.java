package net.gibr.home;

import java.io.IOException;
import java.util.TooManyListenersException;

import net.gibr.command.Processor;
import net.gibr.command.Request;
import net.gibr.command.Response;
import net.gibr.command.ResponseParser;
import net.gibr.command.Response.BooleanResponse;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

public class Test {
	private static final CommCheck COMM_CHECK = new CommCheck();

	private static final class CommCheck implements Request {
		@Override
		public byte[] getBytesToSend() {
			return new byte[] { 0x70, 0x12, 0x34 };
		}

		@Override
		public ResponseParser getResponseParser() {
			return new ResponseParser(3) {
				@Override
				public Response getResponse() {
					byte[] resBytes = getResponseBytes();
					if (resBytes.length == 3 && resBytes[0] == 0x70)
						return BooleanResponse.TRUE_BOOLEAN_RESPONSE;
					return BooleanResponse.FALSE_BOOLEAN_RESPONSE;
				}
			};
		}
	}

	public static void main(String[] args) {
		CommPortIdentifier portIdentifier;
		SerialPort port = null;
		try {
			portIdentifier = CommPortIdentifier.getPortIdentifier("/dev/cu.usbserial");
			System.out.println("found: " + portIdentifier.getName());
			if (portIdentifier.isCurrentlyOwned()) {
				System.out.println("Error: Port is currently in use");
				return;
			}
			port = (SerialPort) portIdentifier.open("Test", 2000);
			port.setSerialPortParams(4800, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			port.setRTS(true);
			port.setDTR(true);
		} catch (NoSuchPortException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PortInUseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedCommOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			Processor proc = new Processor(port);
			Response res;
			do {
				res = proc.execute(COMM_CHECK);
				System.out.println(res);
			} while(!((BooleanResponse) res).getValue());
			proc.close();
		} catch (TooManyListenersException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
