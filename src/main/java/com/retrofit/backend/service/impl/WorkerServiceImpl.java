package com.retrofit.backend.service.impl;

import com.retrofit.backend.annotation.AuditChange;
import com.retrofit.backend.dto.UserCreateDTO;
import com.retrofit.backend.dto.UserDTO;
import com.retrofit.backend.dto.WorkerCreateDTO;
import com.retrofit.backend.dto.WorkerDTO;
import com.retrofit.backend.model.User;
import com.retrofit.backend.model.Worker;
import com.retrofit.backend.repository.UserRepository;
import com.retrofit.backend.repository.WorkerRepository;
import com.retrofit.backend.service.AuditService;
import com.retrofit.backend.service.UserService;
import com.retrofit.backend.service.WorkerService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkerServiceImpl implements WorkerService {
    private final WorkerRepository workerRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AuditService auditService;


    @Override
    @Transactional
    @AuditChange(action = "CREATE", module = "Trabajadores")
    public WorkerDTO createWorker(WorkerCreateDTO dto) {

        if (workerRepository.existsByDni(dto.getDni())) {
            throw new IllegalArgumentException("Worker DNI already exists");
        }
        if (dto.getPhone() != null && !dto.getPhone().isBlank() && workerRepository.existsByPhone(dto.getPhone())) {
            throw new IllegalArgumentException("Worker phone already exists");
        }

        User userAccount = null;

        if (dto.getCreateAccount() != null && dto.getCreateAccount()) {
            if (dto.getEmail() == null || dto.getEmail().isBlank()) {
                throw new IllegalArgumentException("El email es obligatorio para crear una cuenta de usuario.");
            }

            // Lógica de creación de cuenta (Username = DNI si no viene uno)
            String generatedUsername = (dto.getUsername() != null && !dto.getUsername().isBlank())
                    ? dto.getUsername()
                    : dto.getDni();

            UserCreateDTO userDto = UserCreateDTO.builder()
                    .username(generatedUsername)
                    .email(dto.getEmail())
                    .password(dto.getPassword())
                    .name(dto.getName())
                    .lastName(dto.getLastName())
                    .role(dto.getRole())
                    .build();

            UserDTO savedUser = userService.registerUser(userDto);
            userAccount = userRepository.findById(savedUser.getId()).orElse(null);
        }

        String phoneToSave = (dto.getPhone() != null && !dto.getPhone().isBlank()) ? dto.getPhone() : null;

        // El worker se crea con los datos que vengan
        Worker worker = Worker.builder()
                .user(userAccount)
                .name(dto.getName())
                .lastName(dto.getLastName())
                .position(dto.getPosition())
                .dni(dto.getDni())
                .phone(phoneToSave)
                .active(true)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        return mapToDTO(workerRepository.save(worker));
    }

    @Override
    @Transactional
    public WorkerDTO updateWorker(Long id, WorkerCreateDTO dto) {
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Trabajador no encontrado"));

        WorkerDTO estadoAnterior = mapToDTO(worker);

        if (dto.getDni() != null) {
            String dniToUpdate = dto.getDni().isBlank() ? null : dto.getDni();
            if (dniToUpdate != null && !dniToUpdate.equals(worker.getDni())) {
                if (workerRepository.existsByDni(dniToUpdate)) {
                    throw new IllegalArgumentException("Worker DNI already exists");
                }
            }
            if (dniToUpdate != null) worker.setDni(dniToUpdate);
        }

        if (dto.getPhone() != null) {
            String phoneToUpdate = dto.getPhone().isBlank() ? null : dto.getPhone();
            if (phoneToUpdate != null && !phoneToUpdate.equals(worker.getPhone())) {
                if (workerRepository.existsByPhone(phoneToUpdate)) {
                    throw new IllegalArgumentException("Worker phone already exists");
                }
            }
            worker.setPhone(phoneToUpdate);
        }


        if (dto.getName() != null) worker.setName(dto.getName());
        if (dto.getLastName() != null) worker.setLastName(dto.getLastName());
        if (dto.getPosition() != null) worker.setPosition(dto.getPosition());

        worker.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        worker = workerRepository.saveAndFlush(worker);

        if (worker.getUser() != null) {
            User user = worker.getUser();
            if (dto.getName() != null) user.setName(dto.getName());
            if (dto.getLastName() != null) user.setLastName(dto.getLastName());
            if (dto.getEmail() != null) {
                String emailToUpdate = dto.getEmail().isBlank() ? null : dto.getEmail();
                user.setEmail(emailToUpdate);
            }

            userRepository.saveAndFlush(user);
        }

        WorkerDTO estadoNuevo = mapToDTO(worker);
        auditService.logAction("UPDATE", "Trabajadores", worker.getId(), estadoAnterior, estadoNuevo);
        return estadoNuevo;
    }

    @Override
    public WorkerDTO getWorkerById(Long id) {
        return workerRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException("Trabajador no encontrado"));
    }

    @Override
    public Page<WorkerDTO> getAllWorkers(String search, Boolean active, Pageable pageable) {
        String finalSearch = (search == null) ? "" : search.trim();
        return workerRepository.findWithFilters(finalSearch, active, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public List<WorkerDTO> getAvailableWorkers() {
        return workerRepository.findAvailableWorkers().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @AuditChange(action = "DELETE", module = "Trabajadores")
    public void deleteWorker(Long id) {
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Trabajador no encontrado"));
        worker.setActive(false);
        workerRepository.save(worker);
    }

    private WorkerDTO mapToDTO(Worker worker) {
        WorkerDTO.WorkerDTOBuilder builder = WorkerDTO.builder()
                .id(worker.getId())
                .dni(worker.getDni())
                .position(worker.getPosition())
                .phone(worker.getPhone())
                .active(worker.isActive())
                .name(worker.getName())
                .lastName(worker.getLastName());

        if (worker.getUser() != null && worker.getUser().getRole() != null) {
            builder.username(worker.getUser().getUsername())
                    .email(worker.getUser().getEmail())
                    .roleName(worker.getUser().getRole().getName())
                    .hasAccessAccount(true);
        } else {
            builder.hasAccessAccount(false);
        }

        return builder.build();
    }
}
