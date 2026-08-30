import { Fraunces, Inter, IBM_Plex_Mono } from "next/font/google";

// Display face — editorial, travel-poster character for headlines
export const fraunces = Fraunces({
  subsets: ["latin"],
  weight: ["400", "500", "600"],
  style: ["normal", "italic"],
  variable: "--font-fraunces",
  display: "swap",
});

// Body face — clean, quiet, gets out of the way
export const inter = Inter({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
  variable: "--font-inter",
  display: "swap",
});

// Utility face — for timestamps, day labels, ticket-stub codes
export const plexMono = IBM_Plex_Mono({
  subsets: ["latin"],
  weight: ["400", "500"],
  variable: "--font-plex-mono",
  display: "swap",
});

/*
  Note: these are named --font-fraunces / --font-inter / --font-plex-mono
  rather than --font-display / --font-body / --font-mono on purpose.
  Tailwind v4's @theme block in globals.css defines --font-display etc.
  as *its own* CSS variables at :root, and :root's specificity would
  otherwise beat the className-scoped variables next/font applies to
  <html> — giving you the fallback font forever. Keeping the names
  distinct avoids that collision; @theme references these by name.
*/
