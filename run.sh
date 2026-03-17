#!/bin/bash
# Run the local RFC service with JCo native library

cd "$(dirname "$0")"

# Set the native library path for JCo
export LD_LIBRARY_PATH="$(pwd)/lib:$LD_LIBRARY_PATH"

# Default destination configuration (Michael Management training system)
export RFC_DEST_LOCAL_ASHOST="${RFC_DEST_LOCAL_ASHOST:-192.168.1.115}"
export RFC_DEST_LOCAL_SYSNR="${RFC_DEST_LOCAL_SYSNR:-50}"
export RFC_DEST_LOCAL_CLIENT="${RFC_DEST_LOCAL_CLIENT:-800}"
export RFC_DEST_LOCAL_USER="${RFC_DEST_LOCAL_USER:-ABAPID22}"
export RFC_DEST_LOCAL_PASSWD="${RFC_DEST_LOCAL_PASSWD:-sap1234!}"
export RFC_DEST_LOCAL_LANG="${RFC_DEST_LOCAL_LANG:-EN}"
export RFC_DEST_LOCAL_POOL_CAPACITY="${RFC_DEST_LOCAL_POOL_CAPACITY:-5}"
export RFC_DEST_LOCAL_PEAK_LIMIT="${RFC_DEST_LOCAL_PEAK_LIMIT:-10}"

# Optional: API key for security (empty = no auth required)
export SAP_PROXY_API_KEY="${SAP_PROXY_API_KEY:-}"

# Port
export PORT="${PORT:-8090}"

echo "Starting RFC service on port $PORT..."
echo "Destination 'local' configured for: $RFC_DEST_LOCAL_ASHOST system $RFC_DEST_LOCAL_SYSNR client $RFC_DEST_LOCAL_CLIENT"
echo ""
echo "Test with:"
echo "  curl -X POST http://localhost:$PORT/rfc/execute \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"destination\":\"local\",\"functionModule\":\"RFC_PING\",\"importing\":{}}'"
echo ""

# Run with maven (keeps original JCo JAR name on classpath)
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Djava.library.path=$(pwd)/lib"
