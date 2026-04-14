#!/bin/bash

echo "=============================="
echo " DEPLOY SPRING BOOT UMG "
echo "=============================="

# Ir a la carpeta del proyecto
cd "$(dirname "$0")"

echo ">>> Actualizando repositorio..."
git pull

echo ">>> Compilando proyecto..."
./mvnw clean package -DskipTests

# Buscar proceso anterior
echo ">>> Buscando proceso anterior..."
PID=$(pgrep -f "java -jar")

if [ ! -z "$PID" ]; then
    echo ">>> Matando proceso anterior: $PID"
    kill -9 $PID
else
    echo ">>> No había proceso corriendo"
fi

# Buscar el JAR
JAR_FILE=$(find target -name "*.jar" | head -n 1)

echo ">>> Ejecutando: $JAR_FILE"

# Levantar app
nohup java -jar "$JAR_FILE" > app.log 2>&1 &

sleep 5

echo ">>> App levantada. Logs:"
echo "tail -f app.log"