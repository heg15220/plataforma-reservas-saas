-- Publica el catálogo inicial. Los importes son configuración comercial revisable y no habilitan
-- por sí mismos ningún proveedor de cobro ni aplican todavía límites funcionales.

INSERT INTO "Plans" (
  "id",
  "name",
  "nameI18n",
  "slug",
  "priceMonthly",
  "priceYearly",
  "limitsJson",
  "featuresJson",
  "featuresI18nJson"
)
VALUES
  (
    '10000000-0000-4000-8000-000000000001',
    'Gratuito',
    '{
      "sourceLocale": "es",
      "values": {"es": "Gratuito", "en": "Free"}
    }'::jsonb,
    'free',
    0.00,
    0.00,
    '{
      "monthlyReservations": 100,
      "teamResources": 1,
      "customFormFields": 3,
      "galleryImages": 3
    }'::jsonb,
    '["public_profile", "online_booking", "basic_statistics"]'::jsonb,
    '{
      "public_profile": {
        "sourceLocale": "es",
        "values": {"es": "Perfil público", "en": "Public profile"}
      },
      "online_booking": {
        "sourceLocale": "es",
        "values": {"es": "Reservas online", "en": "Online booking"}
      },
      "basic_statistics": {
        "sourceLocale": "es",
        "values": {"es": "Estadísticas básicas", "en": "Basic statistics"}
      }
    }'::jsonb
  ),
  (
    '10000000-0000-4000-8000-000000000002',
    'Profesional',
    '{
      "sourceLocale": "es",
      "values": {"es": "Profesional", "en": "Professional"}
    }'::jsonb,
    'professional',
    29.00,
    290.00,
    '{
      "monthlyReservations": 1000,
      "teamResources": 10,
      "customFormFields": 20,
      "galleryImages": 20
    }'::jsonb,
    '[
      "public_profile",
      "online_booking",
      "basic_statistics",
      "team_management",
      "custom_forms",
      "priority_support"
    ]'::jsonb,
    '{
      "public_profile": {
        "sourceLocale": "es",
        "values": {"es": "Perfil público", "en": "Public profile"}
      },
      "online_booking": {
        "sourceLocale": "es",
        "values": {"es": "Reservas online", "en": "Online booking"}
      },
      "basic_statistics": {
        "sourceLocale": "es",
        "values": {"es": "Estadísticas básicas", "en": "Basic statistics"}
      },
      "team_management": {
        "sourceLocale": "es",
        "values": {"es": "Gestión de equipo", "en": "Team management"}
      },
      "custom_forms": {
        "sourceLocale": "es",
        "values": {"es": "Formularios personalizados", "en": "Custom forms"}
      },
      "priority_support": {
        "sourceLocale": "es",
        "values": {"es": "Soporte prioritario", "en": "Priority support"}
      }
    }'::jsonb
  ),
  (
    '10000000-0000-4000-8000-000000000003',
    'Premium',
    '{
      "sourceLocale": "es",
      "values": {"es": "Premium", "en": "Premium"}
    }'::jsonb,
    'premium',
    59.00,
    590.00,
    '{
      "monthlyReservations": null,
      "teamResources": null,
      "customFormFields": null,
      "galleryImages": null
    }'::jsonb,
    '[
      "public_profile",
      "online_booking",
      "basic_statistics",
      "team_management",
      "custom_forms",
      "priority_support",
      "unlimited_usage",
      "advanced_statistics"
    ]'::jsonb,
    '{
      "public_profile": {
        "sourceLocale": "es",
        "values": {"es": "Perfil público", "en": "Public profile"}
      },
      "online_booking": {
        "sourceLocale": "es",
        "values": {"es": "Reservas online", "en": "Online booking"}
      },
      "basic_statistics": {
        "sourceLocale": "es",
        "values": {"es": "Estadísticas básicas", "en": "Basic statistics"}
      },
      "team_management": {
        "sourceLocale": "es",
        "values": {"es": "Gestión de equipo", "en": "Team management"}
      },
      "custom_forms": {
        "sourceLocale": "es",
        "values": {"es": "Formularios personalizados", "en": "Custom forms"}
      },
      "priority_support": {
        "sourceLocale": "es",
        "values": {"es": "Soporte prioritario", "en": "Priority support"}
      },
      "unlimited_usage": {
        "sourceLocale": "es",
        "values": {"es": "Uso sin límites configurados", "en": "No configured usage limits"}
      },
      "advanced_statistics": {
        "sourceLocale": "es",
        "values": {"es": "Estadísticas avanzadas", "en": "Advanced statistics"}
      }
    }'::jsonb
  );
