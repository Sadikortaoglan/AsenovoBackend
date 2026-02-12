# Backend Restart Commands

## Stop Backend

### Option 1: Stop by process name
```bash
pkill -f "SaraAsansorApiApplication"
```

### Option 2: Stop by port (8080)
```bash
lsof -ti:8080 | xargs kill -9
```

### Option 3: Find and kill manually
```bash
# Find process
ps aux | grep SaraAsansorApiApplication

# Kill by PID (replace XXXX with actual PID)
kill -9 XXXX
```

## Start Backend

### Option 1: Maven (Development)
```bash
cd backend
mvn spring-boot:run
```

### Option 2: Maven with profile
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Option 3: Build JAR and run
```bash
cd backend
mvn clean package -DskipTests
java -jar target/sara-asansor-api-1.0.0.jar
```

### Option 4: Docker Compose
```bash
cd backend
docker-compose down
docker-compose up --build
```

## Quick Restart (Stop + Start)

```bash
cd backend

# Stop
pkill -f "SaraAsansorApiApplication" || lsof -ti:8080 | xargs kill -9

# Wait a moment
sleep 2

# Start
mvn spring-boot:run
```

## Check if Backend is Running

```bash
# Check process
ps aux | grep SaraAsansorApiApplication

# Check port
lsof -i:8080

# Test endpoint
curl http://localhost:8080/api/swagger-ui.html
```

