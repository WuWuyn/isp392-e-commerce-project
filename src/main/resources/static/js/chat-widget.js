/**
 * ReadHub Chat Widget
 * Simple Bootstrap 5 chat interface with RAG functionality
 */
class ReadHubChatWidget {
    constructor() {
        console.log('ReadHubChatWidget constructor called');
        this.sessionToken = this.getOrCreateSessionToken();
        this.isLoading = false;
        this.messages = [];
        this.conversations = [];

        this.initializeWidget();
        this.bindEvents();
        this.loadChatHistory();
        this.loadConversations();
        console.log('ReadHubChatWidget initialized with session:', this.sessionToken);
    }

    /**
     * Initialize the chat widget HTML structure
     */
    initializeWidget() {
        // Create chat button (floating action button)
        const chatButton = `
            <div id="chatButton" class="chat-button" title="Chat with ReadHub Assistant">
                <i class="fas fa-comments"></i>
                <span class="chat-notification" id="chatNotification" style="display: none;"></span>
            </div>
        `;

        // Create chat modal using Bootstrap 5
        const chatModal = `
            <div class="modal fade" id="chatModal" tabindex="-1" aria-labelledby="chatModalLabel" aria-hidden="true">
                <div class="modal-dialog modal-xl">
                    <div class="modal-content">
                        <div class="modal-header bg-primary text-white">
                            <h5 class="modal-title" id="chatModalLabel">
                                <i class="fas fa-robot me-2"></i>ReadHub Assistant
                            </h5>
                            <div class="d-flex gap-2">
                                <button type="button" class="btn btn-outline-light btn-sm" id="newChatBtn">
                                    <i class="fas fa-plus me-1"></i>New Chat
                                </button>
                                <button type="button" class="btn btn-outline-light btn-sm" id="historyBtn">
                                    <i class="fas fa-history me-1"></i>History
                                </button>
                                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                            </div>
                        </div>
                        <div class="modal-body p-0 d-flex">
                            <div id="conversationSidebar" class="conversation-sidebar border-end">
                                <div class="p-3 border-bottom">
                                    <h6 class="mb-0">Conversations</h6>
                                </div>
                                <div id="conversationList" class="conversation-list">
                                    <!-- Conversations will be loaded here -->
                                </div>
                            </div>
                            <div class="chat-main flex-grow-1">
                                <div id="chatMessages" class="chat-messages"></div>
                                <div class="chat-input-container p-3 border-top">
                                    <div class="input-group">
                                        <input type="text" id="chatInput" class="form-control"
                                               placeholder="Ask about books, authors, or store info..."
                                               maxlength="2000">
                                        <button id="sendBtn" class="btn btn-primary" type="button" disabled>
                                            <i class="fas fa-paper-plane"></i>
                                        </button>
                                    </div>
                                    <small class="text-muted mt-1 d-block">
                                        Try: "Recommend fiction books under $20" or "What are your store hours?"
                                    </small>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;

        // Add CSS styles
        const chatStyles = `
            <style>
                .chat-button {
                    position: fixed;
                    bottom: 20px;
                    right: 20px;
                    width: 60px;
                    height: 60px;
                    background: #0d6efd;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: white;
                    font-size: 24px;
                    cursor: pointer;
                    box-shadow: 0 4px 12px rgba(13,110,253,0.3);
                    transition: all 0.3s ease;
                    z-index: 1000;
                    border: none;
                }
                
                .chat-button:hover {
                    background: #0b5ed7;
                    transform: scale(1.1);
                    box-shadow: 0 6px 16px rgba(13,110,253,0.4);
                }
                
                .chat-notification {
                    position: absolute;
                    top: -5px;
                    right: -5px;
                    background: #dc3545;
                    color: white;
                    border-radius: 50%;
                    width: 20px;
                    height: 20px;
                    font-size: 12px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                
                .conversation-sidebar {
                    width: 280px;
                    min-width: 280px;
                    max-width: 280px;
                    background: #f8f9fa;
                    max-height: 500px;
                    overflow-y: auto;
                    flex-shrink: 0;
                }

                .conversation-list {
                    max-height: 450px;
                    overflow-y: auto;
                }

                .conversation-item {
                    padding: 12px 16px;
                    border-bottom: 1px solid #e9ecef;
                    cursor: pointer;
                    transition: background-color 0.2s ease;
                }

                .conversation-item:hover {
                    background-color: #e9ecef;
                }

                .conversation-item.active {
                    background-color: #0d6efd;
                    color: white;
                }

                .conversation-title {
                    font-weight: 500;
                    font-size: 14px;
                    margin-bottom: 4px;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }

                .conversation-time {
                    font-size: 12px;
                    opacity: 0.7;
                }

                .chat-main {
                    display: flex;
                    flex-direction: column;
                }

                .chat-messages {
                    height: 400px;
                    overflow-y: auto;
                    padding: 20px;
                    background: #f8f9fa;
                    flex-grow: 1;
                }
                
                .message {
                    margin-bottom: 15px;
                    display: flex;
                    align-items: flex-start;
                    animation: fadeIn 0.3s ease-in;
                }
                
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(10px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                
                .message.user {
                    justify-content: flex-end;
                }
                
                .message-content {
                    max-width: 70%;
                    padding: 12px 16px;
                    border-radius: 18px;
                    word-wrap: break-word;
                    line-height: 1.4;
                }
                
                .message.user .message-content {
                    background: #0d6efd;
                    color: white;
                    border-bottom-right-radius: 4px;
                }
                
                .message.bot .message-content {
                    background: white;
                    color: #333;
                    border: 1px solid #e9ecef;
                    border-bottom-left-radius: 4px;
                }
                
                .message-avatar {
                    width: 32px;
                    height: 32px;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 14px;
                    margin: 0 8px;
                    flex-shrink: 0;
                }
                
                .message.user .message-avatar {
                    background: #198754;
                    color: white;
                    order: 2;
                }
                
                .message.bot .message-avatar {
                    background: #6c757d;
                    color: white;
                }
                
                .message-time {
                    font-size: 11px;
                    color: #6c757d;
                    margin-top: 4px;
                    opacity: 0.7;
                }
                
                .typing-indicator {
                    display: flex;
                    align-items: center;
                    padding: 12px 16px;
                    background: white;
                    border-radius: 18px;
                    border-bottom-left-radius: 4px;
                    border: 1px solid #e9ecef;
                    max-width: 70%;
                }
                
                .typing-dots {
                    display: flex;
                    gap: 4px;
                }
                
                .typing-dots span {
                    width: 8px;
                    height: 8px;
                    border-radius: 50%;
                    background: #6c757d;
                    animation: typing 1.4s infinite ease-in-out;
                }
                
                .typing-dots span:nth-child(1) { animation-delay: -0.32s; }
                .typing-dots span:nth-child(2) { animation-delay: -0.16s; }
                
                @keyframes typing {
                    0%, 80%, 100% { transform: scale(0.8); opacity: 0.5; }
                    40% { transform: scale(1); opacity: 1; }
                }
                
                .chat-input-container {
                    background: white;
                }
                
                #chatInput:focus {
                    border-color: #0d6efd;
                    box-shadow: 0 0 0 0.2rem rgba(13,110,253,0.25);
                }
                
                .welcome-message {
                    text-align: center;
                    color: #6c757d;
                    font-style: italic;
                    margin: 20px 0;
                    padding: 20px;
                    background: white;
                    border-radius: 10px;
                    border: 1px solid #e9ecef;
                }
                
                .error-message {
                    background: #f8d7da !important;
                    color: #721c24 !important;
                    border-color: #f5c6cb !important;
                }
            </style>
        `;

        // Add to DOM
        document.head.insertAdjacentHTML('beforeend', chatStyles);
        document.body.insertAdjacentHTML('beforeend', chatButton);
        document.body.insertAdjacentHTML('beforeend', chatModal);
    }

    /**
     * Bind event listeners
     */
    bindEvents() {
        console.log('Binding chat widget events');

        // Chat button click
        const chatButton = document.getElementById('chatButton');
        if (chatButton) {
            chatButton.addEventListener('click', () => {
                console.log('Chat button clicked');
                this.openChat();
            });
            console.log('Chat button event bound successfully');
        } else {
            console.error('Chat button not found!');
        }

        // Send button click
        document.getElementById('sendBtn').addEventListener('click', () => {
            this.sendMessage();
        });

        // Enter key in input
        document.getElementById('chatInput').addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendMessage();
            }
        });

        // Input change to enable/disable send button
        document.getElementById('chatInput').addEventListener('input', (e) => {
            const sendBtn = document.getElementById('sendBtn');
            sendBtn.disabled = e.target.value.trim().length === 0 || this.isLoading;
        });

        // New chat button
        document.getElementById('newChatBtn').addEventListener('click', () => {
            this.createNewConversation();
        });

        // History button
        document.getElementById('historyBtn').addEventListener('click', () => {
            this.toggleConversationSidebar();
        });

        // Modal events
        const chatModal = document.getElementById('chatModal');
        chatModal.addEventListener('shown.bs.modal', () => {
            document.getElementById('chatInput').focus();
            this.hideNotification();
        });
    }

    /**
     * Open the chat modal
     */
    openChat() {
        const modal = new bootstrap.Modal(document.getElementById('chatModal'));
        modal.show();
    }

    /**
     * Send a message to the chatbot
     */
    async sendMessage() {
        const input = document.getElementById('chatInput');
        const message = input.value.trim();

        if (!message || this.isLoading) return;

        // Clear input and disable send button
        input.value = '';
        this.updateSendButton(true);

        // Add user message to UI
        this.addMessage(message, 'user');

        // Show typing indicator
        this.showTypingIndicator();

        try {
            const response = await fetch('/api/chat/send', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ 
                    message: message,
                    sessionToken: this.sessionToken
                })
            });

            const data = await response.json();

            // Hide typing indicator
            this.hideTypingIndicator();

            if (data.success) {
                this.addMessage(data.message, 'bot');
                // Update session token if provided
                if (data.sessionToken) {
                    this.sessionToken = data.sessionToken;
                    localStorage.setItem('readhub_chat_session', this.sessionToken);
                }
            } else {
                this.addMessage(data.message || 'Sorry, I encountered an error.', 'bot', true);
            }

        } catch (error) {
            console.error('Chat error:', error);
            this.hideTypingIndicator();
            this.addMessage('Sorry, I\'m having trouble connecting right now. Please try again.', 'bot', true);
        } finally {
            this.updateSendButton(false);
        }
    }

    /**
     * Add a message to the chat UI with proper attribution
     */
    addMessage(content, sender, isError = false) {
        const messagesContainer = document.getElementById('chatMessages');
        const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${sender}`;
        messageDiv.setAttribute('data-sender', sender);
        messageDiv.setAttribute('data-timestamp', new Date().toISOString());

        let messageContent = `
            <div class="message-content ${isError ? 'error-message' : ''}">
                ${this.formatMessageContent(content)}
                <div class="message-time">${time}</div>
            </div>
        `;

        if (sender === 'bot') {
            messageContent = `
                <div class="message-avatar bot-avatar">
                    <i class="fas fa-robot"></i>
                </div>
                ${messageContent}
            `;
        } else {
            messageContent = `
                ${messageContent}
                <div class="message-avatar user-avatar">
                    <i class="fas fa-user"></i>
                </div>
            `;
        }

        messageDiv.innerHTML = messageContent;
        messagesContainer.appendChild(messageDiv);

        // Scroll to bottom smoothly
        messagesContainer.scrollTop = messagesContainer.scrollHeight;

        // Store message with proper metadata
        this.messages.push({
            content,
            sender,
            timestamp: new Date(),
            isError,
            id: Date.now() + Math.random() // Unique ID for debugging
        });

        console.log(`Message added: ${sender} - ${content.substring(0, 50)}...`);
    }

    /**
     * Format message content (convert line breaks, etc.)
     */
    formatMessageContent(content) {
        return content.replace(/\n/g, '<br>');
    }

    /**
     * Show typing indicator
     */
    showTypingIndicator() {
        const messagesContainer = document.getElementById('chatMessages');
        const typingDiv = document.createElement('div');
        typingDiv.className = 'message bot';
        typingDiv.id = 'typingIndicator';
        typingDiv.innerHTML = `
            <div class="message-avatar">
                <i class="fas fa-robot"></i>
            </div>
            <div class="typing-indicator">
                <div class="typing-dots">
                    <span></span>
                    <span></span>
                    <span></span>
                </div>
            </div>
        `;
        messagesContainer.appendChild(typingDiv);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    /**
     * Hide typing indicator
     */
    hideTypingIndicator() {
        const typingIndicator = document.getElementById('typingIndicator');
        if (typingIndicator) {
            typingIndicator.remove();
        }
    }

    /**
     * Update send button state
     */
    updateSendButton(loading) {
        const sendBtn = document.getElementById('sendBtn');
        const input = document.getElementById('chatInput');

        this.isLoading = loading;

        if (loading) {
            sendBtn.disabled = true;
            sendBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        } else {
            sendBtn.disabled = input.value.trim().length === 0;
            sendBtn.innerHTML = '<i class="fas fa-paper-plane"></i>';
        }
    }

    /**
     * Load chat history from server
     */
    async loadChatHistory() {
        try {
            const response = await fetch(`/api/chat/history?sessionToken=${this.sessionToken}`);
            const data = await response.json();

            if (data.success && data.messages && data.messages.length > 0) {
                // Load recent messages (limit to last 10 for UI performance)
                const recentMessages = data.messages.slice(-10);
                recentMessages.forEach(msg => {
                    this.addMessage(msg.content, msg.isUserMessage ? 'user' : 'bot');
                });
            } else {
                // Show welcome message
                this.addWelcomeMessage();
            }
        } catch (error) {
            console.error('Error loading chat history:', error);
            this.addWelcomeMessage();
        }
    }

    /**
     * Add welcome message
     */
    addWelcomeMessage() {
        const messagesContainer = document.getElementById('chatMessages');
        const welcomeDiv = document.createElement('div');
        welcomeDiv.className = 'welcome-message';
        welcomeDiv.innerHTML = `
            <h6><i class="fas fa-robot me-2"></i>Welcome to ReadHub!</h6>
            <p class="mb-2">I'm your AI assistant. I can help you:</p>
            <ul class="list-unstyled mb-0">
                <li>📚 Find books by title, author, or genre</li>
                <li>💰 Get recommendations within your budget</li>
                <li>🏪 Answer questions about store policies</li>
                <li>⏰ Provide store information</li>
            </ul>
            <p class="mt-2 mb-0"><strong>What can I help you with today?</strong></p>
        `;
        messagesContainer.appendChild(welcomeDiv);
    }

    /**
     * Get or create session token
     */
    getOrCreateSessionToken() {
        let token = localStorage.getItem('readhub_chat_session');
        if (!token) {
            token = 'session_' + Date.now() + '_' + Math.random().toString(36).substring(2, 11);
            localStorage.setItem('readhub_chat_session', token);
        }
        return token;
    }

    /**
     * Show notification on chat button
     */
    showNotification(count = '') {
        const notification = document.getElementById('chatNotification');
        if (notification) {
            notification.textContent = count;
            notification.style.display = 'flex';
        }
    }

    /**
     * Hide notification on chat button
     */
    hideNotification() {
        const notification = document.getElementById('chatNotification');
        if (notification) {
            notification.style.display = 'none';
        }
    }

    /**
     * Load user's conversation sessions
     */
    async loadConversations() {
        try {
            const response = await fetch('/api/chat/sessions');
            const data = await response.json();

            if (data.success && data.sessions) {
                this.conversations = data.sessions;
                this.renderConversationList();
            }
        } catch (error) {
            console.error('Error loading conversations:', error);
        }
    }

    /**
     * Render the conversation list in the sidebar
     */
    renderConversationList() {
        const conversationList = document.getElementById('conversationList');
        if (!conversationList) return;

        if (this.conversations.length === 0) {
            conversationList.innerHTML = `
                <div class="p-3 text-center text-muted">
                    <i class="fas fa-comments mb-2 d-block"></i>
                    No conversations yet
                </div>
            `;
            return;
        }

        conversationList.innerHTML = this.conversations.map(conv => {
            const isActive = conv.sessionToken === this.sessionToken;
            const title = conv.title || 'New Conversation';
            const time = new Date(conv.updatedAt).toLocaleDateString();

            return `
                <div class="conversation-item ${isActive ? 'active' : ''}"
                     data-session-token="${conv.sessionToken}">
                    <div class="conversation-title">${title}</div>
                    <div class="conversation-time">${time}</div>
                </div>
            `;
        }).join('');

        // Add click handlers for conversation items
        conversationList.querySelectorAll('.conversation-item').forEach(item => {
            item.addEventListener('click', () => {
                const sessionToken = item.dataset.sessionToken;
                if (sessionToken !== this.sessionToken) {
                    this.switchToConversation(sessionToken);
                }
            });
        });
    }

    /**
     * Create a new conversation
     */
    async createNewConversation() {
        try {
            const response = await fetch('/api/chat/sessions/new', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            const data = await response.json();

            if (data.success) {
                this.sessionToken = data.sessionToken;
                localStorage.setItem('readhub_chat_session', this.sessionToken);

                // Clear current messages
                this.messages = [];
                document.getElementById('chatMessages').innerHTML = '';

                // Reload conversations and show welcome message
                await this.loadConversations();
                this.addWelcomeMessage();

                console.log('New conversation created:', data.sessionToken);
            } else {
                console.error('Failed to create new conversation:', data.message);
            }
        } catch (error) {
            console.error('Error creating new conversation:', error);
        }
    }

    /**
     * Switch to an existing conversation
     */
    async switchToConversation(sessionToken) {
        try {
            const response = await fetch('/api/chat/sessions/switch', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ sessionToken })
            });

            const data = await response.json();

            if (data.success) {
                this.sessionToken = sessionToken;
                localStorage.setItem('readhub_chat_session', this.sessionToken);

                // Clear current messages and load history for this session
                this.messages = [];
                document.getElementById('chatMessages').innerHTML = '';

                await this.loadChatHistory();
                this.renderConversationList(); // Update active state

                console.log('Switched to conversation:', sessionToken);
            } else {
                console.error('Failed to switch conversation:', data.message);
            }
        } catch (error) {
            console.error('Error switching conversation:', error);
        }
    }

    /**
     * Toggle conversation sidebar visibility
     */
    toggleConversationSidebar() {
        const sidebar = document.getElementById('conversationSidebar');
        if (sidebar) {
            sidebar.style.display = sidebar.style.display === 'none' ? 'block' : 'none';
        }
    }
}

// Initialize chat widget when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    // Check if chat widget already exists
    if (document.getElementById('chatButton')) {
        console.log('Chat widget already initialized');
        return;
    }

    // Check if Font Awesome is loaded, if not, load it
    if (!document.querySelector('link[href*="font-awesome"]') && !document.querySelector('link[href*="fontawesome"]')) {
        const fontAwesome = document.createElement('link');
        fontAwesome.rel = 'stylesheet';
        fontAwesome.href = 'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css';
        document.head.appendChild(fontAwesome);
    }

    // Initialize chat widget
    try {
        window.readHubChatWidget = new ReadHubChatWidget();
        console.log('Chat widget initialized successfully');
    } catch (error) {
        console.error('Error initializing chat widget:', error);
    }
});
