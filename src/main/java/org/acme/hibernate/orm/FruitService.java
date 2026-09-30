package org.acme.hibernate.orm;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.TypePredicates;

@ApplicationScoped
public class FruitService {

  private static final Predicate<Class<?>> HIBERNATE = TypePredicates.inPackage("org.hibernate");

  @Inject
  FruitRepository repository;

  @Inject
  AgroalDataSource dataSource;

  @ConfigProperty(name = "quarkus.hibernate-orm.jdbc.statement-batch-size")
  Integer tamanhoBatch;

  private static final Predicate<Field> CAMPOS_EXLUIR = campo -> campo.getName().contains("$")
      || campo.getName().contains("$$")
      || campo.getName().contains("hibernate_")
      || campo.getName().contains("id");

  @Transactional
  public void salvarComFlush(int qtdRegistrosGerar) {
    List<Fruit> frutas = gerarDadosAleatorios(qtdRegistrosGerar);

    for (int i = 0; i < frutas.size(); i++) {
      repository.persist(frutas.get(i));
      if (i > 0 && i % tamanhoBatch == 0) {
        repository.getEntityManager().flush();
        repository.getEntityManager().clear();
      }
    }

    repository.getEntityManager().flush();
    repository.getEntityManager().clear();
  }

  public void salvarComStateless(int qtdRegistrosGerar) {
    List<Fruit> frutas = gerarDadosAleatorios(qtdRegistrosGerar);

    List<Fruit> buffer = new ArrayList<>(tamanhoBatch);
    Iterator<Fruit> iterator = frutas.iterator();
    while (iterator.hasNext()) {
      buffer.add(iterator.next());

      if (buffer.size() == tamanhoBatch) {
        repository.salvarComStateless(buffer);
        buffer.clear();
      }
    }

    if (!buffer.isEmpty()) {
      repository.salvarComStateless(buffer);
    }
  }

  public void salvarComJdbc(int qtdRegistrosGerar) throws SQLException {
    List<Fruit> frutas = gerarDadosAleatorios(qtdRegistrosGerar);
    String sql = "INSERT INTO fruit (id, name) VALUES (nextval('fruit_seq'), ?)";

    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);

      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        int contador = 0;
        
        Iterator<Fruit> origemDados = frutas.iterator();
        while (origemDados.hasNext()) {
          Fruit fruit = origemDados.next();
          statement.setString(1, fruit.getName());

          statement.addBatch();
          contador++;

          if (contador % tamanhoBatch == 0) {
            statement.executeBatch();
            statement.clearBatch();
          }
        }

        statement.executeBatch();
        statement.clearBatch(); 
        connection.commit();
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      }
    }
  }

  private List<Fruit> gerarDadosAleatorios(int qtdRegistrosGerar) {
    return new EasyRandom(
        new EasyRandomParameters().stringLengthRange(10, 39)
            .excludeType(HIBERNATE)
            .excludeField(CAMPOS_EXLUIR)).objects(Fruit.class, qtdRegistrosGerar).toList();
  }
}
