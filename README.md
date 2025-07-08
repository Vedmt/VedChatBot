# Hyundai Mobis AI Chatbot

An intelligent AI-powered chatbot for Hyundai Mobis customer service, built with Spring Boot and Spring AI, featuring function calling capabilities to provide real-time information about genuine parts, dealers, warranty, and more.

## 🚀 Features

- **AI-Powered Conversations**: Leverages OpenAI GPT-4 or Anthropic Claude for natural language understanding
- **Function Calling**: Dynamically calls REST APIs to fetch real-time information
- **Mobis-Specific Knowledge**: Expert knowledge about Hyundai genuine parts, dealers, distributors, and warranties
- **Modern Web Interface**: Responsive React-based frontend with real-time chat
- **Caching System**: Redis-based caching for optimal performance
- **Session Management**: Persistent conversation history and context
- **WebSocket Support**: Real-time messaging capabilities
- **Analytics Dashboard**: Track usage patterns and response times
- **RESTful APIs**: Comprehensive API documentation with Swagger

## 🏗️ Architecture

```
Frontend (React/HTML5)
    ↓ REST API / WebSocket
Spring Boot Application
    ├── ChatController (REST endpoints)
    ├── ChatbotService (Core logic)
    ├── Spring AI ChatClient (AI integration)
    ├── Function Definitions (API calls)
    ├── MobisApiService (External APIs)
    ├── Redis Caching Layer
    └── H2/PostgreSQL Database
    ↓
External Mobis APIs
    ├── Parts Catalog API
    ├── Dealer Locator API
    ├── Warranty System API
    └── Promotions API
```

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- Redis server (for caching)
- OpenAI API key or Anthropic API key
- Node.js (optional, for frontend development)

## 🛠️ Setup Instructions

### 1. Clone the Repository

```bash
git clone <repository-url>
cd mobis-chatbot
```

### 2. Configure Environment Variables

Create an `.env` file or set environment variables:

```bash
# AI Configuration
OPENAI_API_KEY=your-openai-api-key-here
ANTHROPIC_API_KEY=your-anthropic-api-key-here

# Security
JWT_SECRET=your-jwt-secret-key-here

# Database (Optional - defaults to H2)
DATABASE_URL=jdbc:postgresql://localhost:5432/mobis_chatbot
DATABASE_USERNAME=your-db-username
DATABASE_PASSWORD=your-db-password
```

### 3. Start Redis Server

```bash
# Using Docker
docker run -d -p 6379:6379 redis:latest

# Or install locally
sudo systemctl start redis
```

### 4. Build and Run

```bash
# Build the application
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## 🎯 Usage

### Web Interface

1. Open your browser and navigate to `http://localhost:8080`
2. Start chatting with the Mobis AI Assistant
3. Use quick action buttons for common queries:
   - Find Parts
   - Locate Dealers
   - Check Warranty
   - View Offers

### API Endpoints

#### Chat API

```bash
# Send a message
POST /api/v1/chat/message
Content-Type: application/json

{
  "message": "Find brake pads for Hyundai Sonata 2020",
  "sessionId": "session_abc123"
}
```

#### Management API

```bash
# Get conversation history
GET /api/v1/chat/management/session/{sessionId}

# Get analytics
GET /api/v1/chat/management/analytics?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59

# Search conversations
GET /api/v1/chat/management/search?keyword=brake+pads
```

### Example Conversations

**Finding Parts:**

```
User: "I need brake pads for my 2020 Hyundai Sonata"
Bot: "I found genuine brake pads for your 2020 Hyundai Sonata:

🔧 **Genuine Brake Pad Set** (Part #52350-3X000)
- Price: $89.99 USD
- Compatible with: Sonata 2020-2023, Elantra 2019-2023
- Warranty: 12 months
- Status: ✅ In Stock

Would you like me to help you find a nearby dealer or check for current promotions?"
```

**Locating Dealers:**

```
User: "Find dealers near Mumbai"
Bot: "I found 2 authorized Hyundai dealers near Mumbai:

🏪 **Metro Hyundai Service Center**
- Address: 123 Main Street, Mumbai, Maharashtra 400001
- Phone: +91-22-1234-5678
- Distance: 5.2 km
- Services: Parts Sales, Service, Warranty
- Hours: Mon-Sat: 9AM-7PM, Sun: 10AM-5PM

🏪 **Premium Hyundai Center**
- Address: 456 Commercial Road, Mumbai, Maharashtra 400002
- Phone: +91-22-2345-6789
- Distance: 8.7 km
- Services: Parts Sales, Service, Body Shop
- Hours: Mon-Sat: 8AM-8PM, Sun: Closed"
```

