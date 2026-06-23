"use client";

import NextLink, { type LinkProps } from "next/link";
import type { AnchorHTMLAttributes } from "react";

export type NavigationLinkProps = LinkProps &
  Omit<AnchorHTMLAttributes<HTMLAnchorElement>, keyof LinkProps>;

/**
 * Adaptador cliente de Next Link para props `component` de MUI en Next.js 16.
 */
export function NavigationLink(props: NavigationLinkProps) {
  return <NextLink {...props} />;
}
