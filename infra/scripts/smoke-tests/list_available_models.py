"""Lista os modelos do OCI Generative AI disponíveis no compartment/região.

Uso:
    OCI_COMPARTMENT_ID=ocid1.compartment... OCI_REGION=sa-saopaulo-1 python list_available_models.py
"""
import os

import oci

COMPARTMENT_ID = os.environ["OCI_COMPARTMENT_ID"]
REGION = os.environ.get("OCI_REGION", "sa-saopaulo-1")
PROFILE = os.environ.get("OCI_CONFIG_PROFILE", "DEFAULT")

config = oci.config.from_file("~/.oci/config", PROFILE)
client = oci.generative_ai.GenerativeAiClient(
    config=config,
    service_endpoint=f"https://generativeai.{REGION}.oci.oraclecloud.com",
)

response = client.list_models(compartment_id=COMPARTMENT_ID)
for m in response.data.items:
    print(f"{m.display_name!r:45} id={m.id} capabilities={m.capabilities} lifecycle_state={m.lifecycle_state}")
