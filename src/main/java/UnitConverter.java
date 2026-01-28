import java.util.HashMap;
import java.util.Map;

public class UnitConverter {
    // Une Map qui associe le nom de l'ingrédient à ses propres taux de conversion
    private static final Map<String, Map<UnitType, Double>> conversionRates = new HashMap<>();

    static {
        // Tomate : 1 KG = 10 PCS
        addRate("Tomate", UnitType.KG, 1.0);
        addRate("Tomate", UnitType.PCS, 10.0);

        // Laitue : 1 KG = 2 PCS
        addRate("Laitue", UnitType.KG, 1.0);
        addRate("Laitue", UnitType.PCS, 2.0);

        // Chocolat : 1 KG = 10 PCS = 2.5 L
        addRate("Chocolat", UnitType.KG, 1.0);
        addRate("Chocolat", UnitType.PCS, 10.0);
        addRate("Chocolat", UnitType.L, 2.5);

        // Poulet : 1 KG = 8 PCS
        addRate("Poulet", UnitType.KG, 1.0);
        addRate("Poulet", UnitType.PCS, 8.0);

        // Beurre : 1 KG = 4 PCS = 5 L
        addRate("Beurre", UnitType.KG, 1.0);
        addRate("Beurre", UnitType.PCS, 4.0);
        addRate("Beurre", UnitType.L, 5.0);
    }

    private static void addRate(String name, UnitType unit, Double rate) {
        conversionRates.computeIfAbsent(name, k -> new HashMap<>()).put(unit, rate);
    }

    public static double convertToKg(String ingredientName, double quantity, UnitType fromUnit) {
        if (fromUnit == UnitType.KG) return quantity;

        // On récupère la table des taux pour cet ingrédient précis
        Map<UnitType, Double> rates = conversionRates.get(ingredientName);

        // Si l'unité n'existe pas pour cet ingrédient, on lève une exception
        if (rates == null || !rates.containsKey(fromUnit)) {
            throw new RuntimeException("Conversion impossible : " + ingredientName + " n'a pas d'équivalence pour " + fromUnit);
        }

        // Valeur en KG = Quantité / Taux de l'unité
        return quantity / rates.get(fromUnit);
    }
}