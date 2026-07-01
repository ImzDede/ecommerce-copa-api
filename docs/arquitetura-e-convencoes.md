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
- Springdoc OpenAPI
- OpenPDF para geração de relatórios PDF

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
2. O `controller` recebe o JSON ou formulário multipart e converte para DTO.
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
- `src/main/java/br/ufc/smd/ecommercecopa/model/Category.java`
- `src/main/java/br/ufc/smd/ecommercecopa/model/Product.java`
- `src/main/java/br/ufc/smd/ecommercecopa/model/Sku.java`
- `src/main/java/br/ufc/smd/ecommercecopa/model/Review.java`
- `src/main/java/br/ufc/smd/ecommercecopa/model/Cart.java`
- `src/main/java/br/ufc/smd/ecommercecopa/model/Address.java`
- `src/main/java/br/ufc/smd/ecommercecopa/model/Order.java`
- `src/main/java/br/ufc/smd/ecommercecopa/model/OrderItem.java`
- `src/main/java/br/ufc/smd/ecommercecopa/model/Tag.java`
- `src/main/java/br/ufc/smd/ecommercecopa/model/SkuTag.java`

### 4.1 Entidade `User`

- Mapeada para tabela `users` com `@Entity` e `@Table(name = "users")`.
- `id` é chave primária `UUID` com geração automática (`GenerationType.UUID`).
- `email` é único.
- `role` é enum persistido como texto (`EnumType.STRING`).
- `profilePhoto` guarda o caminho da foto de perfil, quando enviada.
- `createdAt` é preenchido no `@PrePersist`.
- `deletedAt` representa exclusão lógica da conta.
- Usuários com `deletedAt` preenchido não conseguem fazer login nem usar sessão existente.

### 4.2 Entidades `Client` e `Admin`

- `Client` e `Admin` representam perfis vinculados a `User`.
- Em ambos, o `userId` é ao mesmo tempo PK e FK para `users.id`.
- O vínculo é `@OneToOne` com `@MapsId`.

Na prática, `@MapsId` significa que a linha de `clients` ou `admins` reutiliza o mesmo ID da linha correspondente em `users`. Isso evita divergência de identidade entre tabelas.

### 4.3 Entidade `Category`

- Mapeada para tabela `categories`.
- Representa a categoria pública dos produtos, como `Álbuns`, `Figurinhas` ou `Chuteiras`.
- Possui `slug` único gerado automaticamente a partir do `title`.
- Possui `image` para imagem/URL pública e `featured` para destacar categorias na navegação.
- Possui `deletedAt` para soft delete.
- Categorias com `deletedAt` preenchido não aparecem nas listagens admin/públicas e não podem ser usadas para criar ou mover products.
- A exclusão de categoria é bloqueada apenas quando existem products ativos vinculados.

### 4.4 Entidade `Product`

- Mapeada para tabela `products`.
- Representa o produto base ou agrupador visual.
- Guarda `name` para a visão administrativa centrada no produto.
- Possui FK para `categories`.
- Possui campo `schema` em JSONB para persistir internamente as opções de variação que o frontend envia como `options[]`.
- Possui `deletedAt` para soft delete.
- A exclusão administrativa de um product marca o próprio product e todos os SKUs vinculados como deletados.

Exemplo de `schema`:

```json
{
  "selectors": [
    {
      "key": "edition",
      "label": "Edição"
    },
    {
      "key": "size",
      "label": "Tamanho"
    }
  ]
}
```

### 4.5 Entidade `Sku`

- Mapeada para tabela `skus`.
- Representa a unidade comprável de fato.
- Possui FK para `products`.
- Guarda `title`, `description`, `price`, `originalPrice`, `photos`, `stock` e `attributes`.
- `photos` armazena os caminhos das imagens enviadas via multipart, não a imagem binária.
- Respostas de visualização expõem apenas `photo`, derivado da primeira imagem de `photos`.
- Possui `deletedAt` para remover do catálogo sem quebrar histórico futuro de compras.

Exemplo de `attributes`:

```json
{
  "edition": "Campo",
  "size": "40",
  "color": "Vermelha"
}
```

### 4.6 Entidade `Review`

- Mapeada para tabela `reviews`.
- Usa chave composta `client_id + sku_id`.
- Representa uma avaliação feita por um cliente para um SKU específico.
- Guarda `stars`, `comment` e `createdAt`.
- Reviews são ligadas a SKUs, não a Products.
- Um cliente pode ter no máximo uma review por SKU por causa da chave composta.

### 4.7 Entidade `Cart`

