import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        DataRetriever retriever = new DataRetriever();

        // tests TD3 des méthodes getDishCost() et getGrossMargin()
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
    }
}