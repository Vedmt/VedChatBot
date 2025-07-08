class BubbleChatbot {
  constructor() {
    this.sessionId = this.generateSessionId();
    this.apiBaseUrl = "/api/v1/chat";
    this.isTyping = false;
    this.conversationHistory = [];

    this.initializeElements();
    this.bindEvents();
    this.updateSessionId();
    this.startConversation();
  }

  initializeElements() {
    this.chatContainer = document.getElementById("chatContainer");
    this.messageInput = document.getElementById("messageInput");
    this.sendButton = document.getElementById("sendButton");
    this.typingIndicator = document.getElementById("typingIndicator");
    this.sessionIdElement = document.getElementById("sessionId");
    this.startOverButton = document.getElementById("startOverButton");
  }

  bindEvents() {
    // Send message on button click
    this.sendButton.addEventListener("click", () => this.sendMessage());

    // Send message on Enter key press
    this.messageInput.addEventListener("keypress", (e) => {
      if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault();
        this.sendMessage();
      }
    });

    // Start over button
    this.startOverButton.addEventListener("click", () =>
      this.startNewConversation()
    );

    // Auto-resize input
    this.messageInput.addEventListener(
      "input",
      this.adjustInputHeight.bind(this)
    );
  }

  generateSessionId() {
    return (
      "session_" + Math.random().toString(36).substr(2, 9) + "_" + Date.now()
    );
  }

  updateSessionId() {
    this.sessionIdElement.textContent = this.sessionId;
  }

  adjustInputHeight() {
    this.messageInput.style.height = "auto";
    this.messageInput.style.height =
      Math.min(this.messageInput.scrollHeight, 120) + "px";
  }

  startConversation() {
    this.addWelcomeMessage();
  }

  startNewConversation() {
    // Clear chat container
    this.chatContainer.innerHTML = "";
    this.conversationHistory = [];

    // Generate new session ID
    this.sessionId = this.generateSessionId();
    this.updateSessionId();

    // Start fresh conversation
    this.startConversation();
  }

  addWelcomeMessage() {
    const welcomeMessage = {
      type: "bot",
      content:
        "Hello! I'm your Hyundai Mobis Genuine Accessories assistant. I can help you explore genuine accessories for your Hyundai vehicle and find the best prices.",
      question: "What would you like to explore?",
      options: [
        "Browse Accessories",
        "Find Dealers & Distributors",
        "Check Current Offers",
        "Get Product Support",
      ],
      conversationType: "main_menu",
    };

    this.addBotMessageWithOptions(welcomeMessage);
  }

  async sendMessage() {
    const message = this.messageInput.value.trim();

    if (!message || this.isTyping) {
      return;
    }

    // Add user message to chat
    this.addUserMessage(message);

    // Clear input
    this.messageInput.value = "";
    this.adjustInputHeight();

    // Show typing indicator
    this.showTypingIndicator();

    try {
      const response = await this.callChatAPI(message);
      this.hideTypingIndicator();

      if (response.success) {
        this.handleBotResponse(response);
      } else {
        this.addBotMessage(
          response.errorMessage ||
            "Sorry, I encountered an error. Please try again."
        );
      }
    } catch (error) {
      this.hideTypingIndicator();
      this.addBotMessage(
        "I'm having trouble connecting. Please check your internet connection and try again."
      );
      console.error("Chat API error:", error);
    }
  }

  async callChatAPI(message) {
    const requestBody = {
      message: message,
      sessionId: this.sessionId,
    };

    const response = await fetch(`${this.apiBaseUrl}/message`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(requestBody),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return await response.json();
  }

  handleBotResponse(response) {
    // Store in conversation history
    this.conversationHistory.push({
      userMessage: this.getLastUserMessage(),
      botResponse: response,
    });

    if (response.options && response.options.length > 0) {
      // Bot is asking a question with options
      this.addBotMessageWithOptions({
        type: "bot",
        content: response.message,
        question: response.question,
        options: response.options,
        conversationType: response.conversationType,
      });
    } else {
      // Bot is giving a direct answer
      this.addBotMessage(response.message);
    }
  }

  getLastUserMessage() {
    const userMessages = this.conversationHistory
      .filter((entry) => entry.userMessage)
      .map((entry) => entry.userMessage);
    return userMessages[userMessages.length - 1] || "";
  }

  addUserMessage(message) {
    const messageElement = this.createUserMessageElement(message);
    this.chatContainer.appendChild(messageElement);
    this.scrollToBottom();
  }

  addBotMessage(message) {
    const messageElement = this.createBotMessageElement(message);
    this.chatContainer.appendChild(messageElement);
    this.scrollToBottom();
  }

  addBotMessageWithOptions(messageData) {
    const messageElement = this.createBotMessageWithOptionsElement(messageData);
    this.chatContainer.appendChild(messageElement);
    this.scrollToBottom();
  }

  createUserMessageElement(message) {
    const messageDiv = document.createElement("div");
    messageDiv.className = "message-bubble";
    messageDiv.innerHTML = `
            <div class="flex items-start space-x-3 justify-end">
                <div class="bg-blue-600 text-white rounded-2xl px-4 py-3 max-w-xs lg:max-w-md">
                    <p class="text-sm">${this.escapeHtml(message)}</p>
                </div>
                <div class="w-10 h-10 bg-gray-600 rounded-full flex items-center justify-center">
                    <i class="fas fa-user text-white"></i>
                </div>
            </div>
        `;
    return messageDiv;
  }

  createBotMessageElement(message) {
    const messageDiv = document.createElement("div");
    messageDiv.className = "message-bubble";
    messageDiv.innerHTML = `
            <div class="flex items-start space-x-3">
                <div class="w-10 h-10 bg-blue-500 rounded-full flex items-center justify-center">
                    <i class="fas fa-robot text-white"></i>
                </div>
                <div class="bg-blue-50 rounded-2xl px-4 py-3 max-w-lg lg:max-w-2xl">
                    <div class="text-sm text-gray-800">${this.formatBotMessage(
                      message
                    )}</div>
                </div>
            </div>
        `;
    return messageDiv;
  }

  createBotMessageWithOptionsElement(messageData) {
    const messageDiv = document.createElement("div");
    messageDiv.className = "message-bubble";

    const optionsHtml = messageData.options
      .map(
        (option, index) => `
            <button 
                class="option-bubble bg-white hover:bg-blue-50 text-gray-700 hover:text-blue-700 px-4 py-3 rounded-xl shadow-sm border border-gray-200 text-sm font-medium transition-all duration-200"
                onclick="chatbot.selectOptionFromHTML('${this.escapeHtml(
                  option
                )}')"
            >
                ${this.escapeHtml(option)}
            </button>
        `
      )
      .join("");

    messageDiv.innerHTML = `
            <div class="flex items-start space-x-3">
                <div class="w-10 h-10 bg-blue-500 rounded-full flex items-center justify-center">
                    <i class="fas fa-robot text-white"></i>
                </div>
                <div class="space-y-4">
                    <div class="bg-blue-50 rounded-2xl px-4 py-3 max-w-lg lg:max-w-2xl">
                        <div class="text-sm text-gray-800">${this.formatBotMessage(
                          messageData.content
                        )}</div>
                    </div>
                    ${
                      messageData.question
                        ? `
                        <div class="space-y-3">
                            <p class="text-sm font-medium text-gray-700">${this.escapeHtml(
                              messageData.question
                            )}</p>
                            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                ${optionsHtml}
                            </div>
                        </div>
                    `
                        : ""
                    }
                </div>
            </div>
        `;
    return messageDiv;
  }

  selectOption(option) {
    // Add the selected option as a user message
    this.addUserMessage(option);

    // Send the option to the backend
    this.sendOptionToBackend(option);
  }

  async sendOptionToBackend(option) {
    if (this.isTyping) return;

    this.showTypingIndicator();

    try {
      const response = await this.callChatAPI(option);
      this.hideTypingIndicator();

      if (response.success) {
        this.handleBotResponse(response);
      } else {
        this.addBotMessage(
          response.errorMessage ||
            "Sorry, I encountered an error. Please try again."
        );
      }
    } catch (error) {
      this.hideTypingIndicator();
      this.addBotMessage(
        "I'm having trouble connecting. Please check your internet connection and try again."
      );
      console.error("Chat API error:", error);
    }
  }

  formatBotMessage(message) {
    // Don't escape HTML for markdown content - let markdown be converted to HTML
    let formatted = message;

    // Convert markdown headers
    formatted = formatted.replace(
      /\*\*(.*?)\*\*/g,
      '<strong class="font-semibold text-gray-900">$1</strong>'
    );

    // Convert markdown bullet points
    formatted = formatted.replace(
      /^• (.*?)$/gm,
      '<div class="flex items-start ml-4 mb-1"><span class="text-blue-500 mr-2">•</span><span>$1</span></div>'
    );

    // Convert emojis to larger size
    formatted = formatted.replace(
      /(🚗|🚙|🚚|🚐|🏍️|🚌|🚛|📊|📈|🔢)/g,
      '<span class="text-lg mr-1">$1</span>'
    );

    // Convert newlines to <br> but preserve structure
    formatted = formatted.replace(/\n\n/g, "<br><br>");
    formatted = formatted.replace(/\n/g, "<br>");

    // Handle italic text
    formatted = formatted.replace(
      /\*(.*?)\*/g,
      '<em class="italic text-gray-600">$1</em>'
    );

    // Make part numbers bold (assuming format like "12345-67890")
    formatted = formatted.replace(/(\d{5}-\d{5})/g, "<strong>$1</strong>");

    // Make phone numbers clickable
    formatted = formatted.replace(
      /(\+\d{1,3}-\d{2}-\d{4}-\d{4})/g,
      '<a href="tel:$1" class="text-blue-600 underline">$1</a>'
    );

    // Make email addresses clickable
    formatted = formatted.replace(
      /([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})/g,
      '<a href="mailto:$1" class="text-blue-600 underline">$1</a>'
    );

    // Make websites clickable
    formatted = formatted.replace(
      /(www\.[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})/g,
      '<a href="http://$1" target="_blank" class="text-blue-600 underline">$1</a>'
    );

    return formatted;
  }

  escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
  }

  showTypingIndicator() {
    this.isTyping = true;
    this.typingIndicator.classList.remove("hidden");
    this.scrollToBottom();
  }

  hideTypingIndicator() {
    this.isTyping = false;
    this.typingIndicator.classList.add("hidden");
  }

  scrollToBottom() {
    setTimeout(() => {
      this.chatContainer.scrollTop = this.chatContainer.scrollHeight;
    }, 100);
  }

  // Public method for option selection (called from HTML)
  selectOptionFromHTML(option) {
    this.selectOption(option);
  }
}

// Initialize the chatbot when the page loads
let chatbot;
document.addEventListener("DOMContentLoaded", () => {
  chatbot = new BubbleChatbot();
});

// Make chatbot globally accessible for option selection
window.chatbot = chatbot;
