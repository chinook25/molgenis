package org.molgenis.app.promise.mapper;

import com.google.common.collect.Iterables;
import org.molgenis.app.promise.client.PromiseDataParser;
import org.molgenis.app.promise.mapper.MappingReport.Status;
import org.molgenis.app.promise.model.BbmriNlCheatSheet;
import org.molgenis.app.promise.model.PromiseMappingProjectMetaData;
import org.molgenis.data.*;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.support.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.google.common.hash.Hashing.md5;
import static java.nio.charset.Charset.forName;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.lang.exception.ExceptionUtils.getStackTrace;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.ACRONYM;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.AGE_HIGH;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.AGE_LOW;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.AGE_UNIT;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.BIOBANKS;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.BIOBANK_DATA_ACCESS_DESCRIPTION;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.BIOBANK_DATA_ACCESS_FEE;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.BIOBANK_DATA_ACCESS_JOINT_PROJECTS;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.BIOBANK_DATA_ACCESS_URI;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.BIOBANK_DATA_SAMPLE_ACCESS_DESCRIPTION;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.BIOBANK_SAMPLE_ACCESS_FEE;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.BIOBANK_SAMPLE_ACCESS_URI;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.CONTACT_PERSON;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.DATA_CATEGORIES;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.DESCRIPTION;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.DISEASE;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.INSTITUTES;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.MATERIALS;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.NAME;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.NUMBER_OF_DONORS;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.OMICS;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.PRINCIPAL_INVESTIGATORS;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.PUBLICATIONS;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.SAMPLE_COLLECTIONS_ENTITY;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.SEX;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.TYPE;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.WEBSITE;

@Component
public class RadboudMapper implements PromiseMapper, ApplicationListener<ContextRefreshedEvent>
{
	private final String ID = "RADBOUD";

	private final PromiseMapperFactory promiseMapperFactory;
	private final PromiseDataParser promiseDataParser;
	private final DataService dataService;

	private static final Logger LOG = LoggerFactory.getLogger(RadboudMapper.class);

	@Autowired
	public RadboudMapper(PromiseDataParser promiseDataParser, DataService dataService,
			PromiseMapperFactory promiseMapperFactory)
	{
		this.promiseDataParser = Objects.requireNonNull(promiseDataParser);
		this.dataService = Objects.requireNonNull(dataService);
		this.promiseMapperFactory = Objects.requireNonNull(promiseMapperFactory);
	}

	@Override
	public String getId()
	{
		return ID;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent arg0)
	{
		promiseMapperFactory.registerMapper(ID, this);
	}

