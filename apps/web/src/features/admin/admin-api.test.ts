import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import {
  fetchAdminCategories,
  fetchAdminAuditLogs,
  fetchAdminIncidents,
  fetchAdminMetrics,
  fetchAdminPenalties,
  fetchAdminPlans,
  fetchAdminVenues,
  fetchPendingDocuments,
  fetchPendingBusinessAccounts,
  loginAdmin,
  saveAdminCategory,
  suspendAdminVenue,
} from "./admin-api";

beforeEach(() => vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://api.test/"));
afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

describe("admin-api", () => {
  it("usa el login segregado y exige metadatos de cuenta admin", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      Response.json({
        userId: "10000000-0000-4000-8000-000000000001",
        accountType: "admin",
        preferredLocale: "es",
        emailVerified: true,
        sessionExpiresAt: "2026-07-30T22:00:00Z",
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await loginAdmin("admin@example.com", "secret-password");

    expect(fetchMock).toHaveBeenCalledWith(
      new URL("http://api.test/api/auth/admin/login"),
      expect.objectContaining({ credentials: "include", method: "POST" }),
    );
  });

  it("valida listados y guarda categorías mediante contratos protegidos", async () => {
    const category = {
      id: "20000000-0000-4000-8000-000000000001",
      slug: "restaurants",
      nameEs: "Restaurantes",
      nameEn: "Restaurants",
      active: true,
      updatedAt: "2026-07-30T14:00:00Z",
    };
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(Response.json({ categories: [category] }))
      .mockResolvedValueOnce(Response.json({ venues: [] }))
      .mockResolvedValueOnce(Response.json(category));
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchAdminCategories()).resolves.toEqual({ categories: [category] });
    await expect(fetchAdminVenues()).resolves.toEqual({ venues: [] });
    await expect(
      saveAdminCategory({
        slug: category.slug,
        nameEs: category.nameEs,
        nameEn: category.nameEn,
        active: true,
      }),
    ).resolves.toEqual(category);
  });

  it("consume suspensión y colas administrativas con contratos acotados", async () => {
    const venue = {
      id: "30000000-0000-4000-8000-000000000001",
      name: "Local Centro",
      slug: "local-centro",
      categoryId: "20000000-0000-4000-8000-000000000001",
      categoryName: "Restaurantes",
      status: "suspended",
      contactEmail: null,
      phone: null,
      address: null,
      city: "Madrid",
      province: null,
      country: "ES",
      postalCode: null,
      updatedAt: "2026-07-30T14:00:00Z",
    };
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(Response.json(venue))
      .mockResolvedValueOnce(Response.json({ incidents: [] }))
      .mockResolvedValueOnce(Response.json({ accounts: [] }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(suspendAdminVenue(venue.id, "Incumplimiento")).resolves.toEqual(venue);
    await expect(fetchAdminIncidents()).resolves.toEqual({ incidents: [] });
    await expect(fetchPendingBusinessAccounts()).resolves.toEqual({ accounts: [] });
    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      new URL(`http://api.test/api/admin/venues/${venue.id}/suspension`),
      expect.objectContaining({ method: "PATCH" }),
    );
  });

  it("valida las colas nuevas de documentos y penalizaciones", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(Response.json({ documents: [] }))
      .mockResolvedValueOnce(Response.json({ penalties: [] }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchPendingDocuments()).resolves.toEqual({ documents: [] });
    await expect(fetchAdminPenalties()).resolves.toEqual({ penalties: [] });
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it("valida planes, métricas agregadas y auditoría minimizada", async () => {
    const metrics = {
      totalVenues: 3,
      publishedVenues: 2,
      suspendedVenues: 1,
      totalReservations: 8,
      confirmedReservations: 6,
      totalBusinessAccounts: 2,
      pendingBusinessReviews: 1,
      activeSubscriptions: 2,
      activePenalties: 1,
      generatedAt: "2026-07-30T16:00:00Z",
    };
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(Response.json({ plans: [] }))
      .mockResolvedValueOnce(Response.json(metrics))
      .mockResolvedValueOnce(Response.json({ logs: [] }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchAdminPlans()).resolves.toEqual({ plans: [] });
    await expect(fetchAdminMetrics()).resolves.toEqual(metrics);
    await expect(fetchAdminAuditLogs()).resolves.toEqual({ logs: [] });
    expect(fetchMock).toHaveBeenCalledTimes(3);
  });
});
