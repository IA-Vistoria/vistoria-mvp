# Interface e roteiro adaptativo Context

**Gathered:** 2026-09-30
**Spec:** `.specs/features/interface-lente-operacional/spec.md`
**Status:** Ready for design

---

## Feature Boundary

Esta entrega redesenha a jornada principal do cliente e substitui o protocolo fixo de 12 itens por um roteiro estruturado por ambientes, persistido em cada vistoria. O fluxo continua terminando na análise da IA, revisão do responsável e Relatório de vistoria por IA.

---

## Implementation Decisions

### Flexibilidade com estrutura

- O tipo do imóvel serve apenas para sugerir ambientes. Ele nunca bloqueia, exige ou remove um ambiente.
- Cada vistoria guarda sua própria lista ordenada de ambientes.
- O roteiro aceita ambientes conhecidos e nomes personalizados.
- Cada ambiente exige uma foto de visão geral para permitir o envio à IA.
- Fotos de detalhe são opcionais e permanecem associadas ao ambiente correto.
- O progresso usa o roteiro persistido da vistoria, nunca um total global fixo.

### Edição segura do roteiro

- O responsável pode adicionar, renomear e reordenar ambientes enquanto a vistoria estiver em rascunho.
- Um ambiente sem evidências pode ser removido.
- Um ambiente que já possui evidências não pode ser removido; a API responde com conflito e preserva os dados.
- “Pular por agora” apenas navega para o próximo ambiente. Não marca o ambiente como concluído.

### Dados enviados

- A criação recebe endereço, tipo do imóvel e a lista final de ambientes em uma única operação.
- Cada upload informa `ambienteId` e a categoria `VISAO_GERAL` ou `DETALHE`.
- A IA não inventa o roteiro e não decide quais ambientes existem.
- Relatório e revisão exibem o ambiente persistido junto de cada evidência.

### Direção visual

- O logo não possui moldura externa; somente a geometria de varredura do símbolo permanece.
- A paleta usa grafite-petróleo, branco mineral, verdigris como ação e âmbar apenas para progresso ou atenção.
- Azul elétrico, amarelo fluorescente, gradientes, brilho e glassmorphism ficam excluídos.

### Agent's Discretion

- Conjuntos iniciais sugeridos para casa, apartamento, comercial e outro.
- Ordem dos ambientes sugeridos e microcópia de orientação.
- Composição responsiva da configuração do roteiro e da captura.

### Declined / Undiscussed Gray Areas → Assumptions

- O roteiro aceita de 1 a 30 ambientes; esse limite cobre o MVP sem permitir payload não limitado.
- Nomes possuem de 2 a 60 caracteres e são únicos por vistoria sem diferenciar maiúsculas de minúsculas.
- A alteração do contrato é compatibilizada para leitura de evidências antigas, mas novos uploads usam somente o formato estruturado.

---

## Specific References

- O usuário definiu que a interface deve deixar claro o que enviar, sem presumir que casas, apartamentos e imóveis comerciais tenham os mesmos cômodos.
- Os conceitos visuais aprovados nesta conversa são a referência de composição e paleta.

---

## Deferred Ideas

- Remover definitivamente o fluxo legado de engenharia.
- Alterar o contrato do provedor VLM externo para receber metadados adicionais; depende da documentação real desse serviço.
