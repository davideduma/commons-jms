package co.com.bancolombia.jms.sample.noreactive.domain.exceptions;

public class ParseMessageException extends RuntimeException {
    public ParseMessageException(Throwable cause) {
        super(cause);
    }
}
