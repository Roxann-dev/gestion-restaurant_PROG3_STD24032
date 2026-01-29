import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

public class RestaurantTable {
    private Integer id;
    private Integer number;
    private List<Order> orders = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }

    public boolean isAvailableAt(Instant t) {
        for (Order order : orders) {
            Instant start = order.getCreationDatetime();
            Instant end = start.plusSeconds(3600);

            if (t.isAfter(start) && t.isBefore(end)) {
                return false;
            }
        }
        return true;
    }
}