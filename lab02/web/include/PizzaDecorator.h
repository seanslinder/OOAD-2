#pragma once

#include "Pizza.h"

class PizzaDecorator : public Pizza {
public:
    explicit PizzaDecorator(PizzaPtr wrappedPizza)
        : wrappedPizza_(std::move(wrappedPizza)) {
    }

protected:
    PizzaPtr wrappedPizza_;
};
