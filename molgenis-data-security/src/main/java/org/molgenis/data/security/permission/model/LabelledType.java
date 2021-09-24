package org.molgenis.data.security.permission.model;

import com.google.auto.value.AutoValue;
import org.molgenis.gson.AutoGson;

@AutoValue
@AutoGson(autoValueClass = AutoValue_LabelledType.class)
@SuppressWarnings("java:S1610") // Abstract classes without fields should be converted to interfaces
public abstract class LabelledType {
  public abstract String getId();

  public abstract String getEntityType();

  public abstract String getLabel();

  public static LabelledType create(String typeId, String entityType, String label) {
    return new AutoValue_LabelledType(typeId, entityType, label);
  }
}
