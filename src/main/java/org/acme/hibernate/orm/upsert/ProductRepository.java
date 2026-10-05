package org.acme.hibernate.orm.upsert;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

@ApplicationScoped
public class ProductRepository implements PanacheRepository<Product> {

  private static final Logger log = Logger.getLogger(ProductRepository.class.getName());

  @Inject
  EntityManager entityManager;

  @Inject
  DataSource dataSource;

  /**
   * Busca pela chave única (code). Se existir, atualiza os dados; caso contrário, persiste uma nova entidade.
   */
  public Product upsertComHibernate(Product product) {
    Optional<Product> existente = find("code", product.getCode()).firstResultOptional();

    if (existente.isPresent()) {
      Product entity = existente.get();
      entity.setName(product.getName());
      entity.setPrice(product.getPrice());
      return entity;
    } else {
      persist(product);
      return product;
    }
  }

  /**
   * Upsert em lote via sql nativa no hibernate/jpa. Para grandes volumes, executa o loop descarregando periodicamente no banco.
   */
  public void upsertEmLoteNativoComHibernate(List<Product> produtos) {
    String sql = """
        INSERT INTO product (id, codigo, name, price)
        VALUES (nextval('product_seq'), :code, :name, :price)
        ON CONFLICT (codigo)
        DO UPDATE SET
            name = EXCLUDED.name,
            price = EXCLUDED.price
        """;

    Query query = getEntityManager().createNativeQuery(sql);

    int count = 0;
    for (Product p : produtos) {
      query.setParameter("code", p.getCode());
      query.setParameter("name", p.getName());
      query.setParameter("price", p.getPrice());

      query.executeUpdate();

      if (++count % 1000 == 0) {
        getEntityManager().flush();
        getEntityManager().clear();
      }
    }
    getEntityManager().flush();
    getEntityManager().clear();
  }

  /**
   * Upsert em lote via jdbc nativo. Indicado para alta vazão / grandes volumes.
   */
  public void upsertEmLoteComJdbcNativo(List<Product> produtos) {
    String sql = """
        INSERT INTO product (id, codigo, name, price)
        VALUES (nextval('product_seq'), ?, ?, ?)
        ON CONFLICT (codigo)
        DO UPDATE SET
            name = EXCLUDED.name,
            price = EXCLUDED.price
        """;

    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);

      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        int count = 0;
        for (Product p : produtos) {
          stmt.setString(1, p.getCode());
          stmt.setString(2, p.getName());
          stmt.setBigDecimal(3, p.getPrice());
          stmt.addBatch();

          if (++count % 1000 == 0) {
            stmt.executeBatch();
          }
        }
        stmt.executeBatch();
      }
      conn.commit();
    } catch (SQLException e) {
      log.log(Level.SEVERE, "Erro ao realizar UPSERT em lote via JDBC", e);
      throw new RuntimeException("Falha no UPSERT JDBC em lote", e);
    }
  }
}
