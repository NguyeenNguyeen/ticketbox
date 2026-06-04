import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { HeroSection } from "./HeroSection";
import { ConcertSection } from "./ConcertSection";

export default function HomePage() {
  return (
    <>
      <Header />
      <main className="flex-1">
        <HeroSection />
        <ConcertSection />
      </main>
      <Footer />
    </>
  );
}
