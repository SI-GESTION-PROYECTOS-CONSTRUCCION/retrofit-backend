-- ==================================================================
-- 1. CREACIÓN DE PROYECTO, RECURSOS, PARTIDAS Y APUS
-- ==================================================================
DO $$ 
DECLARE
    v_project_id INT;
    v_item_id INT;
    
    -- Variables de Recursos (Materiales Originales)
    r_cem INT; r_are_g INT; r_are_f INT; r_pdr_c INT; r_horm INT; r_agua INT;
    r_lad_kk INT; r_lad_pand INT; r_acero INT; r_ala_8 INT; r_ala_16 INT;
    r_madera INT; r_clavos INT; r_ceramico INT; r_pegamento INT; r_pintura INT; r_imprimante INT;
    
    -- Variables de Recursos (Materiales Nuevos para llegar a ~50 partidas)
    r_pint_ext INT; r_puerta INT; r_tub_agua INT; r_tub_des INT; 
    r_inodoro INT; r_lavatorio INT; r_caja_oct INT; r_cable INT; r_tomac INT;

    -- Variables de Recursos (Mano de Obra)
    r_ope INT; r_ofi INT; r_peo INT; r_top INT;
    
    -- Variables de Recursos (Equipos)
    r_herr INT; r_mezcl INT; r_vibr INT; r_nivel INT; r_andamio INT;

