# Test Conversation Flow for Hyundai Mobis Chatbot

## Improved User Experience

The chatbot now provides a much better conversation flow with bubble-based responses and proper pricing information. Here's how the new flow works:

### 1. Main Menu

**User:** "Hello" or "Start"
**Bot:** Shows main menu with bubble options:

- Browse Accessories by Vehicle
- View Accessory Categories
- Find Dealers & Distributors
- Check Current Offers
- Get Product Support

### 2. Accessory Categories Flow

**User:** "View Accessory Categories" or "interior"
**Bot:** Shows accessory categories with bubble options:

- Interior
- Exterior
- Performance
- Safety
- Comfort
- Convenience
- Navigation
- Sound and Vision
- Ride and Handling

### 3. Specific Category Selection

**User:** "interior" or "Interior"
**Bot:** Shows interior accessories with bubble options:

- Seat Covers
- Floor Mats
- Dashboard Trims
- Steering Wheel Covers
- Center Console Organizers
- Door Trim Kits
- Glove Compartments
- Shift Knobs

### 4. Specific Accessory Selection

**User:** "seat covers" or "Seat Covers"
**Bot:** Shows vehicle selection with bubble options:

- New i20
- New Creta
- Alcazar
- Verna
- Venue
- Exter
- Grand i10 NIOS
- Aura

### 5. Vehicle Selection & Pricing

**User:** "New i20"
**Bot:** Shows pricing information:

```
💰 **Seat Covers for New i20**

Here are the available seat covers for your New i20:

🪑 **Premium Seat Covers**
   💰 Price: ₹2500
   📝 High-quality fabric seat covers with perfect fit
   🔧 Part Number: SC-NI20-PREM
   📦 In Stock: ✅ Yes

🪑 **Standard Seat Covers**
   💰 Price: ₹1750
   📝 Comfortable seat covers with good protection
   🔧 Part Number: SC-NI20-STD
   📦 In Stock: ✅ Yes

🪑 **Deluxe Seat Covers**
   💰 Price: ₹3250
   📝 Premium leather-look seat covers with extra comfort
   🔧 Part Number: SC-NI20-DLX
   📦 In Stock: ❌ No

What would you like to do next?
- Find Dealers & Distributors
- Check Current Offers
- Browse Another Accessory
- Start over
```

### 6. Direct Accessory Queries

**User:** "seat covers"
**Bot:** Directly asks for vehicle selection and shows pricing

**User:** "floor mats"
**Bot:** Directly asks for vehicle selection and shows pricing

### 7. Dealer & Offers Integration

After showing pricing, users can:

- Find dealers to purchase the accessories
- Check current offers and discounts
- Browse other accessories
- Start over

## Key Improvements

1. **Bubble-Based Responses**: All options are presented as clickable bubbles
2. **Natural Language Understanding**: Users can type "interior", "seat covers", etc.
3. **Pricing Information**: Clear pricing with part numbers and stock status
4. **Stateful Conversations**: Remembers context and provides relevant follow-up options
5. **Multiple Entry Points**: Users can start from any point in the flow
6. **Error Handling**: Graceful handling of invalid selections
7. **Rich Information**: Includes descriptions, part numbers, and availability

## Testing Commands

```bash
# Test the main flow
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"test123","message":"hello"}'

# Test direct accessory query
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"test123","message":"seat covers"}'

# Test category selection
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"test123","message":"interior"}'
```

The chatbot now provides a much more intuitive and user-friendly experience with proper pricing information and bubble-based navigation!
