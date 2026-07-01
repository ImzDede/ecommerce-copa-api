# Ecommerce Copa API

API backend da aplicação Ecommerce Copa, desenvolvida com Spring Boot, autenticação por sessão e persistência em PostgreSQL.

## Stack

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Security
- PostgreSQL
- OpenPDF
- Maven Wrapper (`mvnw`)

## Pré-requisitos

- Java 21 instalado
- PostgreSQL acessível (local ou Neon)
- Git (opcional, para versionamento)

## Configuração de Ambiente

1. Copie o arquivo de exemplo:
   - Windows PowerShell:
   ```powershell
   Copy-Item .env.example .env
   ```

2. Edite o `.env` com suas credenciais:
   ```env
   DB_URL=jdbc:postgresql://SEU_HOST:5432/SEU_BANCO?sslmode=require&channel_binding=require
   DB_USERNAME=SEU_USUARIO
   DB_PASSWORD=SUA_SENHA
   ```

## Como Rodar o Projeto

No diretório raiz do backend (`ecommerce-copa-api`):

- Windows PowerShell:
```powershell
.\start.cmd
```

Ou, usando Maven Wrapper diretamente:

```powershell
.\mvnw.cmd spring-boot:run
```

A API sobe em:

- `http://localhost:8080`

## Swagger / OpenAPI

Com a API rodando, acesse:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

As rotas protegidas usam autenticação por cookie de sessão (`JSESSIONID`). Para testar no Swagger, faça login em `POST /api/auth/login` e depois chame as rotas privadas na mesma aba do navegador.

## Uploads

- Imagens enviadas para foto de perfil e variantes/SKUs são salvas localmente em `uploads/`.
- A API serve os arquivos por `/uploads/**`, por exemplo `/uploads/products/arquivo.jpg`.
- Formatos aceitos: `image/jpeg`, `image/png` e `image/webp`.
- A pasta `uploads/` é ignorada pelo Git.

## Build (Compilação)

```powershell
.\mvnw.cmd -DskipTests compile
```

## Testes

```powershell
.\mvnw.cmd test
```

## Autenticação

A autenticação é por sessão HTTP (`JSESSIONID`).

Fluxo:

1. `POST /api/auth/register/client`
2. `POST /api/auth/login`
3. Usar rotas privadas com cookie de sessão
4. `POST /api/auth/logout`

## Documentação

- Arquitetura e Convenções: `docs/arquitetura-e-convencoes.md`
- Rotas da API: Swagger UI em `http://localhost:8080/swagger-ui.html`