- Mapeada para tabela `carts`.
- Usa chave composta `client_id + sku_id`.
- Representa um item do carrinho de um cliente.
- Guarda `amount` e `createdAt`.
- Cada SKU aparece no máximo uma vez no carrinho de cada cliente.
- O subtotal é calculado em runtime por `sku.price * amount`.

### 4.8 Entidade `Address`

- Mapeada para tabela `address`.
- Possui FK para `clients`.
- Guarda `name`, `street`, `number`, `state`, `city`, `neighborhood`, `complement`, `postalCode` e `isDefault`.
- Possui `deletedAt` para soft delete.
- Cliente só acessa endereços próprios e não deletados.
- Quando um endereço é marcado como padrão, os demais endereços ativos do cliente deixam de ser padrão.

### 4.9 Entidades `Order` e `OrderItem`

- `Order` é mapeada para tabela `orders`.
- `Order` possui FK para `clients` e `address`.
- `Order` guarda `totalValue`, `status`, `createdAt` e `deletedAt`.
- `status` é enum persistido como texto com valores `PROCESSING`, `SHIPPED`, `DELIVERED` e `CANCELED`.
- `deletedAt` representa cancelamento administrativo da venda.
- `OrderItem` é mapeada para tabela `order_items`.
- `OrderItem` usa chave composta `order_id + sku_id`.
- `OrderItem` guarda snapshot de `price` e `amount` no momento da compra.
- Ao finalizar pedido, o backend valida estoque, cria os itens, decrementa estoque e limpa o carrinho.
- Ao finalizar pedido, o status inicial é `PROCESSING`.
- Ao cancelar venda no admin, o backend define `status = CANCELED`, preenche `orders.deleted_at` e restaura estoque dos SKUs.
- Admin pode alterar status por `PATCH /api/admin/orders/{id}/status`, exceto reabrir pedido cancelado.

### 4.10 Entidades `Tag` e `SkuTag`

- `Tag` é mapeada para tabela `tags`.
- Guarda `text` e `color`.
- Tags são manuais, controladas por admin.
- `SkuTag` é mapeada para tabela `sku_tags`.
- `SkuTag` usa chave composta `sku_id + tag_id`.
- Tags podem representar rótulos como `Mais vendidos`, `Oferta`, `Novo` ou `Edição limitada`.
- As respostas de SKUs no admin e no catálogo incluem a lista de tags vinculadas.

### 4.11 Regra de catálogo

- `Category` organiza a navegação pública.
- `Product` agrupa SKUs relacionados e define o schema de seleção.
- `Sku` é a unidade comprável, com descrição, preço, fotos, estoque e atributos.
- O catálogo público deve listar SKUs ativos e em estoque.
- A página de produto deve abrir por `Product` e exibir os SKUs relacionados.
- Products e SKUs com `deletedAt` preenchido não devem aparecer para o cliente.
- O CRUD admin é centrado em `Product`: criação e atualização sincronizam produto, opções e variantes/SKUs em um único payload multipart.
- A descrição e as imagens pertencem às variantes/SKUs, não ao Product.
- As variantes validam se `attributes` contém exatamente as mesmas chaves definidas em `product.schema.selectors`; produtos sem opções aceitam `attributes` vazio.
- Reviews públicas são listadas por SKU em `/api/catalog/skus/{skuId}/reviews`.
- Listagens públicas de SKU retornam `rating` e `reviewCount` calculados a partir das reviews.
- `GET /api/catalog/skus` aceita múltiplas categorias e ordenação `sort=rating`.
- `GET /api/catalog/categories` expõe categorias públicas ordenadas por destaque e título.
- Carrinho trabalha com SKU porque SKU é a unidade comprável.
- Tags são vinculadas manualmente aos SKUs e retornadas no catálogo.

Rotas públicas de catálogo:

- `GET /api/catalog/skus`: Lista SKUs compráveis em estoque, com filtros de categoria, busca, paginação e ordenação.
- `GET /api/catalog/categories`: Lista categorias públicas.
- `GET /api/catalog/products/{productId}`: Abre a visualização do product base e retorna seus SKUs ativos para montagem dos seletores no frontend.
- `GET /api/catalog/skus/{skuId}/reviews`: Lista avaliações públicas de um SKU ativo.

### 4.12 Relatórios Administrativos

- Relatórios ficam em `/api/admin/reports` e exigem sessão de admin.
- Compras por cliente e receita diária recebem `startDate` e `endDate` no formato `yyyy-MM-dd`.
- O período considera o dia inicial e o dia final.
- Pedidos cancelados são ignorados nos relatórios financeiros.
- SKUs sem estoque lista apenas SKUs e products ativos.
- Os endpoints de relatório retornam `application/pdf` diretamente.
- Compras por cliente são ordenadas por quantidade de compras descrescente.
- SKUs sem estoque são ordenados pela descrição de forma ascendente.
- Receita diária é ordenada pela data de forma ascendente.

