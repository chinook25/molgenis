package org.molgenis.data.postgresql;

import org.molgenis.data.Entity;
import org.molgenis.data.EntityManager;
import org.molgenis.data.Fetch;
import org.molgenis.data.meta.model.AttributeMetaData;
import org.molgenis.data.meta.model.EntityType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.Array;
import java.sql.ResultSet;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singleton;
import static org.mockito.Mockito.*;
import static org.molgenis.MolgenisFieldTypes.AttributeType.*;
import static org.testng.Assert.assertEquals;

public class PostgreSqlEntityFactoryTest
{
	private PostgreSqlEntityFactory postgreSqlEntityFactory;
	private EntityManager entityManager;

	@BeforeMethod
	public void setUpBeforeMethod()
	{
		entityManager = mock(EntityManager.class);
		postgreSqlEntityFactory = new PostgreSqlEntityFactory(entityManager);
	}

	@Test
	public void createRowMapperOneToMany() throws Exception
	{
		AttributeMetaData refIdAttr = mock(AttributeMetaData.class);
		when(refIdAttr.getDataType()).thenReturn(STRING);

		EntityType refEntityMeta = mock(EntityType.class);
		when(refEntityMeta.getIdAttribute()).thenReturn(refIdAttr);

		String oneToManyAttrName = "oneToManyAttr";
		AttributeMetaData oneToManyAttr = mock(AttributeMetaData.class);
		when(oneToManyAttr.getName()).thenReturn(oneToManyAttrName);
		when(oneToManyAttr.getDataType()).thenReturn(ONE_TO_MANY);
		when(oneToManyAttr.getRefEntity()).thenReturn(refEntityMeta);

		EntityType entityType = mock(EntityType.class);
		when(entityType.getAtomicAttributes()).thenReturn(singleton(oneToManyAttr));
		ResultSet rs = mock(ResultSet.class);
		Array oneToManyArray = mock(Array.class);
		when(oneToManyArray.getArray()).thenReturn(new String[][] { { "1", "id0" }, { "0", "id1" } });
		when(rs.getArray(oneToManyAttrName)).thenReturn(oneToManyArray);
		int rowNum = 0;

		Entity entity = mock(Entity.class);
		Fetch fetch = null;
		when(entityManager.create(entityType, fetch)).thenReturn(entity);
		Entity refEntity1 = mock(Entity.class);
		Entity refEntity0 = mock(Entity.class);
		when(entityManager.getReferences(refEntityMeta, newArrayList("id1", "id0")))
				.thenReturn(newArrayList(refEntity1, refEntity0));
		assertEquals(postgreSqlEntityFactory.createRowMapper(entityType, null).mapRow(rs, rowNum), entity);
		verify(entity).set(oneToManyAttrName, newArrayList(refEntity1, refEntity0));
	}

	@Test
	public void createRowMapperXref() throws Exception
	{
		AttributeMetaData refIdAttr = mock(AttributeMetaData.class);
		when(refIdAttr.getDataType()).thenReturn(STRING);

		EntityType refEntityType = mock(EntityType.class);
		when(refEntityType.getIdAttribute()).thenReturn(refIdAttr);

		String xrefAttr = "xrefAttr";
		AttributeMetaData oneToManyAttr = mock(AttributeMetaData.class);
		when(oneToManyAttr.getName()).thenReturn(xrefAttr);
		when(oneToManyAttr.getDataType()).thenReturn(XREF);
		when(oneToManyAttr.getRefEntity()).thenReturn(refEntityType);

		EntityType entityType = mock(EntityType.class);
		when(entityType.getAtomicAttributes()).thenReturn(singleton(oneToManyAttr));
		ResultSet rs = mock(ResultSet.class);
		when(rs.getString(xrefAttr)).thenReturn("id0");
		int rowNum = 0;

		Entity entity = mock(Entity.class);
		Fetch fetch = null;
		//noinspection ConstantConditions
		when(entityManager.create(entityType, fetch)).thenReturn(entity);
		Entity refEntity = mock(Entity.class);
		when(entityManager.getReference(refEntityType, "id0")).thenReturn(refEntity);
		assertEquals(postgreSqlEntityFactory.createRowMapper(entityType, null).mapRow(rs, rowNum), entity);
		verify(entity).set(xrefAttr, refEntity);
	}
}