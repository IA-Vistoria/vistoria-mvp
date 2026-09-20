# Especificação — MVP do módulo de IA (leitura de imagem)

Documento de requisitos do serviço de inferência visual da plataforma de vistoria de imóveis. Escopo limitado à VLM: isolado do backend completo da plataforma.

## Objetivo deste MVP

Provar, da forma mais simples possível, que a IA consegue: receber uma foto de um ambiente (parede ou rodapé), analisar e devolver uma resposta estruturada e coerente sobre os problemas visuais encontrados. Nada além disso faz parte deste MVP.

Fica de fora (pertence à plataforma completa, não a este módulo): login, cadastro de usuário/imóvel/vistoria, histórico, PostgreSQL, JWT, frontend, filas. Este MVP não depende de nenhuma dessas coisas — só imagem entra, JSON sai.

## Critérios de implementação

1. Pesquise o estado atual de modelos de visão (VLM) open-source leves e com licença adequada para uso comercial. Não assuma que um modelo específico ainda é o mais indicado sem checar — o cenário muda rápido. Pontos de partida prováveis (confirme antes de escolher): a família Qwen-VL em tamanhos menores (ex.: Qwen2.5-VL ou Qwen3-VL nas versões pequenas), e alternativas ainda mais leves como SmolVLM2, MiniCPM-V ou Moondream, caso o objetivo seja rodar com o mínimo de recursos possível.
2. Escolha um modelo priorizando ser o mais leve possível dentro do que ainda reconhece bem manchas, fissuras, sinais de umidade etc. em fotos comuns de celular — não o modelo mais potente disponível.
3. Verifique a forma de execução recomendada (Transformers, vLLM, Ollama, llama.cpp, etc.), a quantização disponível e os requisitos de hardware.
4. Defina e fixe as versões exatas de tudo (Python, framework de inferência, biblioteca do modelo, CUDA se aplicável), garantindo que sejam compatíveis entre si e estáveis/seguras — nunca versões soltas ou `latest`.
5. Só depois disso, implemente.

Se encontrar uma opção melhor do que as sugeridas acima, explique antes de aplicar.

## O que a IA deve identificar

Principalmente em paredes e rodapés: manchas, sujeira, sinais de umidade, mofo, descascamento, bolhas, riscos, furos, fissuras/rachaduras aparentes, desgaste, peças quebradas/trincadas, descolamento, partes faltantes e outros problemas visualmente identificáveis.

## Regra de linguagem (crítica, não flexibilizar)

A IA nunca afirma um diagnóstico técnico definitivo — só relata o que é visualmente identificável.

- ❌ "Existe infiltração estrutural." / "Rachadura estrutural confirmada."
- ✅ "Há sinais visuais compatíveis com possível umidade." / "Fissura aparente na superfície."
- Rótulos como `possible_moisture`, `apparent_crack`, e `insufficient_evidence` quando não houver evidência suficiente.

## Escopo técnico do MVP

Um serviço HTTP simples e independente. Este é o mesmo componente que depois vira a "VM 2 — IA" da plataforma completa, então seguir este contrato desde já evita ter que refazer isso depois:

- `POST /analyze` — recebe uma imagem via multipart/form-data, roda o modelo, devolve o JSON de análise.
- `GET /health` — informa se o modelo já está carregado e o serviço está pronto para receber requisições.

Regras:

- O modelo é carregado **uma única vez**, na subida do processo — nunca a cada requisição.
- O serviço não conhece nada do resto do produto: sem usuários, sem banco, sem regra de negócio. Só imagem entra, JSON sai.
- Validar a imagem antes de processar (tipo MIME e tamanho máximo, via variável de ambiente). Se a imagem não puder ser usada, retornar isso em `image_quality`, sem tentar "adivinhar" uma análise.
- Se o modelo falhar ou travar, capturar o erro e responder algo tratável (ex.: HTTP 500 com mensagem genérica) — nunca deixar o processo cair.

## Formato de resposta (contrato)

```json
{
  "analysis_id": "uuid-gerado-na-hora",
  "image_quality": {
    "usable": true,
    "issues": []
  },
  "areas": [
    {
      "area": "parede",
      "issue_type": "possible_moisture",
      "description": "Mancha escura irregular próxima ao rodapé, compatível com possível umidade.",
      "evidence": "Alteração de cor e leve textura na superfície da parede.",
      "severity": "media",
      "confidence": "media",
      "recommendation": "Recomenda-se avaliação presencial da causa da mancha.",
      "location": "canto inferior esquerdo da imagem"
    }
  ],
  "overall_summary": "Resumo em linguagem natural do que foi observado na imagem.",
  "limitations": [
    "Análise baseada apenas em imagem; não substitui vistoria técnica presencial."
  ]
}
```

Cada item de `areas` deve trazer: área, tipo do problema, descrição, evidência, severidade, confiança qualitativa, recomendação e localização (quando possível identificar).

## Configuração via variáveis de ambiente

Mesmo sendo um MVP isolado, nada de valor fixo no código — tudo por `.env`:

```env
MODEL_ID=
MODEL_DEVICE=cuda
MAX_IMAGE_SIZE_MB=15
PORT=8001
```

## Versões fixas (mesma regra do projeto principal)

Fixar exatamente — nunca `latest`, nunca faixas abertas:

- Versão do Python usada pelo serviço.
- Versão do framework de inferência (Transformers/vLLM/etc.) e da biblioteca específica do modelo escolhido.
- Versão do CUDA/driver, se for usar GPU, compatível com a versão do framework escolhido.
- Imagem base do Docker com tag exata (ex.: uma imagem oficial de CUDA com tag de versão específica, nunca `latest`).

Antes de fixar, confirme que essas são de fato as versões estáveis mais recentes e com suporte de segurança ativo — isso muda com frequência.

## Docker

- Um `Dockerfile` só para este serviço, separado de qualquer coisa do backend.
- Se usar GPU, documentar no README como habilitar o runtime NVIDIA no Docker.
- Expor a porta definida em `PORT` (padrão 8001 — mesmo valor já usado como `VLM_URL` no prompt da plataforma completa).

## Forma de testar manualmente

Exemplo de chamada via `curl`, para colocar no README:

```
curl -X POST http://localhost:8001/analyze -F "image=@foto.jpg"
```

Opcionalmente, uma página HTML simples ou um script de linha de comando para upload manual de imagem durante o desenvolvimento — não é o núcleo do MVP, só facilita testar visualmente enquanto ajusta o modelo.

## Testes mínimos

- `/health` responde OK depois do modelo carregado.
- Envio de uma foto real (parede ou rodapé com algum problema visível) retorna JSON no formato do contrato.
- Envio de um arquivo que não é imagem retorna erro tratado, sem derrubar o serviço.
- Envio de imagem válida mas sem nenhum problema visível retorna `areas` vazio ou baixa confiança, sem inventar problema.

## Definição de pronto deste MVP

- [ ] Serviço sobe (`docker run` ou `docker compose up`) sem erro.
- [ ] `/health` responde OK.
- [ ] Envio de uma foto real retorna um JSON estruturado e coerente, no formato do contrato.
- [ ] A linguagem da resposta é sempre "indício visual", nunca diagnóstico técnico definitivo.
- [ ] Imagem inválida ou falha do modelo são tratadas sem derrubar o serviço.
- [ ] O serviço pode futuramente ser chamado pelo backend completo (prompt da plataforma) apenas configurando `VLM_URL` — sem precisar mudar nada aqui.
