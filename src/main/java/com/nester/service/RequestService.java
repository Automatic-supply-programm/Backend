// RequestService.java
package com.nester.service;

import com.nester.model.Request;
import java.util.List;

public interface RequestService {
    List<Request> findAll(boolean includeArchived);
    Request findById(String id);
    Request create(Request request);
    Request update(String id, Request request, String jwtUserId, String userRole);
    Request archive(String id, String jwtUserId);
    long countActive();
    List<Request> findByRequester(String requesterId);
    List<Request> findByDestination(String destinationId);

    Request changeStatus(String id, String status, String comment, String userId, String username, String userRole);

    Request confirmReceipt(String id, String userId, String username, String userRole);
}