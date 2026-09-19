# Frontend Vistor.IA

Interface Next.js das jornadas de cliente e engenheiro do Vistor.IA. A documentação completa do produto, arquitetura e API está no [`README.md`](../README.md) da raiz.

## Execução

```powershell
npm ci
npm run dev
```

A aplicação abre em `http://localhost:3000` e usa `http://localhost:8080/api` por padrão. Para alterar a API, defina `NEXT_PUBLIC_API_URL` antes do build ou da inicialização.

## Verificação

```powershell
npm test -- --run
npm run lint
npm run build
```

## Estrutura

```text
src/
├── app/                  # Rotas e layouts do App Router
├── components/           # Shell e componentes compartilhados
├── features/auth/        # Cadastro, login, sessão e proteção de perfis
├── features/inspections/ # Fluxos de cliente e engenharia
└── lib/                  # Cliente HTTP e armazenamento da sessão
```
