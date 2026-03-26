# SAP RFC Service — Overview

A Spring Boot REST service that proxies SAP RFC/BAPI calls via SAP JCo. Exposes a single `/rfc/execute` endpoint that accepts any RFC function module, executes it against a configured SAP destination, and returns structured JSON.

---

## Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| Java (JDK) | 17+ | Temurin/OpenJDK recommended |
| Maven | 3.6.3+ | Or use the included `mvnw`/`mvnw.cmd` wrapper |
| SAP JCo | 3.1.x | Platform-specific native library required |

---

## JCo Native Library Setup

JCo requires two files placed in the `lib/` directory:

### Windows (x64)
Download **sapjco31P_13-20009381 / sapjco3-ntamd64-3.1.13** from SAP Service Marketplace:
```
lib/
  sapjco3.jar
  sapjco3.dll
```

### Linux (x64)
Download **sapjco31P_13-20009381 / sapjco3-linuxx86_64-3.1.13**:
```
lib/
  sapjco3.jar
  libsapjco3.so
```

> The `sapjco3.jar` is the same for all platforms. Only the native library differs.
> On Linux, ensure the `.so` is readable: `chmod 644 lib/libsapjco3.so`

---

## Configuration

Copy `.env.example` to `.env` and fill in your SAP system details:

```bash
cp .env.example .env
```

```dotenv
# Server port
PORT=8090

# API Key — leave empty to disable auth
SAP_PROXY_API_KEY=

# RFC Destination named "local"
RFC_DEST_LOCAL_ASHOST=eduservice2201.fortiddns.com
RFC_DEST_LOCAL_SYSNR=50
RFC_DEST_LOCAL_CLIENT=800
RFC_DEST_LOCAL_USER=abapid22
RFC_DEST_LOCAL_PASSWD=sap1234!
RFC_DEST_LOCAL_LANG=EN
RFC_DEST_LOCAL_POOL_CAPACITY=5
RFC_DEST_LOCAL_PEAK_LIMIT=10
```

Multiple destinations can be added by repeating the pattern with a different name (e.g., `RFC_DEST_DEV_ASHOST=...`).

---

## Running

### Windows
```bat
run.bat
```

### Linux / macOS
```bash
chmod +x run.sh
./run.sh
```

The script:
1. Loads `.env` automatically
2. Sets `LD_LIBRARY_PATH` / `DYLD_LIBRARY_PATH` so JCo finds its native library
3. Uses `./mvnw` wrapper if present, otherwise falls back to system `mvn`
4. Starts the service on the configured port (default `8090`)

---

## API Endpoints

### Execute an RFC
```
POST /rfc/execute
Content-Type: application/json
```

**Request body:**
```json
{
  "destination": "local",
  "functionModule": "BAPI_VENDOR_GETDETAIL",
  "importing": {
    "VENDORNO": "0000005550",
    "COMPANYCODE": "3000"
  },
  "tables": {},
  "changing": {}
}
```

**Response:**
```json
{
  "success": true,
  "hasErrors": false,
  "errors": [],
  "exporting": {
    "GENERALDETAIL": {
      "VENDOR": "0000005550",
      "NAME": "IDES Consumer Products",
      "CITY": "DENVER",
      "COUNTRY": "US"
    }
  },
  "tables": { "BANKDETAIL": [...] },
  "durationMs": 95
}
```

### Get RFC Metadata
```
GET /rfc/metadata/{destination}/{functionModule}
```
Returns all parameters (name, type, length, optional flag) for a function module — useful for building requests.

```bash
curl http://localhost:8090/rfc/metadata/local/BAPI_VENDOR_GETDETAIL
```

### Health Check
```
GET /rfc/health
GET /actuator/health
```

---

## Authentication

If `SAP_PROXY_API_KEY` is set, all `/rfc/*` requests must include:
```
X-Api-Key: <your-key>
```

Leave it empty to disable auth (development only).

---

## Example Calls

### Ping SAP system
```bash
curl -X POST http://localhost:8090/rfc/execute \
  -H "Content-Type: application/json" \
  -d '{"destination":"local","functionModule":"RFC_PING","importing":{}}'
```

### Read system info
```bash
curl -X POST http://localhost:8090/rfc/execute \
  -H "Content-Type: application/json" \
  -d '{"destination":"local","functionModule":"RFC_SYSTEM_INFO","importing":{}}'
```

### List company codes (IMG)
```bash
curl -X POST http://localhost:8090/rfc/execute \
  -H "Content-Type: application/json" \
  -d '{"destination":"local","functionModule":"BAPI_COMPANYCODE_GETLIST","importing":{}}'
```

