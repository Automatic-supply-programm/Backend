package com.nester.service;

import com.nester.model.Material;
import com.nester.model.MaterialBatch;

import java.util.List;

public interface MaterialService {
    List<Material> findAll(boolean includeArchived);
    Material findById(String id);
    Material create(Material material);
    Material update(String id, Material material);
    Material delete(String id);  // soft delete - archive
    List<Material> getDeficitMaterials();
    long count();

    Material addBatch(String id, MaterialBatch batch);
}