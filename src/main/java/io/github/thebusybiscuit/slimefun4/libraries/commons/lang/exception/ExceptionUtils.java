package io.github.thebusybiscuit.slimefun4.libraries.commons.lang.exception;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class ExceptionUtils {
    static final String WRAPPED_MARKER = " [wrapped] ";
    public static final String LINE_SEPARATOR = getSystemProperty("line.separator");
    private static final Object CAUSE_METHOD_NAMES_LOCK = new Object();
    private static final String[] CAUSE_METHOD_NAMES_ARRAY = new String[] {
        "getCause",
        "getNextException",
        "getTargetException",
        "getException",
        "getSourceException",
        "getRootCause",
        "getCausedByException",
        "getNested",
        "getLinkedException",
        "getNestedException",
        "getLinkedCause",
        "getThrowable"
    };
    private static final List<String> CAUSE_METHOD_NAMES = List.of(CAUSE_METHOD_NAMES_ARRAY);
    private static final Method THROWABLE_CAUSE_METHOD;
    private static final Method THROWABLE_INITCAUSE_METHOD;

    private static String getSystemProperty(String property) {
        try {
            return System.getProperty(property);
        } catch (SecurityException var2) {
            System.err.println("Caught a SecurityException reading the system property '" + property
                    + "'; the SystemUtils property value will default to null.");
            return null;
        }
    }

    public ExceptionUtils() {}

    public static Throwable getCause(Throwable throwable) {
        synchronized (CAUSE_METHOD_NAMES_LOCK) {
            return getCause(throwable, CAUSE_METHOD_NAMES_ARRAY);
        }
    }

    public static Throwable getCause(Throwable throwable, String[] methodNames) {
        if (throwable == null) {
            return null;
        } else {
            Throwable cause = getCauseUsingWellKnownTypes(throwable);
            if (cause == null) {
                if (methodNames == null) {
                    synchronized (CAUSE_METHOD_NAMES_LOCK) {
                        methodNames = CAUSE_METHOD_NAMES_ARRAY;
                    }
                }

                for (int i = 0; i < methodNames.length; ++i) {
                    String methodName = methodNames[i];
                    if (methodName != null) {
                        cause = getCauseUsingMethodName(throwable, methodName);
                        if (cause != null) {
                            break;
                        }
                    }
                }

                if (cause == null) {
                    cause = getCauseUsingFieldName(throwable, "detail");
                }
            }

            return cause;
        }
    }

    private static Throwable getCauseUsingWellKnownTypes(Throwable throwable) {
        if (throwable instanceof Nestable) {
            return ((Nestable) throwable).getCause();
        } else if (throwable instanceof SQLException) {
            return ((SQLException) throwable).getNextException();
        } else {
            return throwable instanceof InvocationTargetException
                    ? ((InvocationTargetException) throwable).getTargetException()
                    : null;
        }
    }

    private static Throwable getCauseUsingMethodName(Throwable throwable, String methodName) {
        Method method = null;

        try {
            method = throwable.getClass().getMethod(methodName, (Class[]) null);
        } catch (NoSuchMethodException var7) {
        } catch (SecurityException var8) {
        }

        if (method != null && Throwable.class.isAssignableFrom(method.getReturnType())) {
            try {
                return (Throwable) method.invoke(throwable, new Object[0]);
            } catch (IllegalAccessException var4) {
            } catch (IllegalArgumentException var5) {
            } catch (InvocationTargetException var6) {
            }
        }

        return null;
    }

    private static Throwable getCauseUsingFieldName(Throwable throwable, String fieldName) {
        Field field = null;

        try {
            field = throwable.getClass().getField(fieldName);
        } catch (NoSuchFieldException var6) {
        } catch (SecurityException var7) {
        }

        if (field != null && (Throwable.class.isAssignableFrom(field.getType()))) {
            try {
                return (Throwable) field.get(throwable);
            } catch (IllegalAccessException var4) {
            } catch (IllegalArgumentException var5) {
            }
        }

        return null;
    }

    public static boolean isThrowableNested() {
        return THROWABLE_CAUSE_METHOD != null;
    }

    public static int getThrowableCount(Throwable throwable) {
        return getThrowableList(throwable).size();
    }

    public static Throwable[] getThrowables(Throwable throwable) {
        List list = getThrowableList(throwable);
        return (Throwable[]) ((Throwable[]) list.toArray(new Throwable[list.size()]));
    }

    public static List getThrowableList(Throwable throwable) {
        ArrayList list;
        for (list = new ArrayList(); throwable != null && !list.contains(throwable); throwable = getCause(throwable)) {
            list.add(throwable);
        }

        return list;
    }

    static String[] getStackFrames(String stackTrace) {
        String linebreak = LINE_SEPARATOR;
        StringTokenizer frames = new StringTokenizer(stackTrace, linebreak);
        List list = new ArrayList();

        while (frames.hasMoreTokens()) {
            list.add(frames.nextToken());
        }

        return (String[]) list.toArray(new String[list.size()]);
    }

    public static void removeCommonFrames(List causeFrames, List wrapperFrames) {
        if (causeFrames != null && wrapperFrames != null) {
            int causeFrameIndex = causeFrames.size() - 1;

            for (int wrapperFrameIndex = wrapperFrames.size() - 1;
                    causeFrameIndex >= 0 && wrapperFrameIndex >= 0;
                    --wrapperFrameIndex) {
                String causeFrame = (String) causeFrames.get(causeFrameIndex);
                String wrapperFrame = (String) wrapperFrames.get(wrapperFrameIndex);
                if (causeFrame.equals(wrapperFrame)) {
                    causeFrames.remove(causeFrameIndex);
                }

                --causeFrameIndex;
            }

        } else {
            throw new IllegalArgumentException("The List must not be null");
        }
    }

    public static String getShortClassName(Object object, String valueIfNull) {
        return object == null ? valueIfNull : getShortClassName(object.getClass());
    }

    public static String getShortClassName(Class cls) {
        return cls == null ? "" : getShortClassName(cls.getName());
    }

    public static String getShortClassName(String className) {
        if (className == null) {
            return "";
        } else if (className.length() == 0) {
            return "";
        } else {
            StringBuilder arrayPrefix = new StringBuilder();
            if (className.startsWith("[")) {
                while (className.charAt(0) == '[') {
                    className = className.substring(1);
                    arrayPrefix.append("[]");
                }

                if (className.charAt(0) == 'L' && className.charAt(className.length() - 1) == ';') {
                    className = className.substring(1, className.length() - 1);
                }
            }

            int lastDotIdx = className.lastIndexOf(46);
            int innerIdx = className.indexOf(36, lastDotIdx == -1 ? 0 : lastDotIdx + 1);
            String out = className.substring(lastDotIdx + 1);
            if (innerIdx != -1) {
                out = out.replace('$', '.');
            }

            return out + arrayPrefix;
        }
    }

    public static String getMessage(Throwable th) {
        if (th == null) {
            return "";
        } else {
            String clsName = getShortClassName(th, (String) null);
            String msg = th.getMessage();
            return clsName + ": " + (msg == null ? "" : msg);
        }
    }

    static {
        Method causeMethod;
        try {
            causeMethod = Throwable.class.getMethod("getCause", (Class[]) null);
        } catch (Exception var3) {
            causeMethod = null;
        }

        THROWABLE_CAUSE_METHOD = causeMethod;

        try {
            causeMethod = Throwable.class.getMethod("initCause", Throwable.class);
        } catch (Exception var2) {
            causeMethod = null;
        }

        THROWABLE_INITCAUSE_METHOD = causeMethod;
    }
}
