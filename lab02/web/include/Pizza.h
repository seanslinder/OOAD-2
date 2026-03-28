#pragma once

#include <memory>
#include <string>

class Pizza {
public:
    virtual ~Pizza() = default;

    virtual std::string name() const = 0;
    virtual int cost() const = 0;
};

using PizzaPtr = std::shared_ptr<Pizza>;
