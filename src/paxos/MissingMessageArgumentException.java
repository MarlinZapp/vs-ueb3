package paxos;

public class MissingMessageArgumentException extends RuntimeException {

    // Constructor that accepts only a message
    public MissingMessageArgumentException(String message) {
        super(message);
    }

    // Constructor that accepts a message and a cause
    public MissingMessageArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

    // Optional: Constructor that accepts only a cause
    public MissingMessageArgumentException(Throwable cause) {
        super(cause);
    }

}
