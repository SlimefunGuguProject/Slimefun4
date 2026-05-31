package io.github.thebusybiscuit.slimefun4.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Final_ROOT
 */
@SuppressWarnings({"GrazieInspection", "JavaExistingMethodCanBeUsed"})
public class JavaUtil {

    @SafeVarargs
    public static <T> @NotNull Set<T> toSet(T @NotNull ... objects) {
        Set<T> result = new HashSet<>(objects.length);
        result.addAll(Arrays.asList(objects));
        return result;
    }

    @SafeVarargs
    public static <T> @NotNull List<T> toList(T @NotNull ... objects) {
        List<T> result = new ArrayList<>(objects.length);
        result.addAll(Arrays.asList(objects));
        return result;
    }

    public static int[] toArray(@NotNull List<Integer> list) {
        int[] result = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }

    public static int[] reserve(int @NotNull [] objects) {
        int[] result = objects.clone();
        for (int i = 0; i < objects.length; i++) {
            result[i] = objects[objects.length - 1 - i];
        }
        return result;
    }

    public static <T> @NotNull List<T> reserve(@NotNull List<T> objectList) {
        List<T> result = new ArrayList<>(objectList);
        for (int i = 0; i < objectList.size(); i++) {
            result.set(i, objectList.get(objectList.size() - 1 - i));
        }
        return result;
    }

    public static int[] shuffle(int @NotNull [] objects) {
        List<Integer> collect = Arrays.stream(objects).boxed().collect(Collectors.toList());
        Collections.shuffle(collect);
        int[] result = objects.clone();
        for (int i = 0; i < collect.size(); i++) {
            result[i] = collect.get(i);
        }
        return result;
    }

    public static <T> T[] shuffle(T @NotNull [] objects) {
        List<T> collect = Arrays.stream(objects).collect(Collectors.toList());
        Collections.shuffle(collect);
        T[] result = objects.clone();
        for (int i = 0; i < collect.size(); i++) {
            result[i] = collect.get(i);
        }
        return result;
    }

    public static <T> @NotNull List<T> shuffle(@NotNull List<T> objectList) {
        List<T> list = new ArrayList<>(objectList);
        Collections.shuffle(list);
        return list;
    }

    public static double[] disperse(int size, Number @NotNull ... value) {
        if (size == 1 && value.length > 0) {
            return new double[] {value[0].doubleValue()};
        } else if (size == 0 || value.length == 0) {
            return new double[0];
        }
        double[] result = new double[size--];
        for (int i = 0; i <= size; i++) {
            double p = ((double) i) / size * (value.length - 1);
            double value1 = value[(int) Math.floor(p)].doubleValue() * (1 - p + Math.floor(p));
            double value2 = value[(int) Math.ceil(p)].doubleValue() * (p - Math.floor(p));
            result[i] = value1 + value2;
        }
        return result;
    }

    public static String @NotNull [] split(@NotNull String string) {
        String[] result = new String[string.length()];
        for (int i = 0; i < string.length(); i++) {
            result[i] = String.valueOf(string.charAt(i));
        }
        return result;
    }

    /**
     * Generate random int[] contains 0 1 2 ...... length-1
     * for example, while the input length = 3, the output may be [0, 1, 2] or [0, 2, 1] or [1, 0, 2] or [1, 2, 0] or [2, 0, 1] or [2, 1, 0]
     *
     * @param length the length of the array
     * @return int[]
     */
    public static int[] generateRandomInts(int length) {
        int[] result = new int[length];
        for (int i = 0; i < result.length; i++) {
            result[i] = i;
        }
        return JavaUtil.shuffle(result);
    }

    public static int[] generateInts(int length) {
        int[] result = new int[length];
        for (int i = 0; i < result.length; i++) {
            result[i] = i;
        }
        return result;
    }

    /**
     * shuffle a list by int[]
     * outputList[0] = inputList[int[0]]
     * for example:
     * input:
     * list: [a, b, c, d, e, f]
     * int:[2, 1, 0, 4, 5, 3]
     * output:
     * list: [c, b, a, e, f, d]
     *
     * @param list the list to shuffle
     * @param ints the int[]
     * @param <T>  the type of the list
     * @return the shuffled list
     */
    public static <T> @NotNull List<T> shuffleByInts(@NotNull List<T> list, int @NotNull [] ints) {
        List<T> result = new ArrayList<>(list.size());
        for (int anInt : ints) {
            result.add(list.get(anInt));
        }
        return result;
    }

    public static String @NotNull [] addToFirst(String value, String @NotNull ... values) {
        String[] result = new String[values.length + 1];
        result[0] = value;
        System.arraycopy(values, 0, result, 1, values.length);
        return result;
    }

    @SafeVarargs
    public static <T> boolean matchOnce(T source, T @NotNull ... targets) {
        for (T object : targets) {
            if (object.equals(source)) {
                return true;
            }
        }
        return false;
    }

    public static long testTime(@NotNull Runnable runnable) {
        long beginTime = System.nanoTime();
        runnable.run();
        return System.nanoTime() - beginTime;
    }

    /**
     * @return In most case, it will not return null. (￣▽￣)"
     */
    @SafeVarargs
    public static <T> @Nullable T getFirstNonnull(T @NotNull ... objects) {
        for (T object : objects) {
            if (object != null) {
                return object;
            }
        }
        return null;
    }
}
