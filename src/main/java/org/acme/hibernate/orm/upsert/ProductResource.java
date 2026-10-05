package org.acme.hibernate.orm.upsert;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.time.StopWatch;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

  private static final Logger log = Logger.getLogger(ProductResource.class.getName());

  @Inject
  ProductService productService;

  /**
   * Lote via SQL Nativo (ON CONFLICT) executado pelo Hibernate EntityManager
   */
  @POST
  @Path("upsert/lote/hibernate-native")
  public Response upsertLoteHibernateNative(@QueryParam("qtdRegistros") @DefaultValue("1000") int qtdRegistros) {
    StopWatch cronometro = StopWatch.createStarted();
    productService.salvarOuAtualizarLoteViaSqlNativaHibernate(qtdRegistros);
    cronometro.stop();

    log.log(Level.INFO, "UPSERT em lote de {0} produtos via Hibernate Native Query concluído em {1}",
        new Object[] {qtdRegistros, cronometro});

    return Response.ok(Map.of(
        "estrategia", "SQL Nativo via Hibernate (createNativeQuery ON CONFLICT Batch)",
        "quantidade", qtdRegistros,
        "tempoExecucao", cronometro.toString())).build();
  }

  /**
   * Lote via JDBC Nativo Puro (PreparedStatement.addBatch / executeBatch)
   */
  @POST
  @Path("upsert/lote/jdbc")
  public Response upsertLoteJdbc(@QueryParam("qtdRegistros") @DefaultValue("1000") int qtdRegistros) {
    StopWatch cronometro = StopWatch.createStarted();
    productService.salvarOuAtualizarLoteViaJdbcNativo(qtdRegistros);
    cronometro.stop();

    log.log(Level.INFO, "UPSERT em lote de {0} produtos via JDBC Nativo concluído em {1}",
        new Object[] {qtdRegistros, cronometro});

    return Response.ok(Map.of(
        "estrategia", "JDBC Nativo Puro (addBatch / executeBatch ON CONFLICT)",
        "quantidade", qtdRegistros,
        "tempoExecucao", cronometro.toString())).build();
  }

  /**
   * Lote via ORM Gerenciado do Hibernate (SELECT prévio por código -> INSERT ou UPDATE com Flush/Clear)
   */
  @POST
  @Path("upsert/lote/hibernate-managed")
  public Response upsertLoteHibernateManaged(@QueryParam("qtdRegistros") @DefaultValue("1000") int qtdRegistros) {
    StopWatch cronometro = StopWatch.createStarted();
    productService.salvarOuAtualizarLoteComHibernateManaged(qtdRegistros);
    cronometro.stop();

    log.log(Level.INFO, "Salvar/Atualizar em lote de {0} produtos via ORM Hibernate concluidos em {1}",
        new Object[] {qtdRegistros, cronometro});

    return Response.ok(Map.of(
        "estrategia", "Hibernate Managed ORM Batch (Select + Insert/Update com Flush/Clear)",
        "quantidade", qtdRegistros,
        "tempoExecucao", cronometro.toString())).build();
  }
}