	@Override
	public MappingReport map(Entity project)
	{
		requireNonNull(project);
		MappingReport report = new MappingReport();

		try
		{
			LOG.info("Downloading data for " + project.getString("name"));

			Entity credentials = project.getEntity(PromiseMappingProjectMetaData.CREDENTIALS);

			Iterable<Entity> promiseBiobankEntities = promiseDataParser.parse(credentials, 0);
			Iterable<Entity> promiseSampleEntities = promiseDataParser.parse(credentials, 1);

			EntityMetaData targetEntityMetaData = requireNonNull(
					dataService.getEntityMetaData(SAMPLE_COLLECTIONS_ENTITY));

			for (Entity promiseBiobankEntity : promiseBiobankEntities)
			{
				Iterable<Entity> promiseBiobankSamplesEntities = getPromiseBiobankSamples(promiseBiobankEntity,
						promiseSampleEntities);

				MapEntity targetEntity = new MapEntity(targetEntityMetaData);
				targetEntity.set(BbmriNlCheatSheet.ID, project.getString("biobank_id"));
				targetEntity.set(NAME, promiseBiobankEntity.getString("TITEL"));
				targetEntity.set(ACRONYM, project.getString("biobank_id")); //TODO Standard mapping based on ID
				targetEntity.set(TYPE, toTypes(promiseBiobankEntity.getString("TYPEBIOBANK")));
				targetEntity.set(DISEASE, null); // TODO Will be supplied by Radboud
				targetEntity.set(DATA_CATEGORIES,
						toDataCategories(promiseBiobankEntity, promiseBiobankSamplesEntities));
				targetEntity.set(MATERIALS, toMaterials(promiseBiobankSamplesEntities));
				targetEntity.set(OMICS, toOmics(promiseBiobankSamplesEntities));
				targetEntity.set(SEX, toSex(promiseBiobankSamplesEntities));
				targetEntity.set(AGE_LOW, toAgeMinOrMax(promiseBiobankSamplesEntities, true));
				targetEntity.set(AGE_HIGH, toAgeMinOrMax(promiseBiobankSamplesEntities, false));
				targetEntity.set(AGE_UNIT, toAgeUnit());
				targetEntity.set(NUMBER_OF_DONORS, Iterables.size(promiseBiobankSamplesEntities));
				targetEntity.set(DESCRIPTION, promiseBiobankEntity.getString("OMSCHRIJVING"));
				targetEntity.set(PUBLICATIONS, null);  // TODO Will be supplied by Radboud
				targetEntity.set(CONTACT_PERSON, getCreatePersons(promiseBiobankEntity));
				targetEntity.set(PRINCIPAL_INVESTIGATORS, toPrincipalInvestigators());
				targetEntity.set(INSTITUTES, toInstitutes());
				targetEntity.set(BIOBANKS, toBiobanks());
				targetEntity.set(WEBSITE, "http://www.radboudbiobank.nl/");
				targetEntity.set(BIOBANK_SAMPLE_ACCESS_FEE, true);
				targetEntity.set(BIOBANK_DATA_ACCESS_JOINT_PROJECTS, true);
				targetEntity.set(BIOBANK_DATA_SAMPLE_ACCESS_DESCRIPTION, null);  // Don't fill in
				targetEntity.set(BIOBANK_SAMPLE_ACCESS_URI,
						"http://www.radboudbiobank.nl/nl/collecties/materiaal-opvragen/");
				targetEntity.set(BIOBANK_DATA_ACCESS_FEE, true);
				targetEntity.set(BIOBANK_DATA_ACCESS_JOINT_PROJECTS, true);
				targetEntity.set(BIOBANK_DATA_ACCESS_DESCRIPTION, null);  // Don't fill in
				targetEntity.set(BIOBANK_DATA_ACCESS_URI,
						"http://www.radboudbiobank.nl/nl/collecties/materiaal-opvragen/");

				dataService.add("bbmri_nl_sample_collections", targetEntity);
			}

			report.setStatus(Status.SUCCESS);
		}
		catch (Exception e)
		{
			report.setStatus(Status.ERROR);
			report.setMessage(e.getMessage());

			LOG.warn(getStackTrace(e));
		}
		return report;
	}

	private Iterable<Entity> toBiobanks()
	{
		Entity biobank = dataService.findOne("bbmri_nl_biobanks", "RBB");
		if (biobank == null)
		{
			throw new RuntimeException("Unknown 'bbmri_nl_biobanks' [RBB]");
		}
		return singletonList(biobank);
	}

	private Iterable<Entity> toInstitutes()
	{
		Entity juristicPerson = dataService.findOne("bbmri_nl_juristic_persons", "83");
		if (juristicPerson == null)
		{
			throw new RuntimeException("Unknown 'bbmri_nl_juristic_persons' [83]");
		}
		return singletonList(juristicPerson);
	}

	private Entity toAgeUnit()
	{
		Entity ageUnit = dataService.findOne("bbmri_nl_age_types", "YEAR");
		if (ageUnit == null)
		{
			throw new RuntimeException("Unknown 'bbmri_nl_age_types' [YEAR]");
		}
		return ageUnit;
	}

