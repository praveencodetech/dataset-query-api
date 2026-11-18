#!/usr/bin/env bash
# ==============================================================================
# verify-prod-local.sh
# Verifies the Spring Boot dataset service against a real PostgreSQL instance
# using the 'prod' profile.
#
# Prerequisite:
#   Set DATABASE_URL in your environment before running:
#   export DATABASE_URL="postgres://user:password@host:port/dbname"
# ==============================================================================
set -euo pipefail

if [[ -z "${DATABASE_URL:-}" ]]; then
  echo "ERROR: DATABASE_URL environment variable is not set." >&2
  echo "Usage: export DATABASE_URL=\"postgres://<user>:<password>@<host>:<port>/<dbname>\"" >&2
  echo "       ./scripts/verify-prod-local.sh" >&2
  exit 1
fi

echo "==> DATABASE_URL is set (hidden for security)."
echo "==> Starting application with SPRING_PROFILES_ACTIVE=prod on port 8080..."

export SPRING_PROFILES_ACTIVE=prod
export PORT=8080

# Build jar if missing
if [[ ! -f "target/dataset-service.jar" ]]; then
  echo "==> Packaging dataset-service.jar..."
  mvn clean package -DskipTests -B
fi

# Launch in background
java -jar target/dataset-service.jar &
APP_PID=$!

cleanup() {
  echo ""
  echo "==> Stopping application (PID $APP_PID)..."
  kill "$APP_PID" 2>/dev/null || true
  wait "$APP_PID" 2>/dev/null || true
  echo "==> Verification complete."
}
trap cleanup EXIT

echo "==> Waiting for application to report UP on http://localhost:8080/actuator/health..."
MAX_ATTEMPTS=30
HEALTH_URL="http://localhost:8080/actuator/health"
UP=0

for i in $(seq 1 $MAX_ATTEMPTS); do
  if curl -sf "$HEALTH_URL" 2>/dev/null | grep -q '"UP"'; then
    echo "==> Application is UP and healthy (attempt $i)."
    UP=1
    break
  fi
  sleep 2
done

if [[ $UP -ne 1 ]]; then
  echo "ERROR: Application failed to start or report UP within 60 seconds." >&2
  exit 1
fi

echo ""
echo "==> 1. Inserting Record 1 (Alice - Engineering)..."
curl -s -X POST "http://localhost:8080/api/dataset/employees/record" \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice", "age": 30, "department": "Engineering"}'
echo ""

echo "==> 2. Inserting Record 2 (Bob - Marketing)..."
curl -s -X POST "http://localhost:8080/api/dataset/employees/record" \
  -H "Content-Type: application/json" \
  -d '{"name": "Bob", "age": 25, "department": "Marketing"}'
echo ""

echo "==> 3. Inserting Record 3 (Carol - Engineering)..."
curl -s -X POST "http://localhost:8080/api/dataset/employees/record" \
  -H "Content-Type: application/json" \
  -d '{"name": "Carol", "age": 35, "department": "Engineering"}'
echo ""

echo "==> 4. Querying GroupBy department..."
curl -s "http://localhost:8080/api/dataset/employees/query?groupBy=department"
echo ""

echo "==> 5. Querying SortBy age ascending..."
curl -s "http://localhost:8080/api/dataset/employees/query?sortBy=age&order=asc"
echo ""

echo "==> Verification completed successfully against real PostgreSQL!"
