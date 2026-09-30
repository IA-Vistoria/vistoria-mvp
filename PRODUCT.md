# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

O usuário principal é a pessoa que recebe, confere ou documenta um imóvel e precisa organizar uma vistoria sem conhecimento técnico especializado. O uso acontece em deslocamento entre ambientes, frequentemente pelo celular, com atenção dividida, iluminação variável e conexão possivelmente instável.

## Product Purpose

O Vistor.IA orienta a captura de evidências fotográficas, organiza a análise visual da IA, permite que o usuário confirme ou corrija o contexto e gera um Relatório de vistoria por IA rastreável. O sucesso é o usuário concluir esse percurso com evidências preservadas, limitações explícitas e um documento compreensível.

## Positioning

O produto mantém um rastro verificável entre foto, achado sugerido pela IA, decisão humana e trecho do relatório. A IA organiza e sugere; o usuário conserva o controle sobre o que entra no documento.

## Operating Context

- Captura móvel em ambientes internos e externos.
- Upload de JPEG, PNG ou WebP para um roteiro próprio de ambientes.
- Processamento assíncrono com retomada posterior.
- Revisão de achados um a um antes da geração do relatório.
- Consulta, impressão e compartilhamento do documento final.

## Capabilities and Constraints

- O fluxo público cria somente usuários clientes.
- A jornada principal contém início, nova vistoria, captura guiada, análise, revisão e relatório.
- A jornada cria cada vistoria com endereço, tipo do imóvel e ambientes ordenados; novos uploads informam ambiente e categoria da evidência.
- Estados legados de engenharia podem permanecer no código por compatibilidade, mas não orientam a jornada principal do MVP.
- A IA aponta indícios visuais; o produto não promete diagnóstico, conformidade normativa, validade jurídica ou certificação profissional.
- Fotos e progresso confirmados devem permanecer recuperáveis após falhas de upload ou análise.

## Brand Commitments

- Nome: **Vistor.IA**.
- Categoria comunicada pela marca: vistoria de imóveis assistida por inteligência artificial, culminando em relatório.
- Direção visual aprovada: **Lente Operacional**, escura, fotográfica, precisa e voltada ao trabalho de campo.
- Marca aprovada: estrutura de imóvel, enquadramento de varredura e nós de IA, sem moldura externa no lockup.
- A paleta usa grafite-petróleo, branco mineral e verdigris; âmbar é reservado a progresso e atenção.
- Linguagem direta, profissional e acessível, sem tom jurídico ou promessas técnicas indevidas.

## Evidence on Hand

- Conceito visual aprovado: `.impeccable/mocks/approved-lente-operacional-v2.png`.
- Fluxos visuais aprovados para implementação: `.impeccable/mocks/flows/`.
- Protótipo navegável anterior: `prototipo/VistorIA-prototipo-navegavel.html`.
- Especificação de produto: `docs/superpowers/specs/2026-09-30-interface-ia-first-design.md`.
- Implementação funcional e testes em `app/frontend`.
- Não existem depoimentos, métricas comerciais ou certificações autorizadas; não devem ser fabricados.

## Product Principles

1. Evidência antes da conclusão.
2. IA explicável, corrigível e limitada.
3. Uma decisão importante por tela.
4. Progresso real, sem sucesso fabricado.
5. Continuidade entre captura, revisão e relatório.

## Accessibility & Inclusion

A interface deve alcançar WCAG 2.2 AA, manter foco visível, alvos de toque de ao menos 44 px, labels persistentes, mensagens de erro anunciadas e alternativa à câmera pela galeria. O layout precisa permanecer utilizável em 390, 768 e 1440 px, com zoom do navegador e preferência por movimento reduzido.
