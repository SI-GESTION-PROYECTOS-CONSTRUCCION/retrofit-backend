package com.retrofit.backend.service.impl;

import com.retrofit.backend.dto.InventoryTransactionRequestDTO;
import com.retrofit.backend.dto.InventoryTransactionResponseDTO;
import com.retrofit.backend.enums.TransactionType;
import com.retrofit.backend.model.InventoryTransaction;
import com.retrofit.backend.model.Project;
import com.retrofit.backend.model.Resource;
import com.retrofit.backend.repository.InventoryTransactionRepository;
import com.retrofit.backend.repository.ProjectItemRepository;
import com.retrofit.backend.repository.ProjectItemResourceRepository;
import com.retrofit.backend.repository.ProjectRepository;
import com.retrofit.backend.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryServiceImplTest {

    @Mock
    private InventoryTransactionRepository transactionRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ProjectItemRepository projectItemRepository;

    @Mock
    private ProjectItemResourceRepository projectItemResourceRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private InventoryTransactionRequestDTO requestDTO;
    private Project project;
    private Resource resource;

    @BeforeEach
    void setUp() {
        requestDTO = new InventoryTransactionRequestDTO();
        requestDTO.setProjectId(1L);
        requestDTO.setResourceId(2L);
        requestDTO.setReason(com.retrofit.backend.enums.TransactionReason.PURCHASE);
        requestDTO.setQuantity(java.math.BigDecimal.valueOf(100.0));
        
        project = new Project();
        project.setId(1L);
        
        resource = new com.retrofit.backend.model.Material();
        resource.setId(2L);
        resource.setName("Cement");

        // Mock Security Context
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", "password", Collections.emptyList())
        );
    }

    @Test
    void testRegisterInbound_Success() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(resourceRepository.findById(2L)).thenReturn(Optional.of(resource));

        InventoryTransaction savedTransaction = new InventoryTransaction();
        savedTransaction.setId(10L);
        savedTransaction.setProject(project);
        savedTransaction.setResource(resource);
        savedTransaction.setTransactionType(TransactionType.INBOUND);
        savedTransaction.setQuantity(java.math.BigDecimal.valueOf(100.0));
        savedTransaction.setCreatedBy("testuser");

        when(transactionRepository.save(any(InventoryTransaction.class))).thenReturn(savedTransaction);

        InventoryTransactionResponseDTO response = inventoryService.registerInbound(requestDTO);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("testuser", response.getCreatedBy());
        assertEquals(TransactionType.INBOUND, response.getTransactionType());
        verify(transactionRepository, times(1)).save(any(InventoryTransaction.class));
    }

    @Test
    void testRegisterInbound_ProjectNotFound() {
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            inventoryService.registerInbound(requestDTO);
        });

        assertEquals("Proyecto no encontrado", exception.getMessage());
        verify(resourceRepository, never()).findById(anyLong());
        verify(transactionRepository, never()).save(any(InventoryTransaction.class));
    }

    @Test
    void testRegisterInbound_ResourceNotFound() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(resourceRepository.findById(2L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            inventoryService.registerInbound(requestDTO);
        });

        assertEquals("Recurso no encontrado", exception.getMessage());
        verify(transactionRepository, never()).save(any(InventoryTransaction.class));
    }
}
