// RequestRepository.java
package com.nester.repository;

import com.nester.model.Request;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface RequestRepository extends MongoRepository<Request, String> {
    List<Request> findByArchived(boolean archived);
    List<Request> findByRequesterId(String requesterId);
    List<Request> findByRequesterIdAndType(String requesterId, String type);
    List<Request> findByDestinationId(String destinationId);
    long countByArchived(boolean archived);
}