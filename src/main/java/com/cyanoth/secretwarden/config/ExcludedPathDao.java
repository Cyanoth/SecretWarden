package com.cyanoth.secretwarden.config;

import com.atlassian.activeobjects.tx.Transactional;
import net.java.ao.Implementation;

import java.util.List;

@Transactional
@Implementation(ExcludedPathDaoImpl.class)
public interface ExcludedPathDao {

    ExcludedPathEntity getExcludedPathById(int id);

    List<ExcludedPathEntity> getAllExcludedPaths();

    ExcludedPathEntity createExcludedPath(String excludedPathPattern) throws IllegalArgumentException;

    ExcludedPathEntity updateExcludedPath(int id, String excludedPath) throws IllegalArgumentException;

    void deleteExcludedPath(int id);

}
