package org.obiba.meta;

import java.util.Set;

public interface VariableValueSourceFactory {

  public Set<VariableValueSource> createSources(ValueSetReferenceResolver connector);

}