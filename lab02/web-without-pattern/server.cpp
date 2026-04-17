#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <unistd.h>

#include <cstring>
#include <iostream>
#include <sstream>
#include <string>
#include <unordered_map>

namespace {

std::string urlDecode(const std::string& input) {
    std::string out;
    out.reserve(input.size());

    for (size_t i = 0; i < input.size(); ++i) {
        const char c = input[i];
        if (c == '%' && i + 2 < input.size()) {
            const std::string hex = input.substr(i + 1, 2);
            char decoded = static_cast<char>(std::strtol(hex.c_str(), nullptr, 16));
            out.push_back(decoded);
            i += 2;
        } else if (c == '+') {
            out.push_back(' ');
        } else {
            out.push_back(c);
        }
    }

    return out;
}

std::unordered_map<std::string, std::string> parseQuery(const std::string& query) {
    std::unordered_map<std::string, std::string> params;
    std::stringstream ss(query);
    std::string pair;

    while (std::getline(ss, pair, '&')) {
        if (pair.empty()) {
            continue;
        }

        const auto pos = pair.find('=');
        if (pos == std::string::npos) {
            params[urlDecode(pair)] = "";
            continue;
        }

        const std::string key = urlDecode(pair.substr(0, pos));
        const std::string value = urlDecode(pair.substr(pos + 1));
        params[key] = value;
    }

    return params;
}

bool isEnabled(const std::unordered_map<std::string, std::string>& params, const std::string& key) {
    const auto it = params.find(key);
    if (it == params.end()) {
        return false;
    }

    return it->second == "1" || it->second == "true" || it->second == "on";
}

std::string escapeJson(const std::string& input) {
    std::string out;
    out.reserve(input.size() + 16);

    for (char c : input) {
        if (c == '\\') {
            out += "\\\\";
        } else if (c == '"') {
            out += "\\\"";
        } else if (c == '\n') {
            out += "\\n";
        } else {
            out.push_back(c);
        }
    }

    return out;
}

std::string buildName(const std::string& base, bool tomato, bool cheese, bool olives, bool mushrooms) {
    std::string name = (base == "bulgarian") ? "Болгарская пицца" : "Итальянская пицца";

    if (tomato) {
        name += ", с томатами";
    }
    if (cheese) {
        name += ", с сыром";
    }
    if (olives) {
        name += ", с оливками";
    }
    if (mushrooms) {
        name += ", с грибами";
    }

    return name;
}

int buildPrice(const std::string& base, bool tomato, bool cheese, bool olives, bool mushrooms) {
    int price = (base == "bulgarian") ? 8 : 10;

    if (tomato) {
        price += 3;
    }
    if (cheese) {
        price += 5;
    }
    if (olives) {
        price += 4;
    }
    if (mushrooms) {
        price += 4;
    }

    return price;
}

std::string responseForRequest(const std::string& method, const std::string& target) {
    if (method == "OPTIONS") {
        return "HTTP/1.1 204 No Content\r\n"
               "Access-Control-Allow-Origin: *\r\n"
               "Access-Control-Allow-Methods: GET, OPTIONS\r\n"
               "Access-Control-Allow-Headers: Content-Type\r\n"
               "Connection: close\r\n\r\n";
    }

    if (method != "GET") {
        const std::string body = "{\"error\":\"Only GET is supported\"}";
        std::ostringstream out;
        out << "HTTP/1.1 405 Method Not Allowed\r\n"
            << "Content-Type: application/json; charset=utf-8\r\n"
            << "Access-Control-Allow-Origin: *\r\n"
            << "Content-Length: " << body.size() << "\r\n"
            << "Connection: close\r\n\r\n"
            << body;
        return out.str();
    }

    if (target.rfind("/api/pizza", 0) != 0) {
        const std::string body = "{\"error\":\"Not found\"}";
        std::ostringstream out;
        out << "HTTP/1.1 404 Not Found\r\n"
            << "Content-Type: application/json; charset=utf-8\r\n"
            << "Access-Control-Allow-Origin: *\r\n"
            << "Content-Length: " << body.size() << "\r\n"
            << "Connection: close\r\n\r\n"
            << body;
        return out.str();
    }

    std::string query;
    const auto queryPos = target.find('?');
    if (queryPos != std::string::npos && queryPos + 1 < target.size()) {
        query = target.substr(queryPos + 1);
    }

        const auto params = parseQuery(query);
        const std::string base = (params.count("base") && params.at("base") == "bulgarian") ? "bulgarian" : "italian";
        const bool tomato = isEnabled(params, "tomato");
        const bool cheese = isEnabled(params, "cheese");
        const bool olives = isEnabled(params, "olives");
        const bool mushrooms = isEnabled(params, "mushrooms");

        const std::string pizzaName = buildName(base, tomato, cheese, olives, mushrooms);
        const int pizzaPrice = buildPrice(base, tomato, cheese, olives, mushrooms);

    std::ostringstream body;
    body << "{"
            << "\"name\":\"" << escapeJson(pizzaName) << "\","
            << "\"price\":" << pizzaPrice << ","
            << "\"base\":\"" << base << "\""
         << "}";

    const std::string bodyText = body.str();
    std::ostringstream out;
    out << "HTTP/1.1 200 OK\r\n"
        << "Content-Type: application/json; charset=utf-8\r\n"
        << "Access-Control-Allow-Origin: *\r\n"
        << "Content-Length: " << bodyText.size() << "\r\n"
        << "Connection: close\r\n\r\n"
        << bodyText;

    return out.str();
}

}  // namespace

int main() {
    const int port = 8091;

    const int serverFd = socket(AF_INET, SOCK_STREAM, 0);
    if (serverFd < 0) {
        std::cerr << "Failed to create socket\n";
        return 1;
    }

    int opt = 1;
    setsockopt(serverFd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    sockaddr_in addr{};
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port = htons(port);

    if (bind(serverFd, reinterpret_cast<sockaddr*>(&addr), sizeof(addr)) < 0) {
        std::cerr << "Failed to bind to port " << port << "\n";
        close(serverFd);
        return 1;
    }

    if (listen(serverFd, 16) < 0) {
        std::cerr << "Failed to listen on socket\n";
        close(serverFd);
        return 1;
    }

    std::cout << "C++ pizza backend (without pattern) started: http://localhost:" << port << "\n";
    std::cout << "Endpoint: GET /api/pizza?base=italian&tomato=1&cheese=0&olives=1&mushrooms=0\n";

    while (true) {
        sockaddr_in clientAddr{};
        socklen_t clientLen = sizeof(clientAddr);
        const int clientFd = accept(serverFd, reinterpret_cast<sockaddr*>(&clientAddr), &clientLen);
        if (clientFd < 0) {
            continue;
        }

        char buffer[8192];
        const ssize_t bytesRead = recv(clientFd, buffer, sizeof(buffer) - 1, 0);
        if (bytesRead <= 0) {
            close(clientFd);
            continue;
        }

        buffer[bytesRead] = '\0';
        std::string request(buffer);

        std::istringstream reqStream(request);
        std::string method;
        std::string target;
        std::string version;
        reqStream >> method >> target >> version;

        const std::string response = responseForRequest(method, target);
        send(clientFd, response.c_str(), response.size(), 0);
        close(clientFd);
    }

    close(serverFd);
    return 0;
}
