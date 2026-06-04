import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
public class TestPdf {
    public static void main(String[] args) {
        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }
}
