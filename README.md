# Quarkus - Padrões de Alta Performance para Ingestão e Leitura em Lote (Batch & Cursor)

Este projeto de demonstração desenvolvido em **Quarkus** exemplifica técnicas avançadas de otimização para **ingestão massiva de dados**, **operações atômicas de UPSERT** e **leitura incremental de grandes volumes com memória constante O(1)**.

O objetivo principal é oferecer um comparativo prático de vazão (*throughput*), consumo de memória RAM e tempo de execução entre abordagens gerenciadas por ORM (JPA/Hibernate) e instruções nativas (JDBC puro e SQL Nativo `ON CONFLICT`).

---

## 🚀 Como Executar o Projeto Localmente

### Pré-requisitos

- **Java JDK 17+**
- **Maven 3.8+**
- Instância do **PostgreSQL** ativa (ou container Docker)

### Executando em Modo de Desenvolvimento (`quarkus:dev`)

Para iniciar a aplicação em modo de desenvolvimento com *Hot Reload* e a interface **Quarkus Dev UI** habilitada, execute o comando na raiz do projeto:

```
mvn quarkus:dev
```

Com o projeto em execução, você pode testar e interagir com a aplicação através dos links abaixo:

- **Aplicação Principal:** http://localhost:8080
- **Quarkus Dev UI:** http://localhost:8080/q/dev
- **Swagger UI (Interface para Rodar os Endpoints):** http://localhost:8080/q/swagger-ui/

> 💡 **Dica de Uso:** Utilize a interface interativa do **Swagger UI** (`http://localhost:8080/q/swagger-ui/`) para testar todos os endpoints de inserção, cursores e UPSERT diretamente pelo navegador, preenchendo os parâmetros `qtdRegistros` sem a necessidade de comandos via terminal.

---

## 🔍 Logs do SQL e Hibernate (`application.properties`)

No arquivo `src/main/resources/application.properties`, as propriedades para exibição e formatação dos SQLs gerados pelo Hibernate vêm **comentadas por padrão** para evitar o *overhead* de I/O de terminal durante os testes de carga e benchmarks de performance.

Caso deseje inspecionar as instruções SQL geradas em tempo de execução (`SELECT`, `INSERT`, `UPDATE` e comandos `ON CONFLICT`), basta **descomentar** as seguintes linhas:

```
Descomente as linhas abaixo para visualizar a geração de SQL nos logs do console:
#%dev.quarkus.hibernate-orm.log.sql=true
#%dev.quarkus.hibernate-orm.log.format-sql=true
#%dev.quarkus.log.category."org.hibernate.orm.jdbc.batch".level=TRACE
#%dev.quarkus.log.category."org.hibernate.orm.jdbc.batch".min-level=TRACE
```

> ⚠️ **Aviso de Performance:** Manter os logs de SQL ativados durante o processamento de centenas de milhares de registros reduz significativamente a vazão da aplicação. Mantenha-os desativados ao realizar medições oficiais de performance.

---

## 📌 Endpoints da Aplicação

A aplicação expõe recursos REST divididos em dois domínios principais: **Fruit** (focado em estratégias de leitura incremental) e **Product** (focado em estratégias de UPSERT em lote).

---

### 1. Domínio `Fruit` (`/fruits`) — Leitura Incremental & Inserções

Este recurso avalia o processamento em lote e a manutenção de memória estável O(1) utilizando cursores de banco de dados e controle de First-Level Cache (`detach`/`clear`).

#### 📤 Inserção em Lote (Geração de Massa com EasyRandom)

| Método | Endpoint | Parâmetro Query | Descrição |
| :--- | :--- | :--- | :--- |
| `POST` | `/fruits/inserir/jpa-flush` | `qtdRegistros` *(padrão: 1000)* | Persistência via JPA/Panache com controle de `flush()` e `clear()` a cada lote. |
| `POST` | `/fruits/inserir/stateless` | `qtdRegistros` *(padrão: 1000)* | Inserção direta sem First-Level Cache via Hibernate `StatelessSession`. |
| `POST` | `/fruits/inserir/jdbc` | `qtdRegistros` *(padrão: 1000)* | Inserção nativa com lote JDBC (`addBatch`/`executeBatch`). |

