import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataRetriever {

    private final DBConnection dbConnection = new DBConnection();

    public Dish findDishById(Integer id) {
        Connection connection = dbConnection.getConnection();
        try {
            String sql = "SELECT id, name, dish_type, price FROM dish WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Dish dish = new Dish();
                dish.setId(rs.getInt("id"));
                dish.setName(rs.getString("name"));
                dish.setDishType(DishTypeEnum.valueOf(rs.getString("dish_type")));
                dish.setPrice(rs.getObject("price") != null ? rs.getDouble("price") : null);
                dish.setIngredients(findDishIngredientByDishId(id));

                return dish;
            }
            throw new RuntimeException("Dish not found " + id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            dbConnection.closeConnection(connection);
        }
    }


    public List<Ingredient> findIngredients(int page, int size) {
        List<Ingredient> ingredients = new ArrayList<>();
        String sql = "SELECT * FROM ingredient ORDER BY name LIMIT ? OFFSET ?";
        Connection conn = dbConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, size);
            ps.setInt(2, (page - 1) * size);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ingredients.add(mapResultSetToIngredient(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur pagination : " + e.getMessage());
        } finally {
            dbConnection.closeConnection(conn);
        }
        return ingredients;
    }

    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        if (newIngredients == null || newIngredients.isEmpty()) return List.of();

        Connection conn = dbConnection.getConnection();
        List<Ingredient> savedIngredients = new ArrayList<>();

        String checkSql = "SELECT 1 FROM ingredient WHERE name = ?";
        String insertSql = "INSERT INTO ingredient (id, name, category, price) VALUES (?, ?, ?::category, ?) RETURNING id";

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement checkPs = conn.prepareStatement(checkSql);
                 PreparedStatement insertPs = conn.prepareStatement(insertSql)) {

                for (Ingredient ing : newIngredients) {
                    checkPs.setString(1, ing.getName());
                    try (ResultSet rsCheck = checkPs.executeQuery()) {
                        if (rsCheck.next()) {
                            conn.rollback();
                            throw new RuntimeException("L'ingrédient existe déjà : " + ing.getName());
                        }
                    }

                    insertPs.setInt(1, ing.getId() != null ? ing.getId() : getNextSerialValue(conn, "ingredient", "id"));
                    insertPs.setString(2, ing.getName());
                    insertPs.setString(3, ing.getCategory().name());
                    insertPs.setDouble(4, ing.getPrice());

                    try (ResultSet rs = insertPs.executeQuery()) {
                        if (rs.next()) {
                            ing.setId(rs.getInt(1));
                            savedIngredients.add(ing);
                        }
                    }
                }
                conn.commit();
                return savedIngredients;

            } catch (SQLException e) {
                if (conn != null) conn.rollback();
                throw new RuntimeException("Erreur SQL lors de l'insertion groupée : " + e.getMessage());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeConnection(conn);
        }
    }


    public Dish saveDish(Dish dish) {
        String upsertDishSql = """
        INSERT INTO dish (id, name, dish_type, price)
        VALUES (?, ?, ?::dish_type, ?)
        ON CONFLICT (id) DO UPDATE
        SET name = EXCLUDED.name,
            dish_type = EXCLUDED.dish_type,
            price = EXCLUDED.price
        RETURNING id
    """;

        Connection conn = dbConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            Integer dishId;
            try (PreparedStatement ps = conn.prepareStatement(upsertDishSql)) {
                ps.setObject(1, dish.getId(), Types.INTEGER);
                ps.setString(2, dish.getName());
                ps.setString(3, dish.getDishType().name());
                ps.setObject(4, dish.getPrice(), Types.DOUBLE);

                ResultSet rs = ps.executeQuery();
                rs.next();
                dishId = rs.getInt(1);
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM dish_ingredient WHERE id_dish = ?")) {
                ps.setInt(1, dishId);
                ps.executeUpdate();
            }

            if (dish.getIngredients() != null) {
                for (DishIngredient di : dish.getIngredients()) {
                    saveDishIngredient(
                            conn,
                            dishId,
                            di.getIngredient().getId(),
                            di.getQuantity(),
                            di.getUnit().name()
                    );
                }
            }
            conn.commit();
            return findDishById(dishId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeConnection(conn);
        }
    }


    public List<Dish> findDishsByIngredientName(String search) {
        List<Dish> dishes = new ArrayList<>();
        String sql = """
            SELECT DISTINCT d.* FROM dish d 
            JOIN dish_ingredient di ON d.id = di.id_dish 
            JOIN ingredient i ON di.id_ingredient = i.id 
            WHERE i.name ILIKE ?""";
        Connection conn = dbConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + search + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Dish d = new Dish();
                d.setId(rs.getInt("id"));
                d.setName(rs.getString("name"));
                d.setDishType(DishTypeEnum.valueOf(rs.getString("dish_type")));
                d.setPrice(rs.getObject("price") != null ? rs.getDouble("price") : null);
                dishes.add(d);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur recherche par ingrédient : " + e.getMessage());
        } finally {
            dbConnection.closeConnection(conn);
        }
        return dishes;
    }

    public List<Ingredient> findIngredientsByCriteria(String name, CategoryEnum cat, String dishName, int page, int size) {
        List<Ingredient> ingredients = new ArrayList<>();
        // Remplace la ligne 126 par :
        StringBuilder sql = new StringBuilder("""
        SELECT DISTINCT i.* FROM ingredient i 
        LEFT JOIN dish_ingredient di ON i.id = di.id_ingredient 
        LEFT JOIN dish d ON di.id_dish = d.id 
        WHERE 1=1""");

        if (name != null) sql.append(" AND i.name ILIKE ?");
        if (cat != null) sql.append(" AND i.category = ?::category");
        if (dishName != null) sql.append(" AND d.name ILIKE ?");
        sql.append(" LIMIT ? OFFSET ?");

        Connection conn = dbConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (name != null) ps.setString(idx++, "%" + name + "%");
            if (cat != null) ps.setString(idx++, cat.name());
            if (dishName != null) ps.setString(idx++, "%" + dishName + "%");
            ps.setInt(idx++, size);
            ps.setInt(idx, (page - 1) * size);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ingredients.add(mapResultSetToIngredient(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur recherche critères : " + e.getMessage());
        } finally {
            dbConnection.closeConnection(conn);
        }
        return ingredients;
    }

    private Ingredient mapResultSetToIngredient(ResultSet rs) throws SQLException {
        Ingredient ing = new Ingredient();
        ing.setId(rs.getInt("id"));
        ing.setName(rs.getString("name"));
        ing.setPrice(rs.getDouble("price"));
        ing.setCategory(CategoryEnum.valueOf(rs.getString("category")));
        return ing;
    }

    private List<DishIngredient> findDishIngredientByDishId(Integer dishId) {
        Connection connection = dbConnection.getConnection();
        List<DishIngredient> result = new ArrayList<>();

        String sql = """
        SELECT
            i.id AS ingredient_id,
            i.name,
            i.price,
            i.category,
            di.quantity,
            di.unit
        FROM dish_ingredient di
        JOIN ingredient i ON i.id = di.id_ingredient
        WHERE di.id_dish = ?
    """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Ingredient ing = new Ingredient();
                ing.setId(rs.getInt("ingredient_id"));
                ing.setName(rs.getString("name"));
                ing.setPrice(rs.getDouble("price"));
                ing.setCategory(CategoryEnum.valueOf(rs.getString("category")));

                DishIngredient di = new DishIngredient(
                        null,
                        null,
                        ing,
                        rs.getDouble("quantity"),
                        UnitType.valueOf(rs.getString("unit"))
                );

                result.add(di);
            }
            return result;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeConnection(connection);
        }
    }

    private int getNextSerialValue(Connection conn, String tableName, String columnName) throws SQLException {
        String sequenceName;
        try (PreparedStatement ps = conn.prepareStatement("SELECT pg_get_serial_sequence(?, ?)")) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) sequenceName = rs.getString(1);
                else throw new RuntimeException("No sequence found");
            }
        }
        String syncSql = String.format("SELECT setval('%s', (SELECT COALESCE(MAX(%s), 0) FROM %s))", sequenceName, columnName, tableName);
        conn.createStatement().executeQuery(syncSql);
        try (ResultSet rs = conn.createStatement().executeQuery(String.format("SELECT nextval('%s')", sequenceName))) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // Nouvelle méthode "save" pour la nouvelle entité de jointure (table dish_ingredient)
    // On ajoute la connexion en paramètre pour rester dans la même transaction
    public void saveDishIngredient(Connection conn, Integer dishId, Integer ingredientId, Double quantity, String unit) {
        String sql = """
        INSERT INTO dish_ingredient (id_dish, id_ingredient, quantity, unit)
        VALUES (?, ?, ?, ?::unit_type)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            ps.setInt(2, ingredientId);
            ps.setDouble(3, quantity);
            ps.setString(4, unit);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lien Plat-Ingrédient : " + e.getMessage());
        }
    }

    // TD4 : gestion de stock
    public Ingredient saveIngredient(Ingredient toSave) {
        if (toSave == null) return null;

        Connection conn = dbConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            String sqlIngredient = """
            INSERT INTO ingredient (id, name, price, category)
            VALUES (?, ?, ?, ?::category)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                price = EXCLUDED.price,
                category = EXCLUDED.category
            RETURNING id
        """;
            try (PreparedStatement ps = conn.prepareStatement(sqlIngredient)) {
                ps.setObject(1, toSave.getId(), Types.INTEGER);
                ps.setString(2, toSave.getName());
                ps.setDouble(3, toSave.getPrice());
                ps.setString(4, toSave.getCategory().name());

                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    toSave.setId(rs.getInt(1));
                }
            }
            saveStockMovements(conn, toSave);

            conn.commit();
            return toSave;
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {}
            throw new RuntimeException("Erreur lors de la sauvegarde de l'ingrédient : " + e.getMessage());
        } finally {
            dbConnection.closeConnection(conn);
        }
    }

    private void saveStockMovements(Connection conn, Ingredient ingredient) throws SQLException {
        List<StockMovement> movements = ingredient.getStockMovementList();
        if (movements == null || movements.isEmpty()) return;

        String sqlMovement = """
        INSERT INTO stock_movement (id, id_ingredient, quantity, type, unit, creation_datetime)
        VALUES (?, ?, ?, ?::movement_type, ?::unit_type, ?)
        ON CONFLICT (id) DO NOTHING
        """;

        try (PreparedStatement psM = conn.prepareStatement(sqlMovement)) {
            for (StockMovement sm : movements) {
                psM.setObject(1, sm.getId(), Types.INTEGER);
                psM.setInt(2, ingredient.getId());
                psM.setDouble(3, sm.getValue().getQuantity());
                psM.setString(4, sm.getType().name());
                psM.setString(5, sm.getValue().getUnit().name());
                psM.setTimestamp(6, Timestamp.from(sm.getCreationDatetime()));

                psM.executeUpdate();
            }
        }
    }

    // pour mes tests :
    public Ingredient findIngredientById(int id) {
        Ingredient ingredient = null;
        String sql = "SELECT id, name FROM ingredient WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                ingredient = new Ingredient();
                ingredient.setId(rs.getInt("id"));
                ingredient.setName(rs.getString("name"));

                ingredient.setStockMovementList(findMovementsByIngredientId(id));
            }
        } catch (SQLException e) {
            throw new RuntimeException(("L'ingrédient avec l'id " + id + " n'existe pas"));
        }
        return ingredient;
    }

    private List<StockMovement> findMovementsByIngredientId(int idIng) {
        List<StockMovement> list = new ArrayList<>();
        String sql = "SELECT quantity, unit, type, creation_datetime FROM stock_movement WHERE id_ingredient = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idIng);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                StockMovement sm = new StockMovement();
                sm.setType(MovementType.valueOf(rs.getString("type")));
                sm.setCreationDatetime(rs.getTimestamp("creation_datetime").toInstant());
                sm.setValue(new StockValue(rs.getDouble("quantity"), UnitType.valueOf(rs.getString("unit"))));
                list.add(sm);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    // TD - annexe 22 janvier
    public Order saveOrder(Order orderToSave, Integer tableId) {
        Connection conn = dbConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            Instant now = orderToSave.getCreationDatetime() != null ? orderToSave.getCreationDatetime() : Instant.now();

            checkStockAvailability(orderToSave, now);

            List<Order> tableOrders = findOrdersByTableId(tableId);
            RestaurantTable requestedTable = new RestaurantTable();
            requestedTable.setId(tableId);
            requestedTable.setOrders(tableOrders);

            if (!requestedTable.isAvailableAt(now)) {
                List<RestaurantTable> availableTables = findAvailableTables(now);

                String availableNumbers = availableTables.stream()
                        .map(t -> String.valueOf(t.getNumber()))
                        .collect(Collectors.joining(", "));

                String errorMsg = "La table " + tableId + " est déjà occupée ! ";
                if (!availableNumbers.isEmpty()) {
                    errorMsg += "Les tables suivantes sont encore disponibles : " + availableNumbers;
                } else {
                    errorMsg += "Désolé, aucune autre table n'est disponible pour le moment.";
                }

                throw new RuntimeException(errorMsg);
            }

            String sql = "INSERT INTO \"Order\" (reference, creation_datetime, id_table) VALUES (?, ?, ?) RETURNING id";
            Integer orderId;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, orderToSave.getReference());
                ps.setTimestamp(2, Timestamp.from(now));
                ps.setInt(3, tableId);
                ResultSet rs = ps.executeQuery();
                rs.next();
                orderId = rs.getInt(1);
            }

            insertOrderItems(conn, orderId, orderToSave.getDishOrders());

            conn.commit();
            orderToSave.setId(orderId);
            return orderToSave;

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) {}
            throw new RuntimeException("Erreur SQL : " + e.getMessage());
        } finally {
            dbConnection.closeConnection(conn);
        }
    }

    private void checkStockAvailability(Order order, Instant t) {
        for (DishOrder dO : order.getDishOrders()) {
            Dish dish = findDishById(dO.getDish().getId());

            for (DishIngredient di : dish.getIngredients()) {
                Ingredient ing = findIngredientById(di.getIngredient().getId());
                double currentStockKg = ing.getStockValueAt(t).getQuantity();

                double requiredPerDishKg = UnitConverter.convertToKg(
                        ing.getName(),
                        di.getQuantity(),
                        di.getUnit()
                );

                double totalRequiredKg = requiredPerDishKg * dO.getQuantity();

                if (currentStockKg < totalRequiredKg) {
                    throw new RuntimeException("Stock insuffisant pour : " + ing.getName());
                }
            }
        }
    }

    private Integer insertOrderHeader(Connection conn, Order order, Instant now) throws SQLException {
        String sql = "INSERT INTO \"Order\" (reference, creation_datetime) VALUES (?, ?) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, order.getReference());
            ps.setTimestamp(2, Timestamp.from(now));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void insertOrderItems(Connection conn, Integer orderId, List<DishOrder> items) throws SQLException {
        String sql = "INSERT INTO dish_order (id_order, id_dish, quantity) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (DishOrder item : items) {
                ps.setInt(1, orderId);
                ps.setInt(2, item.getDish().getId());
                ps.setInt(3, item.getQuantity());
                ps.executeUpdate();
            }
        }
    }

    public Order findOrderByReference(String reference) {
        String sql = "SELECT id, reference, creation_datetime, id_table FROM \"Order\" WHERE reference = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, reference);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Order order = new Order();
                    order.setId(rs.getInt("id"));
                    order.setReference(rs.getString("reference"));
                    order.setCreationDatetime(rs.getTimestamp("creation_datetime").toInstant());
                    order.setDishOrders(findDishOrdersByReference(reference));

                    int tableId = rs.getInt("id_table");
                    if (tableId != 0) {
                        RestaurantTable table = new RestaurantTable();
                        table.setId(tableId);

                        order.setRestaurantTable(table);
                    }

                    return order;
                } else {
                    throw new RuntimeException("Commande introuvable pour la référence : " + reference);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur récupération commande : " + e.getMessage());
        }
    }


    public List<DishOrder> findDishOrdersByReference(String reference) {
        List<DishOrder> list = new ArrayList<>();
        String sql = """
        SELECT dor.id, dor.quantity, d.id as dish_id, d.name, d.price, d.dish_type 
        FROM dish_order dor 
        JOIN "Order" o ON o.id = dor.id_order 
        JOIN dish d ON d.id = dor.id_dish 
        WHERE o.reference = ?
        """;
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, reference);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Dish dish = new Dish();
                    dish.setId(rs.getInt("dish_id"));
                    dish.setName(rs.getString("name"));
                    dish.setPrice(rs.getDouble("price"));
                    dish.setDishType(DishTypeEnum.valueOf(rs.getString("dish_type")));

                    DishOrder dishOrder = new DishOrder();
                    dishOrder.setId(rs.getInt("id"));
                    dishOrder.setQuantity(rs.getInt("quantity"));
                    dishOrder.setDish(dish);

                    list.add(dishOrder);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des plats de la commande : " + e.getMessage());
        }
        return list;
    }

    // Examen 29 janvier :
    public List<RestaurantTable> findAvailableTables(Instant t) {
        List<RestaurantTable> allTables = findAllTables(); // Méthode à créer
        List<RestaurantTable> availableTables = new ArrayList<>();

        for (RestaurantTable table : allTables) {
            table.setOrders(findOrdersByTableId(table.getId()));

            if (table.isAvailableAt(t)) {
                availableTables.add(table);
            }
        }
        return availableTables;
    }

    private List<RestaurantTable> findAllTables() {
        List<RestaurantTable> tables = new ArrayList<>();
        String sql = "SELECT * FROM restaurant_table";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RestaurantTable rt = new RestaurantTable();
                rt.setId(rs.getInt("id"));
                rt.setNumber(rs.getInt("number"));
                tables.add(rt);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return tables;
    }

    private List<Order> findOrdersByTableId(Integer tableId) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT id, reference, creation_datetime FROM \"Order\" WHERE id_table = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Order o = new Order();
                o.setId(rs.getInt("id"));
                o.setReference(rs.getString("reference"));
                o.setCreationDatetime(rs.getTimestamp("creation_datetime").toInstant());
                orders.add(o);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return orders;
    }
}