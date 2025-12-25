package io.github.thebusybiscuit.slimefun4.libraries.commons.lang.exception;

import java.io.PrintStream;
import java.io.PrintWriter;

public interface Nestable {
    Throwable getCause();

    String getMessage();

    String getMessage(int var1);

    String[] getMessages();

    Throwable getThrowable(int var1);

    int getThrowableCount();

    Throwable[] getThrowables();

    int indexOfThrowable(Class var1);

    int indexOfThrowable(Class var1, int var2);

    void printStackTrace(PrintWriter var1);

    void printStackTrace(PrintStream var1);

    void printPartialStackTrace(PrintWriter var1);
}
