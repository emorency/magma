package org.obiba.magma.datasource.neo4j.converter;

import javax.annotation.Nullable;

import org.obiba.magma.Category;
import org.obiba.magma.MagmaRuntimeException;
import org.obiba.magma.Variable;
import org.obiba.magma.datasource.neo4j.domain.CategoryNode;
import org.obiba.magma.datasource.neo4j.domain.ValueTableNode;
import org.obiba.magma.datasource.neo4j.domain.VariableNode;

public class VariableConverter extends AttributeAwareConverter implements Neo4jConverter<VariableNode, Variable> {

  public static VariableConverter getInstance() {
    return new VariableConverter();
  }

  private VariableConverter() {
  }

  @Override
  public VariableNode marshal(Variable variable, Neo4jMarshallingContext context) {
    VariableNode variableNode = getNodeForVariable(variable, context);
    ValueTableNode valueTableNode = context.getValueTable();
    if(variableNode == null) {
      variableNode = new VariableNode(valueTableNode, variable);
      valueTableNode.getVariables().add(variableNode);
    } else {
      variableNode.copyVariableFields(variable);
    }

    if(variableNode.getValueType() != variable.getValueType()) {
      throw new MagmaRuntimeException(
          "Changing the value type of a variable is not supported. Cannot modify variable '" + variable.getName() +
              "' in table '" + valueTableNode.getName() + "'");
    }

    addAttributes(variable, variableNode);
    marshalCategories(variable, variableNode);

    return variableNode;
  }

  @Override
  public Variable unmarshal(VariableNode variableNode, Neo4jMarshallingContext context) {
    Variable.Builder builder = Variable.Builder
        .newVariable(variableNode.getName(), variableNode.getValueType(), variableNode.getEntityType());
    builder.mimeType(variableNode.getMimeType()).occurrenceGroup(variableNode.getOccurrenceGroup())
        .referencedEntityType(variableNode.getReferencedEntityType()).unit(variableNode.getUnit());
    if(variableNode.isRepeatable()) {
      builder.repeatable();
    }

    buildAttributeAware(builder, variableNode);
    unmarshalCategories(builder, variableNode);
    return builder.build();
  }

  @Nullable
  private VariableNode getNodeForVariable(Variable variable, Neo4jMarshallingContext context) {
    for(VariableNode node : context.getValueTable().getVariables()) {
      if(node.getName().equals(variable.getName())) return node;
    }
    return null;
  }

  private void marshalCategories(Variable variable, VariableNode variableNode) {
    for(Category category : variable.getCategories()) {
      CategoryNode categoryNode = variableNode.getCategory(category.getName());
      if(categoryNode == null) {
        variableNode.getCategories().add(new CategoryNode(category));
      } else {
        categoryNode.copyCategoryFields(category);
      }
    }
  }

  private void unmarshalCategories(Variable.Builder builder, VariableNode variableNode) {
    for(CategoryNode categoryNode : variableNode.getCategories()) {
      Category.Builder categoryBuilder = Category.Builder.newCategory(categoryNode.getName())
          .withCode(categoryNode.getCode()).missing(categoryNode.isMissing());
      buildAttributeAware(categoryBuilder, categoryNode);
      builder.addCategory(categoryBuilder.build());
    }
  }

}
