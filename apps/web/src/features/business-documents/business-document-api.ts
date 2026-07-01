import { z } from "zod";

export const businessDocumentTypes = [
  "census_registration_036_037",
  "census_certificate",
  "activity_or_opening_license",
  "equivalent_administrative_document",
  "other",
] as const;

export type BusinessDocumentType = (typeof businessDocumentTypes)[number];

const documentRequestSchema = z.object({
  requestId: z.uuid(),
  reasonCode: z.enum([
    "no_automated_channel",
    "provider_unavailable",
    "insufficient_provider_data",
    "legal_name_unconfirmed",
    "address_unconfirmed",
  ]),
  requestedDocumentTypes: z.array(z.enum(businessDocumentTypes)).min(1),
  status: z.literal("open"),
  requestedAt: z.iso.datetime(),
});

const uploadResponseSchema = z.object({
  documentId: z.uuid(),
  documentRequestId: z.uuid(),
  status: z.literal("pending_review"),
  uploadedAt: z.iso.datetime(),
});

export type BusinessDocumentRequest = z.infer<typeof documentRequestSchema>;
export type BusinessDocumentUploadResult = z.infer<typeof uploadResponseSchema>;

export type BusinessDocumentApiErrorKind =
  | "unauthenticated"
  | "forbidden"
  | "invalid"
  | "conflict"
  | "malware"
  | "rateLimited"
  | "unavailable";

export class BusinessDocumentApiError extends Error {
  constructor(
    public readonly kind: BusinessDocumentApiErrorKind,
    options?: ErrorOptions,
  ) {
    super(kind, options);
    this.name = "BusinessDocumentApiError";
  }
}

/**
 * Consulta la solicitud documental del propietario actual.
 *
 * @returns solicitud abierta o `null` cuando el backend responde 204
 * @throws BusinessDocumentApiError con una categoría segura para presentación
 */
export async function fetchBusinessDocumentRequest(
  signal?: AbortSignal,
): Promise<BusinessDocumentRequest | null> {
  let response: Response;
  try {
    response = await fetch(`${documentApiRootUrl()}/document-request`, {
      method: "GET",
      credentials: "include",
      headers: { Accept: "application/json" },
      signal,
    });
  } catch (error) {
    throw new BusinessDocumentApiError("unavailable", { cause: error });
  }

  if (response.status === 204) {
    return null;
  }
  throwForStatus(response);

  try {
    return documentRequestSchema.parse(await response.json());
  } catch (error) {
    throw new BusinessDocumentApiError("unavailable", { cause: error });
  }
}

/**
 * Transmite un único documento mediante multipart.
 *
 * No fija `Content-Type`: el navegador debe generar el boundary. El archivo no
 * se serializa, cachea ni almacena en ninguna persistencia del cliente.
 */
export async function uploadBusinessDocument(
  requestId: string,
  documentType: BusinessDocumentType,
  file: File,
  signal?: AbortSignal,
): Promise<BusinessDocumentUploadResult> {
  const body = new FormData();
  body.set("documentRequestId", requestId);
  body.set("documentType", documentType);
  body.set("file", file);

  let response: Response;
  try {
    response = await fetch(`${documentApiRootUrl()}/documents`, {
      method: "POST",
      credentials: "include",
      headers: { Accept: "application/json" },
      body,
      signal,
    });
  } catch (error) {
    throw new BusinessDocumentApiError("unavailable", { cause: error });
  }

  throwForStatus(response);

  try {
    return uploadResponseSchema.parse(await response.json());
  } catch (error) {
    throw new BusinessDocumentApiError("unavailable", { cause: error });
  }
}

function throwForStatus(response: Response) {
  const kindByStatus: Partial<Record<number, BusinessDocumentApiErrorKind>> = {
    400: "invalid",
    401: "unauthenticated",
    403: "forbidden",
    409: "conflict",
    422: "malware",
    429: "rateLimited",
  };
  const kind = kindByStatus[response.status];
  if (kind) {
    throw new BusinessDocumentApiError(kind);
  }
  if (!response.ok) {
    throw new BusinessDocumentApiError("unavailable");
  }
}

function documentApiRootUrl() {
  const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;
  if (!apiBaseUrl) {
    throw new BusinessDocumentApiError("unavailable");
  }
  return `${apiBaseUrl.replace(/\/$/, "")}/api/venue/me/business-verification`;
}
