package org.molgenis.data.annotation.filter;

import com.google.common.collect.FluentIterable;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.Entity;
import org.molgenis.data.annotation.entity.ResultFilter;
import org.molgenis.data.vcf.VcfRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * TODO: Support multi-allelic combination fields. These fields contain not only info for ref-alt pairs,
 * but also values for all possible alt-alt pairs. For example, the "AC_Het" field in ExAC:
 * 
 * For 2 alt alleles, there are 3 values in the AC_Het field, the third being the count for T-A:
 * 1	6536051	.	C	T,A	11129.51	PASS	AC_Adj=5,3;AC_Het=5,3,0;AC_Hom=0,0;
 * Alt-allele combinations in AC_Het field occur in the following order: C-T, C-A, T-A.
 * 
 * For 3 alt alleles, there are 6 values (3+2+1):
 * 15	66641732	rs2063690	G	C,A,T	35371281.87	PASS	AC_Adj=13570,3,2;AC_Het=11380,1,2,2,0,0;AC_Hom=1094,0,0;
 * Alt-allele combinations in AC_Het field occur in the following order: G-C, G-A, G-T, C-A, C-T, A-T.
 * 
 * For 4 alt alleles, there are 10 values (4+3+2+1):
 * 21	45650009	rs3831401	T	G,C,TG,A	8366813.26	PASS	AC_Adj=2528,3415,1,0;AC_Het=934,1240,0,0,725,1,0,0,0,0;AC_Hom=434,725,0,0;
 * Alt-allele combinations in AC_Het field occur in the following order: T-G, T-C, T-TG, T-A, G-C, G-TG, G-A, C-TG, C-A, TG-A.
 *
 */
public class MultiAllelicResultFilter implements ResultFilter
{

	private List<AttributeMetaData> attributes;

	public MultiAllelicResultFilter(List<AttributeMetaData> attributes)
	{
		this.attributes = attributes;
	}

	@Override
	public Collection<AttributeMetaData> getRequiredAttributes()
	{
		return Arrays.asList(VcfRepository.REF_META, VcfRepository.ALT_META);
	}

	@Override
	public com.google.common.base.Optional<Entity> filterResults(Iterable<Entity> results, Entity annotatedEntity)
	{
		Map<String, String> alleleValueMap = new HashMap<>();
		List<Entity> processedResults = new ArrayList<>();

		for (Entity entity : results)
		{
			if (entity.get(VcfRepository.REF).equals(annotatedEntity.get(VcfRepository.REF)))
			{
				String[] alts = entity.getString(VcfRepository.ALT).split(",");

				for (AttributeMetaData attributeMetaData : attributes)
				{
					String[] values = entity.getString(attributeMetaData.getName()).split(",");
					for (int i = 0; i < alts.length; i++)
					{
						alleleValueMap.put(alts[i], values[i]);
					}
					StringBuilder newAttributeValue = new StringBuilder();
					String[] annotatedEntityAltAlleles = annotatedEntity.getString(VcfRepository.ALT).split(",");
					for (int i = 0; i < annotatedEntityAltAlleles.length; i++)
					{
						if (i != 0)
						{
							newAttributeValue.append(",");
						}
						if (alleleValueMap.get(annotatedEntityAltAlleles[i]) != null)
						{
							newAttributeValue.append(alleleValueMap.get(annotatedEntityAltAlleles[i]));
						}
						else
						{
							//missing allele in source, add a dot
							newAttributeValue.append(".");
						}
					}
					// add entity only if something was found
					if (!newAttributeValue.toString().equals("."))
					{
						entity.set(attributeMetaData.getName(), newAttributeValue.toString());
						processedResults.add(entity);
					}
				}
			}
		}
		return FluentIterable.from(processedResults).first();
	}
}
