-- Publicaciones de demostración exclusivas del perfil local.
--
-- Los UUID, emails y slugs de este archivo están reservados para desarrollo. El
-- inicializador vuelve a ejecutar el script en cada arranque: los UPSERT restauran
-- el perfil base y la inserción de franjas añade únicamente las fechas que falten.

INSERT INTO "Users" (
  "id", "email", "emailNormalized", "passwordHash", "preferredLocale",
  "emailVerifiedAt", "status", "accountType", "createdAt", "updatedAt"
)
VALUES
  (
    'd0000000-0000-4000-8000-000000000001',
    'multilocal@reserly.local',
    'multilocal@reserly.local',
    '$2a$12$pH0VBVdwFsKxmWkUjReHXeXjvKRSgFmGK/5rShDfWMri3LWJXi8Oe',
    'es', CURRENT_TIMESTAMP, 'active', 'venue_business', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    'd0000000-0000-4000-8000-000000000002',
    'let-padel-ames@reserly.local',
    'let-padel-ames@reserly.local',
    '$2a$10$localDemoAccountCannotAuthenticateWithoutKnownPassword000000000',
    'es', CURRENT_TIMESTAMP, 'active', 'venue_business', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    'd0000000-0000-4000-8000-000000000003',
    'reservas@lume-de-bretema.local',
    'reservas@lume-de-bretema.local',
    '$2a$10$localDemoAccountCannotAuthenticateWithoutKnownPassword000000000',
    'es', CURRENT_TIMESTAMP, 'active', 'venue_business', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  )
ON CONFLICT ("id") DO UPDATE SET
  "email" = EXCLUDED."email",
  "emailNormalized" = EXCLUDED."emailNormalized",
  "passwordHash" = EXCLUDED."passwordHash",
  "preferredLocale" = EXCLUDED."preferredLocale",
  "emailVerifiedAt" = EXCLUDED."emailVerifiedAt",
  "status" = EXCLUDED."status",
  "accountType" = EXCLUDED."accountType",
  "updatedAt" = CURRENT_TIMESTAMP;

INSERT INTO "UserRoles" ("id", "userId", "roleId", "assignedAt")
VALUES
  (
    'd1000000-0000-4000-8000-000000000001',
    'd0000000-0000-4000-8000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    CURRENT_TIMESTAMP
  ),
  (
    'd1000000-0000-4000-8000-000000000002',
    'd0000000-0000-4000-8000-000000000002',
    '10000000-0000-0000-0000-000000000001',
    CURRENT_TIMESTAMP
  ),
  (
    'd1000000-0000-4000-8000-000000000003',
    'd0000000-0000-4000-8000-000000000003',
    '10000000-0000-0000-0000-000000000001',
    CURRENT_TIMESTAMP
  )
ON CONFLICT ("userId", "roleId") DO NOTHING;

INSERT INTO "BusinessAccounts" (
  "id", "ownerUserId", "taxCountry", "businessLegalName",
  "businessTaxIdentifier", "businessTaxIdentifierNormalized", "businessAddress",
  "businessVerificationStatus", "businessVerifiedAt", "businessVerificationExpiresAt",
  "businessVerificationProvider",
  "businessVerificationReference", "multiVenueEnabled", "createdAt", "updatedAt"
)
VALUES
  (
    'd2000000-0000-4000-8000-000000000001',
    'd0000000-0000-4000-8000-000000000001',
    'ES', 'Ames Padel Center · Demostración local', 'LOCAL-APC-001', 'LOCALAPC001',
    'Firmistáns 10A, 15895 Ames, A Coruña',
    'verified', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '365 days',
    'local_fixture', 'local-apc-001', true,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    'd2000000-0000-4000-8000-000000000002',
    'd0000000-0000-4000-8000-000000000002',
    'ES', 'LET Padel Ames · Demostración local', 'LOCAL-LET-002', 'LOCALLET002',
    'Firmistáns 10A, 15895 Ames, A Coruña',
    'verified', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '365 days',
    'local_fixture', 'local-let-002', false,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    'd2000000-0000-4000-8000-000000000003',
    'd0000000-0000-4000-8000-000000000003',
    'ES', 'Lume de Brétema · Demostración local', 'LOCAL-LUME-003', 'LOCALLUME003',
    'Rúa da Lúa Nova 18, 15705 Santiago de Compostela, A Coruña',
    'verified', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '365 days',
    'local_fixture', 'local-lume-003', false,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  )
ON CONFLICT ("id") DO UPDATE SET
  "businessLegalName" = EXCLUDED."businessLegalName",
  "businessAddress" = EXCLUDED."businessAddress",
  "businessVerificationStatus" = 'verified',
  "businessVerifiedAt" = EXCLUDED."businessVerifiedAt",
  "businessVerificationExpiresAt" = EXCLUDED."businessVerificationExpiresAt",
  "businessVerificationProvider" = EXCLUDED."businessVerificationProvider",
  "businessVerificationReference" = EXCLUDED."businessVerificationReference",
  "multiVenueEnabled" = EXCLUDED."multiVenueEnabled",
  "updatedAt" = CURRENT_TIMESTAMP;

INSERT INTO "Venues" (
  "id", "ownerUserId", "businessAccountId", "categoryId", "name", "slug",
  "description", "descriptionI18n", "defaultLocale", "servicesI18n", "rulesI18n",
  "publicTextI18n", "contactEmail", "phone", "address", "city", "province", "country",
  "postalCode", "latitude", "longitude", "mainImageUrl", "mainImageObjectKey",
  "mainImageMediaType", "mainImageSizeBytes", "mainImageWidth", "mainImageHeight",
  "status", "manualAvailabilityStatus", "showPhone", "showEmail", "publishedAt",
  "reservationFormPublished", "reservationFormFallbackApproved",
  "reservationFormPublishedAt", "createdAt", "updatedAt"
)
VALUES
  (
    'd3000000-0000-4000-8000-000000000001',
    'd0000000-0000-4000-8000-000000000001',
    'd2000000-0000-4000-8000-000000000001',
    '20000000-0000-0000-0000-000000000004',
    'Ames Padel Center', 'ames-padel-center',
    'Centro de pádel cubierto en Ames con reserva de pista por franjas.',
    '{"sourceLocale":"es","values":{"es":"Centro de pádel cubierto en Ames con reserva de pista por franjas.","en":"Indoor padel centre in Ames with court booking by time slot."}}'::jsonb,
    'es',
    '{"sourceLocale":"es","values":{"es":"Reserva de pista cubierta durante 90 minutos.","en":"Book an indoor court for 90 minutes."}}'::jsonb,
    '{"sourceLocale":"es","values":{"es":"Llega 10 minutos antes. Uso obligatorio de calzado deportivo.","en":"Arrive 10 minutes early. Sports footwear is required."}}'::jsonb,
    '{"sourceLocale":"es","values":{"es":"Publicación de demostración local. Las reservas y correos son de prueba.","en":"Local demo listing. Bookings and emails are for testing only."}}'::jsonb,
    'ames-padel-center@reserly.local', '625 76 49 12',
    'Firmistáns 10A', 'Ames', 'A Coruña', 'ES', '15895',
    42.859650, -8.651720,
    '/api/public/venue-images/d3000000-0000-4000-8000-000000000001/main',
    'dev-fixtures/venues/ames-padel-center/main.png',
    'image/png', 901862, 907, 808,
    'published', 'available', true, true, CURRENT_TIMESTAMP,
    true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    'd3000000-0000-4000-8000-000000000002',
    'd0000000-0000-4000-8000-000000000001',
    'd2000000-0000-4000-8000-000000000001',
    '20000000-0000-0000-0000-000000000004',
    'LET Padel Ames', 'let-padel-ames',
    'Instalación de pádel en Ames preparada para probar reservas y capacidad en local.',
    '{"sourceLocale":"es","values":{"es":"Instalación de pádel en Ames preparada para probar reservas y capacidad en local.","en":"Padel facility in Ames prepared for local booking and capacity testing."}}'::jsonb,
    'es',
    '{"sourceLocale":"es","values":{"es":"Reserva una pista completa durante 90 minutos.","en":"Book a full court for 90 minutes."}}'::jsonb,
    '{"sourceLocale":"es","values":{"es":"Respeta el horario de la franja y confirma el número real de jugadores.","en":"Respect the slot time and confirm the actual number of players."}}'::jsonb,
    '{"sourceLocale":"es","values":{"es":"Publicación de demostración local. El correo se captura en Mailpit.","en":"Local demo listing. Email is captured in Mailpit."}}'::jsonb,
    'let-padel-ames@reserly.local', '625 76 49 12',
    'Firmistáns 10A', 'Ames', 'A Coruña', 'ES', '15895',
    42.859650, -8.651720,
    '/api/public/venue-images/d3000000-0000-4000-8000-000000000002/main',
    'dev-fixtures/venues/let-padel-ames/main.jpg',
    'image/jpeg', 368039, 1360, 1020,
    'published', 'automatic', true, true, CURRENT_TIMESTAMP,
    true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    'd3000000-0000-4000-8000-000000000003',
    'd0000000-0000-4000-8000-000000000003',
    'd2000000-0000-4000-8000-000000000003',
    '20000000-0000-0000-0000-000000000001',
    'Lume de Brétema', 'lume-de-bretema',
    'Cocina gallega contemporánea, producto atlántico y una sala cálida en el corazón de Santiago.',
    '{"sourceLocale":"es","values":{"es":"Cocina gallega contemporánea, producto atlántico y una sala cálida en el corazón de Santiago.","en":"Contemporary Galician cuisine, Atlantic produce and a warm dining room in the heart of Santiago."}}'::jsonb,
    'es',
    '{"sourceLocale":"es","values":{"es":"Reserva de mesa para almuerzo o cena con carta de temporada y opciones vegetarianas.","en":"Lunch or dinner table booking with a seasonal menu and vegetarian options."}}'::jsonb,
    '{"sourceLocale":"es","values":{"es":"La mesa se mantiene 15 minutos. Indica alergias en el formulario y avisa con al menos 2 horas si necesitas cancelar.","en":"Tables are held for 15 minutes. Add allergies to the form and provide at least 2 hours notice for cancellations."}}'::jsonb,
    '{"sourceLocale":"es","values":{"es":"Restaurante ficticio creado exclusivamente para probar reservas y correos en desarrollo local.","en":"Fictional restaurant created exclusively for testing bookings and emails in local development."}}'::jsonb,
    'reservas@lume-de-bretema.local', '981 00 00 31',
    'Rúa da Lúa Nova 18', 'Santiago de Compostela', 'A Coruña', 'ES', '15705',
    42.881520, -8.545690,
    '/api/public/venue-images/d3000000-0000-4000-8000-000000000003/main',
    'dev-fixtures/venues/lume-de-bretema/main.png',
    'image/png', 2222649, 1536, 1024,
    'published', 'automatic', true, true, CURRENT_TIMESTAMP,
    true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  )
