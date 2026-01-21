import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Ingredient {
    private Integer id;
    private String name;
    private Double price;
    private CategoryEnum category;
    private List<StockMovement> stockMovementList = new ArrayList<>();

    public Ingredient(Integer id, String name, Double price, CategoryEnum category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
    }

    public Ingredient() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public CategoryEnum getCategory() {
        return category;
    }

    public void setCategory(CategoryEnum category) {
        this.category = category;
    }

    public List<StockMovement> getStockMovementList() {
        return stockMovementList;
    }

    public void setStockMovementList(List<StockMovement> stockMovementList) {
        this.stockMovementList = stockMovementList;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Ingredient that = (Ingredient) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(price, that.price) && category == that.category;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, price, category);
    }

    @Override
    public String toString() {
        return "Ingredient{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", category=" + category +
                '}';
    }

    // TD4 : gestion de stock
    public StockValue getStockValueAt(Instant t) {
        double totalQuantity = 0.0;

        if (this.stockMovementList == null || this.stockMovementList.isEmpty()) {
            return new StockValue(0.0, UnitType.KG);
        }

        for (StockMovement sm : this.stockMovementList) {
            if (sm.getCreationDatetime().isBefore(t)) {
                // sm.getValue() retourne l'objet StockValue du mouvement
                double qty = sm.getValue().getQuantity();

                if (sm.getType() == MovementType.IN) {
                    totalQuantity += qty;
                } else if (sm.getType() == MovementType.OUT) {
                    totalQuantity -= qty;
                }
            }
        }
        // On retourne le résultat sous forme d'objet StockValue
        return new StockValue(totalQuantity, UnitType.KG);
    }
}