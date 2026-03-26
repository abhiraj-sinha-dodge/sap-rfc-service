#!/bin/bash
# Run the SAP RFC service with JCo native library
# Works on Linux (sapjco3-linuxx86_64) and macOS (sapjco3-darwinia64)

set -e
cd "$(dirname "$0")"

# Load .env file if it exists
if [ -f .env ]; then
    set -a
    source .env
    set +a
fi

# Apply defaults for any vars not set by .env
export RFC_DEST_LOCAL_ASHOST="${RFC_DEST_LOCAL_ASHOST:-localhost}"
export RFC_DEST_LOCAL_SYSNR="${RFC_DEST_LOCAL_SYSNR:-00}"
export RFC_DEST_LOCAL_CLIENT="${RFC_DEST_LOCAL_CLIENT:-100}"
export RFC_DEST_LOCAL_USER="${RFC_DEST_LOCAL_USER:-DEVELOPER}"
export RFC_DEST_LOCAL_PASSWD="${RFC_DEST_LOCAL_PASSWD:-password}"
export RFC_DEST_LOCAL_LANG="${RFC_DEST_LOCAL_LANG:-EN}"
export RFC_DEST_LOCAL_POOL_CAPACITY="${RFC_DEST_LOCAL_POOL_CAPACITY:-5}"
export RFC_DEST_LOCAL_PEAK_LIMIT="${RFC_DEST_LOCAL_PEAK_LIMIT:-10}"
export SAP_PROXY_API_KEY="${SAP_PROXY_API_KEY:-}"
export PORT="${PORT:-8090}"

# Set the native library path for JCo
export LD_LIBRARY_PATH="$(pwd)/lib:${LD_LIBRARY_PATH:-}"
export DYLD_LIBRARY_PATH="$(pwd)/lib:${DYLD_LIBRARY_PATH:-}"   # macOS

echo "Starting RFC service on port $PORT..."
echo "Destination 'local': $RFC_DEST_LOCAL_ASHOST  sysno=$RFC_DEST_LOCAL_SYSNR  client=$RFC_DEST_LOCAL_CLIENT"
echo ""
echo "Test with:"
echo "  curl -X POST http://localhost:$PORT/rfc/execute \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"destination\":\"local\",\"functionModule\":\"RFC_PING\",\"importing\":{}}'"
echo ""

# Prefer Maven wrapper if available, fall back to system mvn
MVN="mvn"
if [ -f "./mvnw" ]; then
    chmod +x ./mvnw
    MVN="./mvnw"
fi

$MVN spring-boot:run \
    -Dspring-boot.run.jvmArguments="-Djava.library.path=$(pwd)/lib -Dloader.path=$(pwd)/lib"
