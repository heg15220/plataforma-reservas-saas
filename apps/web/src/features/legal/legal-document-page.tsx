import Box from "@mui/material/Box";
import Link from "@mui/material/Link";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";

import { PageContainer, PublicShell, Surface } from "@/components/layout";
import { NavigationLink } from "@/components/navigation-link";

export interface LegalSection {
  title: string;
  paragraphs: string[];
  bullets?: string[];
}

interface LegalDocumentPageProps {
  currentPath: string;
  title: string;
  description: string;
  updatedLabel: string;
  updatedValue: string;
  reviewNotice: string;
  relatedLabel: string;
  relatedHref: string;
  relatedText: string;
  sections: LegalSection[];
}

/**
 * Presenta documentos legales largos con jerarquía semántica, ancho legible y
 * navegación cruzada. El contenido se recibe localizado desde el catálogo del servidor.
 */
export function LegalDocumentPage({
  currentPath,
  title,
  description,
  updatedLabel,
  updatedValue,
  reviewNotice,
  relatedLabel,
  relatedHref,
  relatedText,
  sections,
}: LegalDocumentPageProps) {
  return (
    <PublicShell currentPath={currentPath}>
      <PageContainer compact sx={{ py: { xs: 4, md: 7 } }}>
        <Stack spacing={3}>
          <Box>
            <Typography component="h1" variant="h1">
              {title}
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 2, maxWidth: 760 }}>
              {description}
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }} variant="body2">
              {`${updatedLabel}:`} <time dateTime="2026-08-11">{updatedValue}</time>
            </Typography>
          </Box>

          <Surface component="article" padding="lg">
            <Stack spacing={4}>
              <Typography color="text.secondary" variant="body2">
                {reviewNotice}
              </Typography>
              {sections.map((section) => (
                <Box component="section" key={section.title}>
                  <Typography component="h2" variant="h3">
                    {section.title}
                  </Typography>
                  <Stack spacing={1.5} sx={{ mt: 1.5 }}>
                    {section.paragraphs.map((paragraph) => (
                      <Typography key={paragraph}>{paragraph}</Typography>
                    ))}
                    {section.bullets?.length ? (
                      <Box component="ul" sx={{ m: 0, pl: 3 }}>
                        {section.bullets.map((bullet) => (
                          <Typography component="li" key={bullet} sx={{ mb: 1 }}>
                            {bullet}
                          </Typography>
                        ))}
                      </Box>
                    ) : null}
                  </Stack>
                </Box>
              ))}
            </Stack>
          </Surface>

          <Typography>
            {relatedLabel}{" "}
            <Link component={NavigationLink} href={relatedHref}>
              {relatedText}
            </Link>
          </Typography>
        </Stack>
      </PageContainer>
    </PublicShell>
  );
}
