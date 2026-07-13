import { z } from "zod";

function apiBaseUrl() {
  return (process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080").replace(/\/+$/, "");
}

export const reservationFormFieldTypeSchema = z.enum([
  "short_text", "long_text", "select", "checkbox", "date", "number", "email", "phone",
]);

export type ReservationFormFieldType = z.infer<typeof reservationFormFieldTypeSchema>;

export const reservationFormFieldSchema = z.object({
  id: z.string().uuid(),
  label: z.string(),
  key: z.string(),
  type: reservationFormFieldTypeSchema,
  required: z.boolean(),
  options: z.array(z.string()).nullable(),
  position: z.number().int(),
  active: z.boolean(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
});

export const reservationFormPreviewFieldSchema = z.object({
  id: z.string().uuid().nullable(),
  source: z.enum(["base", "custom"]),
  label: z.string().nullable(),
  labelKey: z.string().nullable(),
  key: z.string(),
  type: z.enum([
    "short_text", "long_text", "select", "checkbox", "date", "number", "email", "phone", "time_slot",
  ]),
  required: z.boolean(),
  editable: z.boolean(),
  options: z.array(z.string()).nullable(),
  position: z.number().int(),
});

const fieldsSchema = z.array(reservationFormFieldSchema);
const previewSchema = z.object({ fields: z.array(reservationFormPreviewFieldSchema) }).transform((value) => value.fields);

export type ReservationFormField = z.infer<typeof reservationFormFieldSchema>;
export type ReservationFormPreviewField = z.infer<typeof reservationFormPreviewFieldSchema>;

export interface ReservationFormFieldInput {
  label: string;
  key: string;
  type: ReservationFormFieldType;
  required: boolean;
  options: string[] | null;
}

export class ReservationFormApiError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
    this.name = "ReservationFormApiError";
  }
}

async function request<T>(path: string, init: RequestInit, schema: z.ZodType<T>): Promise<T> {
  const response = await fetch(`${apiBaseUrl()}${path}`, {
    ...init,
    credentials: "include",
    headers: init.body ? { "Content-Type": "application/json", ...init.headers } : init.headers,
  });
  if (!response.ok) {
    throw new ReservationFormApiError(response.status, `reservation-form:${response.status}`);
  }
  try {
    return schema.parse(await response.json());
  } catch {
    throw new ReservationFormApiError(502, "reservation-form:invalid-response");
  }
}

export function fetchReservationFormFields(signal?: AbortSignal) {
  return request("/api/venue/me/reservation-form/fields", { method: "GET", signal }, fieldsSchema);
}

export function fetchReservationFormPreview(signal?: AbortSignal) {
  return request("/api/venue/me/reservation-form/preview", { method: "GET", signal }, previewSchema);
}

export function createReservationFormField(input: ReservationFormFieldInput) {
  return request(
    "/api/venue/me/reservation-form/fields",
    { method: "POST", body: JSON.stringify(input) },
    reservationFormFieldSchema,
  );
}

export function updateReservationFormField(id: string, input: ReservationFormFieldInput) {
  return request(
    `/api/venue/me/reservation-form/fields/${id}`,
    { method: "PATCH", body: JSON.stringify(input) },
    reservationFormFieldSchema,
  );
}

export async function deleteReservationFormField(id: string): Promise<void> {
  const response = await fetch(`${apiBaseUrl()}/api/venue/me/reservation-form/fields/${id}`, {
    method: "DELETE",
    credentials: "include",
  });
  if (!response.ok) {
    throw new ReservationFormApiError(response.status, `reservation-form:${response.status}`);
  }
}

export function reorderReservationFormFields(fieldIds: string[]) {
  return request(
    "/api/venue/me/reservation-form/fields/order",
    { method: "PUT", body: JSON.stringify({ fieldIds }) },
    fieldsSchema,
  );
}