	private Iterable<Entity> toPrincipalInvestigators()
	{
		MapEntity principalInvestigators = new MapEntity(dataService.getEntityMetaData("bbmri_nl_persons"));
		principalInvestigators.set("id", new UuidGenerator().generateId());
		Entity countryNl = dataService.findOne("bbmri_nl_countries", "NL");
		if (countryNl == null)
		{
			throw new RuntimeException("Unknown 'bbmri_nl_countries' [NL]");
		}
		principalInvestigators.set("country", countryNl);
		dataService.add("bbmri_nl_persons", principalInvestigators);
		return singletonList(principalInvestigators);
	}

	private Iterable<Entity> getCreatePersons(Entity promiseBiobankEntity)
	{
		// TODO what if all fields are null?
		String contactPerson = promiseBiobankEntity.getString("CONTACTPERS");
		String address1 = promiseBiobankEntity.getString("ADRES1");
		String address2 = promiseBiobankEntity.getString("ADRES2");
		String postalCode = promiseBiobankEntity.getString("POSTCODE");
		String city = promiseBiobankEntity.getString("PLAATS");
		String email = promiseBiobankEntity.getString("EMAIL");
		String phoneNumber = promiseBiobankEntity.getString("TELEFOON");

		StringBuilder contentBuilder = new StringBuilder();
		if (contactPerson != null && !contactPerson.isEmpty()) contentBuilder.append(contactPerson);
		if (address1 != null && !address1.isEmpty()) contentBuilder.append(address1);
		if (address2 != null && !address2.isEmpty()) contentBuilder.append(address2);
		if (postalCode != null && !postalCode.isEmpty()) contentBuilder.append(postalCode);
		if (city != null && !city.isEmpty()) contentBuilder.append(city);
		if (email != null && !email.isEmpty()) contentBuilder.append(email);
		if (phoneNumber != null && !phoneNumber.isEmpty()) contentBuilder.append(phoneNumber);

		String personId = md5().newHasher().putString(contentBuilder, forName("UTF-8")).hash().toString();
		Entity person = dataService.findOne("bbmri_nl_persons", personId);
		if (person != null)
		{
			return singletonList(person);
		}
		else
		{
			MapEntity newPerson = new MapEntity(dataService.getEntityMetaData("bbmri_nl_persons"));
			newPerson.set("id", personId);
			if(contactPerson != null)
			{
				newPerson.set("first_name", contactPerson.split(" ")[0]);
			}
			else
			{
				newPerson.set("first_name", contactPerson);
			}
			newPerson.set("last_name", contactPerson);
			newPerson.set("phone", phoneNumber);
			newPerson.set("email", email);

			StringBuilder addressBuilder = new StringBuilder();
			if (address1 != null && !address1.isEmpty()) addressBuilder.append(address1);
			if (address2 != null && !address2.isEmpty())
			{
				if (address1 != null && !address1.isEmpty()) addressBuilder.append(' ');
				addressBuilder.append(address2);
			}
			if (addressBuilder.length() > 0)
			{
				newPerson.set("address", addressBuilder.toString());
			}
			newPerson.set("zip", postalCode);
			newPerson.set("city", city);
			Entity countryNl = dataService.findOne("bbmri_nl_countries", "NL");
			if (countryNl == null)
			{
				throw new RuntimeException("Unknown 'bbmri_nl_countries' [NL]");
			}

			// TODO what to put here, this is a required attribute?
			newPerson.set("country", countryNl);

			dataService.add("bbmri_nl_persons", newPerson);

			return singletonList(newPerson);
		}
	}

