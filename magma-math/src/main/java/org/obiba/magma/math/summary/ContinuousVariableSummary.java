/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.magma.math.summary;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.obiba.magma.Category;
import org.obiba.magma.MagmaRuntimeException;
import org.obiba.magma.Value;
import org.obiba.magma.ValueSource;
import org.obiba.magma.ValueTable;
import org.obiba.magma.Variable;
import org.obiba.magma.math.stat.IntervalFrequency;
import org.obiba.magma.type.IntegerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 *
 */
public class ContinuousVariableSummary extends AbstractVariableSummary implements Serializable {

  private static final long serialVersionUID = -8679001175321206239L;

  private static final Logger log = LoggerFactory.getLogger(ContinuousVariableSummary.class);

  public static final int DEFAULT_INTERVALS = 10;

  static final ImmutableList<Double> DEFAULT_PERCENTILES = ImmutableList.of(0.05d, 0.5d, 5d, 10d, 15d, 20d, 25d, 30d, 35d, 40d, 45d, 50d, 55d, 60d, 65d, 70d, 75d, 80d, 85d, 90d, 95d,
          99.5d, 99.95d);

  @NotNull
  private final Distribution distribution;

  @NotNull
  private List<Double> defaultPercentiles = DEFAULT_PERCENTILES;

  private int intervals = DEFAULT_INTERVALS;

  // Holds missing categories
  // (the case of continuous variables that have "special" values such as 8888 or 9999 that indicate a missing value)
  @NotNull
  private final Set<Value> missing = Sets.newHashSet();

  @NotNull
  private final DescriptiveStatistics descriptiveStats = new DescriptiveStatistics();

  @NotNull
  private final List<Double> percentiles = Lists.newArrayList();

  @NotNull
  private final Collection<Double> distributionPercentiles = Lists.newArrayList();

  @NotNull
  private final Collection<IntervalFrequency.Interval> intervalFrequencies = Lists.newArrayList();

  @Override
  public String getCacheKey(ValueTable table) {
    return ContinuousVariableSummaryFactory
        .getCacheKey(variable, table, distribution, defaultPercentiles, intervals, getOffset(), getLimit());
  }

  private ContinuousVariableSummary(@NotNull Variable variable, @NotNull Distribution distribution) {
    super(variable);
    //noinspection ConstantConditions
    Preconditions.checkArgument(distribution != null, "distribution cannot be null");
    Preconditions.checkArgument(variable.getValueType().isNumeric(), "Continuous variables must be numeric");

    this.distribution = distribution;

    if(variable.hasCategories()) {
      for(Category c : variable.getCategories()) {
        if(c.isMissing()) {
          try {
            missing.add(variable.getValueType().valueOf(c.getName()));
          } catch(MagmaRuntimeException e) {
            // When valueOf expects a integer but get a string category, do not crash
            log.warn("Variable {}, Category {}, ValueType ({}): {}", variable.getName(), c.getName(),
                variable.getValueType().getName(), e.getMessage());
          }
        }
      }
    }
  }

  @NotNull
  public Distribution getDistribution() {
    return distribution;
  }

  public int getIntervals() {
    return intervals;
  }

  @NotNull
  public DescriptiveStatistics getDescriptiveStats() {
    return descriptiveStats;
  }

  @NotNull
  public List<Double> getPercentiles() {
    return percentiles;
  }

  @NotNull
  public Collection<Double> getDistributionPercentiles() {
    return distributionPercentiles;
  }

  @NotNull
  public Collection<IntervalFrequency.Interval> getIntervalFrequencies() {
    return intervalFrequencies;
  }

  @NotNull
  public List<Double> getDefaultPercentiles() {
    return defaultPercentiles;
  }

  public enum Distribution {
    normal {
      @Nullable
      @Override
      public RealDistribution getDistribution(DescriptiveStatistics ds) {
        return ds.getStandardDeviation() > 0 ? new NormalDistribution(ds.getMean(), ds.getStandardDeviation()) : null;
      }
    },
    exponential {
      @NotNull
      @Override
      public RealDistribution getDistribution(DescriptiveStatistics ds) {
        return new ExponentialDistribution(ds.getMean());
      }
    };

