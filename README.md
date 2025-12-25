<div align="center">

# Parking Service

**Intelligent Parking Management System for Tétouan, Morocco**

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen?style=flat-square&logo=springboot)
![React](https://img.shields.io/badge/React-18.3.1-blue?style=flat-square&logo=react)
![TypeScript](https://img.shields.io/badge/TypeScript-5.8.3-blue?style=flat-square&logo=typescript)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Kafka-7.8.3-231F20?style=flat-square&logo=apachekafka)
![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?style=flat-square&logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

[Features](#features) • [Architecture](#architecture) • [Getting Started](#getting-started) • [API Documentation](#api-documentation) • [Contributing](#contributing)

</div>

---

## Overview

Parking Service is a full-stack web application that provides intelligent parking management for the city of Tétouan, Morocco. The system integrates real OpenStreetMap parking data with a custom zone management platform, enabling users to discover available parking spots, make reservations, track active sessions, and manage parking payments.

The application combines modern frontend technologies with a robust Spring Boot backend, featuring real-time updates, asynchronous message processing via Apache Kafka, and OAuth2 authentication through Clerk.

---

## Features

### Core Functionality

- **Interactive Parking Discovery**
  - Real-time parking data from OpenStreetMap via Overpass API
  - Interactive Leaflet-based maps with polygon and point visualization
  - Switchable map layers (Normal CartoDB and Satellite Esri views)
  - Location-based search within Tétouan boundaries

- **Parking Zone Management**
  - View all parking zones with detailed information
  - Dynamic hourly rate pricing (10-15 DHS/hour)
  - Real-time capacity tracking showing total and available spots
  - Geographic coordinates for precise location mapping

- **Session Management**
  - Seamless check-in flow for parking reservation
  - Active session monitoring with live duration tracking
  - Manual check-out with automatic cost calculation
  - Comprehensive session history with timestamps and costs
  - Multiple session states: ACTIVE, COMPLETED, PENDING_PAYMENT

- **User Authentication**
  - OAuth-based authentication via Clerk
  - Webhook integration for user lifecycle management
  - Role-based access control (Driver, Administrator)
  - Secure profile management with email, name, and phone storage

- **Complaint System**
  - Submit parking-related reclamations
  - Asynchronous processing via Apache Kafka
  - Priority-based complaint handling
  - Location tracking and attachment support

- **IoT-Ready Architecture**
  - Sensor-based spot reservation system
  - Entry detection framework for automated check-in
  - Spot locking mechanism to prevent double-booking
  - Each spot mapped to unique sensor identifier

---

## Architecture

### Tech Stack

#### Frontend
- **Framework**: React 18.3.1 with TypeScript 5.8.3
- **Build Tool**: Vite 5.4.19
- **Styling**: Tailwind CSS 3.4.17
- **UI Components**: shadcn-ui (Radix UI primitives)
- **Maps**: Leaflet 1.9.4
- **State Management**: TanStack Query 5.83.0
- **Form Handling**: React Hook Form 7.61.1 with Zod 3.25.76
- **Charts**: Recharts 2.15.4
- **Notifications**: Sonner 1.7.4
- **Icons**: Lucide React 0.462.0

#### Backend
- **Framework**: Spring Boot 3.3.0
- **Language**: Java 21
- **Build Tool**: Apache Maven
- **ORM**: Spring Data JPA with Hibernate
- **Database**: MySQL 8.0
- **Message Queue**: Apache Kafka 7.8.3
- **Authentication**: Clerk OAuth2
- **Libraries**: Lombok, Jackson

#### Infrastructure
- **Containerization**: Docker & Docker Compose
- **Message Broker**: Apache Kafka (KRaft mode)
- **Database**: MySQL 8.0

### System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Frontend Layer                        │
│  (React + TypeScript + Vite + Tailwind + Leaflet)          │
└────────────────────┬────────────────────────────────────────┘
                     │ REST API (JSON)
                     │
┌────────────────────▼────────────────────────────────────────┐
│                      Backend Layer                           │
│              (Spring Boot + JPA + Security)                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Controllers  │  │   Services   │  │  Kafka       │     │
│  │              │──│              │──│  Producers/  │     │
│  │ (REST API)   │  │ (Business    │  │  Consumers   │     │
│  └──────────────┘  │  Logic)      │  └──────────────┘     │
│                     │              │                         │
│                     │  ┌───────────┴──────────┐            │
│                     │  │    Repositories      │            │
│                     │  │   (Data Access)      │            │
│                     │  └──────────┬───────────┘            │
└────────────────────────────────────┼──────────────────────┘
                                     │ JPA/Hibernate
                     ┌───────────────┼──────────────────┐
                     │               │                  │
              ┌──────▼──────┐  ┌────▼─────┐   ┌───────▼──────┐
              │    MySQL    │  │  Kafka   │   │    Clerk     │
              │   Database  │  │  Broker  │   │ Auth Service │
              └─────────────┘  └──────────┘   └──────────────┘
```

### Database Schema

**Core Entities:**

- `utilisateur` - User management with Clerk integration
- `parking_zones` - Parking zone definitions with pricing
- `parking_spots` - Individual parking spots with sensor mapping
- `parking_sessions` - Active and historical parking sessions
- `reclamation` - User complaints and feedback

---

## Getting Started

### Prerequisites

- **Java Development Kit**: JDK 21 or higher
- **Node.js**: Latest LTS version
- **Maven**: 3.6+ (or use Maven wrapper)
- **Docker**: Latest version with Docker Compose
- **Git**: For version control

### Installation

#### 1. Clone the Repository

```bash
git clone <repository-url>
cd parking-service
```

#### 2. Start Infrastructure Services

Start MySQL and Kafka using Docker Compose:

```bash
docker-compose up -d
```

This will initialize:
- MySQL database on port 3306
- Apache Kafka broker on port 9092
- Automatic database schema creation from [data.sql](data.sql)

#### 3. Configure Environment

**Backend Configuration** ([backend/src/main/resources/application.properties](backend/src/main/resources/application.properties)):

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/parking_db
spring.datasource.username=parking_user
spring.datasource.password=password

# Clerk Authentication
clerk.secretKey=your_clerk_secret_key
clerk.baseUrl=https://api.clerk.com/v1

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092
```

#### 4. Run Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

Backend will be available at `http://localhost:8080`

#### 5. Run Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend will be available at `http://localhost:8080` (Vite dev server)

### Development Commands

**Backend:**
```bash
mvn clean install          # Build project
mvn spring-boot:run        # Run application
mvn test                   # Run tests
```

**Frontend:**
```bash
npm install               # Install dependencies
npm run dev              # Start dev server
npm run build            # Production build
npm run preview          # Preview production build
npm run lint             # Lint code
```

**Docker:**
```bash
docker-compose up -d      # Start services
docker-compose down       # Stop services
docker-compose logs -f    # View logs
```

---

## API Documentation

### Base URL

```
http://localhost:8080/api
```

### Endpoints

#### Parking Spots

**Get Active Session**
```http
GET /spots/my-active-session?userId={clerkUserId}
```

**Check In**
```http
POST /spots/{spotId}/check-in
Content-Type: application/json

{
  "userId": "clerk_user_id"
}
```

**Check Out**
```http
POST /spots/check-out?userId={clerkUserId}
```

**Get Session History**
```http
GET /spots/my-history?userId={clerkUserId}
```

#### Parking Zones

**Get All Zones**
```http
GET /zones
```

**Get Zone Spots**
```http
GET /zones/{zoneId}/spots
```

#### Webhooks

**Clerk User Webhook**
```http
POST /webhooks/clerk-user
Content-Type: application/json

{
  "type": "user.created",
  "data": { ... }
}
```

### Response Format

All API responses are in JSON format:

**Success Response:**
```json
{
  "id": 1,
  "spotNumber": "P-101",
  "status": true,
  "sensorId": "SENSOR-01",
  "zone": {
    "id": 1,
    "name": "Centre Ville",
    "hourlyRate": 10.0,
    "capacity": 100
  }
}
```

**Error Response:**
```json
{
  "timestamp": "2025-12-24T10:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Spot is already occupied"
}
```

---

## Project Structure

```
parking-service/
├── frontend/                      # React TypeScript application
│   ├── src/
│   │   ├── components/           # Reusable UI components
│   │   │   ├── parking/         # Parking-specific components
│   │   │   │   ├── ActiveSessionBanner.tsx
│   │   │   │   ├── LeafletMap.tsx
│   │   │   │   ├── MapOverlay.tsx
│   │   │   │   ├── NavigationTabs.tsx
│   │   │   │   ├── ParkingModal.tsx
│   │   │   │   ├── PaymentSummaryModal.tsx
│   │   │   │   ├── SessionHistoryList.tsx
│   │   │   │   └── StopSessionModal.tsx
│   │   │   └── ui/              # shadcn-ui components
│   │   ├── hooks/               # Custom React hooks
│   │   ├── pages/               # Page components
│   │   ├── services/            # API client services
│   │   ├── types/               # TypeScript definitions
│   │   ├── utils/               # Utility functions
│   │   ├── App.tsx              # Root component
│   │   └── main.tsx             # Entry point
│   ├── package.json
│   ├── tailwind.config.ts
│   ├── tsconfig.json
│   └── vite.config.ts
│
├── backend/                       # Spring Boot application
│   ├── src/main/java/org/example/backend/
│   │   ├── config/               # Spring configuration
│   │   │   ├── ClerkConfig.java
│   │   │   ├── KafkaConfig.java
│   │   │   └── SecurityConfig.java
│   │   ├── DTO/                  # Data transfer objects
│   │   │   ├── CheckInRequestDTO.java
│   │   │   ├── ParkingSpotDTO.java
│   │   │   └── ParkingZoneDTO.java
│   │   ├── entities/             # JPA entities
│   │   │   ├── ParkingSession.java
│   │   │   ├── ParkingSpot.java
│   │   │   ├── ParkingZone.java
│   │   │   ├── Reclamation.java
│   │   │   └── Utilisateur.java
│   │   ├── enums/                # Enum types
│   │   │   ├── Role.java
│   │   │   └── SessionStatus.java
│   │   ├── kafka/                # Kafka components
│   │   │   ├── ReclamationConsumer.java
│   │   │   └── ResponseProducer.java
│   │   ├── mappers/              # DTO mappers
│   │   ├── repository/           # JPA repositories
│   │   ├── security/             # Security components
│   │   ├── service/              # Business logic
│   │   └── web/                  # REST controllers
│   │       ├── ClerkWebhookController.java
│   │       ├── ParkingSpotController.java
│   │       └── ParkingZoneController.java
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
│
├── data.sql                       # Database initialization
├── docker-compose.yml             # Docker services
├── .gitignore
└── README.md
```

---

## Key Features Implementation

### Session Management Flow

1. **User selects parking spot** → Frontend displays zone details
2. **Check-in request** → Backend validates availability
3. **Spot reservation** → Database locks spot, creates session
4. **Active session** → Frontend displays timer and session details
5. **Check-out request** → Backend calculates parking duration
6. **Cost calculation** → Based on hourly rate and time parked
7. **Session completion** → Status updated to COMPLETED
8. **History tracking** → Session stored for user reference

### Kafka Message Processing

**Reclamation Submission:**
```
User Form → ReclamationConsumer → Process & Store → ResponseProducer
```

**Topics:**
- `reclamations` - Incoming user complaints
- `reclamation-responses` - Admin response channel

### OpenStreetMap Integration

The application fetches real parking data from OpenStreetMap using the Overpass API:

```typescript
const query = `
  [out:json];
  (
    way["amenity"="parking"](${south},${west},${north},${east});
    node["amenity"="parking"](${south},${west},${north},${east});
  );
  out geom;
`;
```

Bounding box for Tétouan: `35.52°N to 35.62°N, -5.42°W to -5.28°W`

---

## Configuration

### Database Configuration

MySQL is configured with the following default settings:

```yaml
Database: parking_db
User: parking_user
Password: password
Port: 3306
```

Initial data includes:
- 2 parking zones (Centre Ville, Plage Martil)
- 3 parking spots (P-101, P-102, P-201)
- All spots initially available

### Kafka Configuration

Kafka runs in KRaft mode (no Zookeeper required):

```yaml
Bootstrap Servers: localhost:9092
Consumer Group: parking-service-group
Auto Offset Reset: earliest
```

### Authentication

Clerk authentication requires:

1. Create a Clerk account at [clerk.com](https://clerk.com)
2. Obtain your secret key
3. Configure webhook endpoint for user events
4. Update `clerk.secretKey` in [application.properties](backend/src/main/resources/application.properties)

---

## Deployment

### Production Considerations

**Backend Deployment:**
- Package application: `mvn clean package`
- Deploy JAR file: `java -jar target/backend-0.0.1-SNAPSHOT.jar`
- Configure environment variables for sensitive data
- Use managed MySQL instance
- Set up Kafka cluster for high availability

**Frontend Deployment:**
- Build production bundle: `npm run build`
- Deploy to static hosting (Vercel, Netlify, GitHub Pages)
- Configure API base URL for backend endpoint
- Set up CDN for optimal performance

**Docker Deployment:**
- Add Dockerfile for backend containerization
- Use Docker Compose for orchestration
- Configure persistent volumes for database
- Set up reverse proxy (Nginx, Traefik)

---

## Development Roadmap

### Completed
- Core parking zone and spot management
- Check-in and check-out workflow
- Session tracking and history
- Clerk OAuth integration with webhooks
- Kafka producer/consumer setup
- Interactive Leaflet maps with OSM data
- Real-time cost calculation

### In Progress
- OAuth2 JWT token validation
- Admin reclamation response system
- Payment gateway integration
- Real-time spot availability updates

### Planned
- Mobile application (React Native)
- IoT sensor integration for automated entry detection
- Advanced analytics and reporting dashboard
- Push notifications for session updates
- Multi-language support (Arabic, French, English)
- Payment history and invoice generation
- Admin dashboard for zone management

---

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style

- **Backend**: Follow Java code conventions, use Lombok annotations
- **Frontend**: Follow ESLint rules, use TypeScript strict mode
- **Commits**: Use conventional commit messages

---

## License

This project is licensed under the MIT License. See the LICENSE file for details.

---

## Acknowledgments

- OpenStreetMap contributors for parking data
- Clerk for authentication infrastructure
- Spring Boot and React communities
- All open-source libraries used in this project

---

## Contact

For questions, issues, or suggestions, please open an issue on the GitHub repository.

---

<div align="center">

**Built with Java, Spring Boot, React, and TypeScript**

</div>