	private Integer toAgeMinOrMax(Iterable<Entity> promiseBiobankSamplesEntities, boolean lowest)
	{
		Long ageMinOrMax = null;
		for (Entity promiseBiobankSamplesEntity : promiseBiobankSamplesEntities)
		{
			String birthDate = promiseBiobankSamplesEntity.getString("GEBOORTEDATUM");
			if (birthDate != null && !birthDate.isEmpty())
			{
				LocalDate start = LocalDate.parse(birthDate, DateTimeFormatter.ISO_DATE_TIME);
				LocalDate end = LocalDate.now();
				long age = ChronoUnit.YEARS.between(start, end);
				if (ageMinOrMax == null || (lowest && age < ageMinOrMax) || (!lowest && age > ageMinOrMax))
				{
					ageMinOrMax = age;
				}
			}
		}
		return ageMinOrMax != null ? ageMinOrMax.intValue() : null;
	}

	private Iterable<Entity> toSex(Iterable<Entity> promiseBiobankSamplesEntities) throws RuntimeException
	{
		Set<Object> genderTypeIds = new LinkedHashSet<Object>();

		for (Entity promiseBiobankSamplesEntity : promiseBiobankSamplesEntities)
		{
			if ("1".equals(promiseBiobankSamplesEntity.getString("GESLACHT")))
			{
				genderTypeIds.add("FEMALE");
			}
			if ("2".equals(promiseBiobankSamplesEntity.getString("GESLACHT")))
			{
				genderTypeIds.add("MALE");
			}
			if ("3".equals(promiseBiobankSamplesEntity.getString("GESLACHT")))
			{
				genderTypeIds.add("UNKNOWN");
			}
		}

		if (genderTypeIds.isEmpty())
		{
			genderTypeIds.add("NAV");
		}
		Iterable<Entity> genderTypes = dataService.findAll("bbmri_nl_gender_types", genderTypeIds.stream()).collect(
				toList());
		if (!genderTypeIds.iterator().hasNext())
		{
			throw new RuntimeException(
					"Unknown 'bbmri_nl_gender_types' [" + join(genderTypeIds, ',') + "]");
		}
		return genderTypes;
	}

	private Iterable<Entity> toTypes(String promiseTypeBiobank)
	{
		String collectionTypeId;
		if (promiseTypeBiobank == null || promiseTypeBiobank.isEmpty())
		{
			collectionTypeId = "OTHER";
		}
		else
		{
			switch (promiseTypeBiobank)
			{
				case "0":
					collectionTypeId = "OTHER";
					break;
				case "1":
					collectionTypeId = "DISEASE_SPECIFIC";
					break;
				case "2":
					collectionTypeId = "POPULATION_BASED";
					break;
				default:
					throw new RuntimeException("Unknown biobank type [" + promiseTypeBiobank + "]");
			}
		}
		Entity collectionType = dataService.findOne("bbmri_nl_collection_types", collectionTypeId);
		if (collectionType == null)
		{
			throw new RuntimeException("Unknown 'bbmri_nl_collection_types' [" + collectionTypeId + "]");
		}
		return Arrays.asList(collectionType);
	}

	private Iterable<Entity> toDiseases()
	{
		Entity diseaseType = dataService.findOne("bbmri_nl_disease_types", "NAV");
		if (diseaseType == null)
		{
			throw new RuntimeException("Unknown 'bbmri_nl_disease_types' [NAV]");
		}
		return Arrays.asList(diseaseType);
	}

