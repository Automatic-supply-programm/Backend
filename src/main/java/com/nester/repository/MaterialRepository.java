// MaterialRepository.java
package com.nester.repository;

import com.nester.model.Material;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;

public interface MaterialRepository extends MongoRepository<Material, String> {
    List<Material> findByArchived(boolean archived);
    long countByArchived(boolean archived);

    @Query("{ $and: [ { archived: false }, { $expr: { $lte: [ '$currentStock', '$criticalStock' ] } } ] }")
    List<Material> findDeficitMaterials();
}