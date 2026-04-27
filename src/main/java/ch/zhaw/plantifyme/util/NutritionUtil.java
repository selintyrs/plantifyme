package ch.zhaw.plantifyme.util;

public final class NutritionUtil {

    private NutritionUtil() {}

    public static Double toGrams(Double amount, String unit) {
        if (amount == null || unit == null) return null;
        String u = unit.toLowerCase().trim();

        switch (u) {
            case "g": return amount;
            case "kg": return amount * 1000;
            case "mg": return amount / 1000;
            case "ml": return amount;
            case "l": return amount * 1000;
            case "dl": return amount * 100;
            case "el":
            case "esslöffel":
                return amount * 15;
            case "tl":
            case "teelöffel":
                return amount * 5;
            case "tasse":
                return amount * 240;
            case "stück":
            case "stk":
            case "":
            case "prise":
                return null;
            default:
                return null;
        }
    }
}
