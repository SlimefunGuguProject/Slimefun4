package io.github.thebusybiscuit.slimefun4.libraries.commons.lang;

import java.io.PrintStream;
import java.io.PrintWriter;
import org.apache.commons.lang.exception.Nestable;
import org.apache.commons.lang.exception.NestableDelegate;

@Deprecated(forRemoval = true)
public class NotImplementedException extends UnsupportedOperationException implements Nestable {
    private static final String DEFAULT_MESSAGE = "Code is not implemented";
    private static final long serialVersionUID = -6894122266938754088L;
    private NestableDelegate delegate = new NestableDelegate(this);
    private Throwable cause;

    public NotImplementedException() {
        super("Code is not implemented");
    }

    public NotImplementedException(String msg) {
        super(msg == null ? "Code is not implemented" : msg);
    }

    public NotImplementedException(Throwable cause) {
        super("Code is not implemented");
        this.cause = cause;
    }

    public NotImplementedException(String msg, Throwable cause) {
        super(msg == null ? "Code is not implemented" : msg);
        this.cause = cause;
    }

    public NotImplementedException(Class clazz) {
        super(clazz == null ? "Code is not implemented" : "Code is not implemented in " + clazz);
    }

    public Throwable getCause() {
        return this.cause;
    }

    public String getMessage() {
        if (super.getMessage() != null) {
            return super.getMessage();
        } else {
            return this.cause != null ? this.cause.toString() : null;
        }
    }

    public String getMessage(int index) {
        return index == 0 ? super.getMessage() : this.delegate.getMessage(index);
    }

    public String[] getMessages() {
        return this.delegate.getMessages();
    }

    public Throwable getThrowable(int index) {
        return this.delegate.getThrowable(index);
    }

    public int getThrowableCount() {
        return this.delegate.getThrowableCount();
    }

    public Throwable[] getThrowables() {
        return this.delegate.getThrowables();
    }

    public int indexOfThrowable(Class type) {
        return this.delegate.indexOfThrowable(type, 0);
    }

    public int indexOfThrowable(Class type, int fromIndex) {
        return this.delegate.indexOfThrowable(type, fromIndex);
    }

    public void printStackTrace() {
        this.delegate.printStackTrace();
    }

    public void printStackTrace(PrintStream out) {
        this.delegate.printStackTrace(out);
    }

    public void printStackTrace(PrintWriter out) {
        this.delegate.printStackTrace(out);
    }

    public final void printPartialStackTrace(PrintWriter out) {
        super.printStackTrace(out);
    }
}
