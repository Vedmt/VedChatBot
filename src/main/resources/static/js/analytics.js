let analyticsData = null;
let currentPage = 0;
let totalChatPages = 0;

// Initialize dashboard
document.addEventListener("DOMContentLoaded", function () {
  loadAnalyticsData();
  loadChatHistory(0);
});

async function loadAnalyticsData() {
  try {
    const response = await fetch("/api/v1/analytics/dashboard");
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    analyticsData = await response.json();
    updateDashboard();
  } catch (error) {
    console.error("Error loading analytics:", error);
    showError("Failed to load analytics data");
  }
}

function updateDashboard() {
  if (!analyticsData) return;

  // Update stat cards
  document.getElementById("totalUsers").textContent =
    analyticsData.totalUsers.toLocaleString();
  document.getElementById("totalConversations").textContent =
    analyticsData.totalConversations.toLocaleString();
  document.getElementById("totalMessages").textContent =
    analyticsData.totalMessages.toLocaleString();

  // Update top brand
  if (analyticsData.popularBrands && analyticsData.popularBrands.length > 0) {
    document.getElementById("topBrand").textContent =
      analyticsData.popularBrands[0].brandName;
  }

  // Create charts
  createBrandsChart();
  createFunctionsChart();
  createTimelineChart();

  // Hide loading and show dashboard
  document.getElementById("loading").classList.add("hidden");
  document.getElementById("dashboard").classList.remove("hidden");
}

function createBrandsChart() {
  const ctx = document.getElementById("brandsChart").getContext("2d");

  const brands = analyticsData.popularBrands || [];
  const labels = brands.map((b) => b.brandName);
  const data = brands.map((b) => b.queryCount);

  const colors = [
    "#3B82F6",
    "#10B981",
    "#F59E0B",
    "#EF4444",
    "#8B5CF6",
    "#06B6D4",
    "#84CC16",
    "#F97316",
    "#EC4899",
    "#6366F1",
  ];

  new Chart(ctx, {
    type: "doughnut",
    data: {
      labels: labels,
      datasets: [
        {
          data: data,
          backgroundColor: colors.slice(0, data.length),
          borderWidth: 2,
          borderColor: "#ffffff",
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: "bottom",
          labels: {
            padding: 20,
            usePointStyle: true,
          },
        },
      },
    },
  });
}

function createFunctionsChart() {
  const ctx = document.getElementById("functionsChart").getContext("2d");

  const functions = analyticsData.functionUsageStats || {};
  const labels = Object.keys(functions).map((f) => formatFunctionName(f));
  const data = Object.values(functions);

  new Chart(ctx, {
    type: "bar",
    data: {
      labels: labels,
      datasets: [
        {
          label: "Usage Count",
          data: data,
          backgroundColor: "#3B82F6",
          borderColor: "#1E40AF",
          borderWidth: 1,
          borderRadius: 8,
          borderSkipped: false,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: false,
        },
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: {
            stepSize: 1,
          },
        },
        x: {
          ticks: {
            maxRotation: 45,
          },
        },
      },
    },
  });
}

function createTimelineChart() {
  const ctx = document.getElementById("timelineChart").getContext("2d");

  const stats = analyticsData.conversationStats || [];
  const labels = stats.map((s) => s.date);
  const conversationData = stats.map((s) => s.conversationCount);
  const messageData = stats.map((s) => s.messageCount);

  new Chart(ctx, {
    type: "line",
    data: {
      labels: labels,
      datasets: [
        {
          label: "Conversations",
          data: conversationData,
          borderColor: "#3B82F6",
          backgroundColor: "rgba(59, 130, 246, 0.1)",
          tension: 0.4,
          fill: false,
        },
        {
          label: "Messages",
          data: messageData,
          borderColor: "#10B981",
          backgroundColor: "rgba(16, 185, 129, 0.1)",
          tension: 0.4,
          fill: false,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: "top",
        },
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: {
            stepSize: 1,
          },
        },
      },
      interaction: {
        intersect: false,
        mode: "index",
      },
    },
  });
}

async function loadChatHistory(page = 0) {
  try {
    const response = await fetch(
      `/api/v1/analytics/chat-history?page=${page}&size=20`
    );
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    populateChatHistoryTable(data.content);
    updatePagination(data);
  } catch (error) {
    console.error("Error loading chat history:", error);
    showError("Failed to load chat history");
  }
}

