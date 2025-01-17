package org.molgenis.data.elasticsearch.client;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.molgenis.data.elasticsearch.generator.model.FieldMapping;
import org.molgenis.data.elasticsearch.generator.model.Mapping;
import org.molgenis.data.elasticsearch.generator.model.MappingType;

class MappingContentBuilderTest {
  private MappingContentBuilder mappingContentBuilder;

  @BeforeEach
  void setUpBeforeMethod() {
    mappingContentBuilder = new MappingContentBuilder(XContentType.JSON);
  }

  static Iterator<Object[]> createMappingProvider() {
    List<Object[]> dataItems = new ArrayList<>();
    dataItems.add(new Object[] {MappingType.BOOLEAN, JSON_BOOLEAN});
    dataItems.add(new Object[] {MappingType.DATE, JSON_DATE});
    dataItems.add(new Object[] {MappingType.DATE_TIME, JSON_DATE_TIME});
    dataItems.add(new Object[] {MappingType.DOUBLE, JSON_DOUBLE});
    dataItems.add(new Object[] {MappingType.INTEGER, JSON_INTEGER});
    dataItems.add(new Object[] {MappingType.LONG, JSON_LONG});
    dataItems.add(new Object[] {MappingType.TEXT, JSON_TEXT});
    dataItems.add(new Object[] {MappingType.KEYWORD, JSON_KEYWORD});
    return dataItems.iterator();
  }

  @ParameterizedTest
  @MethodSource("createMappingProvider")
  void testCreateMapping(MappingType mappingType, String expectedJson) throws IOException {
    Mapping mapping =
        createMapping(FieldMapping.builder().setName("field").setType(mappingType).build());
    XContentBuilder xContentBuilder = mappingContentBuilder.createMapping(mapping);
    assertEquals(expectedJson, xContentBuilder.getOutputStream().toString());
  }

  @Test
  void testCreateMappingKeywordCaseSensitive() throws IOException {
    Mapping mapping =
        createMapping(
            FieldMapping.builder()
                .setName("field")
                .setType(MappingType.KEYWORD)
                .setCaseSensitive(true)
                .build());

    XContentBuilder xContentBuilder = mappingContentBuilder.createMapping(mapping);
    assertEquals(JSON_KEYWORD_CASE_SENSITIVE, xContentBuilder.getOutputStream().toString());
  }

  @Test
  void testCreateMappingNested() throws IOException {
    FieldMapping nestedFieldMapping =
        FieldMapping.builder().setName("nestedField").setType(MappingType.BOOLEAN).build();
    Mapping mapping =
        createMapping(
            FieldMapping.builder()
                .setName("field")
                .setType(MappingType.NESTED)
                .setNestedFieldMappings(singletonList(nestedFieldMapping))
                .build());
    XContentBuilder xContentBuilder = mappingContentBuilder.createMapping(mapping);
    assertEquals(JSON_NESTED, xContentBuilder.getOutputStream().toString());
  }

  private static Mapping createMapping(FieldMapping fieldMapping) {
    return Mapping.builder().setType("id").setFieldMappings(singletonList(fieldMapping)).build();
  }

  private static final String JSON_BOOLEAN =
      "{\"_source\":{\"enabled\":false},\"properties\":{\"field\":{\"type\":\"boolean\"}}}";
  private static final String JSON_DATE =
      "{\"_source\":{\"enabled\":false},\"properties\":{\"field\":{\"type\":\"date\",\"format\":\"date\",\"fields\":{\"raw\":{\"type\":\"keyword\",\"index\":true}}}}}";
  private static final String JSON_DATE_TIME =
      "{\"_source\":{\"enabled\":false},\"properties\":{\"field\":{\"type\":\"date\",\"format\":\"date_time_no_millis\",\"fields\":{\"raw\":{\"type\":\"keyword\",\"index\":true}}}}}";
  private static final String JSON_DOUBLE =
      "{\"_source\":{\"enabled\":false},\"properties\":{\"field\":{\"type\":\"double\"}}}";
  private static final String JSON_INTEGER =
      "{\"_source\":{\"enabled\":false},\"properties\":{\"field\":{\"type\":\"integer\",\"doc_values\":true}}}";
  private static final String JSON_LONG =
      "{\"_source\":{\"enabled\":false},\"properties\":{\"field\":{\"type\":\"long\"}}}";
  private static final String JSON_TEXT =
      "{\"_source\":{\"enabled\":false},\"properties\":{\"field\":{\"type\":\"text\",\"norms\":true,\"fields\":{\"raw\":{\"type\":\"keyword\",\"index\":true,\"ignore_above\":8191}}}}}";
  private static final String JSON_KEYWORD =
      "{\"_source\":{\"enabled\":false},\"properties\":{\"field\":{\"type\":\"keyword\",\"normalizer\":\"lowercase_asciifold\",\"fields\":{\"raw\":{\"type\":\"keyword\",\"index\":true}}}}}";
  private static final String JSON_KEYWORD_CASE_SENSITIVE =
      "{\"_source\":{\"enabled\":false},\"properties\":{\"field\":{\"type\":\"keyword\",\"fields\":{\"raw\":{\"type\":\"keyword\",\"index\":true}}}}}";
  private static final String JSON_NESTED =
      "{\"_source\":{\"enabled\":false},\"properties\":{\"field\":{\"type\":\"nested\",\"properties\":{\"nestedField\":{\"type\":\"boolean\"}}}}}";
}