ON CONFLICT ("id") DO UPDATE SET
  "name" = EXCLUDED."name",
  "description" = EXCLUDED."description",
  "descriptionI18n" = EXCLUDED."descriptionI18n",
  "servicesI18n" = EXCLUDED."servicesI18n",
  "rulesI18n" = EXCLUDED."rulesI18n",
  "publicTextI18n" = EXCLUDED."publicTextI18n",
  "contactEmail" = EXCLUDED."contactEmail",
  "phone" = EXCLUDED."phone",
  "address" = EXCLUDED."address",
  "city" = EXCLUDED."city",
  "province" = EXCLUDED."province",
  "country" = EXCLUDED."country",
  "postalCode" = EXCLUDED."postalCode",
  "latitude" = EXCLUDED."latitude",
  "longitude" = EXCLUDED."longitude",
  "mainImageUrl" = EXCLUDED."mainImageUrl",
  "mainImageObjectKey" = EXCLUDED."mainImageObjectKey",
  "mainImageMediaType" = EXCLUDED."mainImageMediaType",
  "mainImageSizeBytes" = EXCLUDED."mainImageSizeBytes",
  "mainImageWidth" = EXCLUDED."mainImageWidth",
  "mainImageHeight" = EXCLUDED."mainImageHeight",
  "status" = 'published',
  "manualAvailabilityStatus" = EXCLUDED."manualAvailabilityStatus",
  "showPhone" = true,
  "showEmail" = true,
  "publishedAt" = COALESCE("Venues"."publishedAt", CURRENT_TIMESTAMP),
  "reservationFormPublished" = true,
  "reservationFormFallbackApproved" = true,
  "reservationFormPublishedAt" = COALESCE("Venues"."reservationFormPublishedAt", CURRENT_TIMESTAMP),
  "updatedAt" = CURRENT_TIMESTAMP;

INSERT INTO "VenueImages" (
  "id", "venueId", "url", "altText", "position", "objectKey",
  "mediaType", "sizeBytes", "width", "height", "createdAt"
)
VALUES
  (
    'd4000000-0000-4000-8000-000000000001',
    'd3000000-0000-4000-8000-000000000001',
    '/api/public/venue-gallery-images/d4000000-0000-4000-8000-000000000001',
    'Pistas cubiertas de Ames Padel Center', 0,
    'dev-fixtures/venues/ames-padel-center/gallery-1.jpg',
    'image/jpeg', 249898, 1360, 1016, CURRENT_TIMESTAMP
  ),
  (
    'd4000000-0000-4000-8000-000000000002',
    'd3000000-0000-4000-8000-000000000002',
    '/api/public/venue-gallery-images/d4000000-0000-4000-8000-000000000002',
    'Pistas cubiertas de LET Padel Ames', 0,
    'dev-fixtures/venues/let-padel-ames/gallery-1.jpg',
    'image/jpeg', 249898, 1360, 1016, CURRENT_TIMESTAMP
  ),
  (
    'd4000000-0000-4000-8000-000000000003',
    'd3000000-0000-4000-8000-000000000003',
    '/api/public/venue-gallery-images/d4000000-0000-4000-8000-000000000003',
    'Merluza gallega con verduras de temporada en Lume de Brétema', 0,
    'dev-fixtures/venues/lume-de-bretema/gallery-1.png',
    'image/png', 2213991, 1536, 1024, CURRENT_TIMESTAMP
  ),
  (
    'd4000000-0000-4000-8000-000000000004',
    'd3000000-0000-4000-8000-000000000003',
    '/api/public/venue-gallery-images/d4000000-0000-4000-8000-000000000004',
    'Cocina abierta y barra gastronómica de Lume de Brétema', 1,
    'dev-fixtures/venues/lume-de-bretema/gallery-2.png',
    'image/png', 2247272, 1536, 1024, CURRENT_TIMESTAMP
  )
ON CONFLICT ("id") DO UPDATE SET
  "url" = EXCLUDED."url",
  "altText" = EXCLUDED."altText",
  "position" = EXCLUDED."position",
  "objectKey" = EXCLUDED."objectKey",
  "mediaType" = EXCLUDED."mediaType",
  "sizeBytes" = EXCLUDED."sizeBytes",
  "width" = EXCLUDED."width",
  "height" = EXCLUDED."height";

INSERT INTO "VenueOpeningHours" (
  "id", "venueId", "weekday", "isClosed", "reservationsEnabled",
  "opensAt", "closesAt", "createdAt", "updatedAt"
)
SELECT
  (
    substr(md5(venue_id::text || ':' || weekday::text), 1, 8) || '-' ||
    substr(md5(venue_id::text || ':' || weekday::text), 9, 4) || '-4' ||
    substr(md5(venue_id::text || ':' || weekday::text), 14, 3) || '-8' ||
    substr(md5(venue_id::text || ':' || weekday::text), 18, 3) || '-' ||
    substr(md5(venue_id::text || ':' || weekday::text), 21, 12)
  )::uuid,
  venue_id, weekday, false, true, TIME '10:00', TIME '22:00',
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
  VALUES
    ('d3000000-0000-4000-8000-000000000001'::uuid),
    ('d3000000-0000-4000-8000-000000000002'::uuid)
) AS venues(venue_id)
CROSS JOIN generate_series(1, 7) AS weekdays(weekday)
ON CONFLICT ("venueId", "weekday") DO UPDATE SET
  "isClosed" = false,
  "reservationsEnabled" = true,
  "opensAt" = TIME '10:00',
  "closesAt" = TIME '22:00',
  "updatedAt" = CURRENT_TIMESTAMP;

-- Horario continuo simplificado para el restaurante demo. Las franjas publicadas
-- representan los dos turnos reales de almuerzo y cena dentro de este intervalo.
INSERT INTO "VenueOpeningHours" (
  "id", "venueId", "weekday", "isClosed", "reservationsEnabled",
  "opensAt", "closesAt", "createdAt", "updatedAt"
)
SELECT
  (
    substr(md5(venue_id::text || ':' || weekday::text), 1, 8) || '-' ||
    substr(md5(venue_id::text || ':' || weekday::text), 9, 4) || '-4' ||
    substr(md5(venue_id::text || ':' || weekday::text), 14, 3) || '-8' ||
    substr(md5(venue_id::text || ':' || weekday::text), 18, 3) || '-' ||
    substr(md5(venue_id::text || ':' || weekday::text), 21, 12)
  )::uuid,
  venue_id, weekday, false, true, TIME '12:30', TIME '23:30',
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
  VALUES ('d3000000-0000-4000-8000-000000000003'::uuid)
) AS venues(venue_id)
CROSS JOIN generate_series(1, 7) AS weekdays(weekday)
ON CONFLICT ("venueId", "weekday") DO UPDATE SET
  "isClosed" = false,
  "reservationsEnabled" = true,
  "opensAt" = TIME '12:30',
  "closesAt" = TIME '23:30',
  "updatedAt" = CURRENT_TIMESTAMP;

