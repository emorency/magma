package org.obiba.magma;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.obiba.magma.support.Disposables;
import org.obiba.magma.support.Initialisables;
import org.obiba.magma.support.ValueTableReference;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public class DefaultDatasourceRegistry implements DatasourceRegistry, Disposable {

  private Set<Datasource> datasources = Sets.newHashSet();

  private Set<DatasourceFactory> transientDatasources = Sets.newHashSet();

  private Set<Decorator<Datasource>> decorators = Sets.newHashSet();

  public void dispose() {
    for(Datasource ds : datasources) {
      Disposables.silentlyDispose(ds);
    }
    for(Decorator<Datasource> decorator : decorators) {
      Disposables.silentlyDispose(decorator);
    }
  }

  @Override
  public ValueTableReference createReference(String reference) {
    return new ValueTableReference(reference);
  }

  public Set<Datasource> getDatasources() {
    return ImmutableSet.copyOf(datasources);
  }

  public Datasource getDatasource(final String name) throws NoSuchDatasourceException {
    if(name == null) throw new IllegalArgumentException("name cannot be null");
    try {
      return Iterables.find(datasources, new Predicate<Datasource>() {
        @Override
        public boolean apply(Datasource input) {
          return name.equals(input.getName());
        }
      });
    } catch(NoSuchElementException e) {
      throw new NoSuchDatasourceException(name);
    }
  }

  public boolean hasDatasource(final String name) {
    for(Datasource d : datasources) {
      if(d.getName().equals(name)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void addDecorator(Decorator<Datasource> decorator) {
    if(decorator == null) throw new MagmaRuntimeException("decorator cannot be null.");
    Initialisables.initialise(decorator);
    decorators.add(decorator);

    // TODO: decorate existing datasources
  }

  public Datasource addDatasource(Datasource datasource) {
    // Repeatedly added datasources are silently ignored. They cannot be added to the set more than once.
    if(!datasources.contains(datasource)) {
      for(Datasource ds : datasources) {
        if(ds.getName().equals(datasource.getName())) {
          // Unique datasources with identical names cause exceptions.
          throw new DuplicateDatasourceNameException(ds, datasource);
        }
      }

      for(Decorator<Datasource> decorator : decorators) {
        datasource = decorator.decorate(datasource);
      }

      Initialisables.initialise(datasource);
      datasources.add(datasource);
    }
    return datasource;
  }

  public Datasource addDatasource(final DatasourceFactory factory) {
    Initialisables.initialise(factory);
    return addDatasource(factory.create());
  }

  public void removeDatasource(final Datasource datasource) {
    datasources.remove(datasource);
    Disposables.dispose(datasource);
  }

  /**
   * Register a new transient datasource.
   * @param factory
   * @return a unique identifier that can be used to obtain the registered factory
   */
  public String addTransientDatasource(final DatasourceFactory factory) {
    String uid = randomTransientDatasourceName();
    while(hasTransientDatasource(uid)) {
      uid = randomTransientDatasourceName();
    }

    factory.setName(uid);
    Initialisables.initialise(factory);
    transientDatasources.add(factory);

    return factory.getName();
  }

  /**
   * Check if a transient datasource is registered with given identifier.
   * @param uid
   * @return true when uid is associated with a DatasourceFactory instance
   */
  public boolean hasTransientDatasource(final String uid) {
    return getTransientDatasource(uid) != null;
  }

  /**
   * Remove the transient datasource identified by uid (ignore if none is found).
   * @param uid
   */
  public void removeTransientDatasource(final String uid) {
    DatasourceFactory factory = getTransientDatasource(uid);
    if(factory != null) {
      transientDatasources.remove(factory);
    }
  }

  /**
   * Returns a new (initialized) instance of Datasource obtained by calling DatasourceFactory.create() associated with
   * uid.
   * @param uid
   * @return datasource item
   */
  public Datasource getTransientDatasourceInstance(final String uid) {
    DatasourceFactory factory = getTransientDatasource(uid);
    Datasource datasource = null;
    if(factory != null) {
      datasource = factory.create();
      Initialisables.initialise(datasource);
    } else {
      throw new NoSuchDatasourceException(uid);
    }

    for(Decorator<Datasource> decorator : decorators) {
      datasource = decorator.decorate(datasource);
    }
    return datasource;
  }

  /**
   * Generate a random name.
   * @return
   */
  @VisibleForTesting
  String randomTransientDatasourceName() {
    return UUID.randomUUID().toString();
  }

  /**
   * Look for a datasource factory with given identifier.
   * @param uid
   * @return null if not found
   */
  private DatasourceFactory getTransientDatasource(final String uid) {
    if(uid == null) throw new IllegalArgumentException("uid cannot be null.");
    DatasourceFactory foundFactory = null;
    for(DatasourceFactory factory : transientDatasources) {
      if(factory.getName().equals(uid)) {
        foundFactory = factory;
        break;
      }
    }
    return foundFactory;
  }
}