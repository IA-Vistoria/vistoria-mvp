"""Smoke test: chamada de chat multimodal (texto) contra o OCI Generative AI.

Valida a pendência descrita em docs/spec-backend.md antes de escrever
OciGenAiIntegrationService: formato de request/response da Chat API.

Uso:
    OCI_COMPARTMENT_ID=ocid1.compartment... \
    OCI_REGION=sa-saopaulo-1 \
    GENAI_MODEL_ID=ocid1.generativeaimodel.oc1.sa-saopaulo-1.xxxxx \
    python test_genai_chat.py

O OCID do modelo muda por região/tenancy — descubra o seu com
list_available_models.py antes de rodar este script.
"""
import os

import oci

COMPARTMENT_ID = os.environ["OCI_COMPARTMENT_ID"]
REGION = os.environ.get("OCI_REGION", "sa-saopaulo-1")
MODEL_ID = os.environ["GENAI_MODEL_ID"]
PROFILE = os.environ.get("OCI_CONFIG_PROFILE", "DEFAULT")

config = oci.config.from_file("~/.oci/config", PROFILE)
client = oci.generative_ai_inference.GenerativeAiInferenceClient(
    config=config,
    service_endpoint=f"https://inference.generativeai.{REGION}.oci.oraclecloud.com",
    retry_strategy=oci.retry.NoneRetryStrategy(),
    timeout=(10, 240),
)

content = oci.generative_ai_inference.models.TextContent()
content.text = "Responda em uma frase curta: o que é uma vistoria predial?"

message = oci.generative_ai_inference.models.Message()
message.role = "USER"
message.content = [content]

chat_request = oci.generative_ai_inference.models.GenericChatRequest()
chat_request.api_format = oci.generative_ai_inference.models.BaseChatRequest.API_FORMAT_GENERIC
chat_request.messages = [message]
chat_request.max_tokens = 200
chat_request.temperature = 0.7
chat_request.top_p = 0.75

chat_detail = oci.generative_ai_inference.models.ChatDetails()
chat_detail.compartment_id = COMPARTMENT_ID
chat_detail.serving_mode = oci.generative_ai_inference.models.OnDemandServingMode(model_id=MODEL_ID)
chat_detail.chat_request = chat_request

response = client.chat(chat_detail)
print("STATUS:", response.status)
print("RESPOSTA:")
print(response.data.chat_response.choices[0].message.content[0].text)
print("\nSUCESSO: Generative AI respondeu.")