INSERT INTO "Services" (
  "id", "venueId", "name", "nameI18n", "description", "descriptionI18n",
  "durationMinutes", "capacityRequired", "isActive", "allowsAnyAvailableResource",
  "createdAt", "updatedAt"
)
VALUES
  (
    'd5000000-0000-4000-8000-000000000001',
    'd3000000-0000-4000-8000-000000000001',
    'Reserva de pista 90 min',
    '{"sourceLocale":"es","values":{"es":"Reserva de pista 90 min","en":"90-minute court booking"}}'::jsonb,
    'Pista cubierta para un grupo de hasta cuatro jugadores.',
    '{"sourceLocale":"es","values":{"es":"Pista cubierta para un grupo de hasta cuatro jugadores.","en":"Indoor court for a group of up to four players."}}'::jsonb,
    90, 1, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    'd5000000-0000-4000-8000-000000000002',
    'd3000000-0000-4000-8000-000000000002',
    'Partida de pádel 90 min',
    '{"sourceLocale":"es","values":{"es":"Partida de pádel 90 min","en":"90-minute padel match"}}'::jsonb,
    'Franja de prueba con cuatro plazas actualizadas en tiempo real.',
    '{"sourceLocale":"es","values":{"es":"Franja de prueba con cuatro plazas actualizadas en tiempo real.","en":"Test slot with four seats updated in real time."}}'::jsonb,
    90, 1, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    'd5000000-0000-4000-8000-000000000003',
    'd3000000-0000-4000-8000-000000000003',
    'Reserva de mesa',
    '{"sourceLocale":"es","values":{"es":"Reserva de mesa","en":"Table booking"}}'::jsonb,
    'Mesa durante 90 minutos para almuerzo o cena a la carta.',
    '{"sourceLocale":"es","values":{"es":"Mesa durante 90 minutos para almuerzo o cena a la carta.","en":"A table for a 90-minute à la carte lunch or dinner."}}'::jsonb,
    90, 1, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  )
ON CONFLICT ("id") DO UPDATE SET
  "name" = EXCLUDED."name",
  "nameI18n" = EXCLUDED."nameI18n",
  "description" = EXCLUDED."description",
  "descriptionI18n" = EXCLUDED."descriptionI18n",
  "durationMinutes" = EXCLUDED."durationMinutes",
  "capacityRequired" = EXCLUDED."capacityRequired",
  "isActive" = true,
  "allowsAnyAvailableResource" = true,
  "updatedAt" = CURRENT_TIMESTAMP;

-- Ocho franjas de 90 minutos durante los siguientes 31 días. ON CONFLICT permite
-- conservar reservas y bloqueos existentes mientras cada reinicio extiende el horizonte.
INSERT INTO "TimeSlots" (
  "id", "venueId", "serviceId", "date", "weekday", "startsAt", "endsAt",
  "capacity", "status", "createdByRule", "version", "createdAt", "updatedAt"
)
SELECT
  gen_random_uuid(),
  fixture."venueId",
  fixture."serviceId",
  day_value::date,
  extract(isodow from day_value)::integer,
  (TIME '10:00' + slot_number * INTERVAL '90 minutes')::time,
  (TIME '10:00' + (slot_number + 1) * INTERVAL '90 minutes')::time,
  4,
  'available',
  true,
  0,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM (
  VALUES
    (
      'd3000000-0000-4000-8000-000000000001'::uuid,
      'd5000000-0000-4000-8000-000000000001'::uuid
    ),
    (
      'd3000000-0000-4000-8000-000000000002'::uuid,
      'd5000000-0000-4000-8000-000000000002'::uuid
    )
) AS fixture("venueId", "serviceId")
CROSS JOIN generate_series(CURRENT_DATE, CURRENT_DATE + 30, INTERVAL '1 day') AS days(day_value)
CROSS JOIN generate_series(0, 7) AS slots(slot_number)
ON CONFLICT DO NOTHING;

-- Sustituye el fixture histórico LET Padel Ames por una peluquería y añade tres
-- publicaciones de categorías distintas. Se reutilizan identificadores reservados
-- para que un entorno local existente pierda también el slug antiguo al reiniciar.
UPDATE "Users" SET
  "email" = 'legacy-brisa-account@reserly.local',
  "emailNormalized" = 'legacy-brisa-account@reserly.local',
  "updatedAt" = CURRENT_TIMESTAMP
WHERE "id" = 'd0000000-0000-4000-8000-000000000002'::uuid;

UPDATE "BusinessAccounts" SET
  "businessLegalName" = 'Brisa Studio · Demostración local',
  "businessAddress" = 'Rúa do Hórreo 72, 15701 Santiago de Compostela, A Coruña',
  "updatedAt" = CURRENT_TIMESTAMP
WHERE "id" = 'd2000000-0000-4000-8000-000000000002'::uuid;

UPDATE "Venues" SET
  "ownerUserId" = 'd0000000-0000-4000-8000-000000000001'::uuid,
  "businessAccountId" = 'd2000000-0000-4000-8000-000000000001'::uuid,
  "categoryId" = '20000000-0000-0000-0000-000000000002'::uuid,
  "name" = 'Brisa Studio',
  "slug" = 'brisa-studio',
  "description" = 'Peluquería contemporánea especializada en corte, color y cuidado capilar.',
  "descriptionI18n" = '{"sourceLocale":"es","values":{"es":"Peluquería contemporánea especializada en corte, color y cuidado capilar.","en":"Contemporary hair salon specialising in cuts, colour and hair care."}}'::jsonb,
  "servicesI18n" = '{"sourceLocale":"es","values":{"es":"Corte, peinado, color y tratamientos capilares con cita previa.","en":"Cuts, styling, colour and hair treatments by appointment."}}'::jsonb,
  "rulesI18n" = '{"sourceLocale":"es","values":{"es":"Llega 5 minutos antes e indica alergias a productos cosméticos.","en":"Arrive 5 minutes early and disclose cosmetic product allergies."}}'::jsonb,
  "publicTextI18n" = '{"sourceLocale":"es","values":{"es":"Peluquería ficticia para demostración local.","en":"Fictional hair salon for local demonstration."}}'::jsonb,
  "contactEmail" = 'reservas@brisa-studio.local',
  "phone" = '981 00 00 42', "address" = 'Rúa do Hórreo 72',
  "city" = 'Santiago de Compostela', "province" = 'A Coruña', "postalCode" = '15701',
  "latitude" = 42.870910, "longitude" = -8.545110,
  "mainImageUrl" = '/api/public/venue-images/d3000000-0000-4000-8000-000000000002/main',
  "mainImageObjectKey" = 'dev-fixtures/venues/brisa-studio/main.jpg',
  "mainImageMediaType" = 'image/jpeg', "mainImageSizeBytes" = 39434,
  "mainImageWidth" = 440, "mainImageHeight" = 440, "updatedAt" = CURRENT_TIMESTAMP
WHERE "id" = 'd3000000-0000-4000-8000-000000000002'::uuid;

-- La cuenta multilocal autenticable gestiona Ames Padel Center y Brisa Studio.
-- Credenciales exclusivas de desarrollo: multilocal@reserly.local / ReserlyLocal2026!
UPDATE "Venues"
SET "notificationEmail" = lower(btrim("contactEmail"))
WHERE "id" IN (
  'd3000000-0000-4000-8000-000000000001'::uuid,
  'd3000000-0000-4000-8000-000000000002'::uuid
);

-- Si Azahar & Brasa fue creado manualmente en esta base local, estabiliza la identidad temporal
-- de su propietario para que los reinicios mantengan un acceso de desarrollo conocido.
-- Credenciales exclusivas de desarrollo: azahar@reserly.local / ReserlyLocal2026!
UPDATE "Users" owner_account
SET
  "email" = 'azahar@reserly.local',
  "emailNormalized" = 'azahar@reserly.local',
  "passwordHash" = '$2a$12$pH0VBVdwFsKxmWkUjReHXeXjvKRSgFmGK/5rShDfWMri3LWJXi8Oe',
  "emailVerifiedAt" = COALESCE(owner_account."emailVerifiedAt", CURRENT_TIMESTAMP),
  "status" = 'active',
  "accountType" = 'venue_business',
  "updatedAt" = CURRENT_TIMESTAMP
FROM "Venues" venue
WHERE venue."ownerUserId" = owner_account."id"
  AND venue."slug" = 'azahar-brasa-11176fa9'
  AND (
    owner_account."emailNormalized" = 'azahar@reserly.local'
    OR NOT EXISTS (
      SELECT 1
      FROM "Users" conflicting_account
      WHERE conflicting_account."emailNormalized" = 'azahar@reserly.local'
        AND conflicting_account."id" <> owner_account."id"
    )
  );

UPDATE "Services" SET
  "name" = 'Cita de peluquería',
  "nameI18n" = '{"sourceLocale":"es","values":{"es":"Cita de peluquería","en":"Hair salon appointment"}}'::jsonb,
  "description" = 'Servicio personalizado de 60 minutos.',
  "descriptionI18n" = '{"sourceLocale":"es","values":{"es":"Servicio personalizado de 60 minutos.","en":"Personalised 60-minute service."}}'::jsonb,
  "durationMinutes" = 60, "capacityRequired" = 1, "updatedAt" = CURRENT_TIMESTAMP
WHERE "id" = 'd5000000-0000-4000-8000-000000000002'::uuid;

