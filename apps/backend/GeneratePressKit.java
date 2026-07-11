import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public class GeneratePressKit {
    public static void main(String[] args) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("HIEUTHUHAI - PRESS KIT");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 650);
                contentStream.showText("Biography:");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("HIEUTHUHAI (born 1999) is a prominent Vietnamese rapper.");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("He is known for his unique melodic rap style and handsome appearance.");
                
                contentStream.newLineAtOffset(0, -40);
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                contentStream.showText("Major Hits:");
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("- Cua (2020)");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("- Khong The Say (2022)");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("- Ngu Mot Minh (2023)");
                
                contentStream.endText();
            }

            document.save("press_kit_hieuthuhai.pdf");
            System.out.println("Created press_kit_hieuthuhai.pdf successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
