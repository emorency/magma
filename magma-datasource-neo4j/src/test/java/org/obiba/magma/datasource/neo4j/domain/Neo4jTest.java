/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.magma.datasource.neo4j.domain;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.obiba.magma.datasource.neo4j.repository.DatasourceRepository;
import org.obiba.magma.datasource.neo4j.repository.ValueTableRepository;
import org.obiba.magma.type.TextType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/application-context-test-neo4j.xml")
@Transactional
public class Neo4jTest {

  private static final Logger log = LoggerFactory.getLogger(Neo4jTest.class);

  public static final String DS_NAME = "ds1";

  public static final String TABLE_NAME = "table1";

  public static final String PARTICIPANT = "Participant";

  public static final String VAR_NAME = "var1";

  @Autowired
  private Neo4jTemplate template;

  @Autowired
  private DatasourceRepository datasourceRepository;

  @Autowired
  private ValueTableRepository valueTableRepository;

  @Test
  public void persistedDatasourceShouldBeRetrievableFromGraphDb() {
    DatasourceNode datasource = createDatasource();
    DatasourceNode retrievedDatasource = template.findOne(datasource.getGraphId(), DatasourceNode.class);
    assertThat(retrievedDatasource, is(datasource));
  }

  @Test
  public void canFindDatasourceByName() {
    DatasourceNode datasource = createDatasource();
    DatasourceNode retrievedDatasource = datasourceRepository.findByName(DS_NAME);
    assertThat(retrievedDatasource, is(datasource));
  }

  @SuppressWarnings("ReuseOfLocalVariable")
  @Test
  public void canAddAndQueryTablesToDatasource() {
    DatasourceNode datasource = createDatasource();
    ValueTableNode table = createTable(datasource);
    ValueTableNode retrievedTable = template.findOne(table.getGraphId(), ValueTableNode.class);
    assertThat(retrievedTable, is(table));
    assertThat(retrievedTable.getDatasource(), is(datasource));

    retrievedTable = valueTableRepository.findByDatasourceAndName(datasource.getName(), TABLE_NAME);
    assertThat(retrievedTable, is(table));
    assertThat(retrievedTable.getDatasource(), is(datasource));

    retrievedTable = valueTableRepository.findByDatasourceAndName(datasource, TABLE_NAME);
    assertThat(retrievedTable, is(table));
    assertThat(retrievedTable.getDatasource(), is(datasource));

    DatasourceNode retrievedDatasource = template.findOne(datasource.getGraphId(), DatasourceNode.class);
    assertThat(retrievedDatasource.getValueTables().contains(retrievedTable), is(true));
  }

  @Test
  public void canAddVariablesToTable() {
    DatasourceNode datasource = createDatasource();
    ValueTableNode table = createTable(datasource);
    VariableNode variable = createVariable(table);
    VariableNode retrievedVariable = template.findOne(variable.getGraphId(), VariableNode.class);
    assertThat(retrievedVariable, is(variable));
    assertThat(retrievedVariable.getValueTable(), is(table));
  }

  @Test
  public void canAddValue() {
    DatasourceNode datasource = createDatasource();
    ValueTableNode table = createTable(datasource);
    VariableNode variable = createVariable(table);
    VariableEntityNode entity = template.save(new VariableEntityNode("1", PARTICIPANT));
    ValueNode value = template.save(new ValueNode(TextType.Factory.newValue("value1")));
    ValueSetNode valueSet = createValueSet(table, entity);
    ValueSetValueNode valueSetValue = createValueSetValue(variable, value, valueSet);
  }

  private DatasourceNode createDatasource() {
    return template.save(new DatasourceNode(DS_NAME));
  }

  private ValueTableNode createTable(DatasourceNode datasource) {
    return template.save(new ValueTableNode(TABLE_NAME, PARTICIPANT, datasource));
  }

  private VariableNode createVariable(ValueTableNode table) {
    VariableNode variable = new VariableNode();
    variable.setName(VAR_NAME);
    variable.setEntityType(PARTICIPANT);
    variable.setValueTable(table);
    return template.save(variable);
  }

  private ValueSetNode createValueSet(ValueTableNode table, VariableEntityNode entity) {
    ValueSetNode valueSet = new ValueSetNode();
    valueSet.setValueTable(table);
    valueSet.setVariableEntity(entity);
    return template.save(valueSet);
  }

  private ValueSetValueNode createValueSetValue(VariableNode variable, ValueNode value, ValueSetNode valueSet) {
    ValueSetValueNode valueSetValue = new ValueSetValueNode();
    valueSetValue.setValue(value);
    valueSetValue.setValueSet(valueSet);
    valueSetValue.setVariable(variable);
    return template.save(valueSetValue);
  }

}
