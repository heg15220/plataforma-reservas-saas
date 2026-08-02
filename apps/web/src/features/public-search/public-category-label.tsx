import Chip from "@mui/material/Chip";
import {
  Building2,
  Dumbbell,
  Goal,
  Landmark,
  Scissors,
  Shapes,
  Sparkles,
  Utensils,
  type LucideIcon,
} from "lucide-react";

const CATEGORY_ICONS: Readonly<Record<string, LucideIcon>> = {
  restaurante: Utensils,
  peluqueria: Scissors,
  "campo-de-futbol": Goal,
  "pista-de-padel": Dumbbell,
  "instalacion-municipal": Landmark,
  "centro-deportivo": Building2,
  "centro-de-estetica": Sparkles,
  otros: Shapes,
};

export interface PublicCategoryLabelProps {
  label: string;
  slug: string;
}

/**
 * Etiqueta no interactiva que comparte iconografía y tratamiento outlined con los filtros rápidos.
 *
 * Se renderiza como chip sin eventos para no introducir un botón dentro del enlace de la tarjeta.
 */
export function PublicCategoryLabel({ label, slug }: PublicCategoryLabelProps) {
  const Icon = CATEGORY_ICONS[slug] ?? Shapes;

  return (
    <Chip
      icon={<Icon aria-hidden="true" size={15} strokeWidth={1.9} />}
      label={label}
      size="small"
      sx={{
        alignSelf: "flex-start",
        bgcolor: "rgba(255,255,255,0.88)",
        cursor: "default",
        pointerEvents: "none",
        "& .MuiChip-icon": { color: "primary.main", ml: 1 },
      }}
      variant="outlined"
    />
  );
}
