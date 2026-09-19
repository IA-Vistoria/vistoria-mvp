# Especificação: Autenticação e Acesso

## Feature Overview
Permitir que Clientes e Engenheiros Civis se registrem e autentiquem no sistema Vistoria Predial de forma segura utilizando tokens JWT (JSON Web Tokens).

## Problem Statement
O sistema precisa garantir que apenas usuários autorizados tenham acesso à plataforma, além de diferenciar permissões entre clientes (que criam vistorias) e engenheiros (que homologam laudos). Sem uma autenticação robusta e baseada em JWT stateless, a aplicação seria insegura e difícil de escalar dentro da infraestrutura gratuita adotada.

## Out of Scope
- Implementação de recuperação de senha ("Esqueci minha senha").
- Implementação de Refresh Tokens.
- Login social (Google, Facebook, etc.).
- Verificação de email via link (Double Opt-in).

## Assumptions & Open Questions
- **Assumption:** A comunicação ocorrerá via HTTPS em produção, garantindo a integridade dos dados durante o envio de credenciais.
- **Assumption:** O Frontend gerenciará o armazenamento seguro do token JWT (ex: HttpOnly Cookies ou local storage com devidos cuidados).
- **Open questions:** O CREA informado será validado em alguma API externa da CONFEA/CREA? Por ora assumimos apenas validação de formato e presença.

## User Stories
- Como um cliente da plataforma, quero poder me registrar e fazer login usando email e senha, para ter acesso ao envio de vistorias.
- Como um engenheiro civil, quero me registrar informando meu CREA, para que eu tenha autorização para homologar laudos gerados pela IA.

## Requirements (EARS)

### Cadastro de Usuário
- **AUTH-01**: Quando o usuário envia dados válidos para cadastro (nome, email, senha, perfil), o sistema deve criar a conta e retornar `201 Created` com os dados do usuário (exceto senha).
- **AUTH-02**: Se o usuário tenta se cadastrar com um e-mail já existente, o sistema deve retornar `409 Conflict`.
- **AUTH-03**: Se o usuário escolhe o perfil `ROLE_ENGENHEIRO` e não informa o número do CREA, o sistema deve retornar `422 Unprocessable Content`.
- **AUTH-04**: Se o usuário escolhe o perfil `ROLE_CLIENTE`, o sistema deve ignorar ou anular o campo CREA, pois clientes não possuem CREA.

### Autenticação (Login)
- **AUTH-05**: Quando o usuário envia credenciais corretas (e-mail e senha), o sistema deve retornar `200 OK` contendo um token JWT válido.
- **AUTH-06**: Se o usuário envia uma senha ou e-mail incorretos, o sistema deve retornar `401 Unauthorized`.

### Proteção de Rotas
- **AUTH-07**: Quando uma requisição é feita para uma rota protegida sem um token JWT válido, o sistema deve retornar `401 Unauthorized` no formato `application/problem+json` (RFC 9457).

## Requirement Traceability
| Requirement | Tasks | Tests |
|-------------|-------|-------|
| AUTH-01     | TBD   | TBD   |
| AUTH-02     | TBD   | TBD   |
| AUTH-03     | TBD   | TBD   |
| AUTH-04     | TBD   | TBD   |
| AUTH-05     | TBD   | TBD   |
| AUTH-06     | TBD   | TBD   |
| AUTH-07     | TBD   | TBD   |
