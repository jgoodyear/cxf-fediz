/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.fediz.service.idp.service.jpa;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.apache.cxf.fediz.service.idp.domain.Claim;
import org.apache.cxf.fediz.service.idp.service.ClaimDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
@Transactional
public class ClaimDAOJPAImpl implements ClaimDAO {

    private static final Logger LOG = LoggerFactory.getLogger(ClaimDAOJPAImpl.class);

    private EntityManager em;

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.em = entityManager;
    }

    @Override
    public List<Claim> getClaims(int start, int size) {
        List<Claim> list = new ArrayList<>();

        Query query = em.createQuery("select c from Claim c");

        List<?> claimEntities = query
            .setFirstResult(start)
            .setMaxResults(size)
            .getResultList();

        for (Object obj : claimEntities) {
            ClaimEntity entity = (ClaimEntity) obj;
            list.add(entity2domain(entity));
        }

        return list;
    }

    @Override
    public Claim addClaim(Claim claim) {
        ClaimEntity entity = new ClaimEntity();
        domain2entity(claim, entity);
        em.persist(entity);

        LOG.debug("Claim '{}' added", claim.getClaimType());
        return entity2domain(entity);
    }

    @Override
    public Claim getClaim(String claimType) {
        return entity2domain(getClaimEntity(claimType, em));
    }

    @Override
    public void updateClaim(String claimType, Claim claim) {
        Query query = em.createQuery("select c from Claim c where c.claimtype=:claimtype");
        query.setParameter("claimtype", claimType);

        ClaimEntity claimEntity = (ClaimEntity)query.getSingleResult();

        domain2entity(claim, claimEntity);

        LOG.debug("Claim '{}' added", claim.getClaimType());
        em.persist(claimEntity);
    }

    @Override
    public void deleteClaim(String claimType) {
        Query query = em.createQuery("select c from Claim c where c.claimType=:claimtype");
        query.setParameter("claimtype", claimType);

        Object claimObj = query.getSingleResult();
        em.remove(claimObj);

        LOG.debug("Claim '{}' deleted", claimType);
    }

    static ClaimEntity getClaimEntity(String claimType, EntityManager em) {
        Query query = em.createQuery("select c from Claim c where c.claimType=:claimtype");
        query.setParameter("claimtype", claimType);

        return (ClaimEntity)query.getSingleResult();
    }

    public static void domain2entity(Claim claim, ClaimEntity entity) {
        //The ID must not be updated if the entity has got an id already (update case)
        if (claim.getId() > 0) {
            entity.setId(claim.getId());
        }
        entity.setClaimType(claim.getClaimType().toString());
        entity.setDisplayName(claim.getDisplayName());
        entity.setDescription(claim.getDescription());
    }

    public static Claim entity2domain(ClaimEntity entity) {
        Claim claim = new Claim();
        claim.setId(entity.getId());
        claim.setClaimType(URI.create(entity.getClaimType()));
        claim.setDisplayName(entity.getDisplayName());
        claim.setDescription(entity.getDescription());
        return claim;
    }

}
