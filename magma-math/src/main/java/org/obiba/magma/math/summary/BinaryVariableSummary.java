/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.magma.math.summary;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.obiba.magma.Value;
import org.obiba.magma.ValueSet;
import org.obiba.magma.ValueSource;
import org.obiba.magma.ValueTable;
import org.obiba.magma.Variable;
import org.obiba.magma.VariableEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

/**
 *
 */
public class BinaryVariableSummary extends AbstractVariableSummary implements Serializable {

  private static final long serialVersionUID = 203198842420473154L;

  private static final Logger log = LoggerFactory.getLogger(BinaryVariableSummary.class);

  public static final String NULL_NAME = "N/A";

  public static final String NOT_NULL_NAME = "NOT_NULL";

  private final org.apache.commons.math3.stat.Frequency frequencyDist = new org.apache.commons.math3.stat.Frequency();

  private long n;

  private boolean empty = true;

  private final Collection<Frequency> frequencies = new ArrayList<>();

  private BinaryVariableSummary(@NotNull Variable variable) {
    super(variable);
  }

  @Override
  public String getCacheKey(ValueTable table) {
    return BinaryVariableSummaryFactory.getCacheKey(variable, table, getOffset(), getLimit());
  }

  @NotNull
  public Iterable<Frequency> getFrequencies() {
    return ImmutableList.copyOf(frequencies);
  }

  public long getN() {
    return n;
  }

  public boolean isEmpty() {
    return empty;
  }

  public static class Frequency implements Serializable {

    private static final long serialVersionUID = -2876592652764310324L;

    private final String value;

    private final long freq;

    private final double pct;

    public Frequency(String value, long freq, double pct) {
      this.value = value;
      this.freq = freq;
      this.pct = pct;
    }

    public String getValue() {
      return value;
    }

    public long getFreq() {
      return freq;
    }

    public double getPct() {
      return pct;
    }
  }

  @SuppressWarnings("ParameterHidesMemberVariable")
  public static class Builder implements VariableSummaryBuilder<BinaryVariableSummary, Builder> {

    private final BinaryVariableSummary summary;

    @NotNull
    private final Variable variable;

    private boolean addedTable;

    private boolean addedValue;

    public Builder(@NotNull Variable variable) {
      this.variable = variable;
      summary = new BinaryVariableSummary(variable);
    }

    @Override
    public Builder addValue(@NotNull Value value) {
      if(addedTable) {
        throw new IllegalStateException("Cannot add value for variable " + summary.variable.getName() +
            " because values where previously added from the whole table with addTable().");
      }
      add(value);
      addedValue = true;
      return this;
    }

    @Override
    public Builder addTable(@NotNull ValueTable table, @NotNull ValueSource valueSource) {
      if(addedValue) {
        throw new IllegalStateException("Cannot add table for variable " + summary.variable.getName() +
            " because values where previously added with addValue().");
      }
      add(table, valueSource);
      addedTable = true;

      return this;
    }

    private void add(@NotNull final ValueTable table, @NotNull ValueSource variableValueSource) {
      //noinspection ConstantConditions
      Preconditions.checkArgument(table != null, "table cannot be null");
      //noinspection ConstantConditions
      Preconditions.checkArgument(variableValueSource != null, "variableValueSource cannot be null");

      Iterable<Value> values = Iterables.transform(table.getVariableEntities(), new Function<VariableEntity, Value>() {
        @Nullable
        @Override
        public Value apply(@Nullable VariableEntity input) {
          ValueSet valueSet = table.getValueSet(input);
          Value value = table.getValue(variable, valueSet);
          return value;
        }
      });

      for(Value value : values) {
        add(value);
      }
    }

    private void add(@NotNull Value value) {
      //noinspection ConstantConditions
      Preconditions.checkArgument(value != null, "value cannot be null");

      if(summary.empty) summary.empty = false;
      if(value.isSequence()) {
        if(value.isNull()) {
          summary.frequencyDist.addValue(NULL_NAME);
        } else {
          for(Value v : value.asSequence().getValue()) {
            add(v);
          }
        }
      } else {
        summary.frequencyDist.addValue(value.isNull() ? NULL_NAME : NOT_NULL_NAME);
      }
    }

    /**
     * Returns an iterator of frequencyDist names
     */
    private Iterator<String> freqNames(org.apache.commons.math3.stat.Frequency freq) {
      return Iterators.transform(freq.valuesIterator(), new Function<Comparable<?>, String>() {

        @Override
        public String apply(Comparable<?> input) {
          return input.toString();
        }
      });
    }

    private void compute() {
      log.trace("Start compute default summary {}", summary.variable);
      long max = 0;
      Iterator<String> concat = freqNames(summary.frequencyDist);

      // Iterate over all category names including or not distinct values.
      // The loop will also determine the mode of the distribution (most frequent value)
      while(concat.hasNext()) {
        String value = concat.next();
        long count = summary.frequencyDist.getCount(value);
        if(count > max) {
          max = count;
        }
        summary.frequencies.add(new Frequency(value, summary.frequencyDist.getCount(value),
            Double.isNaN(summary.frequencyDist.getPct(value)) ? 0.0 : summary.frequencyDist.getPct(value)));
      }
      summary.n = summary.frequencyDist.getSumFreq();
    }

    public Builder filter(Integer offset, Integer limit) {
      summary.setOffset(offset);
      summary.setLimit(limit);
      return this;
    }

    @Override
    @NotNull
    public BinaryVariableSummary build() {
      compute();
      return summary;
    }

    @NotNull
    @Override
    public Variable getVariable() {
      return variable;
    }

  }

}