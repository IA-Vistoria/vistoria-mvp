"""Smoke test: sobe um objeto pequeno no bucket e lê de volta.

Uso:
    OCI_COMPARTMENT_ID=ocid1.compartment... \
    OCI_BUCKET_NAME=vistoria-fotos \
    OCI_REGION=sa-saopaulo-1 \
    python test_object_storage.py

Requer ~/.oci/config com um profile válido (padrão: DEFAULT).
"""
import os
import sys

import oci

COMPARTMENT_ID = os.environ["OCI_COMPARTMENT_ID"]
BUCKET_NAME = os.environ.get("OCI_BUCKET_NAME", "vistoria-fotos")
PROFILE = os.environ.get("OCI_CONFIG_PROFILE", "DEFAULT")
OBJECT_NAME = "smoke-test/hello.txt"

config = oci.config.from_file("~/.oci/config", PROFILE)
client = oci.object_storage.ObjectStorageClient(config=config)

namespace = client.get_namespace().data
print(f"Namespace: {namespace}")

client.put_object(
    namespace_name=namespace,
    bucket_name=BUCKET_NAME,
    object_name=OBJECT_NAME,
    put_object_body=b"vistoria-mvp smoke test ok",
)
print(f"Upload OK: {OBJECT_NAME}")

resp = client.get_object(namespace_name=namespace, bucket_name=BUCKET_NAME, object_name=OBJECT_NAME)
content = resp.data.content.decode()
print(f"Download OK, conteudo: {content!r}")

client.delete_object(namespace_name=namespace, bucket_name=BUCKET_NAME, object_name=OBJECT_NAME)
print("Limpeza OK (objeto de teste removido).")

if content != "vistoria-mvp smoke test ok":
    print("FALHOU: conteúdo lido não bate com o que foi escrito.")
    sys.exit(1)

print("SUCESSO: Object Storage acessível e funcionando.")
