package org.molgenis.ontology.core.meta;

import org.molgenis.data.AbstractSystemEntityFactory;
import org.molgenis.data.populate.EntityPopulator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OntologyTermSynonymFactory
		extends AbstractSystemEntityFactory<OntologyTermSynonym, OntologyTermSynonymMetaData, String>
{
	@Autowired
	OntologyTermSynonymFactory(OntologyTermSynonymMetaData OntologyTermSynonymMetaData, EntityPopulator entityPopulator)
	{
		super(OntologyTermSynonym.class, OntologyTermSynonymMetaData, entityPopulator);
	}
}
