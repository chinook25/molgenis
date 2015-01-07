package org.molgenis.generators.db;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.molgenis.MolgenisOptions;
import org.molgenis.generators.Generator;
import org.molgenis.model.elements.Entity;
import org.molgenis.model.elements.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Template;

public class JpaDatabaseGen extends Generator
{
	private static final Logger LOG = LoggerFactory.getLogger(JpaDatabaseGen.class);

	@Override
	public String getDescription()
	{
		return "Generates one Jpa to talk to the data. Encapsulates Database Mappers to do this.";
	}

	@Override
	public void generate(Model model, MolgenisOptions options) throws Exception
	{
		if (options.generate_tests)
		{
		}
		else
		{
			generate(model, options, true);
			generate(model, options, false);
		}
	}

	private void generate(Model model, MolgenisOptions options, boolean secure) throws Exception
	{
		if (options.generate_tests)
		{
		}
		else
		{
			Template template = createTemplate("/" + getClass().getSimpleName() + ".java.ftl");
			Map<String, Object> templateArgs = createTemplateArguments(options);

			List<Entity> entityList = model.getEntities();

			String className = secure ? "SecuredJpaDatabase" : "UnsecuredJpaDatabase";
			File target = new File(this.getSourcePath(options) + APP_DIR + "/" + className + ".java");
			boolean created = target.getParentFile().mkdirs();
			if (!created && !target.getParentFile().exists())
			{
				throw new IOException("could not create " + target.getParentFile());
			}

			templateArgs.put("model", model);
			templateArgs.put("entities", entityList);
			templateArgs.put("package", APP_DIR.replace('/', '.'));
			templateArgs.put("secure", secure);
			templateArgs.put("className", className);
			templateArgs.put("disable_decorators", options.disable_decorators);
			OutputStream targetOut = new FileOutputStream(target);
			template.process(templateArgs, new OutputStreamWriter(targetOut, Charset.forName("UTF-8")));
			targetOut.close();

			LOG.info("generated " + target);
		}
	}

}
