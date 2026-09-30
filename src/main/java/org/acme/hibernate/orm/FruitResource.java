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
import java.sql.SQLException;
import java.util.logging.Logger;
import org.apache.commons.lang3.time.StopWatch;

@Path("frutas/gerar")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FruitResource {

  Logger log = Logger.getLogger(FruitResource.class.getName());
  
  @Inject
  FruitService fruitService;

  @POST
  @Path("jpa-flush")
  public void salvarComFlush(@QueryParam("qtdRegistros") @DefaultValue("1000") int qtdRegistros) {
    StopWatch cronometro = new StopWatch();
    cronometro.start();
    fruitService.salvarComFlush(qtdRegistros);
    cronometro.stop();
    log.info("Salvos %s registros em %s com flush".formatted(qtdRegistros, cronometro));
  }

  @POST
  @Path("stateless")
  public void salvarComStateless(@QueryParam("qtdRegistros") @DefaultValue("1000") int qtdRegistros) {
    StopWatch cronometro = new StopWatch();
    cronometro.start();
    fruitService.salvarComStateless(qtdRegistros);
    cronometro.stop();
    log.info("Salvos %s registros em %s com stateless".formatted(qtdRegistros, cronometro));
  }

  @POST
  @Path("jdbc")
  public void salvarComAddBatch(@QueryParam("qtdRegistros") @DefaultValue("1000") int qtdRegistros) throws SQLException {
    StopWatch cronometro = new StopWatch();
    cronometro.start();
    fruitService.salvarComJdbc(qtdRegistros);
    cronometro.stop();
    log.info("Salvos %s registros em %s com add batch".formatted(qtdRegistros, cronometro));
  }

}