	private Iterable<Entity> toDataCategories(Entity promiseBiobankEntity,
			Iterable<Entity> promiseBiobankSamplesEntities)
	{
		Set<Object> dataCategoryTypeIds = new LinkedHashSet<Object>();

		for (Entity promiseBiobankSamplesEntity : promiseBiobankSamplesEntities)
		{
			if (promiseBiobankSamplesEntity != null)
			{
				String deelbiobanks = promiseBiobankSamplesEntity.getString("DEELBIOBANKS");
				if (deelbiobanks != null && Integer.valueOf(deelbiobanks) >= 1)
				{
					dataCategoryTypeIds.add("BIOLOGICAL_SAMPLES");
				}
			}

			if ("1".equals(promiseBiobankEntity.getString("VOORGESCH")))
			{
				dataCategoryTypeIds.add("OTHER");
			}

			if ("1".equals(promiseBiobankEntity.getString("FAMANAM")))
			{
				dataCategoryTypeIds.add("GENEALOGICAL_RECORDS");
			}

			if ("1".equals(promiseBiobankEntity.getString("BEHANDEL")))
			{
				dataCategoryTypeIds.add("MEDICAL_RECORDS");
			}

			if ("1".equals(promiseBiobankEntity.getString("FOLLOWUP")))
			{
				dataCategoryTypeIds.add("OTHER");
			}

			if ("1".equals(promiseBiobankEntity.getString("BEELDEN")))
			{
				dataCategoryTypeIds.add("IMAGING_DATA");
			}

			if ("1".equals(promiseBiobankEntity.getString("VRAGENLIJST")))
			{
				dataCategoryTypeIds.add("SURVEY_DATA");
			}

			if ("1".equals(promiseBiobankEntity.getString("OMICS")))
			{
				dataCategoryTypeIds.add("PHYSIOLOGICAL_BIOCHEMICAL_MEASUREMENTS");
			}

			if ("1".equals(promiseBiobankEntity.getString("ROUTINEBEP")))
			{
				dataCategoryTypeIds.add("PHYSIOLOGICAL_BIOCHEMICAL_MEASUREMENTS");
			}

			if ("1".equals(promiseBiobankEntity.getString("GWAS")))
			{
				dataCategoryTypeIds.add("OTHER");
			}

			if ("1".equals(promiseBiobankEntity.getString("HISTOPATH")))
			{
				dataCategoryTypeIds.add("OTHER");
			}

			if ("1".equals(promiseBiobankEntity.getString("OUTCOME")))
			{
				dataCategoryTypeIds.add("NATIONAL_REGISTRIES");
			}

			if ("1".equals(promiseBiobankEntity.getString("ANDERS")))
			{
				dataCategoryTypeIds.add("OTHER");
			}
		}

		if (dataCategoryTypeIds.isEmpty())
		{
			dataCategoryTypeIds.add("NAV");
		}

		Iterable<Entity> dataCategoryTypes = dataService.findAll("bbmri_nl_data_category_types", dataCategoryTypeIds.stream()).collect(toList());
		if (!dataCategoryTypes.iterator().hasNext())
		{
			throw new RuntimeException(
					"Unknown 'bbmri_nl_data_category_types' [" + join(dataCategoryTypeIds, ',') + "]");
		}
		return dataCategoryTypes;
	}

