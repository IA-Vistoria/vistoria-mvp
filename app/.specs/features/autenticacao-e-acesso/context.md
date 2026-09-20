# Contexto e Decisões de Negócio: Autenticação e Acesso

Este documento registra as decisões sobre regras ambíguas ou "áreas cinzentas" no escopo de Autenticação e Acesso.

## 1. Tratamento do campo CREA
- **Dilema**: O cadastro recebe um campo único de "perfil". Como lidar com a obrigatoriedade do CREA?
- **Decisão**: 
  - Se o perfil for `ROLE_ENGENHEIRO`, o CREA é obrigatório (validação estrutural de negócio).
  - Se o perfil for `ROLE_CLIENTE`, o CREA **deve ser ignorado e anulado (setado como null)** no Service, independentemente de o cliente tê-lo enviado maliciosamente no JSON.

## 2. Padrão de Senhas
- **Decisão**: Toda senha deve ser codificada com `BCryptPasswordEncoder` antes de ser persistida. A entidade `Usuario` deve receber a senha já criptografada (ou ser atualizada via método seguro no Service). Em momento algum a senha plana trafega fora do contexto de cadastro/login, e nunca deve estar presente no `AuthResponseDto`.

## 3. Gestão de Sessão e JWT
- **Decisão**: A aplicação adotará o modelo 100% Stateless. Sessões HTTP não serão criadas. O JWT conterá o e-mail (subject) e o perfil do usuário (roles).
- **Tempo de Expiração**: 24 horas por padrão.
- **Refresh Token**: Não será implementado no escopo inicial. O usuário deverá fazer login novamente ao expirar o token.

## 4. Retorno de Exceções
- **Decisão**: Seguiremos o padrão RFC 9457 global já implementado (`GlobalExceptionHandler`). 
  - Conflito de e-mail retorna 409.
  - Credenciais incorretas retornam 401.
  - O `AuthenticationEntryPoint` do Spring Security também deve ser customizado para retornar o mesmo JSON RFC 9457 no status 401 quando o token estiver ausente ou inválido.
