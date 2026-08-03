import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { fetchEmployeeResources, saveServiceResources } from "./team-api";

const resource = {
  id: "10000000-0000-4000-8000-000000000001",
  type: "professional",
  firstName: "Ana",
  lastName: "Ruiz",
  publicAlias: "Ana",
  photoUrl: null,
  specialty: "Masaje",
  description: null,
  status: "active",
  publicVisibility: true,
  internalNotes: null,
  createdAt: "2026-07-13T10:00:00Z",
  updatedAt: "2026-07-13T10:00:00Z",
};

const service = {
  id: "20000000-0000-4000-8000-000000000001",
  name: "Masaje",
  nameI18n: null,
  description: null,
  descriptionI18n: null,
  durationMinutes: 60,
  capacityRequired: 1,
  active: true,
  allowsAnyAvailableResource: true,
  bookingMode: "range",
  employeeResourceIds: [resource.id],
  createdAt: "2026-07-13T10:00:00Z",
  updatedAt: "2026-07-13T10:00:00Z",
};

beforeEach(() => {
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080/");
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

describe("team-api", () => {
  it("consulta el equipo con credenciales privadas y valida la respuesta", async () => {
    const fetchMock = vi.fn().mockResolvedValue(Response.json([resource]));
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchEmployeeResources()).resolves.toEqual([resource]);
    expect(String(fetchMock.mock.calls[0][0])).toBe("http://localhost:8080/api/venue/me/team");
    expect(fetchMock.mock.calls[0][1]).toMatchObject({
      credentials: "include",
      method: "GET",
    });
  });

  it("reemplaza las compatibilidades de un servicio de forma idempotente", async () => {
    const fetchMock = vi.fn().mockResolvedValue(Response.json(service));
    vi.stubGlobal("fetch", fetchMock);

    await expect(saveServiceResources(service.id, [resource.id])).resolves.toEqual(service);
    expect(String(fetchMock.mock.calls[0][0])).toBe(
      `http://localhost:8080/api/venue/me/services/${service.id}/resources`,
    );
    expect(fetchMock.mock.calls[0][1]).toMatchObject({
      body: JSON.stringify({ resourceIds: [resource.id] }),
      credentials: "include",
      method: "PUT",
    });
  });
});
