import { NextRequest, NextResponse } from "next/server";

import { loadWebEnvironment } from "../../../../../environment";

const MAX_BODY_BYTES = 128 * 1024;

/**
 * Proxy same-origin que mantiene el token de ingesta fuera del bundle del navegador.
 *
 * No registra ni transforma el body. Backend aplica autenticación, cuota, catálogo y minimización.
 */
export async function POST(request: NextRequest) {
  const length = Number(request.headers.get("content-length") ?? "0");
  if (!Number.isFinite(length) || length > MAX_BODY_BYTES) {
    return NextResponse.json({ error: "EVENT_INVALID" }, { status: 400 });
  }
  const token = process.env.RESERLY_DEMAND_INGESTION_SERVICE_TOKEN;
  if (!token || token.length < 32) {
    return NextResponse.json({ error: "EVENT_INGESTION_UNAVAILABLE" }, { status: 503 });
  }
  const body = await request.text();
  if (new TextEncoder().encode(body).length > MAX_BODY_BYTES) {
    return NextResponse.json({ error: "EVENT_INVALID" }, { status: 400 });
  }
  const target = new URL("/api/internal/demand/v1/events", loadWebEnvironment().internalApiBaseUrl);
  try {
    const response = await fetch(target, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Reserly-Service-Token": token,
      },
      body,
      cache: "no-store",
      signal: AbortSignal.timeout(2_000),
    });
    return new NextResponse(await response.text(), {
      status: response.status,
      headers: { "Content-Type": "application/json" },
    });
  } catch {
    return NextResponse.json({ error: "EVENT_INGESTION_UNAVAILABLE" }, { status: 503 });
  }
}
