import { z } from "zod";

const assignmentSchema = z.object({
  venueId: z.uuid(),
  venueName: z.string().min(1),
  venueSlug: z.string().min(1),
  email: z.email().nullable(),
  updatedAt: z.iso.datetime(),
});

const assignmentsSchema = z.object({ assignments: z.array(assignmentSchema) });

export type VenueEmailAssignment = z.infer<typeof assignmentSchema>;
export type VenueEmailApiErrorKind =
  | "invalid"
  | "unauthenticated"
  | "forbidden"
  | "notFound"
  | "unavailable";

export class VenueEmailApiError extends Error {
  constructor(
    public readonly kind: VenueEmailApiErrorKind,
    options?: ErrorOptions,
  ) {
    super(kind, options);
    this.name = "VenueEmailApiError";
  }
}

/** Lista asociaciones operativas sin exponer propietarios ni datos empresariales. */
export async function fetchVenueEmailAssignments(
  signal?: AbortSignal,
): Promise<VenueEmailAssignment[]> {
  const response = await request("/api/venue/me/email-assignments", { method: "GET", signal });
  await throwForStatus(response);
  return parseJson(response, assignmentsSchema).then((result) => result.assignments);
}

/** Sustituye el destinatario de un local concreto bajo comprobación de ownership backend. */
export async function updateVenueEmailAssignment(
  venueId: string,
  email: string,
  signal?: AbortSignal,
): Promise<VenueEmailAssignment> {
  const response = await request(`/api/venue/me/email-assignments/${venueId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email }),
    signal,
  });
  await throwForStatus(response);
  return parseJson(response, assignmentSchema);
}

async function request(path: string, init: RequestInit): Promise<Response> {
  const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;
  if (!baseUrl) throw new VenueEmailApiError("unavailable");
  try {
    return await fetch(`${baseUrl.replace(/\/$/, "")}${path}`, {
      credentials: "include",
      headers: { Accept: "application/json", ...init.headers },
      ...init,
    });
  } catch (error) {
    throw new VenueEmailApiError("unavailable", { cause: error });
  }
}

async function throwForStatus(response: Response): Promise<void> {
  if (response.ok) return;
  const kinds: Partial<Record<number, VenueEmailApiErrorKind>> = {
    400: "invalid",
    401: "unauthenticated",
    403: "forbidden",
    404: "notFound",
  };
  throw new VenueEmailApiError(kinds[response.status] ?? "unavailable");
}

async function parseJson<T>(response: Response, schema: z.ZodType<T>): Promise<T> {
  try {
    return schema.parse(await response.json());
  } catch (error) {
    throw new VenueEmailApiError("unavailable", { cause: error });
  }
}
