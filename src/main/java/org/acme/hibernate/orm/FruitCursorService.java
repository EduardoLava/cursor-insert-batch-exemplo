package org.acme.hibernate.orm;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.jpa.HibernateHints;

@ApplicationScoped
public class FruitCursorService {

  @Inject
  FruitRepository fruitRepository;

  @Inject
  FruitBatchService fruitBatchService;

  @ConfigProperty(name = "quarkus.hibernate-orm.jdbc.statement-batch-size")
  Integer tamanhoBatch;

  @Inject
  AgroalDataSource dataSource;

  /**
   * Leitura via JPA stream.
   */
  @Transactional
  public void processarComJpaStream() {
    try (Stream<Fruit> stream = fruitRepository.getEntityManager().createQuery("select f from Fruit f order by f.id", Fruit.class)
        .setHint(HibernateHints.HINT_FETCH_SIZE, tamanhoBatch)
        .setHint(HibernateHints.HINT_READ_ONLY, true)
        .getResultStream()) {

      stream.forEach(fruit -> {
        processarRegraDeNegocio(fruit);
        fruitRepository.getEntityManager().detach(fruit);
      });
    }
  }

  /**
   * Leitura via {@link ScrollableResults}.
   */
  @Transactional
  public void processarComScrollableResults() {
    Session session = fruitRepository.getEntityManager().unwrap(Session.class);

    try (ScrollableResults<Fruit> cursor = session.createQuery("select f from Fruit f order by f.id", Fruit.class)
        .setFetchSize(tamanhoBatch)
        .setReadOnly(true)
        .scroll(ScrollMode.FORWARD_ONLY)) {

      while (cursor.next()) {
        Fruit fruit = cursor.get();

        processarRegraDeNegocio(fruit);
        session.detach(fruit);
      }
    }
  }

  /**
   * Leitura via JDBC cursor (Desempenho máximo O(1) sem passar pelo ORM)
   */
  public void processarComJdbcCursor() {
    String sql = "SELECT id, name FROM fruit ORDER BY id";

    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      conn.setReadOnly(true);

      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setFetchSize(tamanhoBatch);

        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next()) {
            long id = rs.getLong("id");
            String name = rs.getString("name");

            processarRegraDeNegocioNativa(id, name);
          }
        }
      }
      conn.commit();
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao processar dados via JDBC Cursor", e);
    }
  }

  private void processarRegraDeNegocio(Fruit fruit) {
    // Lógica de negócio ou envio de mensagem
  }

  private void processarRegraDeNegocioNativa(long id, String name) {
    // Lógica de negócio nativa
  }
}