## 🔧 Configuration

### Application Properties

Key configuration options in `application.properties`:

```properties
# AI Model Configuration
spring.ai.openai.chat.options.model=gpt-4-1106-preview
spring.ai.openai.chat.options.temperature=0.7
spring.ai.openai.chat.options.max-tokens=2000

# Caching Configuration
spring.cache.redis.time-to-live=3600000
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Mobis API Configuration
mobis.api.base-url=https://api.hyundaimobis.com
mobis.api.timeout=30000

# Rate Limiting
mobis.rate-limit.requests-per-minute=100
```

### Function Calling

The chatbot uses Spring AI's function calling to dynamically invoke these APIs:

- `searchPartsFunction`: Search for parts by number, vehicle, or keyword
- `findDealersFunction`: Locate authorized dealers by location
- `findDistributorsFunction`: Find parts distributors in specific regions
- `checkWarrantyFunction`: Verify warranty status for parts/vehicles
- `getOffersFunction`: Retrieve current promotions and discounts
- `getVehicleInfoFunction`: Get detailed vehicle specifications

## 📊 Monitoring and Analytics

### Health Check

```bash
GET /api/v1/chat/health
```

### API Documentation

Swagger UI available at: `http://localhost:8080/swagger-ui.html`

### Database Console

H2 Console (development): `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:mem:mobis_chatbot`
- Username: `sa`
- Password: `password`

## 🚀 Deployment

### Docker Deployment

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/mobis-chatbot-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Production Configuration

1. **Database**: Switch to PostgreSQL for production
2. **Redis**: Configure Redis cluster for high availability
3. **Load Balancing**: Use multiple instances behind a load balancer
4. **SSL/TLS**: Enable HTTPS with proper certificates
5. **Monitoring**: Integrate with tools like Prometheus and Grafana

## 🧪 Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Test the chat API
curl -X POST http://localhost:8080/api/v1/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello, I need help finding parts"}'
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📈 Performance Optimization

- **Caching**: API responses cached in Redis with appropriate TTL
- **Connection Pooling**: Optimized database and Redis connections
- **Async Processing**: Non-blocking API calls where possible
- **CDN**: Static assets served via CDN in production
- **Compression**: Gzip compression enabled for all responses

## 🔒 Security Features

- **Input Validation**: All user inputs validated and sanitized
- **Rate Limiting**: Prevents API abuse
- **CORS Configuration**: Properly configured cross-origin requests
- **SQL Injection Prevention**: JPA queries protect against injection
- **XSS Protection**: HTML output properly escaped

## 📞 Support

For technical support or questions about Hyundai Mobis genuine parts:

- **Email**: support@hyundaimobis.com
- **Phone**: 1-800-MOBIS-HELP
- **Website**: https://hyundaimobisin.com

## 📄 License

This project is proprietary software of Hyundai Mobis. All rights reserved.

## 🔄 Version History

- **v1.0.0** - Initial release with core chat functionality
- **v1.1.0** - Added WebSocket support and enhanced UI
- **v1.2.0** - Implemented function calling and external API integration
- **v2.0.0** - Complete rewrite with Spring AI framework

---

**Built with ❤️ for Hyundai Mobis customers worldwide**

## 🔌 Available APIs and Example Questions

| API                          | Function Name                  | Example Question                      | Expected Response                                             |
| ---------------------------- | ------------------------------ | ------------------------------------- | ------------------------------------------------------------- |
| NHTSA Car Makes              | getCarMakesFunction            | List all car makes                    | List of car makes (e.g., Toyota, Ford, Honda, etc.)           |
| NHTSA Models for Make        | getModelsForMakeFunction       | What models does Toyota make?         | List of Toyota models (e.g., Camry, Corolla, Prius, etc.)     |
| NHTSA Vehicle Types for Make | getVehicleTypesForMakeFunction | What vehicle types does Ford produce? | List of Ford vehicle types (e.g., Passenger Car, Truck, etc.) |
| Cat Facts                    | getCatFactFunction             | Tell me a cat fact                    | A random cat fact                                             |

**If you ask a question outside these APIs, the chatbot will reply:**

> I can only answer questions about car makes, models, vehicle types, or cat facts.
