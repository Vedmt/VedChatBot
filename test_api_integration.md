# Hyundai Mobis API Integration Test

## Flow Overview
1. User starts conversation
2. User selects "Browse Accessories"
3. User selects vehicle model (e.g., "i20")
4. System fetches accessories from Hyundai Mobis API
5. System displays accessories with MRP prices

## Test Steps

### Step 1: Start Application
```bash
mvn spring-boot:run
```

### Step 2: Test API Directly
```bash
curl "https://api.hyundaimobisin.com/service/accessories/getByModelIdYear?modelId=29&year=2018"
```

### Step 3: Test Chat Flow
1. Open browser to http://localhost:8080
2. Click "Browse Accessories"
3. Select "i20" as vehicle model
4. Verify accessories are displayed with MRP prices

## Expected Results

### API Response Structure
```json
[
  {
    "id": 1162,
    "accessoryName": "FRONT BUMPER BEZEL",
    "accessoryCode": "BVF27IH003",
    "body": "Enhance the appearance of your car with these chrome front bezel garnish.",
    "typeId": 1347,
    "type": "Exteriors",
    "subTypeId": 1436,
    "subType": "Garnish",
    "mrp": 989.0,
    "url": "front-bumper-bezel-1162",
    "urlText": "front-bumper-bezel",
    "title": "FRONT BUMPER BEZEL",
    "image": "3372-FRONT-BUMPER-BEZEL-BVF27IH003-.jpg"
  }
]
```

### Chat Response Format
- Accessories grouped by type (Exteriors, Interiors, etc.)
- Each accessory shows:
  - Name
  - MRP price (₹989)
  - Description
  - Part code
  - Sub-category

## Model ID Mapping
- i20: 29
- Creta: 30
- Alcazar: 31
- Verna: 32
- Venue: 33
- Exter: 34
- Grand i10 NIOS: 35
- Aura: 36

## Key Features Implemented
1. ✅ Real API integration with Hyundai Mobis
2. ✅ Proper error handling with fallback to mock data
3. ✅ MRP price display (₹ format)
4. ✅ Accessories grouped by type
5. ✅ Simplified conversation flow
6. ✅ Vehicle model selection
7. ✅ Bubble chat interface updates 