BEGIN
    -- ==========================================
    -- 1. CREAR EL PROYECTO
    -- ==========================================
    INSERT INTO projects (code, name, client, location, description, start_date, status, priority, current_progress, total_budget, general_expenses_percentage, utility_percentage, created_at, updated_at)
    VALUES ('PRY-VIV-002', 'Construcción Vivienda Unifamiliar 1er Piso', 'Inmobiliaria Los Andes', 'Trujillo', 'Proyecto completo con más de 50 partidas incluyendo estructuras, acabados e instalaciones.', '2026-06-10', 'PLANNING', 'MEDIUM', 0.0, 145000.00, 10.0, 8.0, NOW(), NOW())
    RETURNING id INTO v_project_id;

    -- ==========================================
    -- 2. CREACIÓN DE RECURSOS (CATÁLOGO MAESTRO)
    -- ==========================================
    
    -- Materiales Base
    INSERT INTO resources (name, unit, base_price) VALUES ('Cemento Portland Tipo I (42.5kg)', 'bls', 28.50) RETURNING id INTO r_cem; INSERT INTO materials (id) VALUES (r_cem);
    INSERT INTO resources (name, unit, base_price) VALUES ('Arena Gruesa', 'm3', 45.00) RETURNING id INTO r_are_g; INSERT INTO materials (id) VALUES (r_are_g);
    INSERT INTO resources (name, unit, base_price) VALUES ('Arena Fina', 'm3', 50.00) RETURNING id INTO r_are_f; INSERT INTO materials (id) VALUES (r_are_f);
    INSERT INTO resources (name, unit, base_price) VALUES ('Piedra Chancada 1/2"', 'm3', 55.00) RETURNING id INTO r_pdr_c; INSERT INTO materials (id) VALUES (r_pdr_c);
    INSERT INTO resources (name, unit, base_price) VALUES ('Hormigón', 'm3', 38.00) RETURNING id INTO r_horm; INSERT INTO materials (id) VALUES (r_horm);
    INSERT INTO resources (name, unit, base_price) VALUES ('Agua', 'm3', 15.00) RETURNING id INTO r_agua; INSERT INTO materials (id) VALUES (r_agua);
    INSERT INTO resources (name, unit, base_price) VALUES ('Ladrillo King Kong 18 huecos', 'und', 0.85) RETURNING id INTO r_lad_kk; INSERT INTO materials (id) VALUES (r_lad_kk);
    INSERT INTO resources (name, unit, base_price) VALUES ('Ladrillo Pandereta', 'und', 0.65) RETURNING id INTO r_lad_pand; INSERT INTO materials (id) VALUES (r_lad_pand);
    INSERT INTO resources (name, unit, base_price) VALUES ('Acero Corrugado fy=4200', 'kg', 4.50) RETURNING id INTO r_acero; INSERT INTO materials (id) VALUES (r_acero);
    INSERT INTO resources (name, unit, base_price) VALUES ('Alambre Negro #8', 'kg', 5.20) RETURNING id INTO r_ala_8; INSERT INTO materials (id) VALUES (r_ala_8);
    INSERT INTO resources (name, unit, base_price) VALUES ('Alambre Negro #16', 'kg', 5.50) RETURNING id INTO r_ala_16; INSERT INTO materials (id) VALUES (r_ala_16);
    INSERT INTO resources (name, unit, base_price) VALUES ('Madera Tornillo para Encofrado', 'p2', 6.50) RETURNING id INTO r_madera; INSERT INTO materials (id) VALUES (r_madera);
    INSERT INTO resources (name, unit, base_price) VALUES ('Clavos para madera c/c 3"', 'kg', 6.00) RETURNING id INTO r_clavos; INSERT INTO materials (id) VALUES (r_clavos);
    INSERT INTO resources (name, unit, base_price) VALUES ('Cerámico 40x40 antideslizante', 'm2', 25.00) RETURNING id INTO r_ceramico; INSERT INTO materials (id) VALUES (r_ceramico);
    INSERT INTO resources (name, unit, base_price) VALUES ('Pegamento para Cerámicos', 'bls', 18.00) RETURNING id INTO r_pegamento; INSERT INTO materials (id) VALUES (r_pegamento);
    INSERT INTO resources (name, unit, base_price) VALUES ('Pintura Látex Lavable Interior', 'gln', 45.00) RETURNING id INTO r_pintura; INSERT INTO materials (id) VALUES (r_pintura);
    INSERT INTO resources (name, unit, base_price) VALUES ('Imprimante', 'gln', 22.00) RETURNING id INTO r_imprimante; INSERT INTO materials (id) VALUES (r_imprimante);

    -- Materiales Nuevos (Acabados, Sanitarias, Eléctricas)
    INSERT INTO resources (name, unit, base_price) VALUES ('Pintura Látex Exterior Resistente', 'gln', 55.00) RETURNING id INTO r_pint_ext; INSERT INTO materials (id) VALUES (r_pint_ext);
    INSERT INTO resources (name, unit, base_price) VALUES ('Puerta Contraplacada c/marco', 'und', 180.00) RETURNING id INTO r_puerta; INSERT INTO materials (id) VALUES (r_puerta);
    INSERT INTO resources (name, unit, base_price) VALUES ('Tubo PVC Agua Fría 1/2"', 'm', 3.50) RETURNING id INTO r_tub_agua; INSERT INTO materials (id) VALUES (r_tub_agua);
    INSERT INTO resources (name, unit, base_price) VALUES ('Tubo PVC Desagüe 4"', 'm', 8.50) RETURNING id INTO r_tub_des; INSERT INTO materials (id) VALUES (r_tub_des);
    INSERT INTO resources (name, unit, base_price) VALUES ('Inodoro Tanque Bajo Blanco', 'und', 250.00) RETURNING id INTO r_inodoro; INSERT INTO materials (id) VALUES (r_inodoro);
    INSERT INTO resources (name, unit, base_price) VALUES ('Lavatorio de Pared c/pedestal', 'und', 120.00) RETURNING id INTO r_lavatorio; INSERT INTO materials (id) VALUES (r_lavatorio);
    INSERT INTO resources (name, unit, base_price) VALUES ('Caja Octogonal PVC', 'und', 1.50) RETURNING id INTO r_caja_oct; INSERT INTO materials (id) VALUES (r_caja_oct);
    INSERT INTO resources (name, unit, base_price) VALUES ('Cable de Cobre THW 2.5 mm2', 'm', 1.80) RETURNING id INTO r_cable; INSERT INTO materials (id) VALUES (r_cable);
    INSERT INTO resources (name, unit, base_price) VALUES ('Tomacorriente Doble con Toma Tierra', 'und', 12.00) RETURNING id INTO r_tomac; INSERT INTO materials (id) VALUES (r_tomac);

    -- Mano de Obra
    INSERT INTO resources (name, unit, base_price) VALUES ('Operario', 'hh', 24.50) RETURNING id INTO r_ope; INSERT INTO labor_categories (id) VALUES (r_ope);
    INSERT INTO resources (name, unit, base_price) VALUES ('Oficial', 'hh', 19.80) RETURNING id INTO r_ofi; INSERT INTO labor_categories (id) VALUES (r_ofi);
    INSERT INTO resources (name, unit, base_price) VALUES ('Peón', 'hh', 18.20) RETURNING id INTO r_peo; INSERT INTO labor_categories (id) VALUES (r_peo);
    INSERT INTO resources (name, unit, base_price) VALUES ('Topógrafo', 'hh', 28.00) RETURNING id INTO r_top; INSERT INTO labor_categories (id) VALUES (r_top);

    -- Equipos
    INSERT INTO resources (name, unit, base_price) VALUES ('Herramientas Manuales', '%mo', 0.05) RETURNING id INTO r_herr; INSERT INTO equipments (id) VALUES (r_herr);
    INSERT INTO resources (name, unit, base_price) VALUES ('Mezcladora de concreto 9p3', 'hm', 25.00) RETURNING id INTO r_mezcl; INSERT INTO equipments (id) VALUES (r_mezcl);
    INSERT INTO resources (name, unit, base_price) VALUES ('Vibradora de concreto 4HP', 'hm', 15.00) RETURNING id INTO r_vibr; INSERT INTO equipments (id) VALUES (r_vibr);
    INSERT INTO resources (name, unit, base_price) VALUES ('Nivel Topográfico', 'hm', 12.00) RETURNING id INTO r_nivel; INSERT INTO equipments (id) VALUES (r_nivel);
    INSERT INTO resources (name, unit, base_price) VALUES ('Andamio Metálico', 'hm', 5.00) RETURNING id INTO r_andamio; INSERT INTO equipments (id) VALUES (r_andamio);

    -- ==========================================
    -- 3. CREACIÓN DE PARTIDAS Y APUS
    -- ==========================================

    -- --- 01 OBRAS PRELIMINARES ---
    INSERT INTO project_items (project_id, code, description, indent_level, item_order) VALUES (v_project_id, '01', 'OBRAS PROVISIONALES Y PRELIMINARES', 0, 1);
    
    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '01.01', 'Limpieza manual del terreno', 'm2', 120.0, 3.82, 1, 2, 40.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_peo, 1.0, 0.20, 3.64), (v_item_id, r_herr, 0.0, 0.05, 0.18);

    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '01.02', 'Trazo, niveles y replanteo', 'm2', 120.0, 2.76, 1, 3, 200.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_top, 1.0, 0.04, 1.12), (v_item_id, r_peo, 1.0, 0.04, 0.73), (v_item_id, r_madera, 0.0, 0.05, 0.33), (v_item_id, r_nivel, 1.0, 0.04, 0.48), (v_item_id, r_herr, 0.0, 0.05, 0.09);

    -- --- 02 MOVIMIENTO DE TIERRAS ---
    INSERT INTO project_items (project_id, code, description, indent_level, item_order) VALUES (v_project_id, '02', 'MOVIMIENTO DE TIERRAS', 0, 4);
    
    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '02.01', 'Excavación manual de zanjas', 'm3', 45.0, 45.50, 1, 5, 3.5) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_peo, 1.0, 2.28, 41.50), (v_item_id, r_herr, 0.0, 0.05, 2.07);

    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '02.02', 'Relleno con material propio', 'm3', 15.0, 28.50, 1, 6, 5.5) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_peo, 1.0, 1.45, 26.39), (v_item_id, r_agua, 0.0, 0.05, 0.75), (v_item_id, r_herr, 0.0, 0.05, 1.32);

    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '02.03', 'Eliminación de material excedente', 'm3', 35.0, 32.10, 1, 7, 6.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_peo, 1.0, 1.33, 24.21), (v_item_id, r_herr, 0.0, 0.05, 1.21);

    -- --- 03 OBRAS DE CONCRETO SIMPLE ---
    INSERT INTO project_items (project_id, code, description, indent_level, item_order) VALUES (v_project_id, '03', 'OBRAS DE CONCRETO SIMPLE', 0, 8);
    
    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '03.01', 'Cimiento Corrido 1:10 + 30% PG', 'm3', 25.0, 185.40, 1, 9, 25.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_cem, 0.0, 2.50, 71.25), (v_item_id, r_horm, 0.0, 0.80, 30.40), (v_item_id, r_agua, 0.0, 0.10, 1.50), (v_item_id, r_ope, 1.0, 0.32, 7.84), (v_item_id, r_ofi, 1.0, 0.32, 6.34), (v_item_id, r_peo, 4.0, 1.28, 23.30), (v_item_id, r_mezcl, 1.0, 0.32, 8.00), (v_item_id, r_herr, 0.0, 0.05, 1.87);

    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '03.02', 'Falso piso e=10cm mezcla 1:8', 'm2', 95.0, 35.20, 1, 10, 80.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_cem, 0.0, 0.35, 9.98), (v_item_id, r_horm, 0.0, 0.12, 4.56), (v_item_id, r_ope, 1.0, 0.10, 2.45), (v_item_id, r_peo, 3.0, 0.30, 5.46), (v_item_id, r_mezcl, 0.5, 0.05, 1.25), (v_item_id, r_herr, 0.0, 0.05, 0.40);

    -- --- 04 OBRAS DE CONCRETO ARMADO ---
    INSERT INTO project_items (project_id, code, description, indent_level, item_order) VALUES (v_project_id, '04', 'OBRAS DE CONCRETO ARMADO', 0, 11);
    
    -- Zapatas
    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '04.01', 'Concreto en Zapatas f''c=210 kg/cm2', 'm3', 12.0, 320.50, 1, 12, 20.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_cem, 0.0, 8.50, 242.25), (v_item_id, r_are_g, 0.0, 0.50, 22.50), (v_item_id, r_pdr_c, 0.0, 0.55, 30.25), (v_item_id, r_ope, 1.0, 0.40, 9.80), (v_item_id, r_peo, 4.0, 1.60, 29.12), (v_item_id, r_mezcl, 1.0, 0.40, 10.00), (v_item_id, r_vibr, 1.0, 0.40, 6.00);

    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '04.02', 'Acero corrugado en Zapatas', 'kg', 350.0, 6.80, 1, 13, 250.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_acero, 0.0, 1.05, 4.73), (v_item_id, r_ala_16, 0.0, 0.03, 0.17), (v_item_id, r_ope, 1.0, 0.032, 0.78), (v_item_id, r_ofi, 1.0, 0.032, 0.63), (v_item_id, r_herr, 0.0, 0.05, 0.07);

    -- Columnas
    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '04.03', 'Concreto en Columnas f''c=210 kg/cm2', 'm3', 8.5, 345.00, 1, 14, 10.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_cem, 0.0, 9.00, 256.50), (v_item_id, r_are_g, 0.0, 0.45, 20.25), (v_item_id, r_pdr_c, 0.0, 0.55, 30.25), (v_item_id, r_ope, 1.0, 0.80, 19.60), (v_item_id, r_peo, 4.0, 3.20, 58.24);

    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '04.04', 'Encofrado de Columnas', 'm2', 65.0, 48.20, 1, 15, 12.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_madera, 0.0, 4.50, 29.25), (v_item_id, r_clavos, 0.0, 0.15, 0.90), (v_item_id, r_ope, 1.0, 0.67, 16.42), (v_item_id, r_ofi, 1.0, 0.67, 13.27);

    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '04.05', 'Acero corrugado en Columnas', 'kg', 650.0, 6.95, 1, 16, 220.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_acero, 0.0, 1.05, 4.73), (v_item_id, r_ala_16, 0.0, 0.05, 0.28), (v_item_id, r_ope, 1.0, 0.036, 0.88), (v_item_id, r_ofi, 1.0, 0.036, 0.71);

    -- Vigas (NUEVO)
    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '04.06', 'Concreto en Vigas f''c=210 kg/cm2', 'm3', 7.5, 345.00, 1, 17, 10.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_cem, 0.0, 9.00, 256.50), (v_item_id, r_ope, 1.0, 0.80, 19.60), (v_item_id, r_peo, 4.0, 3.20, 58.24);

    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '04.07', 'Encofrado de Vigas', 'm2', 72.0, 48.20, 1, 18, 12.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_madera, 0.0, 4.50, 29.25), (v_item_id, r_ope, 1.0, 0.67, 16.42);

    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '04.08', 'Acero corrugado en Vigas', 'kg', 580.0, 6.95, 1, 19, 220.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_acero, 0.0, 1.05, 4.73), (v_item_id, r_ope, 1.0, 0.036, 0.88);

    -- Losa Aligerada
    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '04.09', 'Concreto en Losa Aligerada f''c=210 kg/cm2', 'm3', 14.0, 310.00, 1, 20, 25.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_cem, 0.0, 8.50, 242.25), (v_item_id, r_ope, 1.0, 0.32, 7.84), (v_item_id, r_peo, 6.0, 1.92, 34.94);

    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '04.10', 'Encofrado de Losa Aligerada', 'm2', 90.0, 35.00, 1, 21, 14.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_madera, 0.0, 2.50, 16.25), (v_item_id, r_ope, 1.0, 0.57, 13.97);

    -- --- 05 ALBAÑILERÍA ---
    INSERT INTO project_items (project_id, code, description, indent_level, item_order) VALUES (v_project_id, '05', 'ALBAÑILERÍA Y TABIQUERÍA', 0, 22);
    
    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '05.01', 'Muro de ladrillo KK de soga', 'm2', 110.0, 72.96, 1, 23, 9.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_lad_kk, 0.0, 39.0, 33.15), (v_item_id, r_ope, 1.0, 0.88, 21.56), (v_item_id, r_peo, 0.5, 0.44, 8.01);

    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '05.02', 'Muro de ladrillo Pandereta de soga', 'm2', 45.0, 58.50, 1, 24, 11.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_lad_pand, 0.0, 38.0, 24.70), (v_item_id, r_ope, 1.0, 0.72, 17.64);

    -- --- 06 REVOQUES Y ENLUCIDOS ---
    INSERT INTO project_items (project_id, code, description, indent_level, item_order) VALUES (v_project_id, '06', 'REVOQUES Y ENLUCIDOS', 0, 25);
    
    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '06.01', 'Tarrajeo en interiores', 'm2', 220.0, 24.80, 1, 26, 15.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_cem, 0.0, 0.15, 4.28), (v_item_id, r_ope, 1.0, 0.53, 12.99), (v_item_id, r_peo, 0.5, 0.26, 4.73);

    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '06.02', 'Tarrajeo de Columnas', 'm2', 45.0, 32.50, 1, 27, 10.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_cem, 0.0, 0.18, 5.13), (v_item_id, r_ope, 1.0, 0.80, 19.60);

    -- Cielorraso (NUEVO)
    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '06.03', 'Tarrajeo de Cielorraso', 'm2', 90.0, 38.50, 1, 28, 12.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_cem, 0.0, 0.20, 5.70), (v_item_id, r_ope, 1.0, 0.67, 16.41);

    -- --- 07 PISOS Y PAVIMENTOS ---
    INSERT INTO project_items (project_id, code, description, indent_level, item_order) VALUES (v_project_id, '07', 'PISOS Y PAVIMENTOS', 0, 29);
    
    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '07.01', 'Contrapiso de 2"', 'm2', 90.0, 38.00, 1, 30, 25.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_cem, 0.0, 0.45, 12.83), (v_item_id, r_ope, 1.0, 0.32, 7.84);

    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '07.02', 'Piso de Cerámico 40x40cm', 'm2', 80.0, 65.50, 1, 31, 12.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_ceramico, 0.0, 1.05, 26.25), (v_item_id, r_pegamento, 0.0, 0.25, 4.50), (v_item_id, r_ope, 1.0, 0.67, 16.42);

    -- Contrazócalo (NUEVO)
    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '07.03', 'Contrazócalo de cerámico h=10cm', 'm', 65.0, 18.50, 1, 32, 25.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_ceramico, 0.0, 0.11, 2.75), (v_item_id, r_ope, 1.0, 0.32, 7.84);

    -- --- 08 ZÓCALOS --- (NUEVO)
    INSERT INTO project_items (project_id, code, description, indent_level, item_order) VALUES (v_project_id, '08', 'ZÓCALOS', 0, 33);
    
    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '08.01', 'Zócalo de cerámico en baños h=1.80m', 'm2', 25.0, 68.00, 1, 34, 10.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_ceramico, 0.0, 1.05, 26.25), (v_item_id, r_pegamento, 0.0, 0.25, 4.50), (v_item_id, r_ope, 1.0, 0.80, 19.60);

    -- --- 09 CARPINTERÍA DE MADERA --- (NUEVO)
    INSERT INTO project_items (project_id, code, description, indent_level, item_order) VALUES (v_project_id, '09', 'CARPINTERÍA DE MADERA', 0, 35);
    
    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '09.01', 'Puertas contraplacadas de madera', 'und', 5.0, 265.00, 1, 36, 2.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_puerta, 0.0, 1.00, 180.00), (v_item_id, r_ope, 1.0, 4.00, 98.00);

    -- --- 10 PINTURA ---
    INSERT INTO project_items (project_id, code, description, indent_level, item_order) VALUES (v_project_id, '10', 'PINTURA', 0, 37);
    
    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '10.01', 'Pintura Látex en interiores 2 manos', 'm2', 220.0, 15.20, 1, 38, 35.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_pintura, 0.0, 0.08, 3.60), (v_item_id, r_imprimante, 0.0, 0.05, 1.10), (v_item_id, r_ope, 1.0, 0.23, 5.64);

    -- Pintura Exterior (NUEVO)
    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '10.02', 'Pintura Látex en exteriores', 'm2', 110.0, 18.50, 1, 39, 30.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_pint_ext, 0.0, 0.08, 4.40), (v_item_id, r_imprimante, 0.0, 0.05, 1.10), (v_item_id, r_ope, 1.0, 0.27, 6.61);

    -- --- 11 INSTALACIONES SANITARIAS --- (NUEVO)
    INSERT INTO project_items (project_id, code, description, indent_level, item_order) VALUES (v_project_id, '11', 'INSTALACIONES SANITARIAS', 0, 40);
    
    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '11.01', 'Salida de agua fría PVC 1/2"', 'pto', 12.0, 45.00, 1, 41, 8.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_tub_agua, 0.0, 3.50, 12.25), (v_item_id, r_ope, 1.0, 1.00, 24.50), (v_item_id, r_peo, 0.5, 0.50, 9.10);

    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '11.02', 'Salida de desagüe PVC 2" y 4"', 'pto', 10.0, 55.00, 1, 42, 6.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_tub_des, 0.0, 1.50, 12.75), (v_item_id, r_ope, 1.0, 1.33, 32.58);

    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '11.03', 'Suministro e instalación de Inodoro', 'und', 2.0, 320.00, 1, 43, 3.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_inodoro, 0.0, 1.00, 250.00), (v_item_id, r_ope, 1.0, 2.67, 65.41);

    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '11.04', 'Suministro e instalación de Lavatorio', 'und', 2.0, 185.00, 1, 44, 4.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_lavatorio, 0.0, 1.00, 120.00), (v_item_id, r_ope, 1.0, 2.00, 49.00);

    -- --- 12 INSTALACIONES ELÉCTRICAS --- (NUEVO)
    INSERT INTO project_items (project_id, code, description, indent_level, item_order) VALUES (v_project_id, '12', 'INSTALACIONES ELÉCTRICAS', 0, 45);
    
    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '12.01', 'Salida para centro de luz', 'pto', 15.0, 42.50, 1, 46, 8.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_caja_oct, 0.0, 1.00, 1.50), (v_item_id, r_cable, 0.0, 4.00, 7.20), (v_item_id, r_ope, 1.0, 1.00, 24.50);

    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '12.02', 'Salida para tomacorrientes', 'pto', 18.0, 48.00, 1, 47, 7.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_tomac, 0.0, 1.00, 12.00), (v_item_id, r_cable, 0.0, 5.00, 9.00), (v_item_id, r_ope, 1.0, 1.14, 27.93);

    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '12.03', 'Tablero de distribución general', 'und', 1.0, 450.00, 1, 48, 1.0) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_ope, 1.0, 8.00, 196.00);

    INSERT INTO project_items (project_id, code, description, unit, total_quantity, unit_price, indent_level, item_order, labor_yield) 
    VALUES (v_project_id, '12.04', 'Pozo de puesta a tierra', 'und', 1.0, 850.00, 1, 49, 1.5) RETURNING id INTO v_item_id;
    INSERT INTO project_item_resources (project_item_id, resource_id, squad, quantity, partial_price) VALUES (v_item_id, r_ope, 1.0, 5.33, 130.58), (v_item_id, r_peo, 2.0, 10.66, 194.01);

