package org.obiba.magma.math;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.obiba.magma.MagmaEngine;
import org.obiba.magma.Value;
import org.obiba.magma.VariableEntity;
import org.obiba.magma.VariableValueSource;
import org.obiba.magma.VectorSource;
import org.obiba.magma.support.Values;
import org.obiba.magma.type.DecimalType;

import com.google.common.collect.ImmutableList;

public class DefaultDescriptiveStatisticsProviderTest {

  SortedSet<VariableEntity> emptySet = new TreeSet<VariableEntity>();

  @Before
  public void startYourEngine() {
    new MagmaEngine();
  }

  @After
  public void stopYourEngine() {
    MagmaEngine.get().shutdown();
  }

  @Test
  public void test_compute_handlesNullVectorSource() {
    VariableValueSource mockSource = createMock(VariableValueSource.class);
    expect(mockSource.asVectorSource()).andReturn(null);

    replay(mockSource);

    DefaultDescriptiveStatisticsProvider defaultProvider = new DefaultDescriptiveStatisticsProvider();
    DescriptiveStatistics ds = defaultProvider.compute(mockSource, emptySet);

    assertThat(ds, notNullValue());
    verify(mockSource);
  }

  @Test
  public void test_compute_obtainsVectorOverCorrectSetOfEntities() {
    VariableValueSource mockSource = createMock(VariableValueSource.class);
    VectorSource mockVector = createMock(VectorSource.class);

    expect(mockSource.asVectorSource()).andReturn(mockVector);
    expect(mockVector.getValues(emptySet)).andReturn(ImmutableList.<Value> of());

    replay(mockSource, mockVector);

    DefaultDescriptiveStatisticsProvider defaultProvider = new DefaultDescriptiveStatisticsProvider();
    DescriptiveStatistics ds = defaultProvider.compute(mockSource, emptySet);

    assertThat(ds, notNullValue());

    verify(mockSource, mockVector);
  }

  @Test
  public void test_compute_excludesNullValues() {
    VariableValueSource mockSource = createMock(VariableValueSource.class);
    VectorSource mockVector = createMock(VectorSource.class);

    expect(mockSource.asVectorSource()).andReturn(mockVector);
    expect(mockVector.getValues(emptySet)).andReturn(Values.asValues(DecimalType.get(), 2d, 4d, 6d, 8d, null, null, 10d));

    replay(mockSource, mockVector);

    DefaultDescriptiveStatisticsProvider defaultProvider = new DefaultDescriptiveStatisticsProvider();
    DescriptiveStatistics ds = defaultProvider.compute(mockSource, emptySet);

    assertThat(ds, notNullValue());
    assertThat(ds.getN(), is(5l));

    verify(mockSource, mockVector);
  }

}