# Smoke opt-in — OCI Generative AI

Este roteiro valida a integração real do backend com o OCI Generative AI. Ele
é separado dos testes automatizados porque usa credenciais externas e pode
consumir créditos da conta Oracle. Não use fotos pessoais, de imóveis reais ou
qualquer evidência que contenha pessoas, documentos ou endereços.

## 1. O que os testes locais já comprovam

- montagem da solicitação multimodal e do schema esperado;
- autenticação selecionável entre `config_file` e `instance_principal`;
- validação do documento v2, resultado por ambiente e persistência idempotente;
- falha explícita para timeout, 401, 403, 404, 429, 5xx e resposta inválida;
- ausência de fallback para mock quando `APP_IA_PROVIDER=oci`.

Esses testes usam fakes e não acessam a Oracle:

```powershell
cd app
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
.\mvnw.cmd "-Dtest=OciGenAiIntegrationServiceTest,OciGenAiConfigurationTest,OciGenAiHealthControllerTest,VistoriaAnalysisProcessorTest" test
```

Com o backend OCI iniciado, um usuário autenticado pode consultar
`GET /api/health/ia`. A resposta informa provider, região, modelo, modo de
autenticação e se as credenciais foram carregadas. O campo
`externalCallPerformed` permanece `false`: esse health check não envia imagem,
não consulta o modelo e não consome créditos.

## 2. Pré-requisitos da chamada real

Configure fora do repositório:

```powershell
$env:APP_IA_PROVIDER = "oci"
$env:OCI_AUTH_MODE = "config_file"
$env:OCI_CONFIG_FILE = "$env:USERPROFILE\.oci\config"
$env:OCI_CONFIG_PROFILE = "DEFAULT"
$env:OCI_REGION = "sa-saopaulo-1"
$env:OCI_COMPARTMENT_ID = "ocid1.compartment.oc1..substitua"
$env:OCI_GENAI_MODEL_ID = "google.gemini-2.5-flash"
$env:JWT_SECRET = "defina-um-segredo-local-com-pelo-menos-32-bytes"
```

O arquivo de configuração, a chave privada e qualquer token permanecem em
`~/.oci`; nunca copie esses arquivos para o projeto. Em workload hospedado na
OCI, use `OCI_AUTH_MODE=instance_principal` e não forneça arquivo de chave.

## 3. Gerar uma fixture não pessoal

O trecho abaixo produz uma imagem sintética com formas geométricas; ela serve
somente para validar transporte, autenticação e formato da resposta.

```powershell
Add-Type -AssemblyName System.Drawing
$fixturePath = Join-Path $env:TEMP "vistoria-oci-smoke-sintetico.png"
$bitmap = [System.Drawing.Bitmap]::new(800, 600)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.Clear([System.Drawing.Color]::Beige)
$graphics.FillRectangle([System.Drawing.Brushes]::DarkSlateGray, 80, 90, 250, 310)
$graphics.DrawLine([System.Drawing.Pens]::DarkRed, 410, 80, 470, 430)
$graphics.DrawString("FIXTURE SINTETICA - SEM DADOS PESSOAIS", [System.Drawing.Font]::new("Arial", 18), [System.Drawing.Brushes]::Black, 60, 520)
$bitmap.Save($fixturePath, [System.Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose()
$bitmap.Dispose()
```

## 4. Gate explícito de custo

Somente prossiga depois de autorização consciente para uma chamada real:

```powershell
$env:RUN_OCI_GENAI_SMOKE = "1"
if ($env:RUN_OCI_GENAI_SMOKE -ne "1") {
    throw "Smoke OCI bloqueado: defina RUN_OCI_GENAI_SMOKE=1 conscientemente."
}
```

Inicie backend e frontend, crie uma conta fictícia, configure uma vistoria com
um único ambiente e envie apenas a fixture gerada. A chamada com custo acontece
ao usar **Enviar para análise da IA**.

## 5. Critérios de aceite

O smoke só recebe `PASS real` quando todos os itens abaixo forem observados:

- a vistoria chega a `RELATORIO_DISPONIVEL` sem ativar o profile `demo`;
- a rastreabilidade mostra provider `oci`, modelo e versão do prompt;
- existe resultado geral, motivo e resultado associado ao ambiente enviado;
- cada achado apresenta critério, descrição, evidência, impacto, gravidade,
  confiança e recomendação, quando houver;
- a manifestação do usuário é opcional e não altera a conclusão original;
- uma credencial inválida ou cota indisponível termina em `FALHA_IA`, sem
  documento parcial e sem resposta mock.

Se houver 401/403, falta de cota, modelo indisponível ou bloqueio da tenancy,
registre `BLOQUEADO EXTERNAMENTE` com data e resposta sanitizada. Nunca converta
esse estado em `PASS` e nunca inclua chaves, OCIDs pessoais ou o payload da
imagem no registro.
