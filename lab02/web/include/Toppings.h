#pragma once

#include "PizzaDecorator.h"

class TomatoTopping final : public PizzaDecorator {
public:
    explicit TomatoTopping(PizzaPtr wrappedPizza)
        : PizzaDecorator(std::move(wrappedPizza)) {
    }

    std::string name() const override {
        return wrappedPizza_->name() + ", с томатами";
    }

    int cost() const override {
        return wrappedPizza_->cost() + 3;
    }
};

class CheeseTopping final : public PizzaDecorator {
public:
    explicit CheeseTopping(PizzaPtr wrappedPizza)
        : PizzaDecorator(std::move(wrappedPizza)) {
    }

    std::string name() const override {
        return wrappedPizza_->name() + ", с сыром";
    }

    int cost() const override {
        return wrappedPizza_->cost() + 5;
    }
};

class OlivesTopping final : public PizzaDecorator {
public:
    explicit OlivesTopping(PizzaPtr wrappedPizza)
        : PizzaDecorator(std::move(wrappedPizza)) {
    }

    std::string name() const override {
        return wrappedPizza_->name() + ", с оливками";
    }

    int cost() const override {
        return wrappedPizza_->cost() + 4;
    }
};

class MushroomsTopping final : public PizzaDecorator {
public:
    explicit MushroomsTopping(PizzaPtr wrappedPizza)
        : PizzaDecorator(std::move(wrappedPizza)) {
    }

    std::string name() const override {
        return wrappedPizza_->name() + ", с грибами";
    }

    int cost() const override {
        return wrappedPizza_->cost() + 4;
    }
};
