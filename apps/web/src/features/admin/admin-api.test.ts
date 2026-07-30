import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { fetchAdminCategories, fetchAdminVenues, loginAdmin, saveAdminCategory } from "./admin-api";

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
});
