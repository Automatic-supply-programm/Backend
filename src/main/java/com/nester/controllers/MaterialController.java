// MaterialController.java
package com.nester.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nester.dto.ResponseResult;
import com.nester.model.Material;
import com.nester.model.MaterialBatch;
import com.nester.model.EventLog;
import com.nester.security.jwt.JwtUser;
import com.nester.service.MaterialService;
import com.nester.service.EventLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/materials")
@RequiredArgsConstructor
public class MaterialController {
    private final MaterialService materialService;
    private final EventLogService eventLogService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public void getAll(@RequestParam(defaultValue = "false") boolean archived,
                       @RequestParam(required = false) String search,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String warehouseId,
                       @RequestParam(required = false) String status,
                       HttpServletResponse response) throws IOException {
        List<Material> materials = materialService.findAll(archived);
        if (search != null && !search.isEmpty()) {
            materials = materials.stream()
                    .filter(m -> m.getArticle().contains(search) || m.getName().contains(search))
                    .collect(Collectors.toList());
        }
        if (category != null && !category.isEmpty()) {
            materials = materials.stream().filter(m -> category.equals(m.getCategory())).collect(Collectors.toList());
        }
        if (warehouseId != null && !warehouseId.isEmpty()) {
            materials = materials.stream().filter(m -> m.getWarehouses().contains(warehouseId)).collect(Collectors.toList());
        }
        if (status != null && !status.isEmpty()) {
            materials = materials.stream().filter(m -> status.equals(m.getStatus())).collect(Collectors.toList());
        }
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, materials));
    }

    @GetMapping("/deficit")
    public void getDeficit(HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        List<Material> materials = materialService.getDeficitMaterials();
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, materials));
    }

    @GetMapping("/{id}")
    public void getById(@PathVariable String id, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        try {
            Material material = materialService.findById(id);
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, material));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(e.getMessage(), null));
        }
    }

    @PostMapping
    public void create(@Valid @RequestBody Material material, Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        try {
            Material created = materialService.create(material);

            // Логируем создание
            JwtUser jwtUser = (JwtUser) auth.getPrincipal();
            EventLog eventLog = new EventLog();
            eventLog.setTimestamp(LocalDateTime.now());
            eventLog.setUserId(jwtUser.getId());
            eventLog.setUserFullName(jwtUser.getUsername());
            eventLog.setUserRole(jwtUser.getAuthorities().iterator().next().getAuthority());
            eventLog.setEventType("MATERIAL_CREATED");
            eventLog.setObjectType("MATERIAL");
            eventLog.setObjectId(created.getId());
            eventLog.setObjectNumber(created.getArticle());
            eventLog.setDescription("Создана карточка материала: " + created.getName());
            eventLog.setResult("CREATED");
            eventLogService.save(eventLog);

            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, created));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(e.getMessage(), null));
        }
    }

    @PutMapping("/{id}")
    public void update(@PathVariable String id, @RequestBody Material material, Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        try {
            Material updated = materialService.update(id, material);

            // Логируем изменение
            JwtUser jwtUser = (JwtUser) auth.getPrincipal();
            EventLog eventLog = new EventLog();
            eventLog.setTimestamp(LocalDateTime.now());
            eventLog.setUserId(jwtUser.getId());
            eventLog.setUserFullName(jwtUser.getUsername());
            eventLog.setUserRole(jwtUser.getAuthorities().iterator().next().getAuthority());
            eventLog.setEventType("MATERIAL_UPDATED");
            eventLog.setObjectType("MATERIAL");
            eventLog.setObjectId(updated.getId());
            eventLog.setObjectNumber(updated.getArticle());
            eventLog.setDescription("Изменена карточка материала: " + updated.getName());
            eventLog.setResult("UPDATED");
            eventLogService.save(eventLog);

            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, updated));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(e.getMessage(), null));
        }
    }

    @PostMapping("/{id}/batch")
    public void addBatch(@PathVariable String id, @RequestBody MaterialBatch batch, Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        try {
            batch.setBatchNumber("BATCH-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4));
            Material updated = materialService.addBatch(id, batch);

            // Логируем поступление партии
            JwtUser jwtUser = (JwtUser) auth.getPrincipal();
            EventLog eventLog = new EventLog();
            eventLog.setTimestamp(LocalDateTime.now());
            eventLog.setUserId(jwtUser.getId());
            eventLog.setUserFullName(jwtUser.getUsername());
            eventLog.setUserRole(jwtUser.getAuthorities().iterator().next().getAuthority());
            eventLog.setEventType("BATCH_RECEIVED");
            eventLog.setObjectType("BATCH");
            eventLog.setObjectId(updated.getId());
            eventLog.setObjectNumber(batch.getBatchNumber());
            eventLog.setDescription("Поступление партии материала: " + updated.getName() + ", кол-во: " + batch.getInitialQuantity());
            eventLog.setResult("RECEIVED");
            eventLogService.save(eventLog);

            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, updated));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(e.getMessage(), null));
        }
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id, Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        Material deleted = materialService.delete(id);

        // Логируем архивирование
        JwtUser jwtUser = (JwtUser) auth.getPrincipal();
        EventLog eventLog = new EventLog();
        eventLog.setTimestamp(LocalDateTime.now());
        eventLog.setUserId(jwtUser.getId());
        eventLog.setUserFullName(jwtUser.getUsername());
        eventLog.setUserRole(jwtUser.getAuthorities().iterator().next().getAuthority());
        eventLog.setEventType("MATERIAL_ARCHIVED");
        eventLog.setObjectType("MATERIAL");
        eventLog.setObjectId(deleted.getId());
        eventLog.setObjectNumber(deleted.getArticle());
        eventLog.setDescription("Архивирована карточка материала: " + deleted.getName());
        eventLog.setResult("ARCHIVED");
        eventLogService.save(eventLog);

        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, deleted));
    }
}