    @Nullable
    abstract RealDistribution getDistribution(DescriptiveStatistics ds);

  }

  @SuppressWarnings("ParameterHidesMemberVariable")
  public static class Builder implements VariableSummaryBuilder<ContinuousVariableSummary, Builder> {

    private final ContinuousVariableSummary summary;

    @NotNull
    private final Variable variable;

    private boolean addedTable;

    private boolean addedValue;

    public Builder(@NotNull Variable variable, @NotNull Distribution distribution) {
      this.variable = variable;
      summary = new ContinuousVariableSummary(variable, distribution);
    }

    public Builder intervals(int intervals) {
      summary.intervals = intervals;
      return this;
    }

    public Builder defaultPercentiles(@Nullable List<Double> defaultPercentiles) {
      summary.defaultPercentiles = defaultPercentiles == null || defaultPercentiles.isEmpty()
          ? DEFAULT_PERCENTILES
          : defaultPercentiles;
      return this;
    }

    public Builder filter(Integer offset, Integer limit) {
      summary.setOffset(offset);
      summary.setLimit(limit);
      return this;
    }

    @Override
    public Builder addValue(@NotNull Value value) {
      if(addedTable) {
        throw new IllegalStateException("Cannot add value for variable " + summary.getVariable().getName() +
            " because values where previously added from the whole table with addTable().");
      }
      add(value);
      addedValue = true;
      return this;
    }

    @Override
    public Builder addTable(@NotNull ValueTable table, @NotNull ValueSource variableValueSource) {
      if(addedValue) {
        throw new IllegalStateException("Cannot add table for variable " + summary.getVariable().getName() +
            " because values where previously added with addValue().");
      }
      add(table, variableValueSource);
      addedTable = true;

      return this;
    }

    private void add(@NotNull ValueTable table, @NotNull ValueSource variableValueSource) {
      //noinspection ConstantConditions
      Preconditions.checkArgument(table != null, "table cannot be null");
      //noinspection ConstantConditions
      Preconditions.checkArgument(variableValueSource != null, "variableValueSource cannot be null");

      if(!variableValueSource.supportVectorSource()) return;
      for(Value value : variableValueSource.asVectorSource().getValues(summary.getVariableEntities(table))) {
        add(value);
      }
    }

    private void add(@NotNull Value value) {
      //noinspection ConstantConditions
      Preconditions.checkArgument(value != null, "value cannot be null");
      if(!value.isNull() && !summary.missing.contains(value)) {
        if(value.isSequence()) {
          for(Value v : value.asSequence().getValue()) {
            add(v);
          }
        } else {
          summary.descriptiveStats.addValue(((Number) value.getValue()).doubleValue());
        }
      }
    }

    @SuppressWarnings("MagicNumber")
    private void compute() {
      log.trace("Start compute continuous {}", summary.variable.getName());
      double variance = summary.descriptiveStats.getVariance();
      if(Double.isNaN(variance) || Double.isInfinite(variance) || variance <= 0) return;

      IntervalFrequency intervalFrequency = new IntervalFrequency(summary.descriptiveStats.getMin(),
          summary.descriptiveStats.getMax(), summary.intervals,
          summary.getVariable().getValueType() == IntegerType.get());
      for(double d : summary.descriptiveStats.getSortedValues()) {
        intervalFrequency.add(d);
      }

      for(IntervalFrequency.Interval interval : intervalFrequency.intervals()) {
        summary.intervalFrequencies.add(interval);
      }

      RealDistribution realDistribution = summary.distribution.getDistribution(summary.descriptiveStats);
      for(Double p : summary.defaultPercentiles) {
        summary.percentiles.add(summary.descriptiveStats.getPercentile(p));
        if(realDistribution != null) {
          summary.distributionPercentiles.add(realDistribution.inverseCumulativeProbability(p / 100d));
        }
      }
    }

    @NotNull
    @Override
    public Variable getVariable() {
      return variable;
    }

    @Override
    @NotNull
    public ContinuousVariableSummary build() {
      compute();
      return summary;
    }
  }

}