# Design: Autenticação e Acesso

## Arquitetura de Componentes

Seguindo o padrão do monólito modular por feature, os componentes desta entrega residirão inteiramente no pacote `br.com.vistoriapredial.usuario` (e `config` para as configurações de segurança globais).

### 1. Camada Domain (`br.com.vistoriapredial.usuario.domain`)
- **`Usuario` (Entity)**: Entidade central que mapeia a tabela `tb_usuario`. Conterá métodos de validação no construtor para garantir a obrigatoriedade dos campos (exceto CREA). Construtor vazio `protected` apenas para uso do Hibernate.
- **`PerfilEnum` (Enum)**: Mapeia as Roles `ROLE_CLIENTE` e `ROLE_ENGENHEIRO`.

### 2. Camada Persistence (`br.com.vistoriapredial.usuario.persistence`)
- **`UsuarioRepository` (Interface)**: Extende `JpaRepository`. Define o método base `Optional<Usuario> findByEmail(String email)`.

### 3. Camada Application (`br.com.vistoriapredial.usuario.application`)
- **`UsuarioService` (Class)**: Casos de uso de negócio. Valida a restrição do CREA, realiza o Hash da senha com BCrypt, verifica conflitos de e-mail e chama o repositório.
- **`AuthRequestDto` / `RegisterRequestDto` (Record)**: DTOs imutáveis com anotações do Bean Validation (`@NotBlank`, `@Email`, etc.).
- **`AuthResponseDto` (Record)**: Contrato de saída com os dados públicos do usuário e o token.
- **`UsuarioConflictException` (Exception)**: Lançada caso o email já exista (traduzida para 409 pelo Handler global).

### 4. Camada Web (`br.com.vistoriapredial.usuario.web`)
- **`AuthController` (Class)**: Fachada REST. Endpoint `POST /api/auth/register` e `POST /api/auth/login`.

### 5. Configurações Compartilhadas (`br.com.vistoriapredial.config.security`)
- **`SecurityConfig`**: Define a cadeia de filtros stateless, desabilita CSRF e abre rotas de registro/login.
- **`JwtService` / `JwtAuthFilter`**: Componente de geração de tokens e filtro que intercepta as requisições para preencher o contexto de autenticação do Spring Security.
- **`CustomAuthenticationEntryPoint`**: Adicional para capturar falhas de token e responder no formato RFC 9457.
