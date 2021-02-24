package com.example.accessingdatajpa;

import javax.persistence.EntityManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;

public class MockRepositoryFactory extends JpaRepositoryFactory {

    protected Log log = LogFactory.getLog(getClass());

    /**
     * Creates a new {@link JpaRepositoryFactory}.
     *
     * @param entityManager must not be {@literal null}
     */
    public MockRepositoryFactory(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public <T> T getRepository(Class<T> repositoryInterface, RepositoryFragments fragments) {
        long start = System.currentTimeMillis();
        try {
            return Mockito.mock(repositoryInterface);
            //return super.getRepository(repositoryInterface, fragments);
        } finally {
            long end = System.currentTimeMillis();
            log.debug("getRepository: " + (end - start));
        }
    }
}
