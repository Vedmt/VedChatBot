class MobisChatbot {
  constructor() {
    this.sessionId = this.generateSessionId();
    this.apiBaseUrl = "/api/v1/chat";
    this.isTyping = false;

    this.initializeElements();
    this.bindEvents();
    this.updateSessionId();
  }

  initializeElements() {
    this.chatContainer = document.getElementById("chatContainer");
    this.messageInput = document.getElementById("messageInput");
    this.sendButton = document.getElementById("sendButton");
    this.typingIndicator = document.getElementById("typingIndicator");
    this.sessionIdElement = document.getElementById("sessionId");
    this.quickActionButtons = document.querySelectorAll(".quick-action");
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

    // Quick action buttons
    this.quickActionButtons.forEach((button) => {
      button.addEventListener("click", (e) => {
        const message = e.currentTarget.dataset.message;
        this.messageInput.value = message;
        this.sendMessage();
      });
    });

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
        this.addBotMessage(response.message);
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

  addUserMessage(message) {
    const messageElement = this.createMessageElement(message, "user");
    this.chatContainer.appendChild(messageElement);
    this.scrollToBottom();
  }

  addBotMessage(message) {
    const messageElement = this.createMessageElement(message, "bot");
    this.chatContainer.appendChild(messageElement);
    this.scrollToBottom();
  }

  createMessageElement(message, sender) {
    const messageDiv = document.createElement("div");
    messageDiv.className = "message-bubble";

    if (sender === "user") {
      messageDiv.innerHTML = `
                <div class="flex items-start space-x-3 justify-end">
                    <div class="bg-blue-600 text-white rounded-lg p-3 max-w-xs lg:max-w-md">
                        <p class="text-sm">${this.escapeHtml(message)}</p>
                    </div>
                    <div class="w-8 h-8 bg-gray-600 rounded-full flex items-center justify-center">
                        <i class="fas fa-user text-white text-sm"></i>
                    </div>
                </div>
            `;
    } else {
      messageDiv.innerHTML = `
                <div class="flex items-start space-x-3">
                    <div class="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center">
                        <i class="fas fa-robot text-white text-sm"></i>
                    </div>
                    <div class="bg-blue-100 rounded-lg p-3 max-w-xs lg:max-w-md">
                        <p class="text-sm">${this.formatBotMessage(message)}</p>
                    </div>
                </div>
            `;
    }

    return messageDiv;
  }

  formatBotMessage(message) {
    // Basic formatting for bot messages
    let formatted = this.escapeHtml(message);

    // Convert newlines to <br>
    formatted = formatted.replace(/\n/g, "<br>");

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
    this.sendButton.disabled = true;
    this.sendButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
    this.scrollToBottom();
  }

  hideTypingIndicator() {
    this.isTyping = false;
    this.typingIndicator.classList.add("hidden");
    this.sendButton.disabled = false;
    this.sendButton.innerHTML = '<i class="fas fa-paper-plane"></i>';
  }

  scrollToBottom() {
    setTimeout(() => {
      this.chatContainer.scrollTop = this.chatContainer.scrollHeight;
    }, 100);
  }

  // Public method to add a message programmatically
  sendProgrammaticMessage(message) {
    this.messageInput.value = message;
    this.sendMessage();
  }
}

// Initialize the chatbot when the page loads
document.addEventListener("DOMContentLoaded", () => {
  window.mobisChatbot = new MobisChatbot();
});

// Export for potential use in other scripts
if (typeof module !== "undefined" && module.exports) {
  module.exports = MobisChatbot;
}
