import type { Metadata } from "next";
import { fraunces, inter, plexMono } from "./lib/Fonts";
import "./globals.css";

export const metadata: Metadata = {
  title: "Plan your next trip in minutes",
  description: "Your Travel Itinerary Planner",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html
      lang="en"
      className={`${fraunces.variable} ${inter.variable} ${plexMono.variable}`}
    >
      <body suppressHydrationWarning>{children}</body>
    </html>
  );
}