END $$;

-- ==================================================================
-- 2. CREACIÓN DE AVANCE, ENTRADAS, SALIDAS Y CURVA S
-- ==================================================================
DO $$ 
DECLARE
    v_project_id INT;
    v_report_id INT;
    v_current_date TIMESTAMP;
    
    -- Variables para partidas
    v_item_01_01 INT; v_item_01_02 INT; 
    v_item_02_01 INT; v_item_03_01 INT;
    v_item_04_01 INT; v_item_04_02 INT; v_item_04_03 INT; v_item_04_04 INT; v_item_04_05 INT;
    v_item_04_06 INT; v_item_04_07 INT; v_item_04_08 INT; v_item_04_09 INT; v_item_04_10 INT;
    v_item_05_01 INT; v_item_06_01 INT; v_item_07_02 INT;

    -- Variables para Recursos Materiales
    r_cem INT; r_are_g INT; r_pdr_c INT; r_horm INT; r_agua INT;
    r_lad_kk INT; r_acero INT; r_madera INT; r_ceram INT;

    -- Variables para Mano de Obra
    r_ope INT; r_peo INT; r_top INT;

BEGIN
    -- ==================================================================
    -- 1. OBTENER EL PROYECTO
    -- ==================================================================
    SELECT id INTO v_project_id 
    FROM projects 
    WHERE code = 'PRY-VIV-002' 
    ORDER BY id DESC 
    LIMIT 1;

    IF v_project_id IS NULL THEN
        RAISE EXCEPTION 'Proyecto PRY-VIV-002 no encontrado. Asegúrate de ejecutar primero el script de creación del proyecto.';
    END IF;

    -- ==================================================================
    -- 2. OBTENER IDs DE PARTIDAS (Soporta '01.01' o '1.1')
    -- ==================================================================
    SELECT id INTO v_item_01_01 FROM project_items WHERE project_id = v_project_id AND code IN ('01.01', '1.1');
    SELECT id INTO v_item_01_02 FROM project_items WHERE project_id = v_project_id AND code IN ('01.02', '1.2');
    SELECT id INTO v_item_02_01 FROM project_items WHERE project_id = v_project_id AND code IN ('02.01', '2.1');
    SELECT id INTO v_item_03_01 FROM project_items WHERE project_id = v_project_id AND code IN ('03.01', '3.1');
    SELECT id INTO v_item_04_01 FROM project_items WHERE project_id = v_project_id AND code IN ('04.01', '4.1');
    SELECT id INTO v_item_04_02 FROM project_items WHERE project_id = v_project_id AND code IN ('04.02', '4.2');
    SELECT id INTO v_item_04_03 FROM project_items WHERE project_id = v_project_id AND code IN ('04.03', '4.3');
    SELECT id INTO v_item_04_04 FROM project_items WHERE project_id = v_project_id AND code IN ('04.04', '4.4');
    SELECT id INTO v_item_04_05 FROM project_items WHERE project_id = v_project_id AND code IN ('04.05', '4.5');
    SELECT id INTO v_item_04_06 FROM project_items WHERE project_id = v_project_id AND code IN ('04.06', '4.6');
    SELECT id INTO v_item_04_07 FROM project_items WHERE project_id = v_project_id AND code IN ('04.07', '4.7');
    SELECT id INTO v_item_04_08 FROM project_items WHERE project_id = v_project_id AND code IN ('04.08', '4.8');
    SELECT id INTO v_item_04_09 FROM project_items WHERE project_id = v_project_id AND code IN ('04.09', '4.9');
    SELECT id INTO v_item_04_10 FROM project_items WHERE project_id = v_project_id AND code IN ('04.10', '4.10');
    SELECT id INTO v_item_05_01 FROM project_items WHERE project_id = v_project_id AND code IN ('05.01', '5.1');
    SELECT id INTO v_item_06_01 FROM project_items WHERE project_id = v_project_id AND code IN ('06.01', '6.1');
    SELECT id INTO v_item_07_02 FROM project_items WHERE project_id = v_project_id AND code IN ('07.02', '7.2');

    -- ==================================================================
    -- 3. OBTENER IDs DE RECURSOS
    -- ==================================================================
    SELECT id INTO r_cem FROM resources WHERE name LIKE '%Cemento%' LIMIT 1;
    SELECT id INTO r_are_g FROM resources WHERE name = 'Arena Gruesa' LIMIT 1;
    SELECT id INTO r_pdr_c FROM resources WHERE name = 'Piedra Chancada 1/2"' LIMIT 1;
    SELECT id INTO r_horm FROM resources WHERE name = 'Hormigón' LIMIT 1;
    SELECT id INTO r_agua FROM resources WHERE name = 'Agua' LIMIT 1;
    SELECT id INTO r_lad_kk FROM resources WHERE name LIKE '%Ladrillo King Kong%' LIMIT 1;
    SELECT id INTO r_acero FROM resources WHERE name LIKE '%Acero Corrugado%' LIMIT 1;
    SELECT id INTO r_madera FROM resources WHERE name LIKE '%Madera%' LIMIT 1;
    SELECT id INTO r_ceram FROM resources WHERE name LIKE '%Cerámico%' LIMIT 1;
    
    SELECT id INTO r_ope FROM resources WHERE name = 'Operario' LIMIT 1;
    SELECT id INTO r_peo FROM resources WHERE name = 'Peón' LIMIT 1;
    SELECT id INTO r_top FROM resources WHERE name = 'Topógrafo' LIMIT 1;

    -- ==================================================================
    -- ETAPA 1: COLA INFERIOR DE LA "S" (TRABAJOS LENTOS Y DE BAJO COSTO)
    -- ==================================================================
    
    -- Compras iniciales de almacén
    v_current_date := '2026-06-10 08:00:00';
    INSERT INTO inventory_transactions (project_id, resource_id, transaction_type, reason, quantity, transaction_date, created_by) 
    VALUES (v_project_id, r_madera, 'INBOUND', 'PURCHASE', 500.0, v_current_date, 'admin');

    -- Limpieza y Trazo (Costo casi plano)
    v_current_date := '2026-06-15 17:00:00';
    INSERT INTO progress_reports (project_item_id, report_date, executed_quantity, observations, created_at) 
    VALUES (v_item_01_01, v_current_date, 120.0, 'Limpieza completa', v_current_date) RETURNING id INTO v_report_id;
    INSERT INTO progress_report_resources (report_id, resource_id, theoretical_quantity, real_quantity) VALUES (v_report_id, r_peo, 24.0, 25.0);

    INSERT INTO progress_reports (project_item_id, report_date, executed_quantity, observations, created_at) 
    VALUES (v_item_01_02, v_current_date, 120.0, 'Trazo perimetral', v_current_date) RETURNING id INTO v_report_id;
    INSERT INTO progress_report_resources (report_id, resource_id, theoretical_quantity, real_quantity) VALUES (v_report_id, r_top, 4.8, 5.0), (v_report_id, r_peo, 4.8, 5.0);

    -- Excavación de zanjas (Avanza despacio el gasto)
    v_current_date := '2026-06-22 17:00:00';
    INSERT INTO progress_reports (project_item_id, report_date, executed_quantity, observations, created_at) 
    VALUES (v_item_02_01, v_current_date, 45.0, 'Excavación manual', v_current_date) RETURNING id INTO v_report_id;
    INSERT INTO progress_report_resources (report_id, resource_id, theoretical_quantity, real_quantity) VALUES (v_report_id, r_peo, 102.6, 108.0);

    -- ==================================================================
    -- ETAPA 2: LA PENDIENTE VERTICAL (DISPARO DE ESTRUCTURAS Y ACERO)
    -- ==================================================================
    
    v_current_date := '2026-06-28 08:00:00';
    INSERT INTO inventory_transactions (project_id, resource_id, transaction_type, reason, quantity, transaction_date, created_by) VALUES 
    (v_project_id, r_acero, 'INBOUND', 'PURCHASE', 3000.0, v_current_date, 'admin'),
    (v_project_id, r_cem, 'INBOUND', 'PURCHASE', 1000.0, v_current_date, 'admin');

    -- Cimientos y Acero en Zapatas (El primer gran salto)
    v_current_date := '2026-07-05 17:00:00';
    INSERT INTO progress_reports (project_item_id, report_date, executed_quantity, observations, created_at) 
    VALUES (v_item_03_01, v_current_date, 25.0, 'Cimientos corridos', v_current_date) RETURNING id INTO v_report_id;
    INSERT INTO progress_report_resources (report_id, resource_id, theoretical_quantity, real_quantity) VALUES (v_report_id, r_cem, 62.5, 65.0), (v_report_id, r_peo, 32.0, 32.0);

    INSERT INTO progress_reports (project_item_id, report_date, executed_quantity, observations, created_at) 
    VALUES (v_item_04_02, v_current_date, 350.0, 'Acero Zapatas', v_current_date) RETURNING id INTO v_report_id;
    INSERT INTO progress_report_resources (report_id, resource_id, theoretical_quantity, real_quantity) VALUES (v_report_id, r_acero, 367.5, 385.0), (v_report_id, r_ope, 11.2, 13.0);

    -- Concreto Zapatas y Acero en Columnas (Inyección fuerte de costo)
    v_current_date := '2026-07-15 17:00:00';
    INSERT INTO progress_reports (project_item_id, report_date, executed_quantity, observations, created_at) 
    VALUES (v_item_04_01, v_current_date, 12.0, 'Concreto Zapatas', v_current_date) RETURNING id INTO v_report_id;
    INSERT INTO progress_report_resources (report_id, resource_id, theoretical_quantity, real_quantity) VALUES (v_report_id, r_cem, 102.0, 104.0), (v_report_id, r_are_g, 6.0, 6.5);

    INSERT INTO progress_reports (project_item_id, report_date, executed_quantity, observations, created_at) 
    VALUES (v_item_04_05, v_current_date, 650.0, 'Acero Columnas', v_current_date) RETURNING id INTO v_report_id;
    INSERT INTO progress_report_resources (report_id, resource_id, theoretical_quantity, real_quantity) VALUES (v_report_id, r_acero, 682.5, 710.0), (v_report_id, r_ope, 23.4, 25.0);

    -- Columnas (Encofrado + Concreto)
    v_current_date := '2026-07-25 17:00:00';
    INSERT INTO progress_reports (project_item_id, report_date, executed_quantity, observations, created_at) 
    VALUES (v_item_04_04, v_current_date, 65.0, 'Encofrado Columnas', v_current_date) RETURNING id INTO v_report_id;
    INSERT INTO progress_report_resources (report_id, resource_id, theoretical_quantity, real_quantity) VALUES (v_report_id, r_madera, 292.5, 305.0), (v_report_id, r_ope, 43.5, 46.0);

    INSERT INTO progress_reports (project_item_id, report_date, executed_quantity, observations, created_at) 
    VALUES (v_item_04_03, v_current_date, 8.5, 'Concreto Columnas', v_current_date) RETURNING id INTO v_report_id;
    INSERT INTO progress_report_resources (report_id, resource_id, theoretical_quantity, real_quantity) VALUES (v_report_id, r_cem, 76.5, 78.0);

    -- Vigas y Losas (Pico más alto de gasto acumulado)
    v_current_date := '2026-08-05 17:00:00';
    INSERT INTO progress_reports (project_item_id, report_date, executed_quantity, observations, created_at) 
    VALUES (v_item_04_08, v_current_date, 580.0, 'Acero Vigas', v_current_date) RETURNING id INTO v_report_id;
    INSERT INTO progress_report_resources (report_id, resource_id, theoretical_quantity, real_quantity) VALUES (v_report_id, r_acero, 609.0, 620.0);

    INSERT INTO progress_reports (project_item_id, report_date, executed_quantity, observations, created_at) 
    VALUES (v_item_04_09, v_current_date, 14.0, 'Concreto Losa Aligerada', v_current_date) RETURNING id INTO v_report_id;
    INSERT INTO progress_report_resources (report_id, resource_id, theoretical_quantity, real_quantity) VALUES (v_report_id, r_cem, 119.0, 116.0);

    -- ==================================================================
    -- ETAPA 3: LA CABEZA PLANO / PANZA DE LA "S" (ACABADOS Y ALBAÑILERÍA)
    -- ==================================================================
    
    v_current_date := '2026-08-12 08:00:00';
    INSERT INTO inventory_transactions (project_id, resource_id, transaction_type, reason, quantity, transaction_date, created_by) 
    VALUES (v_project_id, r_lad_kk, 'INBOUND', 'PURCHASE', 5000.0, v_current_date, 'admin');

    -- Albañilería Muros 50%
    v_current_date := '2026-08-20 17:00:00';
    INSERT INTO progress_reports (project_item_id, report_date, executed_quantity, observations, created_at) 
    VALUES (v_item_05_01, v_current_date, 60.0, 'Asentado de muros 50%', v_current_date) RETURNING id INTO v_report_id;
    INSERT INTO progress_report_resources (report_id, resource_id, theoretical_quantity, real_quantity) VALUES (v_report_id, r_lad_kk, 2340.0, 2400.0), (v_report_id, r_cem, 15.0, 16.0);

    -- Albañilería Muros 100% (Sube más despacio)
    v_current_date := '2026-08-30 17:00:00';
    INSERT INTO progress_reports (project_item_id, report_date, executed_quantity, observations, created_at) 
    VALUES (v_item_05_01, v_current_date, 50.0, 'Asentado de muros 100%', v_current_date) RETURNING id INTO v_report_id;
    INSERT INTO progress_report_resources (report_id, resource_id, theoretical_quantity, real_quantity) VALUES (v_report_id, r_lad_kk, 1950.0, 1980.0);

    -- Tarrajeos y Pisos (La curva se vuelve casi horizontal hacia finales de Septiembre)
    v_current_date := '2026-09-10 17:00:00';
    INSERT INTO progress_reports (project_item_id, report_date, executed_quantity, observations, created_at) 
    VALUES (v_item_06_01, v_current_date, 200.0, 'Tarrajeo interior', v_current_date) RETURNING id INTO v_report_id;
    INSERT INTO progress_report_resources (report_id, resource_id, theoretical_quantity, real_quantity) VALUES (v_report_id, r_cem, 30.0, 31.0);

    v_current_date := '2026-09-20 17:00:00';
    INSERT INTO progress_reports (project_item_id, report_date, executed_quantity, observations, created_at) 
    VALUES (v_item_07_02, v_current_date, 80.0, 'Cerámico 100%', v_current_date) RETURNING id INTO v_report_id;
    INSERT INTO progress_report_resources (report_id, resource_id, theoretical_quantity, real_quantity) VALUES (v_report_id, r_ceram, 84.0, 85.0);

    -- ==================================================================
    -- 4. ACTUALIZAR PROGRESO GLOBAL DEL PROYECTO
    -- ==================================================================
    UPDATE projects SET current_progress = (
        SELECT COALESCE(SUM(pr_sum.total_executed * pi.unit_price), 0) / NULLIF(MAX(p.total_budget), 0) * 100 
        FROM project_items pi
        JOIN projects p ON p.id = pi.project_id
        LEFT JOIN (
            SELECT project_item_id, COALESCE(SUM(executed_quantity), 0) as total_executed
            FROM progress_reports
            GROUP BY project_item_id
        ) pr_sum ON pr_sum.project_item_id = pi.id
        WHERE pi.project_id = v_project_id AND pi.total_quantity > 0
    ) WHERE id = v_project_id;

END $$;