function populateChatHistoryTable(messages) {
  const tbody = document.getElementById("chatHistoryTable");
  tbody.innerHTML = "";

  messages.forEach((message) => {
    const row = document.createElement("tr");
    row.className = "hover:bg-gray-50 cursor-pointer";
    row.onclick = () => showMessageDetails(message);

    row.innerHTML = `
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                ${formatDateTime(message.timestamp)}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                <span class="font-mono text-xs">${message.sessionId.substring(
                  0,
                  8
                )}...</span>
            </td>
            <td class="px-6 py-4 text-sm text-gray-900">
                <div class="max-w-xs truncate">${
                  message.userMessage || "-"
                }</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                <span class="px-2 py-1 bg-blue-100 text-blue-800 rounded-full text-xs">
                    ${formatFunctionName(message.functionsCalled)}
                </span>
            </td>
        `;

    tbody.appendChild(row);
  });
}

function updatePagination(data) {
  currentPage = data.number;
  totalChatPages = data.totalPages;

  document.getElementById("currentPageSpan").textContent = currentPage + 1;
  document.getElementById("showingCount").textContent = data.numberOfElements;
  document.getElementById("totalCount").textContent = data.totalElements;

  // Update button states
  document.getElementById("prevBtn").disabled = data.first;
  document.getElementById("nextBtn").disabled = data.last;

  if (data.first) {
    document
      .getElementById("prevBtn")
      .classList.add("opacity-50", "cursor-not-allowed");
  } else {
    document
      .getElementById("prevBtn")
      .classList.remove("opacity-50", "cursor-not-allowed");
  }

  if (data.last) {
    document
      .getElementById("nextBtn")
      .classList.add("opacity-50", "cursor-not-allowed");
  } else {
    document
      .getElementById("nextBtn")
      .classList.remove("opacity-50", "cursor-not-allowed");
  }
}

async function searchChats() {
  const keyword = document.getElementById("searchInput").value.trim();
  if (!keyword) {
    loadChatHistory(0);
    return;
  }

  try {
    const response = await fetch(
      `/api/v1/analytics/search?keyword=${encodeURIComponent(keyword)}`
    );
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const messages = await response.json();
    populateChatHistoryTable(messages);

    // Update pagination for search results
    document.getElementById("showingCount").textContent = messages.length;
    document.getElementById("totalCount").textContent = messages.length;
    document.getElementById("currentPageSpan").textContent = "1";
    document.getElementById("prevBtn").disabled = true;
    document.getElementById("nextBtn").disabled = true;
  } catch (error) {
    console.error("Error searching chats:", error);
    showError("Failed to search chat history");
  }
}

function showMessageDetails(message) {
  alert(
    `Session: ${message.sessionId}\n\nUser: ${message.userMessage}\n\nBot: ${
      message.botResponse
    }\n\nFunction: ${
      message.functionsCalled || "None"
    }\n\nTime: ${formatDateTime(message.timestamp)}`
  );
}

function formatDateTime(timestamp) {
  return new Date(timestamp).toLocaleString();
}

function formatFunctionName(functionName) {
  if (!functionName) return "None";

  const functionMap = {
    getCarMakesFunction: "Car Makes",
    getModelsForMakeFunction: "Car Models",
    getVehicleTypesForMakeFunction: "Vehicle Types",
    getCatFactFunction: "Cat Facts",
  };

  return functionMap[functionName] || functionName;
}

function refreshData() {
  document.getElementById("loading").classList.remove("hidden");
  document.getElementById("dashboard").classList.add("hidden");

  loadAnalyticsData();
  loadChatHistory(currentPage);
}

function showError(message) {
  const errorDiv = document.createElement("div");
  errorDiv.className =
    "bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded fixed top-4 right-4 z-50";
  errorDiv.innerHTML = `
        <div class="flex">
            <div class="flex-shrink-0">
                <i class="fas fa-exclamation-circle"></i>
            </div>
            <div class="ml-3">
                <p class="text-sm">${message}</p>
            </div>
        </div>
    `;

  document.body.appendChild(errorDiv);

  setTimeout(() => {
    document.body.removeChild(errorDiv);
  }, 5000);
}

// Handle search on Enter key
document
  .getElementById("searchInput")
  .addEventListener("keypress", function (e) {
    if (e.key === "Enter") {
      searchChats();
    }
  });
