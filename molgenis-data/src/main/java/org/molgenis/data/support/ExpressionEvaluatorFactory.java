package org.molgenis.data.support;

import com.google.gson.Gson;
import org.molgenis.data.meta.model.Attribute;
import org.molgenis.data.meta.model.EntityType;
import org.molgenis.util.UnexpectedEnumException;

class ExpressionEvaluatorFactory {
  private ExpressionEvaluatorFactory() {}

  static ExpressionEvaluator createExpressionEvaluator(Attribute attribute, EntityType entityType) {
    ExpressionEvaluator expressionEvaluator;

    Object expressionJson = new Gson().fromJson(attribute.getExpression(), Object.class);
    if (expressionJson instanceof String) {
      expressionEvaluator = new StringExpressionEvaluator(attribute, entityType);
    } else {
      expressionEvaluator = switch (attribute.getDataType()) {
        case BOOL, CATEGORICAL, CATEGORICAL_MREF, COMPOUND, DATE,
            DATE_TIME, DECIMAL, FILE, INT, LONG, MREF, ONE_TO_MANY, XREF ->
            new MapOfStringsExpressionEvaluator(attribute, entityType);
        case EMAIL, ENUM, HTML, HYPERLINK, SCRIPT, STRING, TEXT ->
            new TemplateExpressionEvaluator(attribute, entityType);
      };
    }

    return expressionEvaluator;
  }
}
