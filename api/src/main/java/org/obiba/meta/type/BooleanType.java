package org.obiba.meta.type;

import java.lang.ref.WeakReference;

import javax.xml.namespace.QName;

import org.obiba.meta.MetaEngine;
import org.obiba.meta.ValueType;

public class BooleanType implements ValueType {

  private static final long serialVersionUID = -149385659514790222L;

  private static final WeakReference<BooleanType> instance = MetaEngine.get().registerInstance(new BooleanType());

  private BooleanType() {

  }

  public static BooleanType get() {
    return instance.get();
  }

  @Override
  public boolean isDateTime() {
    return false;
  }

  @Override
  public boolean isNumeric() {
    return false;
  }

  @Override
  public Class<?> getJavaClass() {
    return Boolean.class;
  }

  @Override
  public String getName() {
    return "boolean";
  }

  @Override
  public QName getXsdType() {
    return new QName("xsd", "boolean");
  }

  @Override
  public boolean acceptsJavaClass(Class<?> clazz) {
    return Boolean.class.isAssignableFrom(clazz) || boolean.class.isAssignableFrom(clazz);
  }
}