package org.obiba.magma.views;

import org.obiba.magma.Datasource;
import org.obiba.magma.Initialisable;
import org.obiba.magma.NoSuchValueSetException;
import org.obiba.magma.NoSuchVariableException;
import org.obiba.magma.Value;
import org.obiba.magma.ValueSet;
import org.obiba.magma.ValueTable;
import org.obiba.magma.ValueType;
import org.obiba.magma.Variable;
import org.obiba.magma.VariableEntity;
import org.obiba.magma.VariableValueSource;
import org.obiba.magma.support.AbstractValueTableWrapper;
import org.obiba.magma.support.Initialisables;
import org.obiba.magma.views.support.AllClause;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class View extends AbstractValueTableWrapper implements Initialisable {
  //
  // Instance Variables
  //

  private ViewAwareDatasource viewDatasource;

  private String name;

  private ValueTable from;

  private SelectClause select;

  private WhereClause where;

  //
  // Constructors
  //

  /**
   * No-arg constructor for XStream.
   */
  public View() {

  }

  public View(String name, ValueTable from, SelectClause selectClause, WhereClause whereClause) {
    this.name = name;
    this.from = from;

    setSelectClause(selectClause);
    setWhereClause(whereClause);
  }

  public View(String name, ValueTable from) {
    this(name, from, new AllClause(), new AllClause());
  }

  //
  // Initialisable Methods
  //

  public void initialise() {
    Initialisables.initialise(select, where);
  }

  //
  // AbstractValueTableWrapper Methods
  //

  @Override
  public Datasource getDatasource() {
    return viewDatasource;
  }

  public ValueTable getWrappedValueTable() {
    return from;
  }

  public boolean hasValueSet(VariableEntity entity) {
    boolean hasValueSet = super.hasValueSet(entity);
    if(hasValueSet) {
      ValueSet valueSet = super.getValueSet(entity);
      hasValueSet = where.where(valueSet);
    }
    return hasValueSet;
  }

  public Iterable<ValueSet> getValueSets() {
    // Get a ValueSet Iterable, taking into account the WhereClause.
    Iterable<ValueSet> valueSets = super.getValueSets();
    Iterable<ValueSet> filteredValueSets = Iterables.filter(valueSets, new Predicate<ValueSet>() {
      public boolean apply(ValueSet input) {
        return where.where(input);
      }
    });

    // Transform the Iterable, replacing each ValueSet with one that points at the current View.
    Iterable<ValueSet> viewValueSets = Iterables.transform(filteredValueSets, getValueSetTransformer());

    return viewValueSets;
  }

  public ValueSet getValueSet(VariableEntity entity) throws NoSuchValueSetException {
    ValueSet valueSet = super.getValueSet(entity);
    if(!where.where(valueSet)) {
      throw new NoSuchValueSetException(this, entity);
    }

    return getValueSetTransformer().apply(valueSet);
  }

  public Iterable<Variable> getVariables() {
    Iterable<Variable> variables = super.getVariables();
    Iterable<Variable> filteredVariables = Iterables.filter(variables, new Predicate<Variable>() {
      public boolean apply(Variable input) {
        return select.select(input);
      }
    });

    return filteredVariables;
  }

  @Override
  public Variable getVariable(String name) throws NoSuchVariableException {
    Variable variable = super.getVariable(name);
    if(select.select(variable)) {
      return variable;
    } else {
      throw new NoSuchVariableException(name);
    }
  }

  @Override
  public Value getValue(Variable variable, ValueSet valueSet) {
    if(!where.where(valueSet)) {
      throw new NoSuchValueSetException(this, valueSet.getVariableEntity());
    }

    return super.getValue(variable, ((ValueSetWrapper) valueSet).getWrappedValueSet());
  }

  @Override
  public VariableValueSource getVariableValueSource(String name) throws NoSuchVariableException {
    // Call getVariable(name) to check the SelectClause (if there is one). If the specified variable
    // is not selected by the SelectClause, this will result in a NoSuchVariableException.
    getVariable(name);

    // Variable "survived" the SelectClause. Go ahead and call the base class method.
    return getVariableValueSourceTransformer().apply(super.getVariableValueSource(name));
  }

  //
  // Methods
  //

  public void setDatasource(ViewAwareDatasource datasource) {
    this.viewDatasource = datasource;
  }

  public String getName() {
    return name;
  }

  public void setSelectClause(SelectClause selectClause) {
    if(selectClause == null) {
      throw new IllegalArgumentException("null selectClause");
    }
    this.select = selectClause;
  }

  public void setWhereClause(WhereClause whereClause) {
    if(whereClause == null) {
      throw new IllegalArgumentException("null whereClause");
    }
    this.where = whereClause;
  }

  public Function<VariableEntity, VariableEntity> getVariableEntityTransformer() {
    return new Function<VariableEntity, VariableEntity>() {
      public VariableEntity apply(VariableEntity from) {
        return from;
      }
    };
  }

  public Function<ValueSet, ValueSet> getValueSetTransformer() {
    return new Function<ValueSet, ValueSet>() {
      public ValueSet apply(ValueSet from) {
        return new ValueSetWrapper(View.this, from);
      }
    };
  }

  public Function<VariableValueSource, VariableValueSource> getVariableValueSourceTransformer() {
    return new Function<VariableValueSource, VariableValueSource>() {
      public VariableValueSource apply(VariableValueSource from) {
        return new VariableValueSourceWrapper(from);
      }
    };
  }

  //
  // ValueSetWrapper
  //

  static class VariableValueSourceWrapper implements VariableValueSource {
    private VariableValueSource wrapped;

    VariableValueSourceWrapper(VariableValueSource wrapped) {
      this.wrapped = wrapped;
    }

    @Override
    public Variable getVariable() {
      return wrapped.getVariable();
    }

    @Override
    public Value getValue(ValueSet valueSet) {
      return wrapped.getValue(((ValueSetWrapper) valueSet).getWrappedValueSet());
    }

    @Override
    public ValueType getValueType() {
      return wrapped.getValueType();
    }
  }

  //
  // Builder
  //

  public static class Builder {

    private View view;

    public Builder(String name, ValueTable from) {
      view = new View(name, from);
    }

    public static Builder newView(String name, ValueTable from) {
      return new Builder(name, from);
    }

    public Builder select(SelectClause selectClause) {
      view.setSelectClause(selectClause);
      return this;
    }

    public Builder where(WhereClause whereClause) {
      view.setWhereClause(whereClause);
      return this;
    }

    public View build() {
      return view;
    }
  }

}