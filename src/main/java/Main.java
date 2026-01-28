import javax.xml.crypto.Data;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        DataRetriever retriever = new DataRetriever();

        for (int i = 1; i <= 5; i++) {
            try {
                Dish plat = retriever.findDishById(i);

                Double cout = plat.getDishCost();

                System.out.println("Plat : " + plat.getName());
                System.out.println(" - Coût de revient : " + cout + " Ar");

                try {
                    Double marge = plat.getGrossMargin();
                    System.out.println(" - Marge brute : " + marge + " Ar");
                } catch (RuntimeException e) {
                    System.out.println(" - Marge : Erreur -> " + e.getMessage());
                }
                System.out.println("-------------------------------------------");
            } catch (Exception e) {
                System.out.println("Erreur pour l'ID " + i + " : " + e.getMessage());
            }
        }

        System.out.println();

        // tests gestion de stock avec conversion unité
        DataRetriever dataRetriever = new DataRetriever();
        Ingredient laitue = dataRetriever.findIngredientById(1);
        Ingredient tomate = dataRetriever.findIngredientById(2);
        Ingredient poulet = dataRetriever.findIngredientById(3);
        Ingredient chocolat = dataRetriever.findIngredientById(4);
        Ingredient beurre = dataRetriever.findIngredientById(5);

        Instant t = Instant.parse("2024-01-06T12:00:00Z");

        StockValue stock = laitue.getStockValueAt(t);
        StockValue stock2 = tomate.getStockValueAt(t);
        StockValue stock3 = poulet.getStockValueAt(t);
        StockValue stock4 = chocolat.getStockValueAt(t);
        StockValue stock5 = beurre.getStockValueAt(t);

        System.out.println("--- ETATS DES STOCKS ---");
        System.out.println(laitue.getName() + " : " + stock.getQuantity() + stock.getUnit());
        System.out.println(tomate.getName() + " : " + stock2.getQuantity() + stock2.getUnit());
        System.out.println(poulet.getName() + " : " + stock3.getQuantity() + stock3.getUnit());
        System.out.println(chocolat.getName() + " : " + stock4.getQuantity() + stock4.getUnit());
        System.out.println(beurre.getName() + " : " + stock5.getQuantity() + stock5.getUnit());
    }
}
