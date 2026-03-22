#!/bin/bash
# Script de deploy para Raspberry Pi
# Uso: ./deploy.sh <ip-do-raspberry>
# Exemplo: ./deploy.sh 192.168.1.50

RASPBERRY_IP=${1:-"192.168.1.50"}
RASPBERRY_USER="pi"
DEPLOY_DIR="/home/pi/space-cloud-server"
JAR_NAME="space-cloud-server.jar"

echo "==> Build do fat JAR..."
./gradlew buildFatJar

echo "==> Criando diretório no Raspberry..."
ssh ${RASPBERRY_USER}@${RASPBERRY_IP} "mkdir -p ${DEPLOY_DIR}/data"

echo "==> Copiando JAR..."
scp build/libs/${JAR_NAME} ${RASPBERRY_USER}@${RASPBERRY_IP}:${DEPLOY_DIR}/

echo "==> Copiando serviço systemd..."
scp wol-controller.service ${RASPBERRY_USER}@${RASPBERRY_IP}:/tmp/

echo "==> Instalando serviço..."
ssh ${RASPBERRY_USER}@${RASPBERRY_IP} "
    sudo cp /tmp/wol-controller.service /etc/systemd/system/
    sudo systemctl daemon-reload
    sudo systemctl enable wol-controller
    sudo systemctl restart wol-controller
    sudo systemctl status wol-controller --no-pager
"

echo ""
echo "==> Deploy concluído!"
echo "    Acesse: http://${RASPBERRY_IP}:8080"