#### 📥 Leitura Incremental com Cursor (Memory Footprint O(1))

| Método | Endpoint | Parâmetro Query | Descrição |
| :--- | :--- | :--- | :--- |
| `POST` | `/fruits/cursor/jpa-stream` | `qtdRegistros` *(padrão: 10000)* | Salva a massa de dados e realiza a leitura streaming via `JPA Stream` desanexando entidades individualmente. |
| `POST` | `/fruits/cursor/scrollable-results` | `qtdRegistros` *(padrão: 10000)* | Salva a massa de dados e realiza a leitura via cursor `ScrollableResults` do Hibernate. |
| `POST` | `/fruits/cursor/jdbc` | `qtdRegistros` *(padrão: 10000)* | Salva a massa de dados e realiza a leitura streaming via `ResultSet` nativo com `setFetchSize()` habilitado. |

---

### 2. Domínio `Product` (`/products`) — Processamento em Lote com UPSERT

Este recurso compara o desempenho do padrão **UPSERT** (Inserir ou Atualizar em caso de colisão pela restrição única de negócio `codigo`) entre ORM traduzido e instruções nativas do SGBD.

#### 🔄 Lote de UPSERTs (Inserção/Atualização Massiva)

| Método | Endpoint | Parâmetro Query | Descrição |
| :--- | :--- | :--- | :--- |
| `POST` | `/products/upsert/lote/hibernate-native` | `qtdRegistros` *(padrão: 1000)* | UPSERT em lote executando comando SQL nativo (`INSERT ... ON CONFLICT DO UPDATE`) via `EntityManager.createNativeQuery()`. |
| `POST` | `/products/upsert/lote/jdbc` | `qtdRegistros` *(padrão: 1000)* | UPSERT em lote nativo executando `addBatch()`/`executeBatch()` diretamente via conexão JDBC e `PreparedStatement`. |
| `POST` | `/products/upsert/lote/hibernate-managed` | `qtdRegistros` *(padrão: 1000)* | UPSERT em lote tradicional via ORM (executa `SELECT` para verificar existência do código e em seguida dispara `INSERT` ou `UPDATE` gerenciado com controle de `flush()` e `clear()`). |

---

## 🧪 Exemplos de Execução dos Endpoints

Você pode acionar e benchmarkar as rotinas através da interface visual do **Swagger UI** (`http://localhost:8080/q/swagger-ui/`) ou via terminal com os comandos `curl` abaixo:

### Ingestão Massiva de Frutas (JDBC Native Batch)

```
curl -X POST "http://localhost:8080/fruits/inserir/jdbc?qtdRegistros=50000"
```

### Processamento com Cursor JPA Stream

```
curl -X POST "http://localhost:8080/fruits/cursor/jpa-stream?qtdRegistros=20000"
```

### Benchmark Comparativo de UPSERT em Lote (50.000 Produtos)

1. UPSERT via SQL Nativo no Hibernate
```
curl -X POST "http://localhost:8080/products/upsert/lote/hibernate-native?qtdRegistros=50000"
```

2. UPSERT via JDBC Nativo Puro
 ```
curl -X POST "http://localhost:8080/products/upsert/lote/jdbc?qtdRegistros=50000"
```

3. UPSERT via ORM Gerenciado (Select + Save/Update)
```
curl -X POST "http://localhost:8080/products/upsert/lote/hibernate-managed?qtdRegistros=50000"
```

---

## 💡 Principais Padrões de Arquitetura Demonstrados

1. **Memory Leak Prevention:** Uso criterioso de `entityManager.detach(entity)` e `session.clear()` durante a iteração de cursores para garantir estabilidade da memória Heap da JVM.
2. **Stateless Processing:** Ingestão desacoplada de First-Level Cache via `StatelessSession` e `createNativeQuery()`.
3. **Atomics no SGBD:** Eliminação de buscas prévias (`SELECT`) em cenários de chave única utilizando o comando SQL nativo `ON CONFLICT (codigo) DO UPDATE`.
4. **Respeito às Sequences do Banco:** Uso de `@SequenceGenerator` com `allocationSize = 100` alinhado à busca de IDs diretamente na sequence do banco (`nextval('fruit_seq')`).
