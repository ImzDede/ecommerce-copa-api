# Arquitetura e Convenções do Backend

Este documento descreve a organização do código, as convenções adotadas e os principais fluxos técnicos do backend.

## 1. Stack Técnica

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA (Hibernate)
- Spring Security
- Bean Validation
- PostgreSQL

## 2. Organização em Camadas

O projeto segue uma arquitetura em camadas para separar responsabilidades e reduzir acoplamento.

- `controller`: Expõe endpoints HTTP, valida o formato da entrada e delega para serviços.
- `service`: Centraliza regras de negócio, autenticação por sessão e autorização por perfil.
- `repository`: Realiza acesso a dados com Spring Data JPA.
- `model`: Define entidades persistidas no banco.
- `dto`: Define contratos de entrada e saída da API.
- `config`: Configura segurança, CORS e beans técnicos.
- `exception`: Padroniza o tratamento de erros da aplicação.

Fluxo padrão de uma requisição:

1. O cliente chama um endpoint.
2. O `controller` recebe o JSON e converte para DTO.
3. O `service` valida regras e executa o caso de uso.
4. O `repository` persiste ou consulta entidades via JPA.
5. O `controller` retorna envelope de sucesso (`data`) ou erro (`error`).

## 3. Convenções de Código e API

- Endpoints usam prefixo base `/api`.
- JSON usa `camelCase`.
- Banco usa `snake_case`.
- Classes usam `PascalCase`.
- Métodos e variáveis usam `camelCase`.
- Respostas de sucesso seguem `{ "data": ... }`.
- Respostas de erro seguem `{ "error": { "code", "message", "details" } }`.
- Entidades JPA não são retornadas diretamente para o cliente; sempre há DTO.

## 4. Modelo de Dados e Entidades JPA

Arquivos principais:

- `src/main/java/br/ufc/smd/ecommercecopa/model/User.java`
- `src/main/java/br/ufc/smd/ecommercecopa/model/Client.java`
- `src/main/java/br/ufc/smd/ecommercecopa/model/Admin.java`
- `src/main/java/br/ufc/smd/ecommercecopa/model/UserRole.java`

### 4.1 Entidade `User`

- Mapeada para tabela `users` com `@Entity` e `@Table(name = "users")`.
- `id` é chave primária `UUID` com geração automática (`GenerationType.UUID`).
- `email` é único.
- `role` é enum persistido como texto (`EnumType.STRING`).
- `createdAt` é preenchido no `@PrePersist`.

### 4.2 Entidades `Client` e `Admin`

- `Client` e `Admin` representam perfis vinculados a `User`.
- Em ambos, o `userId` é ao mesmo tempo PK e FK para `users.id`.
- O vínculo é `@OneToOne` com `@MapsId`.

Na prática, `@MapsId` significa que a linha de `clients` ou `admins` reutiliza o mesmo ID da linha correspondente em `users`. Isso evita divergência de identidade entre tabelas.

## 5. Funcionamento do Spring Data JPA no Projeto

Arquivos principais:

- `src/main/java/br/ufc/smd/ecommercecopa/repository/UserRepository.java`
- `src/main/java/br/ufc/smd/ecommercecopa/repository/ClientRepository.java`
- `src/main/java/br/ufc/smd/ecommercecopa/repository/AdminRepository.java`

Cada repositório estende `JpaRepository<Entidade, Id>`, o que disponibiliza CRUD sem SQL manual para operações comuns.

Exemplos de métodos disponíveis automaticamente:

- `save(...)`
- `findById(...)`
- `findAll()`
- `deleteById(...)`

### 5.1 Query Methods

O Spring também cria consultas a partir do nome do método.

Exemplos em `UserRepository`:

- `findByEmail(String email)`
- `existsByEmail(String email)`
- `existsByEmailAndIdNot(String email, UUID id)`

Sem escrever SQL, o framework gera a consulta equivalente com base no nome do método.

### 5.2 Ciclo de Persistência

Quando uma entidade é enviada ao `save(...)`:

- Se não há ID persistido, o JPA executa `INSERT`.
- Se já há ID persistido, o JPA executa `UPDATE`.

Com `@Transactional`, várias operações de um mesmo caso de uso são executadas na mesma transação.

Exemplo de cadastro de cliente:

1. Salva `User`.
2. Associa `Client` ao `User`.
3. Salva `Client` com o mesmo ID via `@MapsId`.

Se ocorrer falha no meio do processo, a transação é revertida.

## 6. Autenticação e Autorização

Arquivos principais:

- `src/main/java/br/ufc/smd/ecommercecopa/service/AuthService.java`
- `src/main/java/br/ufc/smd/ecommercecopa/service/ClientService.java`
- `src/main/java/br/ufc/smd/ecommercecopa/service/SessionKeys.java`
- `src/main/java/br/ufc/smd/ecommercecopa/config/SecurityConfig.java`

### 6.1 Autenticação por Sessão

O backend usa `HttpSession` em vez de JWT.

No login:

1. O serviço busca usuário por email.
2. Compara senha informada com hash BCrypt.
3. Em sucesso, grava `AUTH_USER_ID` na sessão.
4. O servidor retorna cookie `JSESSIONID`.

Nas próximas chamadas, o navegador envia o cookie e o backend recupera o usuário da sessão.

### 6.2 Autorização por Perfil

Além de sessão válida, rotas de cliente exigem `UserRole.CLIENT`.

No `ClientService`, o método `ensureClient(...)` impede que usuários de outro perfil acessem recursos de cliente.

### 6.3 Logout

O logout invalida a sessão com `session.invalidate()`.

Após isso, novas chamadas em rotas protegidas retornam `401` até novo login.

## 7. Segurança e CORS

- Senhas são armazenadas apenas como hash (`BCryptPasswordEncoder`).
- CORS é configurado para origens locais de frontend.
- `allowCredentials=true` é necessário para enviar cookie de sessão no browser.

No frontend, chamadas autenticadas devem usar `credentials: "include"`.

## 8. Validação e Tratamento de Erros

Arquivos principais:

- `src/main/java/br/ufc/smd/ecommercecopa/exception/AppException.java`
- `src/main/java/br/ufc/smd/ecommercecopa/exception/GlobalExceptionHandler.java`
- `src/main/java/br/ufc/smd/ecommercecopa/dto/ApiErrorResponse.java`

O projeto centraliza erros no `GlobalExceptionHandler`, garantindo resposta consistente para o cliente.

Status mais usados:

- `400` para payload inválido.
- `401` para ausência de autenticação.
- `403` para falta de permissão.
- `404` para recurso inexistente.
- `409` para conflito de unicidade/integridade.
- `415` para `Content-Type` inválido.
- `422` para violação de regra de negócio.

## 9. Configuração de Runtime

Arquivos:

- `src/main/resources/application.properties`
- `src/main/resources/schema.sql`

Pontos relevantes:

- Conexão com PostgreSQL via `spring.datasource.*`.
- `spring.config.import=optional:file:.env[.properties]` para carregar variáveis locais sem versionar segredos.
- `spring.jpa.hibernate.ddl-auto=update` para evolução de schema em desenvolvimento.
- `server.servlet.session.timeout=30m` para expiração de sessão.
- `spring.sql.init.mode=always` para executar scripts SQL de inicialização.
