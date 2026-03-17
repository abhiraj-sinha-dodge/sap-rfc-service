# SAP RFC Service

A standalone REST API service for executing SAP RFC calls using JCo (Java Connector).

## Features

- Execute any RFC function module via REST API
- Get function module metadata
- Support multiple SAP destinations
- API key authentication (optional)
- Cross-platform (Linux, Windows, macOS)

## Prerequisites

1. **Java 17+** - [Download](https://adoptium.net/)
2. **Maven 3.6+** - [Download](https://maven.apache.org/download.cgi)
3. **SAP JCo 3.1+** - Download from [SAP Support Portal](https://support.sap.com/en/product/connectors/jco.html)

## Setup

### 1. Download SAP JCo

Download the appropriate JCo package for your platform from SAP:
- Windows x64: `sapjco3-ntamd64-3.1.x.zip`
- Linux x64: `sapjco3-linuxx86_64-3.1.x.zip`
- macOS: `sapjco3-darwinintel64-3.1.x.zip`

### 2. Install JCo Files

Extract and copy to the `lib/` folder:

```
lib/
├── sapjco3.jar          # Java archive (all platforms)
├── sapjco3.dll          # Windows native library
├── libsapjco3.so        # Linux native library
└── libsapjco3.dylib     # macOS native library
```

### 3. Configure Destination

Copy `.env.example` to `.env` and edit:

```bash
# Windows
copy .env.example .env
notepad .env

# Linux/macOS
cp .env.example .env
nano .env
```

Configure your SAP connection:

```env
RFC_DEST_LOCAL_ASHOST=your-sap-host
RFC_DEST_LOCAL_SYSNR=00
RFC_DEST_LOCAL_CLIENT=100
RFC_DEST_LOCAL_USER=your-user
RFC_DEST_LOCAL_PASSWD=your-password
RFC_DEST_LOCAL_LANG=EN
```

### 4. Run

**Windows:**
```cmd
run.bat
```

**Linux/macOS:**
```bash
./run.sh
```

The service starts on `http://localhost:8090`

## API Endpoints

### Health Check
```
GET /rfc/health
```

### Execute RFC
```
POST /rfc/execute
Content-Type: application/json

{
  "destination": "local",
  "functionModule": "RFC_READ_TABLE",
  "importing": {
    "QUERY_TABLE": "T001",
    "ROWCOUNT": "10"
  },
  "tables": {
    "FIELDS": [
      {"FIELDNAME": "BUKRS"},
      {"FIELDNAME": "BUTXT"}
    ]
  }
}
```

### Get Metadata
```
GET /rfc/metadata/{destination}/{functionModule}
```

## Examples

### RFC_PING
```bash
curl -X POST http://localhost:8090/rfc/execute \
  -H "Content-Type: application/json" \
  -d '{"destination":"local","functionModule":"RFC_PING","importing":{}}'
```

### RFC_SYSTEM_INFO
```bash
curl -X POST http://localhost:8090/rfc/execute \
  -H "Content-Type: application/json" \
  -d '{"destination":"local","functionModule":"RFC_SYSTEM_INFO","importing":{}}'
```

### Read Table
```bash
curl -X POST http://localhost:8090/rfc/execute \
  -H "Content-Type: application/json" \
  -d '{
    "destination": "local",
    "functionModule": "RFC_READ_TABLE",
    "importing": {
      "QUERY_TABLE": "T001",
      "ROWCOUNT": "5"
    },
    "tables": {
      "FIELDS": [
        {"FIELDNAME": "BUKRS"},
        {"FIELDNAME": "BUTXT"},
        {"FIELDNAME": "WAERS"}
      ]
    }
  }'
```

### BAPI Call
```bash
curl -X POST http://localhost:8090/rfc/execute \
  -H "Content-Type: application/json" \
  -d '{
    "destination": "local",
    "functionModule": "BAPI_COMPANYCODE_GETLIST",
    "importing": {},
    "tables": {}
  }'
```

## Multiple Destinations

Add more destinations via environment variables:

```env
# Development system
RFC_DEST_DEV_ASHOST=dev-sap.example.com
RFC_DEST_DEV_SYSNR=00
RFC_DEST_DEV_CLIENT=100
RFC_DEST_DEV_USER=DEV_USER
RFC_DEST_DEV_PASSWD=secret

# Production system (read-only)
RFC_DEST_PROD_ASHOST=prod-sap.example.com
RFC_DEST_PROD_SYSNR=00
RFC_DEST_PROD_CLIENT=100
RFC_DEST_PROD_USER=RFC_USER
RFC_DEST_PROD_PASSWD=secret
```

Then use `"destination": "dev"` or `"destination": "prod"` in API calls.

## Security

Enable API key authentication:

```env
SAP_PROXY_API_KEY=your-secret-key
```

Then include header in requests:
```bash
curl -H "x-api-key: your-secret-key" ...
```

## Troubleshooting

### JCo native library not found
Ensure the native library is in `lib/` and matches your platform:
- Windows: `sapjco3.dll`
- Linux: `libsapjco3.so`
- macOS: `libsapjco3.dylib`

### Connection refused
- Verify SAP host is reachable: `ping your-sap-host`
- Check system number (port = 33XX where XX is SYSNR)
- Verify firewall allows connection

### Authentication failed
- Verify username/password
- Check user is not locked in SAP
- Verify client number

## License

MIT
