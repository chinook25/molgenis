package org.molgenis.data;

import org.molgenis.data.meta.model.AttributeMetaData;
import org.molgenis.data.meta.model.EntityType;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;

/**
 * Applies {@link Repository} decorators to all {@link RepositoryCollection} repositories.
 */
class RepositoryCollectionDecorator implements RepositoryCollection
{
	private final RepositoryCollection decoratedRepositoryCollection;
	private final RepositoryDecoratorFactory repositoryDecoratorFactory;

	RepositoryCollectionDecorator(RepositoryCollection decoratedRepositoryCollection,
			RepositoryDecoratorFactory repositoryDecoratorFactory)
	{
		this.decoratedRepositoryCollection = requireNonNull(decoratedRepositoryCollection);
		this.repositoryDecoratorFactory = requireNonNull(repositoryDecoratorFactory);
	}

	@Override
	public Iterator<Repository<Entity>> iterator()
	{
		return StreamSupport.stream(spliteratorUnknownSize(decoratedRepositoryCollection.iterator(), ORDERED), false)
				.map(repositoryDecoratorFactory::createDecoratedRepository).iterator();
	}

	@Override
	public String getName()
	{
		return decoratedRepositoryCollection.getName();
	}

	@Override
	public Set<RepositoryCollectionCapability> getCapabilities()
	{
		return decoratedRepositoryCollection.getCapabilities();
	}

	@Override
	public Repository<Entity> createRepository(EntityType entityType)
	{
		return repositoryDecoratorFactory
				.createDecoratedRepository(decoratedRepositoryCollection.createRepository(entityType));
	}

	@Override
	public Iterable<String> getEntityNames()
	{
		return decoratedRepositoryCollection.getEntityNames();
	}

	@Override
	public Repository<Entity> getRepository(String name)
	{
		Repository<Entity> repository = decoratedRepositoryCollection.getRepository(name);
		return repository != null ? repositoryDecoratorFactory.createDecoratedRepository(repository) : null;
	}

	@Override
	public Repository<Entity> getRepository(EntityType entityType)
	{
		Repository<Entity> repository = decoratedRepositoryCollection.getRepository(entityType);
		return repository != null ? repositoryDecoratorFactory.createDecoratedRepository(repository) : null;
	}

	@Override
	public boolean hasRepository(String name)
	{
		return decoratedRepositoryCollection.hasRepository(name);
	}

	@Override
	public boolean hasRepository(EntityType entityType)
	{
		return decoratedRepositoryCollection.hasRepository(entityType);
	}

	@Override
	public void deleteRepository(EntityType entityType)
	{
		decoratedRepositoryCollection.deleteRepository(entityType);
	}

	@Override
	public void addAttribute(EntityType entityType, AttributeMetaData attribute)
	{
		decoratedRepositoryCollection.addAttribute(entityType, attribute);
	}

	@Override
	public void updateAttribute(EntityType entityType, AttributeMetaData attr, AttributeMetaData updatedAttr)
	{
		decoratedRepositoryCollection.updateAttribute(entityType, attr, updatedAttr);
	}

	@Override
	public void deleteAttribute(EntityType entityType, AttributeMetaData attr)
	{
		decoratedRepositoryCollection.deleteAttribute(entityType, attr);
	}
}
