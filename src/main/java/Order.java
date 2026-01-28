import java.time.Instant;
import java.util.List;

public class Order {
    private Integer id;
    private String reference;
    private Instant creationDatetime;
    private List<DishOrder> dishOrders;

    public Order(Integer id, String reference, Instant creationDatetime, List<DishOrder> dishOrders) {
        this.id = id;
        this.reference = reference;
        this.creationDatetime = creationDatetime;
        this.dishOrders = dishOrders;
    }

    public Order() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Instant getCreationDatetime() {
        return creationDatetime;
    }

    public void setCreationDatetime(Instant creationDatetime) {
        this.creationDatetime = creationDatetime;
    }

    public List<DishOrder> getDishOrders() {
        return dishOrders;
    }

    public void setDishOrders(List<DishOrder> dishOrders) {
        this.dishOrders = dishOrders;
    }

    public Double getTotalAmountWithoutVAT() {
        if (dishOrders == null) return 0.0;
        return dishOrders.stream()
                .mapToDouble(dO -> dO.getDish().getPrice() * dO.getQuantity())
                .sum();
    }

    // Calcul du montant total TTC (exemple avec TVA à 20%)
    public Double getTotalAmountWithVAT() {
        return getTotalAmountWithoutVAT() * 1.20;
    }
}
