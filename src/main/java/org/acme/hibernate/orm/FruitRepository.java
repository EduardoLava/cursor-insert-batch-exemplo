package org.acme.hibernate.orm;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import org.hibernate.StatelessSession;

@ApplicationScoped
public class FruitRepository implements PanacheRepositoryBase< Fruit, Integer> {

  @Inject
  StatelessSession statelessSession;
  
  @Transactional
  public void salvarComStateless(List<Fruit> frutas) {
    statelessSession.insertMultiple(frutas);
  }
}
