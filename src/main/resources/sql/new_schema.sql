CREATE TYPE unit_type AS ENUM ('PCS', 'KG', 'L');

CREATE TABLE dish_ingredient (
                                 id SERIAL CONSTRAINT dish_ingredient_pk PRIMARY KEY,
                                 id_dish INTEGER NOT NULL,
                                 id_ingredient INTEGER NOT NULL,
                                 quantity_required NUMERIC NOT NULL,
                                 unit unit_type NOT NULL,
                                 CONSTRAINT fk_dish FOREIGN KEY (id_dish) REFERENCES dish(id),
                                 CONSTRAINT fk_ingredient FOREIGN KEY (id_ingredient) REFERENCES ingredient(id)
);

ALTER TABLE ingredient DROP COLUMN IF EXISTS id_dish;
ALTER TABLE ingredient DROP COLUMN IF EXISTS required_quantity;

--------------------------------------------------------------------------------------------------------------------
// TD4 : gestion de stock
CREATE TYPE movement_type as ENUM('IN', 'OUT');

CREATE TABLE stock_movement (
    id SERIAL PRIMARY KEY NOT NULL,
    id_ingredient INTEGER NOT NULL,
    quantity NUMERIC NOT NULL,
    type movement_type NOT NULL,
    unit unit_type NOT NULL,
    creation_datetime TIMESTAMP NOT NULL,
    CONSTRAINT fk_ingredient FOREIGN KEY (id_ingredient) REFERENCES ingredient(id)
);

--------------------------------------------------------------------------------------------------------------------
// TD4 : annexe
CREATE TABLE "Order" (
    id SERIAL PRIMARY KEY NOT NULL,
    reference VARCHAR(8) NOT NULL,
    creation_datetime TIMESTAMP NOT NULL
);

CREATE TABLE dish_order (
    id SERIAL PRIMARY KEY NOT NULL,
    id_order INT NOT NULL,
    id_dish INT NOT NULL,
    quantity NUMERIC NOT NULL,
    CONSTRAINT fk_order FOREIGN KEY (id_order) REFERENCES "Order"(id),
    CONSTRAINT fk_dish FOREIGN KEY (id_dish) REFERENCES dish(id)
);
