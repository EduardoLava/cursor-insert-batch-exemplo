package org.acme.hibernate.orm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.time.StopWatch;

@Path("fruits")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FruitResource {

  Logger log = Logger.getLogger(FruitResource.class.getName());
  
  @Inject
  FruitBatchService fruitBatchService;
  
  @Inject
  FruitCursorService fruitCursorService;

  @POST
  @Path("inserir/jpa-flush")
  public Response salvarComFlush(@QueryParam("qtdRegistros") @DefaultValue("1000") int qtdRegistros) {
      StopWatch cronometro = StopWatch.createStarted();
      fruitBatchService.salvarComFlush(qtdRegistros);
      cronometro.stop();

      log.log(Level.INFO, "Salvos {0} registros em {1} com JPA Flush", new Object[]{qtdRegistros, cronometro});

      return Response.ok(Map.of(
          "estrategiaInsercao", "JPA Flush + Clear",
          "quantidade", qtdRegistros,
          "tempoExecucao", cronometro.toString()
      )).build();
  }

  @POST
  @Path("inserir/stateless")
  public Response salvarComStateless(@QueryParam("qtdRegistros") @DefaultValue("1000") int qtdRegistros) {
      StopWatch cronometro = StopWatch.createStarted();
      fruitBatchService.salvarComStateless(qtdRegistros);
      cronometro.stop();

      log.log(Level.INFO, "Salvos {0} registros em {1} com StatelessSession", new Object[]{qtdRegistros, cronometro});

      return Response.ok(Map.of(
          "estrategiaInsercao", "Hibernate StatelessSession",
          "quantidade", qtdRegistros,
          "tempoExecucao", cronometro.toString()
      )).build();
  }

  @POST
  @Path("inserir/jdbc")
  public Response salvarComAddBatch(@QueryParam("qtdRegistros") @DefaultValue("1000") int qtdRegistros) throws SQLException {
      StopWatch cronometro = StopWatch.createStarted();
      fruitBatchService.salvarComJdbc(qtdRegistros);
      cronometro.stop();

      log.log(Level.INFO, "Salvos {0} registros em {1} com JDBC AddBatch", new Object[]{qtdRegistros, cronometro});

      return Response.ok(Map.of(
          "estrategiaInsercao", "JDBC Native Batch (addBatch/executeBatch)",
          "quantidade", qtdRegistros,
          "tempoExecucao", cronometro.toString()
      )).build();
  }
  
  @POST
  @Path("cursor/jdbc")
  public Response processarComCursorJdbc(@QueryParam("qtdRegistros") @DefaultValue("10000") int qtdRegistros) throws SQLException {
      log.log(Level.INFO, "Iniciando persistência de {0} registros via JDBC", qtdRegistros);
      fruitBatchService.salvarComJdbc(qtdRegistros);

      log.info("Fim persistência, iniciando busca com cursor via JDBC puro");
      StopWatch cronometro = StopWatch.createStarted();
      fruitCursorService.processarComJdbcCursor();
      cronometro.stop();

      log.log(Level.INFO, "Fim processamento utilizando cursor via JDBC em {0}", cronometro);

      return Response.ok(Map.of(
          "estrategia", "JDBC Nativo",
          "quantidade", qtdRegistros,
          "tempoExecucao", cronometro.toString()
      )).build();
  }

  @POST
  @Path("cursor/scrollable-results")
  public Response processarComScroll(@QueryParam("qtdRegistros") @DefaultValue("10000") int qtdRegistros) {
      log.log(Level.INFO, "Iniciando persistência de {0} registros via StatelessSession", qtdRegistros);
      fruitBatchService.salvarComStateless(qtdRegistros);

      log.info("Fim persistência, iniciando busca com cursor via Scrollable Results");
      StopWatch cronometro = StopWatch.createStarted();
      fruitCursorService.processarComScrollableResults();
      cronometro.stop();

      log.log(Level.INFO, "Fim processamento utilizando cursor via Scrollable Results em {0}", cronometro);

      return Response.ok(Map.of(
          "estrategia", "Scrollable Results (Hibernate)",
          "quantidade", qtdRegistros,
          "tempoExecucao", cronometro.toString()
      )).build();
  }

  @POST
  @Path("cursor/jpa-stream")
  public Response processarComJpaStream(@QueryParam("qtdRegistros") @DefaultValue("10000") int qtdRegistros) {
      log.log(Level.INFO, "Iniciando persistência de {0} registros via JPA Flush", qtdRegistros);
      fruitBatchService.salvarComFlush(qtdRegistros);

      log.info("Fim persistência, iniciando busca com cursor via JPA Stream");
      StopWatch cronometro = StopWatch.createStarted();
      fruitCursorService.processarComJpaStream();
      cronometro.stop();

      log.log(Level.INFO, "Fim processamento utilizando cursor via JPA Stream em {0}", cronometro);

      return Response.ok(Map.of(
          "estrategia", "JPA Stream",
          "quantidade", qtdRegistros,
          "tempoExecucao", cronometro.toString()
      )).build();
  }
}
