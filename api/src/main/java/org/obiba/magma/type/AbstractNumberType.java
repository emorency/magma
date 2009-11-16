package org.obiba.magma.type;

import org.obiba.magma.Value;

public abstract class AbstractNumberType extends AbstractValueType {

  private static final long serialVersionUID = -5271259966499174607L;

  protected AbstractNumberType() {

  }

  public boolean isDateTime() {
    return false;
  }

  public boolean isNumeric() {
    return true;
  }

  @Override
  public String toString(Value value) {
    return value.isNull() ? null : value.getValue().toString();
  }
}