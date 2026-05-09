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
        System.out.println("Iniciando carga de data inicial...");

        try {
            createPermissionsAndRoles();
            System.out.println("Data inicial cargada correctamente.");
        } catch (Exception e) {
            System.err.println("Error al cargar data inicial: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private void createPermissionsAndRoles() {
        // PERMISOOOOOOOOSSS
        Permission adminPermission = createPermissionIfNotExists("ADMIN_ACCESS");
        Permission engineerPermission = createPermissionIfNotExists("ENGINEER_ACCESS");
        Permission keeperPermission = createPermissionIfNotExists("ALMACENER_ACCESS");

        // Roles con sus permisos
        createRoleIfNotExists("ADMIN", "ROL ADMIN", Set.of(adminPermission));
        createRoleIfNotExists("INGENIERO_RESIDENTE", "ROL INGENIERO RESIDENTE", Set.of(engineerPermission));
        createRoleIfNotExists("ALMACENERO",  "ROL ALMACENERO", Set.of(keeperPermission));
    }



    private Permission createPermissionIfNotExists(String name) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(new Permission(name)));
    }

    private void createRoleIfNotExists(String name, String description,Set<Permission> permissions) {
        if (roleRepository.findByName(name).isEmpty()) {
            RoleE role = new RoleE();
            role.setName(name);
            role.setDescription(description);
            role.setPermissions(permissions);
            roleRepository.save(role);
        }
    }
}

