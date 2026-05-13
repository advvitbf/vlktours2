import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

const inter = Inter({ subsets: ["latin"] });

/** Root layout wraps every page — sets fonts and global HTML shell. */
export const metadata: Metadata = {
  title: { default: "Tours & Travelers", template: "%s | Tours & Travelers" },
  description: "Company trips, itineraries, and traveler updates.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className={inter.className}>{children}</body>
    </html>
  );
}
