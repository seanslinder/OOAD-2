#pragma once

#include "Pizza.h"

class ItalianPizza final : public Pizza {
public:
    std::string name() const override {
        return "Итальянская пицца";
    }

    int cost() const override {
        return 10;
    }
};

class BulgarianPizza final : public Pizza {
public:
    std::string name() const override {
        return "Болгарская пицца";
    }

    int cost() const override {
        return 8;
    }
};
