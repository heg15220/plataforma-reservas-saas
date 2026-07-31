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
    'ames-padel-center@reserly.local',
    'ames-padel-center@reserly.local',
    '$2a$10$localDemoAccountCannotAuthenticateWithoutKnownPassword000000000',
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
  "businessVerificationReference", "createdAt", "updatedAt"
)
VALUES
  (
    'd2000000-0000-4000-8000-000000000001',
    'd0000000-0000-4000-8000-000000000001',
    'ES', 'Ames Padel Center · Demostración local', 'LOCAL-APC-001', 'LOCALAPC001',
    'Firmistáns 10A, 15895 Ames, A Coruña',
    'verified', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '365 days',
    'local_fixture', 'local-apc-001',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    'd2000000-0000-4000-8000-000000000002',
    'd0000000-0000-4000-8000-000000000002',
    'ES', 'LET Padel Ames · Demostración local', 'LOCAL-LET-002', 'LOCALLET002',
    'Firmistáns 10A, 15895 Ames, A Coruña',
    'verified', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '365 days',
    'local_fixture', 'local-let-002',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    'd2000000-0000-4000-8000-000000000003',
    'd0000000-0000-4000-8000-000000000003',
    'ES', 'Lume de Brétema · Demostración local', 'LOCAL-LUME-003', 'LOCALLUME003',
    'Rúa da Lúa Nova 18, 15705 Santiago de Compostela, A Coruña',
    'verified', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '365 days',
    'local_fixture', 'local-lume-003',
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
    'published', 'automatic', true, true, CURRENT_TIMESTAMP,
    true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    'd3000000-0000-4000-8000-000000000002',
    'd0000000-0000-4000-8000-000000000002',
    'd2000000-0000-4000-8000-000000000002',
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
  "manualAvailabilityStatus" = 'automatic',
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
