package org.obiba.magma.datasource.csv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.obiba.magma.MagmaRuntimeException;
import org.obiba.magma.Value;
import org.obiba.magma.ValueSet;
import org.obiba.magma.ValueTableWriter;
import org.obiba.magma.Variable;
import org.obiba.magma.VariableEntity;
import org.obiba.magma.datasource.csv.converter.VariableConverter;

import au.com.bytecode.opencsv.CSVWriter;

public class CsvValueTableWriter implements ValueTableWriter {

  private final CsvValueTable valueTable;

  public CsvValueTableWriter(CsvValueTable valueTable) {
    this.valueTable = valueTable;
  }

  @Override
  public ValueSetWriter writeValueSet(VariableEntity entity) {
    return new CsvValueSetWriter(entity);
  }

  @Override
  public VariableWriter writeVariables() {
    return new CsvVariableWriter();
  }

  @Override
  public void close() throws IOException {
  }

  private class CsvVariableWriter implements VariableWriter {

    @Override
    public void writeVariable(Variable variable) {
      try {
        CSVWriter writer = valueTable.getVariableWriter();
        VariableConverter variableConverter = valueTable.getVariableConverter();
        if(valueTable.isVariablesFileEmpty()) {
          // Write Header
          writer.writeNext(variableConverter.getHeader());
          valueTable.setVariablesFileEmpty(false);
        } else if(valueTable.hasVariable(variable.getName())) {
          // doing an update.
          valueTable.clearVariable(variable);
        }

        String[] line = variableConverter.marshal(variable);
        long lastByte = valueTable.getVariablesLastByte();
        writer.writeNext(line);
        writer.close();
        valueTable.updateVariableIndex(variable, lastByte, line);
      } catch(IOException e) {
        throw new MagmaRuntimeException(e);
      }
    }

    @Override
    public void close() throws IOException {
      // Not used.
    }
  }

  private class CsvValueSetWriter implements ValueSetWriter {

    private final VariableEntity entity;

    private final CsvLine csvLine;

    public CsvValueSetWriter(VariableEntity entity) {
      this.entity = entity;
      csvLine = new CsvLine(entity);

      // Populate with existing values, if available
      if(valueTable.hasValueSet(entity)) {
        ValueSet valueSet = valueTable.getValueSet(entity);
        for(Variable variable : valueTable.getVariables()) {
          writeValue(variable, valueTable.getValue(variable, valueSet));
        }
      }
    }

    @Override
    public void writeValue(Variable variable, Value value) {
      csvLine.setValue(variable, value);
    }

    @Override
    public void close() throws IOException {

      CSVWriter writer = valueTable.getValueWriter();
      if(valueTable.getVariables().size() == 0) {
        // Write Header
        writer.writeNext(csvLine.getHeader());
      } else {
        // Test header is a subset
        List<String> extraHeaders = getExtraHeadersFromNewValueSet(getExistingHeaderMap(), csvLine.getHeaderMap());
        if(extraHeaders.size() != 0) {
          StringBuilder sb = new StringBuilder();
          for(String header : extraHeaders) {
            sb.append(header).append(" ");
          }
          throw new MagmaRuntimeException("Cannot update the CSV ValueTable [" + valueTable.getName() + "]. The new ValueSet (record) included the following unexpected Variables (fields): " + sb.toString());
        }

        if(valueTable.hasValueSet(entity)) {
          // Delete existing value set.
          valueTable.clearEntity(entity);
        }
        // Set existing header
        csvLine.setHeaderMap(getExistingHeaderMap());
      }

      // Writer Value set. Throw exception if doesn't match header
      long lastByte = valueTable.getDataLastByte();
      String[] line = csvLine.getLine();
      writer.writeNext(line);
      writer.close();
      // Update index
      valueTable.updateDataIndex(entity, lastByte, line);
    }

    private Map<String, Integer> getExistingHeaderMap() {
      if(valueTable.getDataHeaderMap() == null) {
        throw new MagmaRuntimeException("The header map for the data file does not exist.");
      }
      return valueTable.getDataHeaderMap();
    }

    private List<String> getExtraHeadersFromNewValueSet(Map<String, Integer> existingHeaderMap, Map<String, Integer> valueSetHeaderMap) {
      List<String> result = new ArrayList<String>();
      for(Map.Entry<String, Integer> entry : valueSetHeaderMap.entrySet()) {
        if(!existingHeaderMap.containsKey(entry.getKey())) {
          result.add(entry.getKey());
        }
      }
      return result;
    }
  }

}