UPDATE "VenueImages" SET
  "url" = '/api/public/venue-gallery-images/d4000000-0000-4000-8000-000000000002',
  "altText" = 'Zona de lavado y cuidado capilar de Brisa Studio',
  "objectKey" = 'dev-fixtures/venues/brisa-studio/gallery-1.jpg',
  "mediaType" = 'image/jpeg', "sizeBytes" = 33626, "width" = 440, "height" = 443
WHERE "id" = 'd4000000-0000-4000-8000-000000000002'::uuid;

INSERT INTO "Users" (
  "id", "email", "emailNormalized", "passwordHash", "preferredLocale",
  "emailVerifiedAt", "status", "accountType", "createdAt", "updatedAt"
) VALUES
  ('d0000000-0000-4000-8000-000000000004', 'reservas@campo-do-sar.local',
   'reservas@campo-do-sar.local', '$2a$10$localDemoAccountCannotAuthenticateWithoutKnownPassword000000000',
   'es', CURRENT_TIMESTAMP, 'active', 'venue_business', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('d0000000-0000-4000-8000-000000000005', 'reservas@norte-fitness-lab.local',
   'reservas@norte-fitness-lab.local', '$2a$10$localDemoAccountCannotAuthenticateWithoutKnownPassword000000000',
   'es', CURRENT_TIMESTAMP, 'active', 'venue_business', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('d0000000-0000-4000-8000-000000000006', 'reservas@aura-atlantica.local',
   'reservas@aura-atlantica.local', '$2a$10$localDemoAccountCannotAuthenticateWithoutKnownPassword000000000',
   'es', CURRENT_TIMESTAMP, 'active', 'venue_business', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT ("id") DO UPDATE SET
  "email" = EXCLUDED."email", "emailNormalized" = EXCLUDED."emailNormalized",
  "emailVerifiedAt" = CURRENT_TIMESTAMP, "status" = 'active', "updatedAt" = CURRENT_TIMESTAMP;

INSERT INTO "UserRoles" ("id", "userId", "roleId", "assignedAt") VALUES
  ('d1000000-0000-4000-8000-000000000004', 'd0000000-0000-4000-8000-000000000004', '10000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP),
  ('d1000000-0000-4000-8000-000000000005', 'd0000000-0000-4000-8000-000000000005', '10000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP),
  ('d1000000-0000-4000-8000-000000000006', 'd0000000-0000-4000-8000-000000000006', '10000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP)
ON CONFLICT ("userId", "roleId") DO NOTHING;

INSERT INTO "BusinessAccounts" (
  "id", "ownerUserId", "taxCountry", "businessLegalName", "businessTaxIdentifier",
  "businessTaxIdentifierNormalized", "businessAddress", "businessVerificationStatus",
  "businessVerifiedAt", "businessVerificationExpiresAt", "businessVerificationProvider",
  "businessVerificationReference", "createdAt", "updatedAt"
) VALUES
  ('d2000000-0000-4000-8000-000000000004', 'd0000000-0000-4000-8000-000000000004',
   'ES', 'Campo do Sar · Demostración local', 'LOCAL-SAR-004', 'LOCALSAR004',
   'Rúa das Brañas do Sar 9, 15702 Santiago de Compostela, A Coruña', 'verified',
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '365 days', 'local_fixture', 'local-sar-004',
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('d2000000-0000-4000-8000-000000000005', 'd0000000-0000-4000-8000-000000000005',
   'ES', 'Norte Fitness Lab · Demostración local', 'LOCAL-NFL-005', 'LOCALNFL005',
   'Rúa de Fernando III 12, 15701 Santiago de Compostela, A Coruña', 'verified',
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '365 days', 'local_fixture', 'local-nfl-005',
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('d2000000-0000-4000-8000-000000000006', 'd0000000-0000-4000-8000-000000000006',
   'ES', 'Aura Atlántica · Demostración local', 'LOCAL-AUR-006', 'LOCALAUR006',
   'Rúa Nova de Abaixo 21, 15706 Santiago de Compostela, A Coruña', 'verified',
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '365 days', 'local_fixture', 'local-aur-006',
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT ("id") DO UPDATE SET
  "businessLegalName" = EXCLUDED."businessLegalName", "businessAddress" = EXCLUDED."businessAddress",
  "businessVerificationStatus" = 'verified', "businessVerifiedAt" = CURRENT_TIMESTAMP,
  "businessVerificationExpiresAt" = EXCLUDED."businessVerificationExpiresAt", "updatedAt" = CURRENT_TIMESTAMP;

INSERT INTO "Venues" (
  "id", "ownerUserId", "businessAccountId", "categoryId", "name", "slug", "description",
  "descriptionI18n", "defaultLocale", "servicesI18n", "rulesI18n", "publicTextI18n",
  "contactEmail", "phone", "address", "city", "province", "country", "postalCode",
  "latitude", "longitude", "mainImageUrl", "mainImageObjectKey", "mainImageMediaType",
  "mainImageSizeBytes", "mainImageWidth", "mainImageHeight", "status", "manualAvailabilityStatus",
  "showPhone", "showEmail", "publishedAt", "reservationFormPublished",
  "reservationFormFallbackApproved", "reservationFormPublishedAt", "createdAt", "updatedAt"
) VALUES
  ('d3000000-0000-4000-8000-000000000004', 'd0000000-0000-4000-8000-000000000004',
   'd2000000-0000-4000-8000-000000000004', '20000000-0000-0000-0000-000000000003',
   'Campo do Sar', 'campo-do-sar', 'Campo de fútbol de césped artificial para equipos y grupos.',
   '{"sourceLocale":"es","values":{"es":"Campo de fútbol de césped artificial para equipos y grupos.","en":"Artificial turf football pitch for teams and groups."}}'::jsonb, 'es',
   '{"sourceLocale":"es","values":{"es":"Reserva del campo completo durante 90 minutos.","en":"Book the full pitch for 90 minutes."}}'::jsonb,
   '{"sourceLocale":"es","values":{"es":"Usa calzado adecuado y respeta la hora de salida.","en":"Wear suitable footwear and respect the finishing time."}}'::jsonb,
   '{"sourceLocale":"es","values":{"es":"Instalación ficticia para demostración local.","en":"Fictional facility for local demonstration."}}'::jsonb,
   'reservas@campo-do-sar.local', '981 00 00 53', 'Rúa das Brañas do Sar 9',
   'Santiago de Compostela', 'A Coruña', 'ES', '15702', 42.873340, -8.534820,
   '/api/public/venue-images/d3000000-0000-4000-8000-000000000004/main',
   'dev-fixtures/venues/campo-do-sar/main.jpg', 'image/jpeg', 44335, 440, 440,
   'published', 'automatic', true, true, CURRENT_TIMESTAMP, true, true, CURRENT_TIMESTAMP,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('d3000000-0000-4000-8000-000000000005', 'd0000000-0000-4000-8000-000000000005',
   'd2000000-0000-4000-8000-000000000005', '20000000-0000-0000-0000-000000000006',
   'Norte Fitness Lab', 'norte-fitness-lab', 'Centro deportivo con sala de fuerza y entrenamiento funcional.',
   '{"sourceLocale":"es","values":{"es":"Centro deportivo con sala de fuerza y entrenamiento funcional.","en":"Sports centre with strength and functional training areas."}}'::jsonb, 'es',
   '{"sourceLocale":"es","values":{"es":"Sesiones de entrenamiento guiado y acceso por franjas.","en":"Guided training sessions and time-slot access."}}'::jsonb,
   '{"sourceLocale":"es","values":{"es":"Trae toalla y sigue las indicaciones del entrenador.","en":"Bring a towel and follow the coach instructions."}}'::jsonb,
   '{"sourceLocale":"es","values":{"es":"Centro deportivo ficticio para demostración local.","en":"Fictional sports centre for local demonstration."}}'::jsonb,
   'reservas@norte-fitness-lab.local', '981 00 00 64', 'Rúa de Fernando III 12',
   'Santiago de Compostela', 'A Coruña', 'ES', '15701', 42.872110, -8.551640,
   '/api/public/venue-images/d3000000-0000-4000-8000-000000000005/main',
   'dev-fixtures/venues/norte-fitness-lab/main.jpg', 'image/jpeg', 49991, 440, 440,
   'published', 'automatic', true, true, CURRENT_TIMESTAMP, true, true, CURRENT_TIMESTAMP,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('d3000000-0000-4000-8000-000000000006', 'd0000000-0000-4000-8000-000000000006',
   'd2000000-0000-4000-8000-000000000006', '20000000-0000-0000-0000-000000000007',
   'Aura Atlántica', 'aura-atlantica', 'Centro de estética y bienestar con tratamientos faciales y corporales.',
   '{"sourceLocale":"es","values":{"es":"Centro de estética y bienestar con tratamientos faciales y corporales.","en":"Beauty and wellness centre offering facial and body treatments."}}'::jsonb, 'es',
   '{"sourceLocale":"es","values":{"es":"Tratamientos personalizados de belleza y relajación.","en":"Personalised beauty and relaxation treatments."}}'::jsonb,
   '{"sourceLocale":"es","values":{"es":"Comunica alergias o contraindicaciones antes de la cita.","en":"Disclose allergies or contraindications before the appointment."}}'::jsonb,
   '{"sourceLocale":"es","values":{"es":"Centro de estética ficticio para demostración local.","en":"Fictional beauty centre for local demonstration."}}'::jsonb,
   'reservas@aura-atlantica.local', '981 00 00 75', 'Rúa Nova de Abaixo 21',
   'Santiago de Compostela', 'A Coruña', 'ES', '15706', 42.869680, -8.553110,
   '/api/public/venue-images/d3000000-0000-4000-8000-000000000006/main',
   'dev-fixtures/venues/aura-atlantica/main.jpg', 'image/jpeg', 32714, 444, 440,
   'published', 'automatic', true, true, CURRENT_TIMESTAMP, true, true, CURRENT_TIMESTAMP,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT ("id") DO UPDATE SET
  "categoryId" = EXCLUDED."categoryId", "name" = EXCLUDED."name", "slug" = EXCLUDED."slug",
  "description" = EXCLUDED."description", "descriptionI18n" = EXCLUDED."descriptionI18n",
  "servicesI18n" = EXCLUDED."servicesI18n", "rulesI18n" = EXCLUDED."rulesI18n",
  "publicTextI18n" = EXCLUDED."publicTextI18n", "contactEmail" = EXCLUDED."contactEmail",
  "phone" = EXCLUDED."phone", "address" = EXCLUDED."address", "city" = EXCLUDED."city",
  "province" = EXCLUDED."province", "postalCode" = EXCLUDED."postalCode",
  "latitude" = EXCLUDED."latitude", "longitude" = EXCLUDED."longitude",
  "mainImageUrl" = EXCLUDED."mainImageUrl", "mainImageObjectKey" = EXCLUDED."mainImageObjectKey",
  "mainImageMediaType" = EXCLUDED."mainImageMediaType", "mainImageSizeBytes" = EXCLUDED."mainImageSizeBytes",
  "mainImageWidth" = EXCLUDED."mainImageWidth", "mainImageHeight" = EXCLUDED."mainImageHeight",
  "status" = 'published', "updatedAt" = CURRENT_TIMESTAMP;

INSERT INTO "VenueImages" (
  "id", "venueId", "url", "altText", "position", "objectKey", "mediaType",
  "sizeBytes", "width", "height", "createdAt"
) VALUES
  ('d4000000-0000-4000-8000-000000000005', 'd3000000-0000-4000-8000-000000000004',
   '/api/public/venue-gallery-images/d4000000-0000-4000-8000-000000000005',
   'Portería y grada del Campo do Sar', 0, 'dev-fixtures/venues/campo-do-sar/gallery-1.jpg',
   'image/jpeg', 55547, 440, 443, CURRENT_TIMESTAMP),
  ('d4000000-0000-4000-8000-000000000006', 'd3000000-0000-4000-8000-000000000005',
   '/api/public/venue-gallery-images/d4000000-0000-4000-8000-000000000006',
   'Sala de entrenamiento funcional de Norte Fitness Lab', 0,
   'dev-fixtures/venues/norte-fitness-lab/gallery-1.jpg', 'image/jpeg', 39956, 440, 443, CURRENT_TIMESTAMP),
  ('d4000000-0000-4000-8000-000000000007', 'd3000000-0000-4000-8000-000000000006',
   '/api/public/venue-gallery-images/d4000000-0000-4000-8000-000000000007',
   'Cabina de tratamientos de Aura Atlántica', 0,
   'dev-fixtures/venues/aura-atlantica/gallery-1.jpg', 'image/jpeg', 32567, 444, 443, CURRENT_TIMESTAMP)
ON CONFLICT ("id") DO UPDATE SET
  "altText" = EXCLUDED."altText", "objectKey" = EXCLUDED."objectKey", "mediaType" = EXCLUDED."mediaType",
  "sizeBytes" = EXCLUDED."sizeBytes", "width" = EXCLUDED."width", "height" = EXCLUDED."height";

INSERT INTO "Services" (
  "id", "venueId", "name", "nameI18n", "description", "descriptionI18n",
  "durationMinutes", "capacityRequired", "isActive", "allowsAnyAvailableResource",
  "createdAt", "updatedAt"
) VALUES
  ('d5000000-0000-4000-8000-000000000004', 'd3000000-0000-4000-8000-000000000004',
   'Reserva de campo 90 min', '{"sourceLocale":"es","values":{"es":"Reserva de campo 90 min","en":"90-minute pitch booking"}}'::jsonb,
   'Campo completo para entrenamiento o partido.', '{"sourceLocale":"es","values":{"es":"Campo completo para entrenamiento o partido.","en":"Full pitch for training or a match."}}'::jsonb,
   90, 1, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('d5000000-0000-4000-8000-000000000005', 'd3000000-0000-4000-8000-000000000005',
   'Entrenamiento funcional', '{"sourceLocale":"es","values":{"es":"Entrenamiento funcional","en":"Functional training"}}'::jsonb,
   'Sesión guiada para un grupo reducido.', '{"sourceLocale":"es","values":{"es":"Sesión guiada para un grupo reducido.","en":"Guided small-group session."}}'::jsonb,
   60, 1, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('d5000000-0000-4000-8000-000000000006', 'd3000000-0000-4000-8000-000000000006',
   'Tratamiento de bienestar', '{"sourceLocale":"es","values":{"es":"Tratamiento de bienestar","en":"Wellness treatment"}}'::jsonb,
   'Tratamiento facial o corporal personalizado.', '{"sourceLocale":"es","values":{"es":"Tratamiento facial o corporal personalizado.","en":"Personalised facial or body treatment."}}'::jsonb,
   60, 1, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT ("id") DO UPDATE SET
  "name" = EXCLUDED."name", "nameI18n" = EXCLUDED."nameI18n",
  "description" = EXCLUDED."description", "descriptionI18n" = EXCLUDED."descriptionI18n",
  "durationMinutes" = EXCLUDED."durationMinutes", "isActive" = true, "updatedAt" = CURRENT_TIMESTAMP;

INSERT INTO "VenueOpeningHours" (
  "id", "venueId", "weekday", "isClosed", "reservationsEnabled",
  "opensAt", "closesAt", "createdAt", "updatedAt"
)
SELECT (
    substr(md5(venue_id::text || ':' || weekday::text), 1, 8) || '-' ||
    substr(md5(venue_id::text || ':' || weekday::text), 9, 4) || '-4' ||
    substr(md5(venue_id::text || ':' || weekday::text), 14, 3) || '-8' ||
    substr(md5(venue_id::text || ':' || weekday::text), 18, 3) || '-' ||
    substr(md5(venue_id::text || ':' || weekday::text), 21, 12)
  )::uuid, venue_id, weekday, false, true, TIME '09:00', TIME '22:00',
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
  ('d3000000-0000-4000-8000-000000000004'::uuid),
  ('d3000000-0000-4000-8000-000000000005'::uuid),
  ('d3000000-0000-4000-8000-000000000006'::uuid)
) AS venues(venue_id)
CROSS JOIN generate_series(1, 7) AS weekdays(weekday)
ON CONFLICT ("venueId", "weekday") DO UPDATE SET
  "isClosed" = false, "reservationsEnabled" = true, "opensAt" = TIME '09:00',
  "closesAt" = TIME '22:00', "updatedAt" = CURRENT_TIMESTAMP;

INSERT INTO "TimeSlots" (
  "id", "venueId", "serviceId", "date", "weekday", "startsAt", "endsAt",
  "capacity", "status", "createdByRule", "version", "createdAt", "updatedAt"
)
SELECT gen_random_uuid(), fixture."venueId", fixture."serviceId", day_value::date,
  extract(isodow from day_value)::integer,
  (TIME '09:00' + slot_number * INTERVAL '2 hours')::time,
  (TIME '10:00' + slot_number * INTERVAL '2 hours')::time,
  fixture.capacity, 'available', true, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
  ('d3000000-0000-4000-8000-000000000004'::uuid, 'd5000000-0000-4000-8000-000000000004'::uuid, 22),
  ('d3000000-0000-4000-8000-000000000005'::uuid, 'd5000000-0000-4000-8000-000000000005'::uuid, 12),
  ('d3000000-0000-4000-8000-000000000006'::uuid, 'd5000000-0000-4000-8000-000000000006'::uuid, 1)
) AS fixture("venueId", "serviceId", capacity)
CROSS JOIN generate_series(CURRENT_DATE, CURRENT_DATE + 30, INTERVAL '1 day') AS days(day_value)
CROSS JOIN generate_series(0, 5) AS slots(slot_number)
ON CONFLICT DO NOTHING;

-- Dos turnos de almuerzo y dos de cena durante 31 días. La capacidad representa
-- comensales y permite verificar visualmente su reducción después de cada reserva.
DELETE FROM "TimeSlots" AS obsolete
WHERE obsolete."venueId" = 'd3000000-0000-4000-8000-000000000003'::uuid
  AND obsolete."startsAt" = TIME '22:00'
  AND obsolete."endsAt" = TIME '23:30'
  AND NOT EXISTS (
    SELECT 1 FROM "Reservations" AS reservation
    WHERE reservation."timeSlotId" = obsolete."id"
  );

INSERT INTO "TimeSlots" (
  "id", "venueId", "serviceId", "date", "weekday", "startsAt", "endsAt",
  "capacity", "status", "createdByRule", "version", "createdAt", "updatedAt"
)
SELECT
  gen_random_uuid(),
  'd3000000-0000-4000-8000-000000000003'::uuid,
  'd5000000-0000-4000-8000-000000000003'::uuid,
  day_value::date,
  extract(isodow from day_value)::integer,
  turn."startsAt",
  turn."endsAt",
  18,
  'available',
  true,
  0,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM generate_series(CURRENT_DATE, CURRENT_DATE + 30, INTERVAL '1 day') AS days(day_value)
CROSS JOIN (
  VALUES
    (TIME '13:00', TIME '14:30'),
    (TIME '14:30', TIME '16:00'),
    (TIME '20:30', TIME '22:00'),
    (TIME '21:00', TIME '22:30')
) AS turn("startsAt", "endsAt")
ON CONFLICT DO NOTHING;

-- Información editorial inventada para que cada categoría pueda demostrar su oferta y precios.
-- Se publica como HTML seguro localizado mediante el mismo contrato que usan los propietarios.
DELETE FROM "VenueCustomTabs"
WHERE "venueId" IN (
  'd3000000-0000-4000-8000-000000000001'::uuid,
  'd3000000-0000-4000-8000-000000000002'::uuid,
  'd3000000-0000-4000-8000-000000000003'::uuid,
  'd3000000-0000-4000-8000-000000000004'::uuid,
  'd3000000-0000-4000-8000-000000000005'::uuid,
  'd3000000-0000-4000-8000-000000000006'::uuid
);

INSERT INTO "VenueCustomTabs" (
  "id", "venueId", "position", "isActive", "titleI18n", "contentI18n",
  "contentFormat", "createdAt", "updatedAt"
) VALUES
  (
    'd6000000-0000-4000-8000-000000000001',
    'd3000000-0000-4000-8000-000000000001', 0, true,
    '{"sourceLocale":"es","values":{"es":"Tarifas y alquiler","en":"Rates and equipment hire"}}'::jsonb,
    '{"sourceLocale":"es","values":{"es":"<h3>Pistas y material</h3><ul><li><strong>Pista cubierta, 90 min:</strong> 28 &euro;</li><li><strong>Hora valle, 90 min:</strong> 22 &euro;</li><li><strong>Bono de 5 reservas:</strong> 125 &euro;</li><li><strong>Alquiler de pala:</strong> 3 &euro;</li><li><strong>Bote de pelotas:</strong> 6,50 &euro;</li></ul><p>La reserva incluye vestuario y aparcamiento.</p>","en":"<h3>Courts and equipment</h3><ul><li><strong>Indoor court, 90 min:</strong> &euro;28</li><li><strong>Off-peak court, 90 min:</strong> &euro;22</li><li><strong>Five-booking pass:</strong> &euro;125</li><li><strong>Racket hire:</strong> &euro;3</li><li><strong>Ball tube:</strong> &euro;6.50</li></ul><p>Changing rooms and parking are included.</p>"}}'::jsonb,
    'safe_html', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    'd6000000-0000-4000-8000-000000000002',
    'd3000000-0000-4000-8000-000000000002', 0, true,
    '{"sourceLocale":"es","values":{"es":"Estilos y precios","en":"Styles and prices"}}'::jsonb,
    '{"sourceLocale":"es","values":{"es":"<h3>Cortes, color y acabado</h3><ul><li><strong>Corte clásico o degradado:</strong> 18 &euro;</li><li><strong>Corte bob o capas largas:</strong> 29 &euro;</li><li><strong>Peinado con ondas:</strong> 24 &euro;</li><li><strong>Balayage luminoso:</strong> desde 72 &euro;</li><li><strong>Color completo:</strong> desde 48 &euro;</li><li><strong>Tratamiento de hidratación:</strong> 22 &euro;</li></ul><p>El diagnóstico previo y el lavado están incluidos.</p>","en":"<h3>Cuts, colour and finish</h3><ul><li><strong>Classic cut or fade:</strong> &euro;18</li><li><strong>Bob or long layers:</strong> &euro;29</li><li><strong>Wavy blow-dry:</strong> &euro;24</li><li><strong>Luminous balayage:</strong> from &euro;72</li><li><strong>Full colour:</strong> from &euro;48</li><li><strong>Hydration treatment:</strong> &euro;22</li></ul><p>Consultation and wash are included.</p>"}}'::jsonb,
    'safe_html', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    'd6000000-0000-4000-8000-000000000003',
    'd3000000-0000-4000-8000-000000000003', 0, true,
    '{"sourceLocale":"es","values":{"es":"Carta y precios","en":"Menu and prices"}}'::jsonb,
    '{"sourceLocale":"es","values":{"es":"<h3>Entrantes</h3><ul><li><strong>Croquetas de centollo:</strong> 12 &euro;</li><li><strong>Tomate, queso de Arzúa y albahaca:</strong> 11 &euro;</li></ul><h3>Principales</h3><ul><li><strong>Merluza atlántica con verduras:</strong> 24 &euro;</li><li><strong>Arroz meloso de setas:</strong> 19 &euro;</li><li><strong>Carrillera de ternera gallega:</strong> 23 &euro;</li></ul><h3>Postres</h3><ul><li><strong>Tarta cremosa de Santiago:</strong> 7 &euro;</li><li><strong>Menú degustación de cinco pases:</strong> 46 &euro;</li></ul>","en":"<h3>Starters</h3><ul><li><strong>Spider crab croquettes:</strong> &euro;12</li><li><strong>Tomato, Arzúa cheese and basil:</strong> &euro;11</li></ul><h3>Main courses</h3><ul><li><strong>Atlantic hake with vegetables:</strong> &euro;24</li><li><strong>Creamy mushroom rice:</strong> &euro;19</li><li><strong>Galician beef cheek:</strong> &euro;23</li></ul><h3>Desserts</h3><ul><li><strong>Creamy Santiago cake:</strong> &euro;7</li><li><strong>Five-course tasting menu:</strong> &euro;46</li></ul>"}}'::jsonb,
    'safe_html', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    'd6000000-0000-4000-8000-000000000004',
    'd3000000-0000-4000-8000-000000000004', 0, true,
    '{"sourceLocale":"es","values":{"es":"Modalidades y tarifas","en":"Formats and rates"}}'::jsonb,
    '{"sourceLocale":"es","values":{"es":"<h3>Alquiler del campo</h3><ul><li><strong>Fútbol 7, 90 min:</strong> 68 &euro;</li><li><strong>Fútbol 11, 90 min:</strong> 118 &euro;</li><li><strong>Entrenamiento escolar, 60 min:</strong> 44 &euro;</li><li><strong>Iluminación nocturna:</strong> 12 &euro;</li><li><strong>Petos y balones:</strong> 8 &euro;</li></ul><p>Incluye dos vestuarios y acceso 15 minutos antes.</p>","en":"<h3>Pitch hire</h3><ul><li><strong>Seven-a-side, 90 min:</strong> &euro;68</li><li><strong>Eleven-a-side, 90 min:</strong> &euro;118</li><li><strong>School training, 60 min:</strong> &euro;44</li><li><strong>Floodlights:</strong> &euro;12</li><li><strong>Bibs and balls:</strong> &euro;8</li></ul><p>Two changing rooms and access 15 minutes early are included.</p>"}}'::jsonb,
    'safe_html', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    'd6000000-0000-4000-8000-000000000005',
    'd3000000-0000-4000-8000-000000000005', 0, true,
    '{"sourceLocale":"es","values":{"es":"Entrenamientos y cuotas","en":"Training and memberships"}}'::jsonb,
    '{"sourceLocale":"es","values":{"es":"<h3>Sesiones y bonos</h3><ul><li><strong>Entrenamiento funcional en grupo:</strong> 12 &euro;</li><li><strong>Sesión personal, 60 min:</strong> 38 &euro;</li><li><strong>Valoración inicial:</strong> 25 &euro;</li><li><strong>Bono de 8 sesiones:</strong> 82 &euro;</li><li><strong>Cuota mensual libre:</strong> 39 &euro;</li></ul><p>Las sesiones guiadas admiten un máximo de 12 personas.</p>","en":"<h3>Sessions and passes</h3><ul><li><strong>Group functional training:</strong> &euro;12</li><li><strong>Personal session, 60 min:</strong> &euro;38</li><li><strong>Initial assessment:</strong> &euro;25</li><li><strong>Eight-session pass:</strong> &euro;82</li><li><strong>Open monthly membership:</strong> &euro;39</li></ul><p>Guided sessions are limited to 12 people.</p>"}}'::jsonb,
    'safe_html', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    'd6000000-0000-4000-8000-000000000006',
    'd3000000-0000-4000-8000-000000000006', 0, true,
    '{"sourceLocale":"es","values":{"es":"Tratamientos y precios","en":"Treatments and prices"}}'::jsonb,
    '{"sourceLocale":"es","values":{"es":"<h3>Facial y corporal</h3><ul><li><strong>Higiene facial atlántica:</strong> 42 &euro;</li><li><strong>Ritual hidratante con algas:</strong> 55 &euro;</li><li><strong>Masaje relajante, 50 min:</strong> 48 &euro;</li><li><strong>Tratamiento corporal drenante:</strong> 62 &euro;</li><li><strong>Diseño y laminado de cejas:</strong> 31 &euro;</li></ul><p>Cada cita comienza con una valoración de piel y contraindicaciones.</p>","en":"<h3>Face and body</h3><ul><li><strong>Atlantic facial cleanse:</strong> &euro;42</li><li><strong>Seaweed hydration ritual:</strong> &euro;55</li><li><strong>Relaxing massage, 50 min:</strong> &euro;48</li><li><strong>Draining body treatment:</strong> &euro;62</li><li><strong>Brow design and lamination:</strong> &euro;31</li></ul><p>Each appointment starts with a skin and contraindication assessment.</p>"}}'::jsonb,
    'safe_html', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  )
ON CONFLICT ("id") DO UPDATE SET
  "venueId" = EXCLUDED."venueId", "position" = EXCLUDED."position",
  "isActive" = true, "titleI18n" = EXCLUDED."titleI18n",
  "contentI18n" = EXCLUDED."contentI18n", "contentFormat" = 'safe_html',
  "updatedAt" = CURRENT_TIMESTAMP;

-- Clínica privada multidisciplinar para comprobar el recorrido especialidad ->
-- profesional -> fecha -> hora exacta. Pertenece a la cuenta multilocal autenticable
-- y todas sus identidades, imágenes y citas son exclusivamente datos ficticios.
INSERT INTO "Venues" (
  "id", "ownerUserId", "businessAccountId", "categoryId", "name", "slug",
  "description", "descriptionI18n", "defaultLocale", "servicesI18n", "rulesI18n",
  "publicTextI18n", "contactEmail", "phone", "address", "city", "province", "country",
  "postalCode", "latitude", "longitude", "mainImageUrl", "mainImageObjectKey",
  "mainImageMediaType", "mainImageSizeBytes", "mainImageWidth", "mainImageHeight",
  "status", "manualAvailabilityStatus", "showPhone", "showEmail", "publishedAt",
  "reservationFormPublished", "reservationFormFallbackApproved",
  "reservationFormPublishedAt", "createdAt", "updatedAt"
) VALUES (
  'e3000000-0000-4000-8000-000000000001',
  'd0000000-0000-4000-8000-000000000001',
  'd2000000-0000-4000-8000-000000000001',
  '20000000-0000-0000-0000-000000000008',
  'Clínica Alba Integral', 'clinica-alba-integral',
  'Clínica privada multidisciplinar en Santiago de Compostela con atención personalizada en psiquiatría, ginecología y psicología clínica.',
  '{"sourceLocale":"es","values":{"es":"Clínica privada multidisciplinar en Santiago de Compostela con atención personalizada en psiquiatría, ginecología y psicología clínica.","en":"Private multidisciplinary clinic in Santiago de Compostela offering personalised psychiatry, gynaecology and clinical psychology care."}}'::jsonb,
  'es',
  '{"sourceLocale":"es","values":{"es":"Consultas presenciales con profesionales identificados y cita previa a una hora exacta.","en":"In-person consultations with named professionals booked at an exact appointment time."}}'::jsonb,
  '{"sourceLocale":"es","values":{"es":"Llega 10 minutos antes. Puedes cancelar con 24 horas de antelación. Si es tu primera visita, trae la documentación clínica relevante.","en":"Arrive 10 minutes early. You may cancel up to 24 hours in advance. Bring relevant clinical documents to your first visit."}}'::jsonb,
  '{"sourceLocale":"es","values":{"es":"Clínica y profesionales ficticios creados exclusivamente para comprobar el flujo de reservas en desarrollo local. No introduzcas datos médicos reales.","en":"Fictional clinic and professionals created solely to test the local booking flow. Do not enter real medical information."}}'::jsonb,
  'citas@clinica-alba.local', '981 00 24 80',
  'Rúa de Rosalía de Castro 48', 'Santiago de Compostela', 'A Coruña', 'ES', '15701',
  42.873710, -8.548240,
  '/api/public/venue-images/e3000000-0000-4000-8000-000000000001/main',
  'dev-fixtures/venues/clinica-alba-integral/main.png',
  'image/png', 2278206, 1536, 1024,
  'published', 'automatic', true, true, CURRENT_TIMESTAMP,
  true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT ("id") DO UPDATE SET
  "ownerUserId" = EXCLUDED."ownerUserId", "businessAccountId" = EXCLUDED."businessAccountId",
  "categoryId" = EXCLUDED."categoryId", "name" = EXCLUDED."name", "slug" = EXCLUDED."slug",
  "description" = EXCLUDED."description", "descriptionI18n" = EXCLUDED."descriptionI18n",
  "servicesI18n" = EXCLUDED."servicesI18n", "rulesI18n" = EXCLUDED."rulesI18n",
  "publicTextI18n" = EXCLUDED."publicTextI18n", "contactEmail" = EXCLUDED."contactEmail",
  "phone" = EXCLUDED."phone", "address" = EXCLUDED."address", "city" = EXCLUDED."city",
  "province" = EXCLUDED."province", "country" = EXCLUDED."country",
  "postalCode" = EXCLUDED."postalCode", "latitude" = EXCLUDED."latitude",
  "longitude" = EXCLUDED."longitude", "mainImageUrl" = EXCLUDED."mainImageUrl",
  "mainImageObjectKey" = EXCLUDED."mainImageObjectKey",
  "mainImageMediaType" = EXCLUDED."mainImageMediaType",
  "mainImageSizeBytes" = EXCLUDED."mainImageSizeBytes",
  "mainImageWidth" = EXCLUDED."mainImageWidth", "mainImageHeight" = EXCLUDED."mainImageHeight",
  "status" = 'published', "manualAvailabilityStatus" = 'automatic',
  "showPhone" = true, "showEmail" = true,
  "publishedAt" = COALESCE("Venues"."publishedAt", CURRENT_TIMESTAMP),
  "reservationFormPublished" = true, "reservationFormFallbackApproved" = true,
  "reservationFormPublishedAt" = COALESCE("Venues"."reservationFormPublishedAt", CURRENT_TIMESTAMP),
  "updatedAt" = CURRENT_TIMESTAMP;

INSERT INTO "VenueOpeningHours" (
  "id", "venueId", "weekday", "isClosed", "reservationsEnabled",
  "opensAt", "closesAt", "createdAt", "updatedAt"
)
SELECT (
    substr(md5(venue_id::text || ':' || weekday::text), 1, 8) || '-' ||
    substr(md5(venue_id::text || ':' || weekday::text), 9, 4) || '-4' ||
    substr(md5(venue_id::text || ':' || weekday::text), 14, 3) || '-8' ||
    substr(md5(venue_id::text || ':' || weekday::text), 18, 3) || '-' ||
    substr(md5(venue_id::text || ':' || weekday::text), 21, 12)
  )::uuid,
  venue_id, weekday, weekday > 5, weekday <= 5,
  CASE WHEN weekday <= 5 THEN TIME '08:30' ELSE NULL END,
  CASE WHEN weekday <= 5 THEN TIME '20:00' ELSE NULL END,
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES ('e3000000-0000-4000-8000-000000000001'::uuid)) AS venues(venue_id)
CROSS JOIN generate_series(1, 7) AS weekdays(weekday)
ON CONFLICT ("venueId", "weekday") DO UPDATE SET
  "isClosed" = EXCLUDED."isClosed", "reservationsEnabled" = EXCLUDED."reservationsEnabled",
  "opensAt" = EXCLUDED."opensAt", "closesAt" = EXCLUDED."closesAt",
  "updatedAt" = CURRENT_TIMESTAMP;

INSERT INTO "Services" (
  "id", "venueId", "name", "nameI18n", "description", "descriptionI18n",
  "durationMinutes", "capacityRequired", "isActive", "allowsAnyAvailableResource",
  "bookingMode", "createdAt", "updatedAt"
) VALUES
  (
    'e5000000-0000-4000-8000-000000000001', 'e3000000-0000-4000-8000-000000000001',
    'Psiquiatría', '{"sourceLocale":"es","values":{"es":"Psiquiatría","en":"Psychiatry"}}'::jsonb,
    'Primera consulta o seguimiento individual con un especialista.',
    '{"sourceLocale":"es","values":{"es":"Primera consulta o seguimiento individual con un especialista.","en":"Initial or follow-up individual consultation with a specialist."}}'::jsonb,
    45, 1, true, false, 'exact_time', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    'e5000000-0000-4000-8000-000000000002', 'e3000000-0000-4000-8000-000000000001',
    'Ginecología', '{"sourceLocale":"es","values":{"es":"Ginecología","en":"Gynaecology"}}'::jsonb,
    'Consulta ginecológica de valoración o seguimiento.',
    '{"sourceLocale":"es","values":{"es":"Consulta ginecológica de valoración o seguimiento.","en":"Gynaecological assessment or follow-up consultation."}}'::jsonb,
    30, 1, true, false, 'exact_time', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    'e5000000-0000-4000-8000-000000000003', 'e3000000-0000-4000-8000-000000000001',
    'Psicología clínica', '{"sourceLocale":"es","values":{"es":"Psicología clínica","en":"Clinical psychology"}}'::jsonb,
    'Sesión individual de evaluación o intervención psicológica.',
    '{"sourceLocale":"es","values":{"es":"Sesión individual de evaluación o intervención psicológica.","en":"Individual psychological assessment or therapy session."}}'::jsonb,
    50, 1, true, false, 'exact_time', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  )
ON CONFLICT ("id") DO UPDATE SET
  "name" = EXCLUDED."name", "nameI18n" = EXCLUDED."nameI18n",
  "description" = EXCLUDED."description", "descriptionI18n" = EXCLUDED."descriptionI18n",
  "durationMinutes" = EXCLUDED."durationMinutes", "capacityRequired" = 1,
  "isActive" = true, "allowsAnyAvailableResource" = false,
  "bookingMode" = 'exact_time', "updatedAt" = CURRENT_TIMESTAMP;

INSERT INTO "EmployeeResources" (
  "id", "venueId", "type", "firstName", "lastName", "publicAlias", "photoUrl",
  "specialty", "description", "status", "publicVisibility", "internalNotes",
  "createdAt", "updatedAt"
) VALUES
  ('e6000000-0000-4000-8000-000000000001', 'e3000000-0000-4000-8000-000000000001',
   'professional', 'Laura', 'Seoane', 'Dra. Laura Seoane', NULL, 'Psiquiatría',
   'Especialista en psiquiatría de adultos y seguimiento clínico.', 'active', true, NULL,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('e6000000-0000-4000-8000-000000000002', 'e3000000-0000-4000-8000-000000000001',
   'professional', 'Mateo', 'Rivas', 'Dr. Mateo Rivas', NULL, 'Psiquiatría',
   'Especialista en psiquiatría general y trastornos del sueño.', 'active', true, NULL,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('e6000000-0000-4000-8000-000000000003', 'e3000000-0000-4000-8000-000000000001',
   'professional', 'Inés', 'Varela', 'Dra. Inés Varela', NULL, 'Ginecología',
   'Especialista en ginecología preventiva y salud integral de la mujer.', 'active', true, NULL,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('e6000000-0000-4000-8000-000000000004', 'e3000000-0000-4000-8000-000000000001',
   'professional', 'Paula', 'Souto', 'Paula Souto', NULL, 'Psicología clínica',
   'Psicóloga sanitaria especializada en evaluación e intervención individual.', 'active', true, NULL,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT ("id") DO UPDATE SET
  "type" = 'professional', "firstName" = EXCLUDED."firstName", "lastName" = EXCLUDED."lastName",
  "publicAlias" = EXCLUDED."publicAlias", "specialty" = EXCLUDED."specialty",
  "description" = EXCLUDED."description", "status" = 'active', "publicVisibility" = true,
  "updatedAt" = CURRENT_TIMESTAMP;

INSERT INTO "EmployeeResourceHours" (
  "id", "employeeResourceId", "weekday", "isAvailable", "startsAt", "endsAt",
  "createdAt", "updatedAt"
)
SELECT gen_random_uuid(), resource_id, weekday, weekday <= 5,
  CASE WHEN weekday <= 5 THEN starts_at ELSE NULL END,
  CASE WHEN weekday <= 5 THEN ends_at ELSE NULL END,
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
  ('e6000000-0000-4000-8000-000000000001'::uuid, TIME '09:00', TIME '14:00'),
  ('e6000000-0000-4000-8000-000000000002'::uuid, TIME '09:00', TIME '14:00'),
  ('e6000000-0000-4000-8000-000000000003'::uuid, TIME '09:00', TIME '14:00'),
  ('e6000000-0000-4000-8000-000000000004'::uuid, TIME '15:00', TIME '20:00')
) AS resources(resource_id, starts_at, ends_at)
CROSS JOIN generate_series(1, 7) AS weekdays(weekday)
ON CONFLICT ("employeeResourceId", "weekday") DO UPDATE SET
  "isAvailable" = EXCLUDED."isAvailable", "startsAt" = EXCLUDED."startsAt",
  "endsAt" = EXCLUDED."endsAt", "updatedAt" = CURRENT_TIMESTAMP;

INSERT INTO "ServiceEmployeeResources" ("serviceId", "employeeResourceId", "createdAt") VALUES
  ('e5000000-0000-4000-8000-000000000001', 'e6000000-0000-4000-8000-000000000001', CURRENT_TIMESTAMP),
  ('e5000000-0000-4000-8000-000000000001', 'e6000000-0000-4000-8000-000000000002', CURRENT_TIMESTAMP),
  ('e5000000-0000-4000-8000-000000000002', 'e6000000-0000-4000-8000-000000000003', CURRENT_TIMESTAMP),
  ('e5000000-0000-4000-8000-000000000003', 'e6000000-0000-4000-8000-000000000004', CURRENT_TIMESTAMP)
ON CONFLICT ("serviceId", "employeeResourceId") DO NOTHING;

-- Las citas se regeneran sobre un horizonte móvil de 45 días laborables. La
-- duración interna evita solapes, aunque la interfaz pública enseña únicamente
-- 09:00, 10:00, etc. por tratarse de servicios exact_time.
INSERT INTO "TimeSlots" (
  "id", "venueId", "serviceId", "date", "weekday", "startsAt", "endsAt",
  "capacity", "status", "createdByRule", "version", "createdAt", "updatedAt"
)
SELECT gen_random_uuid(), 'e3000000-0000-4000-8000-000000000001'::uuid,
  appointment.service_id, day_value::date, extract(isodow from day_value)::integer,
  appointment.starts_at, appointment.ends_at, appointment.capacity,
  'available', true, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM generate_series(CURRENT_DATE + 1, CURRENT_DATE + 45, INTERVAL '1 day') AS days(day_value)
CROSS JOIN (VALUES
  ('e5000000-0000-4000-8000-000000000001'::uuid, TIME '09:00', TIME '09:45', 2),
  ('e5000000-0000-4000-8000-000000000001'::uuid, TIME '10:00', TIME '10:45', 2),
  ('e5000000-0000-4000-8000-000000000001'::uuid, TIME '11:00', TIME '11:45', 2),
  ('e5000000-0000-4000-8000-000000000002'::uuid, TIME '09:30', TIME '10:00', 1),
  ('e5000000-0000-4000-8000-000000000002'::uuid, TIME '10:30', TIME '11:00', 1),
  ('e5000000-0000-4000-8000-000000000002'::uuid, TIME '12:00', TIME '12:30', 1),
  ('e5000000-0000-4000-8000-000000000003'::uuid, TIME '16:00', TIME '16:50', 1),
  ('e5000000-0000-4000-8000-000000000003'::uuid, TIME '17:00', TIME '17:50', 1),
  ('e5000000-0000-4000-8000-000000000003'::uuid, TIME '18:00', TIME '18:50', 1)
) AS appointment(service_id, starts_at, ends_at, capacity)
WHERE extract(isodow from day_value) BETWEEN 1 AND 5
ON CONFLICT DO NOTHING;

INSERT INTO "VenueCustomTabs" (
  "id", "venueId", "position", "isActive", "titleI18n", "contentI18n",
  "contentFormat", "createdAt", "updatedAt"
) VALUES (
  'e8000000-0000-4000-8000-000000000001',
  'e3000000-0000-4000-8000-000000000001', 0, true,
  '{"sourceLocale":"es","values":{"es":"Especialidades y profesionales","en":"Specialties and professionals"}}'::jsonb,
  '{"sourceLocale":"es","values":{"es":"<h3>Psiquiatría</h3><p>Dra. Laura Seoane y Dr. Mateo Rivas · consulta de 45 minutos.</p><h3>Ginecología</h3><p>Dra. Inés Varela · consulta de 30 minutos.</p><h3>Psicología clínica</h3><p>Paula Souto · sesión de 50 minutos.</p><p><strong>Aviso:</strong> todos los nombres y datos de esta ficha son ficticios.</p>","en":"<h3>Psychiatry</h3><p>Dr Laura Seoane and Dr Mateo Rivas · 45-minute consultation.</p><h3>Gynaecology</h3><p>Dr Inés Varela · 30-minute consultation.</p><h3>Clinical psychology</h3><p>Paula Souto · 50-minute session.</p><p><strong>Notice:</strong> every name and detail in this listing is fictional.</p>"}}'::jsonb,
  'safe_html', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT ("id") DO UPDATE SET
  "position" = 0, "isActive" = true, "titleI18n" = EXCLUDED."titleI18n",
  "contentI18n" = EXCLUDED."contentI18n", "contentFormat" = 'safe_html',
  "updatedAt" = CURRENT_TIMESTAMP;
