import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";

export default function AboutPage() {
  return (
    <>
      <Header />
      <main className="flex-1 flex flex-col items-center justify-center min-h-[60vh] px-4 text-center">
        <h1 className="text-4xl font-bold mb-4">Giới thiệu</h1>
        <p className="text-muted-foreground max-w-lg">
          Trang này đang trong quá trình xây dựng. Vui lòng quay lại sau!
        </p>
      </main>
      <Footer />
    </>
  );
}
