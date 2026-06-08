import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { HeroSection } from "./HeroSection";
import { ConcertSection } from "./ConcertSection";
import type { ConcertListItem } from "@/types/concert";

async function getConcerts(): Promise<ConcertListItem[]> {
  try {
    const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/concerts`, {
      next: { revalidate: 60 },
    });
    if (!res.ok) {
      console.error("Failed to fetch concerts:", res.status, res.statusText);
      return [];
    }
    const data = await res.json();
    return Array.isArray(data) ? data : [];
  } catch (error) {
    console.error("Error fetching concerts:", error);
    return [];
  }
}

export default async function HomePage() {
  const concerts = await getConcerts();

  return (
    <>
      <Header />
      <main className="flex-1">
        <HeroSection />
        <ConcertSection concerts={concerts} />
      </main>
      <Footer />
    </>
  );
}
