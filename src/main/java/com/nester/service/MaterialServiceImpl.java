// MaterialServiceImpl.java
package com.nester.service;

import com.nester.model.Material;
import com.nester.model.MaterialBatch;
import com.nester.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {
    private final MaterialRepository materialRepository;

    @Override
    public List<Material> findAll(boolean includeArchived) {
        if (includeArchived) {
            return materialRepository.findAll();
        }
        return materialRepository.findByArchived(false);
    }

    @Override
    public Material findById(String id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Материал не найден"));
    }

    @Override
    public Material create(Material material) {
        try {
            material.setCurrentStock(0);
            material.setLastReceiptDate(null);
            material.setLastIssueDate(null);
            return materialRepository.save(material);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("Артикул уже существует");
        }
    }

    @Override
    public Material update(String id, Material material) {
        Material existing = findById(id);
        existing.setArticle(material.getArticle());
        existing.setName(material.getName());
        existing.setCategory(material.getCategory());
        existing.setUnit(material.getUnit());
        existing.setCriticalStock(material.getCriticalStock());
        existing.setStorageLocation(material.getStorageLocation());
        existing.setWarehouses(material.getWarehouses());
        existing.setDescription(material.getDescription());
        return materialRepository.save(existing);
    }

    @Override
    public Material addBatch(String id, MaterialBatch batch) {
        Material material = findById(id);
        batch.setReceiptDate(LocalDateTime.now());
        material.getBatches().add(batch);

        // Обновляем общий остаток
        material.setCurrentStock(material.getCurrentStock() + batch.getInitialQuantity());
        material.setLastReceiptDate(LocalDateTime.now());

        return materialRepository.save(material);
    }

    @Override
    public Material delete(String id) {
        Material material = findById(id);
        material.setArchived(true);
        return materialRepository.save(material);
    }

    @Override
    public List<Material> getDeficitMaterials() {
        // Возвращаем материалы с остатком <= критическому или 0
        return materialRepository.findDeficitMaterials();
    }

    @Override
    public long count() {
        return materialRepository.countByArchived(false);
    }
}