# Makefile
.PHONY: help install dev build test deploy clean

help:
	@echo "LuckXpress Admin Dashboard Commands:"
	@echo "  make install    - Install all dependencies"
	@echo "  make dev        - Start development servers (backend + frontend)"
	@echo "  make backend    - Start backend only"
	@echo "  make frontend   - Start frontend only"
	@echo "  make build      - Build for production"
	@echo "  make test       - Run all tests"
	@echo "  make clean      - Clean build artifacts"

install:
	@echo "Installing backend dependencies..."
	cd backend && mvn clean install -DskipTests
	@echo "Installing frontend dependencies..."
	cd frontend && npm install

backend:
	@echo "Starting Java Spring Boot backend..."
	cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev

frontend:
	@echo "Starting React admin dashboard..."
	cd frontend && npm start

dev:
	@echo "Starting development servers..."
	make backend &
	make frontend

build:
	@echo "Building backend..."
	cd backend && mvn clean package -DskipTests
	@echo "Building frontend..."
	cd frontend && npm run build

test:
	@echo "Running backend tests..."
	cd backend && mvn test
	@echo "Running frontend tests..."
	cd frontend && npm test

clean:
	@echo "Cleaning backend..."
	cd backend && mvn clean
	@echo "Cleaning frontend..."
	cd frontend && rm -rf build node_modules

docker-build:
	docker-compose -f docker/docker-compose.yml build

docker-up:
	docker-compose -f docker/docker-compose.yml up -d

docker-down:
	docker-compose -f docker/docker-compose.yml down
