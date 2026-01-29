import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        DataRetriever dataRetriever = new DataRetriever();

        try {
            // lister les tables dispo
            Instant now = Instant.now();
            List<RestaurantTable> availableTables = dataRetriever.findAvailableTables(now);
            System.out.println("Nombre de tables disponibles : " + availableTables.size());

            if (availableTables.isEmpty()) {
                System.out.println("Attention : Aucune table trouvée en base. Vérifiez vos inserts SQL.");
                return;
            }

            Integer tableIdToTest = availableTables.get(0).getId();
            System.out.println("Test sur la Table ID : " + tableIdToTest);

            // 2. PRÉPARATION D'UNE COMMANDE (id = 1)
            Dish dish = dataRetriever.findDishById(1);
            DishOrder dishOrder = new DishOrder();
            dishOrder.setDish(dish);
            dishOrder.setQuantity(1);

            List<DishOrder> items = new ArrayList<>();
            items.add(dishOrder);

            Order firstOrder = new Order();
            firstOrder.setReference("ORD00001");
            firstOrder.setDishOrders(items);
            firstOrder.setCreationDatetime(now);

            // Sauvegarde de la première commande
            System.out.println("Tentative de sauvegarde Commande 1...");
            dataRetriever.saveOrder(firstOrder, tableIdToTest);
            System.out.println("Commande 1 sauvegardée avec succès !");

            // commande sur la MÊME TABLE au MÊME MOMENT
            System.out.println("\nTentative de sauvegarde Commande 2 sur la même table...");
            Order secondOrder = new Order();
            secondOrder.setReference("ORD00002");
            secondOrder.setDishOrders(items);
            secondOrder.setCreationDatetime(now);

            try {
                dataRetriever.saveOrder(secondOrder, tableIdToTest);
                System.out.println("Erreur : La commande aurait dû être rejetée (table occupée)");
            } catch (RuntimeException e) {
                System.out.println("Succès du test : L'exception a bien été levée : " + e.getMessage());
            }

            // récupération (findOrderByReference)
            System.out.println("\nTest de récupération de la commande...");
            Order fetched = dataRetriever.findOrderByReference("ORD00001");
            System.out.println("Commande récupérée : " + fetched.getReference());
            if (fetched.getRestaurantTable() != null) {
                System.out.println("Reliée à la Table ID : " + fetched.getRestaurantTable().getId());
            }

        } catch (Exception e) {
            System.err.println("ERREUR DURANT LES TESTS : " + e.getMessage());
        }
    }
}