### Read vendor/supplier
```bash
curl -X POST http://localhost:8090/rfc/execute \
  -H "Content-Type: application/json" \
  -d '{
    "destination": "local",
    "functionModule": "BAPI_VENDOR_GETDETAIL",
    "importing": {
      "VENDORNO": "0000005550",
      "COMPANYCODE": "3000"
    }
  }'
```

### Read table (generic)
```bash
curl -X POST http://localhost:8090/rfc/execute \
  -H "Content-Type: application/json" \
  -d '{
    "destination": "local",
    "functionModule": "RFC_READ_TABLE",
    "importing": {
      "QUERY_TABLE": "T024E",
      "ROWCOUNT": 10,
      "DELIMITER": "|"
    },
    "tables": {
      "FIELDS": [
        {"FIELDNAME": "EKORG"},
        {"FIELDNAME": "EKOTX"}
      ]
    }
  }'
```

---

## Project Structure

```
sap-rfc-service/
├── lib/
│   ├── sapjco3.jar          # JCo Java library (add manually)
│   ├── sapjco3.dll          # Windows native library (add manually)
│   └── libsapjco3.so        # Linux native library (add manually)
├── src/main/java/com/dodge/rfc/
│   ├── RfcApplication.java
│   ├── config/
│   │   └── JCoDestinationProvider.java   # Reads RFC_DEST_* env vars
│   ├── controller/
│   │   └── RfcController.java            # REST endpoints
│   ├── model/
│   │   ├── RfcRequest.java
│   │   ├── RfcResponse.java
│   │   └── RfcMetadata.java
│   ├── security/
│   │   └── ApiKeyFilter.java
│   └── service/
│       └── RfcService.java               # JCo execution logic
├── src/main/resources/
│   └── application.yml
├── .env                     # Your local config (git-ignored)
├── .env.example             # Template
├── run.sh                   # Linux/macOS launcher
├── run.bat                  # Windows launcher
└── pom.xml
```

---

## Docker

### What you need first

The Linux JCo native library must be in `lib/` before building the image:

```
lib/
  sapjco3.jar        ← already there (same for all platforms)
  libsapjco3.so      ← ADD THIS: download sapjco3-linuxx86_64-3.1.x from SAP
  sapjco3.dll        ← Windows only, ignored by Docker (.dockerignore)
```

Download from SAP Service Marketplace: search **SAP JCo 3.x Linux x86_64**.

### Build & run

```bash
# One command: build image + start container
make up

# Or manually
make docker-build
docker compose up -d
```

```bash
make logs      # follow logs
make health    # check /rfc/health
make test      # RFC_PING
make down      # stop
make restart   # restart without rebuild
```

### Why not Alpine?
SAP JCo's native lib (`libsapjco3.so`) requires **glibc**. Alpine Linux uses musl libc and will crash at startup. The Dockerfile uses `eclipse-temurin:17-jre-jammy` (Ubuntu 22.04) which has glibc.

---

## Makefile targets

```
make build        Compile fat JAR (skips tests)
make run          Run locally via run.sh / run.bat
make clean        Delete target/

make docker-build Build Docker image
make up           docker compose up -d --build
make down         docker compose down
make logs         Follow container logs
make restart      Restart container (no rebuild)

make test         POST RFC_PING to running service
make health       GET /rfc/health
make meta RFC=X   GET /rfc/metadata/local/X
```

---

## Deploying on Linux

1. **Install JDK 17:**
   ```bash
   # Ubuntu/Debian
   sudo apt install openjdk-17-jdk

   # RHEL/Amazon Linux
   sudo yum install java-17-amazon-corretto
   ```

2. **Install Maven** (or rely on the included `mvnw` wrapper — no install needed):
   ```bash
   # Optional system install
   sudo apt install maven
   ```

3. **Place Linux JCo files in `lib/`:**
   ```
   lib/sapjco3.jar
   lib/libsapjco3.so
   ```

4. **Configure `.env`** with your SAP system details.

5. **Run:**
   ```bash
   ./run.sh
   ```

6. **(Optional) Run as a systemd service:**
   ```ini
   # /etc/systemd/system/rfc-service.service
   [Unit]
   Description=SAP RFC Service
   After=network.target

   [Service]
   User=rfcsvc
   WorkingDirectory=/opt/rfc-service
   EnvironmentFile=/opt/rfc-service/.env
   Environment=LD_LIBRARY_PATH=/opt/rfc-service/lib
   ExecStart=/usr/bin/java -jar /opt/rfc-service/rfc-service.jar \
     -Djava.library.path=/opt/rfc-service/lib
   Restart=on-failure

   [Install]
   WantedBy=multi-user.target
   ```
   ```bash
   sudo systemctl enable --now rfc-service
   ```
   > To build the fat JAR for systemd: `./mvnw package -DskipTests`
   > The JAR will be at `target/rfc-service.jar`
