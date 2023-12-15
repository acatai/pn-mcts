package utils;

public class Utils {
    public static String ratio(int wins, int games) {
        if (games <= 0)
            return "NaN";
        return "" + Math.round(1000.0 * wins / games) / 1000.0;
    }
}
