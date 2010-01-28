package org.obiba.magma.audit;

import java.util.Map;

import org.obiba.magma.Datasource;
import org.obiba.magma.Value;
import org.obiba.magma.VariableEntity;

/**
 * Interface for interacting with audit logs. It provides the ability to obtain an instance of VariableEntityAuditLog
 * for a specific VariableEntity.
 */
public interface VariableEntityAuditLogManager {

  /**
   * Obtain an instance of VariableEntityAuditLog for a specific VariableEntity.
   * 
   * @param entity Entity from which to obtain the VariableEntityAuditLog.
   * @return A VariableEntityAuditLog
   */
  public VariableEntityAuditLog getAuditLog(VariableEntity entity);

  /**
   * Allows creating new entries (events) within a log.
   * 
   * @param log The log in which the event will be created.
   * @param datasource The datasource where the event stems from.
   * @param type The application-specific nature of the event. For example, an application may define "CREATE" and
   * "DELETE" types. Although Magma may define some types, this API does not define any type.
   * @param details A list of event-specific values that provide additional context.
   * 
   * @return The event created
   */
  public VariableEntityAuditEvent createAuditEvent(VariableEntityAuditLog log, Datasource datasource, String type, Map<String, Value> details);

}
