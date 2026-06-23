import Chip from "@mui/material/Chip";
import {
  CheckCircle2,
  CircleAlert,
  CircleMinus,
  Clock3,
  Info,
  type LucideIcon,
} from "lucide-react";

import { visualTokens } from "@/theme/visual-tokens";

export type StatusTone = "success" | "warning" | "danger" | "neutral" | "info";

export interface StatusChipProps {
  label: string;
  tone: StatusTone;
}

const statusPresentation: Record<
  StatusTone,
  { background: string; foreground: string; icon: LucideIcon }
> = {
  success: {
    background: visualTokens.color.status.successSoft,
    foreground: visualTokens.color.status.successText,
    icon: CheckCircle2,
  },
  warning: {
    background: visualTokens.color.status.warningSoft,
    foreground: visualTokens.color.status.warningText,
    icon: Clock3,
  },
  danger: {
    background: visualTokens.color.status.dangerSoft,
    foreground: visualTokens.color.status.dangerText,
    icon: CircleAlert,
  },
  neutral: {
    background: visualTokens.color.status.neutralSoft,
    foreground: visualTokens.color.status.neutralText,
    icon: CircleMinus,
  },
  info: {
    background: visualTokens.color.status.infoSoft,
    foreground: visualTokens.color.status.infoText,
    icon: Info,
  },
};

/**
 * Estado compacto con semántica redundante: texto, color e icono.
 *
 * El consumidor aporta el texto ya localizado. El icono es decorativo porque el
 * significado completo permanece disponible en la etiqueta.
 */
export function StatusChip({ label, tone }: StatusChipProps) {
  const presentation = statusPresentation[tone];
  const Icon = presentation.icon;

  return (
    <Chip
      icon={<Icon aria-hidden="true" size={16} strokeWidth={2} />}
      label={label}
      size="small"
      sx={{
        bgcolor: presentation.background,
        color: presentation.foreground,
        "& .MuiChip-icon": {
          color: "inherit",
        },
      }}
    />
  );
}