## 5. Funcionamento do Spring Data JPA no Projeto

Arquivos principais:

- `src/main/java/br/ufc/smd/ecommercecopa/repository/UserRepository.java`
- `src/main/java/br/ufc/smd/ecommercecopa/repository/ClientRepository.java`
- `src/main/java/br/ufc/smd/ecommercecopa/repository/AdminRepository.java`
- `src/main/java/br/ufc/smd/ecommercecopa/repository/CategoryRepository.java`
- `src/main/java/br/ufc/smd/ecommercecopa/repository/ProductRepository.java`
- `src/main/java/br/ufc/smd/ecommercecopa/repository/SkuRepository.java`
- `src/main/java/br/ufc/smd/ecommercecopa/repository/ReviewRepository.java`
- `src/main/java/br/ufc/smd/ecommercecopa/repository/CartRepository.java`
- `src/main/java/br/ufc/smd/ecommercecopa/repository/AddressRepository.java`
- `src/main/java/br/ufc/smd/ecommercecopa/repository/OrderRepository.java`
- `src/main/java/br/ufc/smd/ecommercecopa/repository/OrderItemRepository.java`
- `src/main/java/br/ufc/smd/ecommercecopa/repository/TagRepository.java`
- `src/main/java/br/ufc/smd/ecommercecopa/repository/SkuTagRepository.java`

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

Exemplos em `CategoryRepository`:

- `findBySlug(String slug)`
- `existsBySlug(String slug)`
- `existsBySlugAndIdNot(String slug, UUID id)`

Esses métodos são usados para gerar slugs únicos automaticamente.

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
2. Rejeita usuário com `deletedAt` preenchido.
3. Compara senha informada com hash BCrypt.
4. Em sucesso, grava `AUTH_USER_ID` na sessão.
5. O servidor retorna cookie `JSESSIONID`.

Nas próximas chamadas, o navegador envia o cookie e o backend recupera o usuário da sessão.

### 6.2 Autorização por Perfil

Além de sessão válida, rotas de cliente exigem `UserRole.CLIENT`.

No `ClientService`, o método `ensureClient(...)` impede que usuários de outro perfil acessem recursos de cliente.

Rotas administrativas usam `AuthService.requireAdmin(...)`.

Esse método exige sessão válida e `UserRole.ADMIN` antes de executar regras de CRUD administrativo.

### 6.3 Logout

O logout invalida a sessão com `session.invalidate()`.

Após isso, novas chamadas em rotas protegidas retornam `401` até novo login.

### 6.4 Exclusão de Conta

- `DELETE /api/users/me` faz soft delete da conta preenchendo `users.deleted_at`.
- A sessão é invalidada após a exclusão.
- A foto de perfil é removida do filesystem em best effort.
- Para preservar histórico, `clients`, `orders`, `order_items`, `address` e `reviews` não são apagados.

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
- `413` para upload acima do limite configurado.
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
- `springdoc.swagger-ui.path=/swagger-ui.html` para expor a interface Swagger.
- `springdoc.api-docs.path=/api-docs` para expor o documento OpenAPI em JSON.
- `app.upload-dir=uploads` para definir a pasta local de uploads.
- `spring.servlet.multipart.max-file-size=5MB` e `spring.servlet.multipart.max-request-size=5MB` para limitar uploads.

## 10. Uploads

Arquivos principais:

- `src/main/java/br/ufc/smd/ecommercecopa/service/UploadService.java`
- `src/main/java/br/ufc/smd/ecommercecopa/config/WebMvcConfig.java`

Convenções:

- Imagens são recebidas via `multipart/form-data`.
- Formatos aceitos: `image/jpeg`, `image/png` e `image/webp`.
- Arquivos ficam na pasta local definida por `app.upload-dir`.
- O backend serve arquivos por `/uploads/**`.
- O banco armazena apenas o caminho retornado, por exemplo `/uploads/products/arquivo.jpg`.
- A pasta `uploads/` é ignorada pelo Git.

Usos atuais:

- `PATCH /api/users/me/photo` salva foto de perfil em `/uploads/profiles/`.
- `POST /api/admin/products` e `PATCH /api/admin/products/{id}` salvam imagens das variantes/SKUs em `/uploads/products/`.

## 11. Documentação OpenAPI

O projeto usa Springdoc OpenAPI para gerar documentação interativa das rotas.

URLs:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

A configuração fica em `src/main/java/br/ufc/smd/ecommercecopa/config/OpenApiConfig.java`.

Rotas protegidas são documentadas com o esquema `sessionAuth`, baseado no cookie `JSESSIONID` criado após login.
