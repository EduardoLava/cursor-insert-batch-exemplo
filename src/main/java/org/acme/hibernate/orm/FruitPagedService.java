package org.acme.hibernate.orm;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class FruitPagedService {

  @Inject
  FruitRepository fruitRepository;

  /**
   * Realiza a busca paginada no banco de dados utilizando Panache.
   * 
   * @param paginaNumero Número da página desejada (0-indexed, ex: 0 para primeira página)
   * @param tamanhoPagina Quantidade de registros por página
   * @return DTO/Map contendo a lista de frutas e os metadados da paginação
   */
  public Map<String, Object> buscarPaginado(int paginaNumero, int tamanhoPagina) {
    Page pagina = Page.of(paginaNumero, tamanhoPagina);

    List<Fruit> frutas = fruitRepository.findAll(Sort.by("id"))
        .page(pagina)
        .list();

    long totalRegistros = fruitRepository.count();
    int totalPaginas = fruitRepository.findAll().page(pagina).pageCount();

    return Map.of(
        "conteudo", frutas,
        "paginaAtual", paginaNumero,
        "tamanhoPagina", tamanhoPagina,
        "totalRegistros", totalRegistros,
        "totalPaginas", totalPaginas);
  }
  
  /**
   * Realiza a busca paginada via Keyset (Seek-Based).
   *
   * @param lastId O último ID recebido da página anterior (null na primeira chamada)
   * @param size Quantidade de registros a retornar por página
   * @return Conteúdo da página e o próximo 'lastId' para o cursor do cliente
   */
  public Map<String, Object> buscarComKeyset(Integer lastId, int size) {
      List<Fruit> frutas;

      Sort ordenacao = Sort.by("id").ascending();
      if (lastId == null || lastId <= 0) {
          frutas = fruitRepository.findAll(ordenacao)
                  .page(0, size)
                  .list();
      } else {
          frutas = fruitRepository.find("id > ?1", ordenacao, lastId)
                  .page(0, size)
                  .list();
      }

      Integer proximoLastId = frutas.isEmpty() ? null : frutas.get(frutas.size() - 1).getId();

      return Map.of(
          "conteudo", frutas,
          "tamanhoPagina", frutas.size(),
          "proximoLastId", proximoLastId != null ? proximoLastId : "null"
      );
  }
}
