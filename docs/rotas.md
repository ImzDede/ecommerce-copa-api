# Rotas da API

Base URL:
- `/api`

Padrão de resposta:
- Sucesso: `{ "data": ... }`
- Erro: `{ "error": { "code", "message", "details" } }`

## 1) Cadastro de cliente

Método e rota:
- `POST /api/auth/register/client`

Request body:
```json
{
  "name": "Maria Silva",
  "email": "maria@email.com",
  "password": "Senha@123",
  "cpf": "12345678901",
  "dateOfBirth": "2000-05-01"
}
```

Response body (201):
```json
{
  "data": {
    "id": "0fbc97f0-8524-4d22-8a2a-8980dff498ab",
    "name": "Maria Silva",
    "email": "maria@email.com",
    "role": "CLIENT"
  }
}
```

## 2) Login (email + senha)

Método e rota:
- `POST /api/auth/login`

Request body:
```json
{
  "email": "maria@email.com",
  "password": "Senha@123"
}
```

Response body (200):
```json
{
  "data": {
    "userId": "0fbc97f0-8524-4d22-8a2a-8980dff498ab",
    "name": "Maria Silva",
    "email": "maria@email.com",
    "role": "CLIENT",
    "authenticated": true
  }
}
```

Observação:
- Esta rota cria sessão HTTP e retorna cookie `JSESSIONID`.

## 3) Usuário autenticado atual

Método e rota:
- `GET /api/auth/me`

Request body:
- Sem body.

Response body (200):
```json
{
  "data": {
    "userId": "0fbc97f0-8524-4d22-8a2a-8980dff498ab",
    "name": "Maria Silva",
    "email": "maria@email.com",
    "role": "CLIENT",
    "authenticated": true
  }
}
```

## 4) Logout

Método e rota:
- `POST /api/auth/logout`

Request body:
- Sem body.

Response body (204):
- Sem corpo.

## 5) Perfil do cliente logado

Método e rota:
- `GET /api/clients/me`

Request body:
- Sem body.

Response body (200):
```json
{
  "data": {
    "userId": "0fbc97f0-8524-4d22-8a2a-8980dff498ab",
    "name": "Maria Silva",
    "email": "maria@email.com",
    "cpf": "12345678901",
    "dateOfBirth": "2000-05-01"
  }
}
```

## 6) Atualizar perfil do cliente logado

Método e rota:
- `PATCH /api/clients/me`

Request body (Todos os campos opcionais, mas deve enviar pelo menos um):
```json
{
  "name": "Maria A. Silva",
  "email": "maria.novo@email.com",
  "password": "NovaSenha@123"
}
```

Response body (200):
```json
{
  "data": {
    "userId": "0fbc97f0-8524-4d22-8a2a-8980dff498ab",
    "name": "Maria A. Silva",
    "email": "maria.novo@email.com",
    "cpf": "12345678901",
    "dateOfBirth": "2000-05-01"
  }
}
```

## 7) Excluir própria conta de cliente

Método e rota:
- `DELETE /api/clients/me`

Request body:
- Sem body.

Response body (204):
- Sem corpo.

## Erros comuns

Exemplo de erro padrão:
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Dados inválidos",
    "details": [
      {
        "field": "email",
        "message": "Deve ser um email válido"
      }
    ]
  }
}
```

Códigos de status mais usados:
- `400`: Body inválido
- `401`: Sem sessão ou sessão expirada
- `403`: Sem permissão
- `409`: Conflito de unicidade (ex.: email/cpf duplicado)
- `415`: Content-Type incorreto (usar `application/json`)
- `422`: Regra de negócio inválida
