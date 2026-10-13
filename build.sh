#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v java >/dev/null 2>&1; then echo "ERRO: Java não encontrado. Instale Java 21."; exit 1; fi
if ! command -v mvn >/dev/null 2>&1; then echo "ERRO: Maven não encontrado. No Arch: sudo pacman -S maven"; exit 1; fi
java -version 2>&1 | grep -q '21' || { echo "AVISO: recomendado Java 21."; }
mvn -U clean package
if [[ ! -s target/pStaff-1.0.0.jar ]]; then echo "ERRO: JAR não foi gerado."; exit 1; fi
echo "Build concluído: $(pwd)/target/pStaff-1.0.0.jar"
