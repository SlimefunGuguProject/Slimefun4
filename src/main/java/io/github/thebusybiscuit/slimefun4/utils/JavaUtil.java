package io.github.thebusybiscuit.slimefun4.utils;

import org.jetbrains.annotations.NotNull;

/**
 * @author Final_ROOT
 */
public class JavaUtil {
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
}
