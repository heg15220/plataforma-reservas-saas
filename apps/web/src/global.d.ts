import { supportedLocales } from "@/i18n/config";
import messages from "../locales/en.json";

declare module "next-intl" {
  interface AppConfig {
    Locale: (typeof supportedLocales)[number];
    Messages: typeof messages;
  }
}
