package org.obiba.magma.datasource.generated;

import java.util.List;

import org.obiba.magma.Category;
import org.obiba.magma.Value;
import org.obiba.magma.Variable;

import com.google.common.collect.Lists;

class CategoricalValueGenerator extends AbstractMissingValueVariableValueGenerator {

  private final List<Category> dataCategories = Lists.newArrayList();

  CategoricalValueGenerator(Variable variable) {
    super(variable);
    for(Category c : variable.getCategories()) {
      if(!c.isMissing()) {
        dataCategories.add(c);
      }
    }
  }

  @Override
  protected Value nonMissingValue(Variable variable, GeneratedValueSet gvs) {
    if(dataCategories.isEmpty()) return variable.getValueType().nullValue();
    int c = gvs.valueGenerator.nextInt(dataCategories.size());
    return variable.getValueType().valueOf(dataCategories.get(c).getName());
  }

}
