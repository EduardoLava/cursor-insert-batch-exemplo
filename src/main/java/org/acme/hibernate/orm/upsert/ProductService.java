package org.acme.hibernate.orm.upsert;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.TypePredicates;

@ApplicationScoped
public class ProductService {

  private static final Logger log = Logger.getLogger(ProductService.class.getName());
  private static final int BATCH_SIZE = 1000;
  private static final Predicate<Class<?>> HIBERNATE = TypePredicates.inPackage("org.hibernate");
  private static final Predicate<Field> CAMPOS_EXLUIR = campo -> campo.getName().contains("$")
      || campo.getName().contains("$$")
      || campo.getName().contains("hibernate_")
      || campo.getName().contains("id");

  @Inject
  ProductRepository productRepository;

  /**
   * Executa UPSERT usando hibernate e SQL nativo.
   * 
   * @param quantidade
   */
  @Transactional
  public void salvarOuAtualizarLoteViaSqlNativaHibernate(int quantidade) {
    log.log(Level.INFO, "Gerando {0} produtos aleatórios para UPSERT SQL Nativo em lote via Hibernate", quantidade);
    List<Product> produtos = gerarProdutosAleatorios(quantidade);
    productRepository.upsertEmLoteNativoComHibernate(produtos);
  }

  /**
   * Executa UPSERT em Lote para grande volume via JDBC Nativo
   */
  public void salvarOuAtualizarLoteViaJdbcNativo(int quantidade) {
    log.log(Level.INFO, "Gerando {0} produtos aleatórios para UPSERT JDBC em lote", quantidade);
    List<Product> produtos = gerarProdutosAleatorios(quantidade);
    productRepository.upsertEmLoteComJdbcNativo(produtos);
  }

  /**
   * Lote via ORM Hibernate Tradicional (Select -> Insert/Update + Flush/Clear)
   */
  @Transactional
  public void salvarOuAtualizarLoteComHibernateManaged(int quantidade) {
    log.log(Level.INFO, "Gerando {0} produtos para ORM Managed em lote via Hibernate", quantidade);
    List<Product> produtos = gerarProdutosAleatorios(quantidade);

    for (int i = 0; i < produtos.size(); i++) {
      Product p = produtos.get(i);

      // Busca pelo código único e atualiza ou insere
      productRepository.upsertComHibernate(p);

      if ((i + 1) % BATCH_SIZE == 0) {
        productRepository.flush();
        productRepository.getEntityManager().clear();
      }
    }
    productRepository.flush();
    productRepository.getEntityManager().clear();
  }

  private List<Product> gerarProdutosAleatorios(int quantidade) {
    EasyRandomParameters parameters = new EasyRandomParameters()
        .randomize(field -> field.getName().equals("id"), () -> null)
        .randomize(field -> field.getName().equals("code"), () -> "COD-" + (int) (Math.random() * (quantidade / 2))) // Força colisões para
                                                                                                                     // testar o UPDATE do
                                                                                                                     // UPSERT
        .stringLengthRange(5, 20)
        .excludeField(CAMPOS_EXLUIR)
        .excludeType(HIBERNATE);

    EasyRandom easyRandom = new EasyRandom(parameters);
    return easyRandom.objects(Product.class, quantidade)
        .toList();
  }
}
