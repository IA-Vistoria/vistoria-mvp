package br.com.vistoriapredial.integration.oci.genai;

import com.oracle.bmc.generativeaiinference.requests.ChatRequest;
import com.oracle.bmc.generativeaiinference.responses.ChatResponse;

@FunctionalInterface
public interface OciGenAiChatClient {

    ChatResponse chat(ChatRequest request);
}
