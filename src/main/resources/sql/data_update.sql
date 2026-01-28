UPDATE dish SET price = 3500.00 WHERE id = 1;
UPDATE dish SET price = 12000.00 WHERE id = 2;
UPDATE dish SET price = NULL WHERE id = 3;
UPDATE dish SET price = 8000.00 WHERE id = 4;
UPDATE dish SET price = NULL WHERE id = 5;

INSERT INTO dish_ingredient (id, id_dish, id_ingredient, quantity_required, unit)
VALUES
    (1, 1, 1, 0.20, 'KG'),
    (2, 1, 2, 0.15, 'KG'),
    (3, 2, 3, 1.00, 'KG'),
    (4, 4, 4, 0.30, 'KG'),
    (5, 4, 5, 0.20, 'KG');

------------------------------------------------------------------------------------------------------------------
/* TD4 : gestion de stock (insertion) */

INSERT INTO stock_movement (id, id_ingredient, quantity, type, unit, creation_datetime) VALUES
    (1, 1, 5.0, 'IN', 'KG', '2024-01-05 08:00'),
    (2, 1, 0.2, 'OUT', 'KG', '2024-01-06 12:00'),
    (3, 2, 4.0, 'IN', 'KG', '2024-01-05 08:00'),
    (4, 2, 0.15, 'OUT', 'KG', '2024-01-06 12:00'),
    (5, 3, 10.0, 'IN', 'KG', '2024-01-04 09:00'),
    (6, 3, 1.0, 'OUT', 'KG', '2024-01-06 13:00'),
    (7, 4, 3.0, 'IN', 'KG', '2024-01-05 10:00'),
    (8, 4, 0.3, 'OUT', 'KG', '2024-01-06 14:00'),
    (9, 5, 2.5, 'IN', 'KG', '2024-01-05 10:00'),
    (10, 5, 0.2, 'OUT', 'KG', '2024-01-06 14:00');

-- Synchronisation de la séquence après insertion manuelle des IDs
SELECT setval('stock_movement_id_seq', (SELECT MAX(id) FROM stock_movement));

-----------------------------------------------------------------------
/* conversion unité - BONUS */
INSERT INTO stock_movement (id, id_ingredient, quantity, type, unit, creation_datetime, commentaire) VALUES
    (2, 1, 2, 'OUT', 'PCS', '2024-01-06 12:00', 'Préparation salade'),
    (4, 2, 5, 'OUT', 'PCS', '2024-01-06 12:00', 'Préparation salade'),
    (6, 3, 4, 'OUT', 'PCS', '2024-01-06 13:00', 'Plat principal'),
    (8, 4, 1, 'OUT', 'L', '2024-01-06 14:00', 'Dessert'),
    (10, 5, 1, 'OUT', 'L', '2024-01-06 14:00', 'Pâtisserie');