package com.retrofit.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AuditDatabaseConfig implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            log.info("Verificando políticas de inmutabilidad en la tabla audit_log...");
            String sql = """
                        DO $$
                        BEGIN
                            IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'audit_log') THEN
                                IF NOT EXISTS (
                                    SELECT 1 FROM pg_proc WHERE proname = 'prevent_audit_log_modification'
                                ) THEN
                                    CREATE OR REPLACE FUNCTION prevent_audit_log_modification()
                                    RETURNS TRIGGER AS $func$
                                    BEGIN
                                        RAISE EXCEPTION 'Operación rechazada: Los registros de auditoría son estrictamente inmutables. No se permite UPDATE ni DELETE.';
                                    END;
                                    $func$ LANGUAGE plpgsql;
                                END IF;

                                IF NOT EXISTS (
                                    SELECT 1 FROM pg_trigger WHERE tgname = 'tr_audit_log_immutable'
                                ) THEN
                                    CREATE TRIGGER tr_audit_log_immutable
                                    BEFORE UPDATE OR DELETE ON audit_log
                                    FOR EACH ROW EXECUTE FUNCTION prevent_audit_log_modification();
                                END IF;
                            END IF;
                        END $$;
                    """;
            jdbcTemplate.execute(sql);
            log.info("Garantía de inmutabilidad en audit_log configurada correctamente.");
        } catch (Exception e) {
            log.warn("No se pudo aplicar el trigger de inmutabilidad en audit_log: {}", e.getMessage());
        }
    }
}
