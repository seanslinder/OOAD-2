const toppingConfig = [
  { key: "tomato", layerId: "tomatoLayer" },
  { key: "cheese", layerId: "cheeseLayer" },
  { key: "olives", layerId: "olivesLayer" },
  { key: "mushrooms", layerId: "mushroomsLayer" },
];

const backendUrl = "http://localhost:8091/api/pizza";

const baseImage = document.getElementById("baseImage");
const nameNode = document.getElementById("pizzaName");
const priceNode = document.getElementById("pizzaPrice");

const controls = [
  ...document.querySelectorAll("input[name='base']"),
  ...document.querySelectorAll("input[type='checkbox']"),
];

function getSelectedBase() {
  const selected = document.querySelector("input[name='base']:checked");
  return selected ? selected.value : "italian";
}

function getSelectedToppings() {
  return toppingConfig.filter((item) => {
    const input = document.querySelector(`input[name='${item.key}']`);
    return input && input.checked;
  });
}

function updateBaseImage(base) {
  const source =
    base === "italian"
      ? "assets/images/base-italian.svg"
      : "assets/images/base-bulgarian.svg";
  baseImage.src = source;
  baseImage.alt = `${base} pizza base`;
}

function updateToppingLayers(selectedToppings) {
  const selectedSet = new Set(selectedToppings.map((item) => item.key));

  toppingConfig.forEach((item) => {
    const layer = document.getElementById(item.layerId);
    if (!layer) {
      return;
    }

    if (selectedSet.has(item.key)) {
      layer.classList.add("visible");
    } else {
      layer.classList.remove("visible");
    }
  });
}

async function updateSummaryFromBackend(base, selectedToppings) {
  const selectedSet = new Set(selectedToppings.map((item) => item.key));
  const params = new URLSearchParams({
    base,
    tomato: selectedSet.has("tomato") ? "1" : "0",
    cheese: selectedSet.has("cheese") ? "1" : "0",
    olives: selectedSet.has("olives") ? "1" : "0",
    mushrooms: selectedSet.has("mushrooms") ? "1" : "0",
  });

  try {
    const response = await fetch(`${backendUrl}?${params.toString()}`);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const payload = await response.json();
    nameNode.textContent = payload.name;
    priceNode.textContent = `${payload.price} credits`;
  } catch (error) {
    nameNode.textContent = "Backend unavailable";
    priceNode.textContent = "-";
    // Keep console logging for diagnostics when localhost backend is not running.
    console.error("Failed to fetch pizza summary from C++ backend:", error);
  }
}

async function render() {
  const base = getSelectedBase();
  const toppings = getSelectedToppings();

  updateBaseImage(base);
  updateToppingLayers(toppings);
  await updateSummaryFromBackend(base, toppings);
}

controls.forEach((node) => node.addEventListener("change", render));

void render();
