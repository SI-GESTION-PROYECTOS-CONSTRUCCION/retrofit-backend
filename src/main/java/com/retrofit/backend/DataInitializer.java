package com.retrofit.backend;

import com.retrofit.backend.model.Permission;
import com.retrofit.backend.model.RoleE;
import com.retrofit.backend.repository.PermissionRepository;
import com.retrofit.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public void run(String... args) {
        System.out.println("Iniciando carga de data inicial de Seguridad Dinámica (RBAC)...");

        try {
            createPermissionsAndRoles();
            System.out.println("Matriz de Seguridad cargada correctamente.");
        } catch (Exception e) {
            System.err.println("Error al cargar data inicial: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createPermissionsAndRoles() {
        // ==========================================
        // 1. MÓDULO: PROYECTOS (ProjectController / ProjectItemController)
        // ==========================================
        Permission pCreate = createPermissionIfNotExists("PROJECT_CREATE");
        Permission pRead   = createPermissionIfNotExists("PROJECT_READ");
        Permission pUpdate = createPermissionIfNotExists("PROJECT_UPDATE");
        Permission pDelete = createPermissionIfNotExists("PROJECT_DELETE");

        // ==========================================
        // 2. MÓDULO: RECURSOS (Material, Labor, Equipment, Resource Controllers)
        // ==========================================
        Permission rCreate = createPermissionIfNotExists("RESOURCE_CREATE");
        Permission rRead   = createPermissionIfNotExists("RESOURCE_READ");
        Permission rUpdate = createPermissionIfNotExists("RESOURCE_UPDATE");
        Permission rDelete = createPermissionIfNotExists("RESOURCE_DELETE");

        // ==========================================
        // 3. MÓDULO: REPORTES DE AVANCE (ProgressReportController)
        // ==========================================
        Permission repCreate = createPermissionIfNotExists("REPORT_CREATE");
        Permission repRead   = createPermissionIfNotExists("REPORT_READ");
        Permission repUpdate = createPermissionIfNotExists("REPORT_UPDATE");
        Permission repDelete = createPermissionIfNotExists("REPORT_DELETE");

        // ==========================================
        // 4. MÓDULO: TRABAJADORES Y ASIGNACIONES (WorkerController / ProjectAssignmentController)
        // ==========================================
        Permission wCreate = createPermissionIfNotExists("WORKER_CREATE");
        Permission wRead   = createPermissionIfNotExists("WORKER_READ");
        Permission wUpdate = createPermissionIfNotExists("WORKER_UPDATE");
        Permission wDelete = createPermissionIfNotExists("WORKER_DELETE");

        // ==========================================
        // 5. MÓDULO: USUARIOS DEL SISTEMA (UserController)
        // ==========================================
        Permission uCreate = createPermissionIfNotExists("USER_CREATE");
        Permission uRead   = createPermissionIfNotExists("USER_READ");
        Permission uUpdate = createPermissionIfNotExists("USER_UPDATE");
        Permission uDelete = createPermissionIfNotExists("USER_DELETE");

        // ==========================================
        // 6. MÓDULO: SEGURIDAD Y ROLES
        // ==========================================
        Permission secCreate = createPermissionIfNotExists("SECURITY_CREATE");
        Permission secRead   = createPermissionIfNotExists("SECURITY_READ");
        Permission secUpdate = createPermissionIfNotExists("SECURITY_UPDATE");
        Permission secDelete = createPermissionIfNotExists("SECURITY_DELETE");

        // ==========================================
        // 7. MÓDULO: INVENTARIO (Almacén)
        // ==========================================
        Permission invCreate = createPermissionIfNotExists("INVENTORY_CREATE");
        Permission invRead   = createPermissionIfNotExists("INVENTORY_READ");
        Permission invUpdate = createPermissionIfNotExists("INVENTORY_UPDATE");
        Permission invDelete = createPermissionIfNotExists("INVENTORY_DELETE");

        // ==========================================
        // 8. MÓDULO: AUDITORÍA (AuditController)
        // ==========================================
        Permission auditRead   = createPermissionIfNotExists("AUDIT_READ");


        // ==========================================
        // ASIGNACIÓN A ROLES
        // ==========================================

        // ROL: ADMIN (Tiene acceso a TODO, incluyendo AUDITORÍA)
        createRoleIfNotExists("ADMIN", "Administrador General del Sistema", Set.of(
                pCreate, pRead, pUpdate, pDelete,
                rCreate, rRead, rUpdate, rDelete,
                repCreate, repRead, repUpdate, repDelete,
                wCreate, wRead, wUpdate, wDelete,
                uCreate, uRead, uUpdate, uDelete,
                secCreate, secRead, secUpdate, secDelete,
                invCreate, invRead, invUpdate, invDelete,
                auditRead
        ));

        // ROL: INGENIERO RESIDENTE (Control Operativo de Campo)
        createRoleIfNotExists("INGENIERO_RESIDENTE", "Ingeniero Residente de Obra", Set.of(
                pRead,
                rRead,
                repCreate, repRead, repUpdate,
                wRead, wUpdate,
                invRead, invCreate
        ));

        // ROL: ALMACENERO (Control Logístico)
        createRoleIfNotExists("ALMACENERO", "Almacenero de Obra", Set.of(
                pRead,
                rRead,
                repCreate, repRead,
                invCreate, invRead
        ));
    }


    private Permission createPermissionIfNotExists(String name) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(new Permission(name)));
    }

    private void createRoleIfNotExists(String name, String description, Set<Permission> permissions) {
        RoleE role = roleRepository.findByName(name).orElse(new RoleE());
        role.setName(name);
        role.setDescription(description);
        role.setPermissions(permissions); // Inserta o actualiza la lista de permisos en la tabla intermedia
        roleRepository.save(role);
    }
}