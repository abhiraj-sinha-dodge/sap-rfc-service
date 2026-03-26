.PHONY: build run docker-build up down logs restart test clean help

# ── Config ────────────────────────────────────────────────────────────────────
PORT     ?= 8090
IMAGE    ?= dodge/rfc-service:latest

# Detect OS for local run
ifeq ($(OS),Windows_NT)
  RUN_CMD = run.bat
else
  RUN_CMD = ./run.sh
endif

# ── Local development ─────────────────────────────────────────────────────────

## build: Compile and package the fat JAR (skips tests)
build:
	./mvnw package -DskipTests -q
	@echo "Built: target/rfc-service.jar"

## run: Run locally using run.sh / run.bat (loads .env automatically)
run:
	$(RUN_CMD)

## clean: Remove build output
clean:
	./mvnw clean -q

# ── Docker ────────────────────────────────────────────────────────────────────

## docker-build: Build the Docker image
docker-build:
	docker build -t $(IMAGE) .

## up: Start service with Docker Compose (detached)
up:
	docker compose up -d --build
	@echo "Service started on port $(PORT). Run 'make logs' to follow output."

## down: Stop and remove containers
down:
	docker compose down

## logs: Follow container logs
logs:
	docker compose logs -f rfc-service

## restart: Restart the container (no rebuild)
restart:
	docker compose restart rfc-service

# ── Testing ───────────────────────────────────────────────────────────────────

## test: Ping the running service
test:
	@curl -sf -X POST http://localhost:$(PORT)/rfc/execute \
	  -H "Content-Type: application/json" \
	  -d '{"destination":"local","functionModule":"RFC_PING","importing":{}}' \
	  | python3 -m json.tool 2>/dev/null || \
	  curl -sf -X POST http://localhost:$(PORT)/rfc/execute \
	  -H "Content-Type: application/json" \
	  -d '{"destination":"local","functionModule":"RFC_PING","importing":{}}'

## health: Check service health endpoint
health:
	@curl -sf http://localhost:$(PORT)/rfc/health | python3 -m json.tool 2>/dev/null || \
	 curl -sf http://localhost:$(PORT)/rfc/health

## meta: Show metadata for an RFC (usage: make meta RFC=BAPI_VENDOR_GETDETAIL)
RFC ?= RFC_PING
meta:
	@curl -sf http://localhost:$(PORT)/rfc/metadata/local/$(RFC) | python3 -m json.tool 2>/dev/null || \
	 curl -sf http://localhost:$(PORT)/rfc/metadata/local/$(RFC)

# ── Help ──────────────────────────────────────────────────────────────────────

## help: Show this help
help:
	@grep -E '^## ' Makefile | sed 's/## /  /'
