package io.github.thebusybiscuit.slimefun4.libraries.commons.lang.exception;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;

public class NestableDelegate implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final transient String MUST_BE_THROWABLE =
            "The Nestable implementation passed to the NestableDelegate(Nestable) constructor must extend java.lang.Throwable";

    @Nonnull
    private Throwable nestable;

    public static boolean topDown = true;
    public static boolean trimStackFrames = true;
    public static boolean matchSubclasses = true;

    public NestableDelegate(io.github.thebusybiscuit.slimefun4.libraries.commons.lang.exception.Nestable nestable) {
        if (nestable instanceof Throwable) {
            this.nestable = (Throwable) nestable;
        } else {
            throw new IllegalArgumentException(
                    "The Nestable implementation passed to the NestableDelegate(Nestable) constructor must extend java.lang.Throwable");
        }
    }

    public String getMessage(int index) {
        Throwable t = this.getThrowable(index);
        return (t instanceof io.github.thebusybiscuit.slimefun4.libraries.commons.lang.exception.Nestable delegate
                ? delegate.getMessage(0)
                : t.getMessage());
    }

    public String getMessage(String baseMsg) {
        Throwable nestedCause = ExceptionUtils.getCause(this.nestable);
        String causeMsg = nestedCause == null ? null : nestedCause.getMessage();
        if (nestedCause != null && causeMsg != null) {
            return baseMsg == null ? causeMsg : baseMsg + ": " + causeMsg;
        } else {
            return baseMsg;
        }
    }

    public String[] getMessages() {
        Throwable[] throwables = this.getThrowables();
        String[] msgs = new String[throwables.length];

        for (int i = 0; i < throwables.length; ++i) {
            msgs[i] = (throwables[i] instanceof Nestable
                    ? ((Nestable) throwables[i]).getMessage(0)
                    : throwables[i].getMessage());
        }

        return msgs;
    }

    public Throwable getThrowable(int index) {
        if (index == 0) {
            return this.nestable;
        } else {
            Throwable[] throwables = this.getThrowables();
            return throwables[index];
        }
    }

    public int getThrowableCount() {
        return ExceptionUtils.getThrowableCount(this.nestable);
    }

    public Throwable[] getThrowables() {
        return ExceptionUtils.getThrowables(this.nestable);
    }

    public int indexOfThrowable(Class type, int fromIndex) {
        if (type == null) {
            return -1;
        } else if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("The start index was out of bounds: " + fromIndex);
        } else {
            Throwable[] throwables = ExceptionUtils.getThrowables(this.nestable);
            if (fromIndex >= throwables.length) {
                throw new IndexOutOfBoundsException(
                        "The start index was out of bounds: " + fromIndex + " >= " + throwables.length);
            } else {
                int i;
                if (matchSubclasses) {
                    for (i = fromIndex; i < throwables.length; ++i) {
                        if (type.isAssignableFrom(throwables[i].getClass())) {
                            return i;
                        }
                    }
                } else {
                    for (i = fromIndex; i < throwables.length; ++i) {
                        if (type.equals(throwables[i].getClass())) {
                            return i;
                        }
                    }
                }

                return -1;
            }
        }
    }

    public void printStackTrace() {
        this.printStackTrace(System.err);
    }

    public void printStackTrace(PrintStream out) {
        synchronized (out) {
            PrintWriter pw = new PrintWriter(out, false);
            this.printStackTrace(pw);
            pw.flush();
        }
    }

    public void printStackTrace(PrintWriter out) {
        Throwable throwable = this.nestable;
        if (ExceptionUtils.isThrowableNested()) {
            if (throwable instanceof Nestable) {
                ((Nestable) throwable).printPartialStackTrace(out);
            } else {
                throwable.printStackTrace(out);
            }

        } else {
            ArrayList stacks;
            for (stacks = new ArrayList(); throwable != null; throwable = ExceptionUtils.getCause(throwable)) {
                String[] st = this.getStackFrames(throwable);
                stacks.add(st);
            }

            String separatorLine = "Caused by: ";
            if (!topDown) {
                separatorLine = "Rethrown as: ";
                Collections.reverse(stacks);
            }

            if (trimStackFrames) {
                this.trimStackFrames(stacks);
            }

            synchronized (out) {
                Iterator iter = stacks.iterator();

                while (iter.hasNext()) {
                    String[] st = (String[]) ((String[]) iter.next());
                    int i = 0;

                    for (int len = st.length; i < len; ++i) {
                        out.println(st[i]);
                    }

                    if (iter.hasNext()) {
                        out.print(separatorLine);
                    }
                }
            }
        }
    }

    protected String[] getStackFrames(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        if (t instanceof Nestable) {
            ((Nestable) t).printPartialStackTrace(pw);
        } else {
            t.printStackTrace(pw);
        }

        return ExceptionUtils.getStackFrames(sw.getBuffer().toString());
    }

    protected void trimStackFrames(List stacks) {
        int size = stacks.size();

        for (int i = size - 1; i > 0; --i) {
            String[] curr = (String[]) ((String[]) stacks.get(i));
            String[] next = (String[]) ((String[]) stacks.get(i - 1));
            List currList = new ArrayList(Arrays.asList(curr));
            List nextList = new ArrayList(Arrays.asList(next));
            ExceptionUtils.removeCommonFrames(currList, nextList);
            int trimmed = curr.length - currList.size();
            if (trimmed > 0) {
                currList.add("\t... " + trimmed + " more");
                stacks.set(i, currList.toArray(new String[currList.size()]));
            }
        }
    }
}
