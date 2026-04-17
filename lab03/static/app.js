const messageInput = document.getElementById("message");
const checkBtn = document.getElementById("checkBtn");
const resultPanel = document.getElementById("resultPanel");
const resultStatus = document.getElementById("resultStatus");
const resultFilter = document.getElementById("resultFilter");
const resultReason = document.getElementById("resultReason");
const charCount = document.getElementById("charCount");
const filterInputs = [...document.querySelectorAll("input[data-filter]")];
const modelSelect = document.getElementById("modelSelect");
const modelStatus = document.getElementById("modelStatus");

function setModelStatus(text) {
  modelStatus.textContent = text;
}

function setModels(models, defaultModel) {
  modelSelect.innerHTML = "";

  if (!Array.isArray(models) || models.length === 0) {
    modelSelect.disabled = true;
    const option = document.createElement("option");
    option.value = "";
    option.textContent = "Модели не найдены";
    modelSelect.append(option);
    setModelStatus("Не удалось получить список моделей из Ollama");
    return;
  }

  for (const model of models) {
    const option = document.createElement("option");
    option.value = model;
    option.textContent = model;
    modelSelect.append(option);
  }

  const preferred =
    defaultModel && models.includes(defaultModel) ? defaultModel : models[0];
  modelSelect.value = preferred;
  modelSelect.disabled = false;
  setModelStatus(`Найдено моделей: ${models.length}`);
}

async function loadModels() {
  setModelStatus("Загрузка списка моделей...");
  modelSelect.disabled = true;

  try {
    const response = await fetch("/api/models");
    const data = await response.json();

    if (!response.ok) {
      setModels([], "");
      if (data.error) {
        setModelStatus(`Ошибка Ollama: ${data.error}`);
      }
      return;
    }

    setModels(data.models, data.default_model);
  } catch {
    setModels([], "");
    setModelStatus("Сетевая ошибка при получении моделей");
  }
}

function collectFilters() {
  const filters = {};
  for (const input of filterInputs) {
    filters[input.dataset.filter] = input.checked;
  }
  return filters;
}

function clearResult() {
  resultStatus.textContent = "Ожидаем результат";
  resultFilter.textContent = "";
  resultReason.textContent = "";
  resultStatus.classList.remove("ok", "fail");
  resultStatus.classList.add("waiting");
}

function renderResult(data) {
  resultStatus.classList.remove("ok", "fail");

  if (data.system_error) {
    resultStatus.textContent = "Проверка временно недоступна";
    resultStatus.classList.add("fail");
  } else if (data.ok) {
    resultStatus.textContent = "Сообщение прошло модерацию";
    resultStatus.classList.add("ok");
  } else {
    resultStatus.textContent = "Сообщение отклонено";
    resultStatus.classList.add("fail");
  }

  resultFilter.textContent = data.filter
    ? `Сработал фильтр: ${data.filter}`
    : "";
  resultReason.textContent = data.reason ? `Причина: ${data.reason}` : "";

  resultPanel.animate(
    [
      { transform: "translateY(0)", opacity: 1 },
      { transform: "translateY(-2px)", opacity: 0.95 },
      { transform: "translateY(0)", opacity: 1 },
    ],
    { duration: 320, easing: "ease-out" },
  );
}

function updateCharCount() {
  charCount.textContent = String(messageInput.value.length);
}

async function moderate() {
  const message = messageInput.value.trim();
  clearResult();
  if (!message) {
    renderResult({
      ok: false,
      reason: "Введите текст сообщения перед проверкой",
    });
    return;
  }

  checkBtn.disabled = true;
  checkBtn.textContent = "Проверяем...";

  const payload = {
    message: message,
    filters: collectFilters(),
    model: modelSelect.value || null,
  };

  try {
    const response = await fetch("/api/moderate", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    const data = await response.json();
    renderResult(data);
  } catch {
    renderResult({
      ok: false,
      reason: "Не удалось получить ответ от сервера",
    });
  } finally {
    checkBtn.disabled = false;
    checkBtn.textContent = "Проверить";
  }
}

checkBtn.addEventListener("click", moderate);
messageInput.addEventListener("input", updateCharCount);
messageInput.addEventListener("keydown", (event) => {
  if ((event.ctrlKey || event.metaKey) && event.key === "Enter") {
    moderate();
  }
});

updateCharCount();
loadModels();
