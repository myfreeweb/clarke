package technology.unrelenting.clarke;

public class CompilerException extends Exception {

    public CompilerException(String message) {
        super(message);
    }

    public CompilerException(Throwable cause) {
        super(cause);
    }

    public CompilerException(String message, Throwable cause) {
        super(message, cause);
    }

}
