create type dish_type as enum ('START', 'MAIN', 'DESSERT');
create type category as enum ('VEGETABLE', 'ANIMAL', 'MARINE', 'DAIRY', 'OTHER');

create table Dish (
    id serial constraint dish_pk primary key,
    name varchar(50) not null,
    dish_type dish_type not null
);

create table Ingredient (
    id serial constraint Ingredient_pk primary key,
    name varchar(50) not null,
    price numeric(10,2) not null,
    category_ingredient category not null,
    id_dish integer,
    constraint fk_dish foreign key (id_dish) references Dish (id)
);