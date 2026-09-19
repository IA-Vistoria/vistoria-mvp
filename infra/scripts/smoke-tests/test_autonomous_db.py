"""Smoke test: conecta na Autonomous Database via wallet (mTLS) e roda um SELECT.

Uso:
    WALLET_DIR=/caminho/para/wallet-descompactado \
    DB_USER=ADMIN \
    DB_PASSWORD=... \
    WALLET_PASSWORD=... \
    DB_DSN=vistoriadb_high \
    python test_autonomous_db.py

O wallet precisa estar DESCOMPACTADO em WALLET_DIR (não aponte pro .zip).
Peça o wallet e as senhas ao administrador do projeto por um canal seguro
(gerenciador de senhas) — nunca por chat ou e-mail. Ver
docs/onboarding-backend-dev.md.
"""
import os

import oracledb

WALLET_DIR = os.environ["WALLET_DIR"]
DB_USER = os.environ.get("DB_USER", "ADMIN")
DB_PASSWORD = os.environ["DB_PASSWORD"]
WALLET_PASSWORD = os.environ["WALLET_PASSWORD"]
DB_DSN = os.environ.get("DB_DSN", "vistoriadb_high")

connection = oracledb.connect(
    user=DB_USER,
    password=DB_PASSWORD,
    dsn=DB_DSN,
    config_dir=WALLET_DIR,
    wallet_location=WALLET_DIR,
    wallet_password=WALLET_PASSWORD,
)

with connection.cursor() as cursor:
    cursor.execute("SELECT 'vistoria-mvp smoke test ok' FROM dual")
    (result,) = cursor.fetchone()
    print("Resultado:", result)

connection.close()
print("SUCESSO: conexão com a Autonomous Database funcionando.")
