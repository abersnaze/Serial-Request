package net.gibr.command;

public interface Response {
	public static final class BooleanResponse implements Response {
		public static final Response TRUE_BOOLEAN_RESPONSE = new BooleanResponse(true);
		public static final Response FALSE_BOOLEAN_RESPONSE = new BooleanResponse(false);

		private final boolean value;

		private BooleanResponse(boolean value) {
			this.value = value;
		}

		public boolean getValue() {
			return value;
		}

		@Override
		public String toString() {
			return super.toString() + " value=" + value;
		}
	}

	public static final class IntegerResponse implements Response {
		public static IntegerResponse valueOf(int value) {
			return new IntegerResponse(value);
		}

		private final int value;

		private IntegerResponse(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		@Override
		public String toString() {
			return super.toString() + " value=" + value;
		}
	}
}