	private Iterable<Entity> toMaterials(Iterable<Entity> promiseBiobankSamplesEntities)
	{
		Set<Object> materialTypeIds = new LinkedHashSet<Object>();

		for (Entity promiseBiobankSamplesEntity : promiseBiobankSamplesEntities)
		{
			if ("1".equals(promiseBiobankSamplesEntity.getString("DNA"))
					|| "1".equals(promiseBiobankSamplesEntity.getString("DNABEENMERG")))
			{
				materialTypeIds.add("DNA");
			}

			if ("1".equals(promiseBiobankSamplesEntity.getString("BLOED")))
			{
				materialTypeIds.add("WHOLE_BLOOD");
			}

			if ("1".equals(promiseBiobankSamplesEntity.getString("BLOEDPLASMA")))
			{
				materialTypeIds.add("PLASMA");
			}

			if ("1".equals(promiseBiobankSamplesEntity.getString("BLOEDSERUM")))
			{
				materialTypeIds.add("SERUM");
			}

			if ("1".equals(promiseBiobankSamplesEntity.getString("WEEFSELSOORT")))
			{
				materialTypeIds.add("TISSUE_PARAFFIN_EMBEDDED");
			}
			else if ("2".equals(promiseBiobankSamplesEntity.getString("WEEFSELSOORT")))
			{
				materialTypeIds.add("TISSUE_FROZEN");
			}

			if ("1".equals(promiseBiobankSamplesEntity.getString("URINE")))
			{
				materialTypeIds.add("URINE");
			}

			if ("1".equals(promiseBiobankSamplesEntity.getString("SPEEKSEL")))
			{
				materialTypeIds.add("SALIVA");
			}

			if ("1".equals(promiseBiobankSamplesEntity.getString("FECES")))
			{
				materialTypeIds.add("FECES");
			}

			if ("1".equals(promiseBiobankSamplesEntity.getString("RNA"))
					|| "1".equals(promiseBiobankSamplesEntity.getString("RNABEENMERG")))
			{
				materialTypeIds.add("MICRO_RNA");
			}

			if ("1".equals(promiseBiobankSamplesEntity.getString("GASTROINTMUC"))
					|| "1".equals(promiseBiobankSamplesEntity.getString("LIQUOR"))
					|| "1".equals(promiseBiobankSamplesEntity.getString("CELLBEENMERG"))
					|| "1".equals(promiseBiobankSamplesEntity.getString("MONONUCLBLOED"))
					|| "1".equals(promiseBiobankSamplesEntity.getString("MONONUCMERG"))
					|| "1".equals(promiseBiobankSamplesEntity.getString("GRANULOCYTMERG"))
					|| "1".equals(promiseBiobankSamplesEntity.getString("MONOCYTMERG"))
					|| "1".equals(promiseBiobankSamplesEntity.getString("MICROBIOOM")))
			{
				materialTypeIds.add("OTHER");
			}
		}

		if (materialTypeIds.isEmpty())
		{
			materialTypeIds.add("NAV");
		}
		Iterable<Entity> materialTypes = dataService.findAll("bbmri_nl_material_types", materialTypeIds.stream()).collect(
				toList());
		if (!materialTypes.iterator().hasNext())
		{
			throw new RuntimeException(
					"Unknown 'bbmri_nl_material_types' [" + join(materialTypeIds, ',') + "]");
		}

		return materialTypes;
	}

	private Iterable<Entity> toOmics(Iterable<Entity> promiseBiobankSamplesEntities)
	{
		Set<Object> omicsTypeIds = new LinkedHashSet<Object>();

		for (Entity promiseBiobankSamplesEntity : promiseBiobankSamplesEntities)
		{
			if ("1".equals(promiseBiobankSamplesEntity.getString("GWASOMNI"))
					|| "1".equals(promiseBiobankSamplesEntity.getString("GWAS370CNV"))
					|| "1".equals(promiseBiobankSamplesEntity.getString("EXOOMCHIP")))
			{
				omicsTypeIds.add("GENOMICS");
			}
		}

		if (omicsTypeIds.isEmpty())
		{
			omicsTypeIds.add("NAV");
		}
		Iterable<Entity> omicsTypes = dataService.findAll("bbmri_nl_omics_data_types", omicsTypeIds.stream()).collect(
				toList());
		if (!omicsTypes.iterator().hasNext())
		{
			throw new RuntimeException(
					"Unknown 'bbmri_nl_omics_data_types' [" + join(omicsTypeIds, ',') + "]");
		}
		return omicsTypes;
	}

	private Iterable<Entity> getPromiseBiobankSamples(Entity promiseBiobankEntity,
			Iterable<Entity> promiseSampleEntities)
	{
		List<Entity> promiseBiobankSampleEntities = new ArrayList<Entity>();
		String biobankId = promiseBiobankEntity.getString("ID") + promiseBiobankEntity.getString("IDAA");
		for (Entity promiseSampleEntity : promiseSampleEntities)
		{
			String biobankSamplesId = promiseSampleEntity.getString("ID") + promiseSampleEntity.getString("IDAA");
			if (biobankId.equals(biobankSamplesId))
			{
				promiseBiobankSampleEntities.add(promiseSampleEntity);
			}
		}
		return promiseBiobankSampleEntities;
	}

}
