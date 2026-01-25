package io.deskpilot.engine;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class OcrEngine {

    private final Tesseract tesseract;

    public OcrEngine() {
        try {
            Path tessdataDir = extractTessdataDir(); // .../<temp>/tessdata
            Path eng = tessdataDir.resolve("eng.traineddata");

            if (!Files.exists(eng) || Files.size(eng) < 1024 * 1024) {
                throw new RuntimeException("eng.traineddata missing or too small at: " + eng);
            }

            this.tesseract = new Tesseract();

            // IMPORTANT: datapath must be the folder that directly contains eng.traineddata
            tesseract.setDatapath(tessdataDir.toAbsolutePath().toString());
            tesseract.setLanguage("eng");

            // ✅ Key fix for UI buttons/labels
            tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK);

            System.out.println("OCR tessdataDir=" + tessdataDir.toAbsolutePath());
            System.out.println("OCR eng.traineddata=" + eng.toAbsolutePath());

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize OCR. Ensure resources/tessdata/eng.traineddata exists.", e);
        }
    }

    /** Simple OCR: read all text from an image. */
    public String readText(BufferedImage img) {
        try {
            String s = tesseract.doOCR(img);
            return (s == null) ? "" : s.trim().toLowerCase();
        } catch (TesseractException e) {
            throw new RuntimeException("OCR failed: " + e.getMessage(), e);
        }
    }

    /**
     * Word-level OCR with bounding boxes.
     * Used by OCR-click targets.
     */
    public List<OcrWord> readWords(BufferedImage img) {
        try {
            List<Word> words = tesseract.getWords(img, ITessAPI.TessPageIteratorLevel.RIL_WORD);
            List<OcrWord> out = new ArrayList<>();
            if (words == null) return out;

            for (Word w : words) {
                if (w == null) continue;

                String text = (w.getText() == null) ? "" : w.getText().trim().toLowerCase();
                Rectangle box = w.getBoundingBox();

                if (text.isEmpty() || box == null) continue;
                out.add(new OcrWord(text, box));
            }

            return out;
        } catch (Exception e) {
            throw new RuntimeException("OCR readWords failed: " + e.getMessage(), e);
        }
    }

    /** Small DTO so we don’t leak tess4j Word outside OCR engine. */
    public static final class OcrWord {
        public final String text;
        public final Rectangle box;

        public OcrWord(String text, Rectangle box) {
            this.text = text;
            this.box = box;
        }
    }

    private static Path extractTessdataDir() throws IOException {
        Path tessdataDir = Files.createTempDirectory("deskpilot-tessdata-").resolve("tessdata");
        Files.createDirectories(tessdataDir);

        copyResource("/tessdata/eng.traineddata", tessdataDir.resolve("eng.traineddata"));

        return tessdataDir;
    }

    private static void copyResource(String resourcePath, Path dest) throws IOException {
        try (InputStream in = OcrEngine.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new FileNotFoundException("Classpath resource not found: " + resourcePath);
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
