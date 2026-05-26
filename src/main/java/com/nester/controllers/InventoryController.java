package com.nester.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nester.dto.ResponseResult;
import com.nester.model.Request;
import com.nester.model.RequestItem;
import com.nester.repository.RequestRepository;
import com.nester.security.jwt.JwtUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final RequestRepository requestRepository;
    private final ObjectMapper objectMapper;

    /**
     * Возвращает остатки материалов на производственном участке текущего EMPLOYEE.
     * Остаток = Σ CONFIRMED ISSUE - Σ ACCEPTED RETURN для каждого materialId.
     */
    @GetMapping("/production-line")
    public void getProductionLineInventory(Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        JwtUser jwtUser = (JwtUser) auth.getPrincipal();
        String requesterId = jwtUser.getId();

        List<Request> issued = requestRepository.findByRequesterIdAndType(requesterId, "ISSUE");
        List<Request> returned = requestRepository.findByRequesterIdAndType(requesterId, "RETURN");

        Map<String, InventoryEntry> inventory = new LinkedHashMap<>();

        for (Request r : issued) {
            if (!"CONFIRMED".equals(r.getStatus())) continue;
            if (r.getItems() == null) continue;
            for (RequestItem item : r.getItems()) {
                inventory.computeIfAbsent(item.getMaterialId(),
                        id -> new InventoryEntry(id, item.getMaterialName(), item.getUnit()))
                        .quantity += item.getQuantity();
            }
        }

        for (Request r : returned) {
            if (!"ACCEPTED".equals(r.getStatus())) continue;
            if (r.getItems() == null) continue;
            for (RequestItem item : r.getItems()) {
                InventoryEntry entry = inventory.get(item.getMaterialId());
                if (entry != null) {
                    entry.quantity -= item.getQuantity();
                }
            }
        }

        List<InventoryEntry> result = inventory.values().stream()
                .filter(e -> e.quantity > 0)
                .sorted(Comparator.comparing(e -> e.materialName))
                .collect(java.util.stream.Collectors.toList());

        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, result));
    }

    public static class InventoryEntry {
        public String materialId;
        public String materialName;
        public String unit;
        public double quantity;

        public InventoryEntry(String materialId, String materialName, String unit) {
            this.materialId = materialId;
            this.materialName = materialName;
            this.unit = unit;
        }
    }
}
