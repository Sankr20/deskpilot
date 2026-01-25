package io.deskpilot.engine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EngineSmoke {

    private static final Logger log = LogManager.getLogger(EngineSmoke.class);

    public static void main(String[] args) throws Exception {

        // Best effort DPI awareness (helps Win32 coords be consistent)
        DpiAwareness.enable();

        log.info("DeskPilot Window Capture Smoke Test START");
        System.out.println("Click on the target window in 5 seconds...");
        Thread.sleep(5000);

        // 1) Pick target window
        var hwnd = WindowManager.pickWindowHandle();
        hwnd = WindowManager.toTopLevel(hwnd);

        String title = WindowManager.getWindowTitle(hwnd);

        // Win32 client rect in screen coords
        Rectangle clientRectWin32 = WindowManager.getClientRectOnScreenOrThrow(hwnd);

        log.info("Selected window title: {}", title);
        log.info("Client rect (win32): {}", clientRectWin32);

        System.out.println("Selected window: " + title);
        System.out.println("Client rect (win32): " + clientRectWin32);

        // Bring to front (best effort)
        WindowManager.bringToFront(hwnd);
        Thread.sleep(300);

        // Output folder
        Path outDir = Paths.get("runs", "smoke");
        Files.createDirectories(outDir);

        DesktopDriver driver = new DesktopDriver();

        // 2) Capture full client ONCE using Robot coords
        Rectangle clientRectRobot = RobotCoords.toRobotRect(clientRectWin32);
        System.out.println("Client rect (robot): " + clientRectRobot);

        BufferedImage clientImg = driver.screenshot(clientRectRobot);

        Path clientFile = outDir.resolve("client.png");
        javax.imageio.ImageIO.write(clientImg, "png", clientFile.toFile());
        log.info("Saved client screenshot to: {}", clientFile.toAbsolutePath());
        System.out.println("Saved client screenshot to: " + clientFile.toAbsolutePath());

        // 3) Define a normalized region (relative to client)
        // Tune these to point at the exact panel you want.
        NormalizedRegion nr = new NormalizedRegion(0.02, 0.08, 0.28, 0.35);
        System.out.println("Normalized region used: " + nr);

        // Region rect in WIN32 screen coords
        Rectangle regionRectWin32 = nr.toScreenRect(clientRectWin32);
        System.out.println("Region rect (win32): " + regionRectWin32);

        // Convert region rect to ROBOT screen coords (for positioning)
        Rectangle regionRectRobot = RobotCoords.toRobotRect(regionRectWin32);
        System.out.println("Region rect (robot): " + regionRectRobot);

        // 4) Crop REGION FROM THE CLIENT IMAGE (NO SECOND ROBOT SCREENSHOT)
        Rectangle regionInClient = new Rectangle(
                regionRectRobot.x - clientRectRobot.x,
                regionRectRobot.y - clientRectRobot.y,
                regionRectRobot.width,
                regionRectRobot.height
        );

        // Clamp to image bounds to avoid RasterFormatException
        regionInClient = clampToImage(regionInClient, clientImg);

        System.out.println("Region relative to client image: " + regionInClient);

        BufferedImage regionImg = clientImg.getSubimage(
                regionInClient.x,
                regionInClient.y,
                regionInClient.width,
                regionInClient.height
        );

        Path regionFile = outDir.resolve("region.png");
        javax.imageio.ImageIO.write(regionImg, "png", regionFile.toFile());
        log.info("Saved region screenshot to: {}", regionFile.toAbsolutePath());
        System.out.println("Saved region screenshot to: " + regionFile.toAbsolutePath());

        // 5) Optional: overlay debug (draw the region rectangle on client image)
        BufferedImage overlay = new BufferedImage(clientImg.getWidth(), clientImg.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = overlay.createGraphics();
        g.drawImage(clientImg, 0, 0, null);
        g.setStroke(new BasicStroke(3));
        g.drawRect(regionInClient.x, regionInClient.y, regionInClient.width, regionInClient.height);
        g.dispose();

        Path overlayFile = outDir.resolve("overlay.png");
        javax.imageio.ImageIO.write(overlay, "png", overlayFile.toFile());
        System.out.println("Saved overlay screenshot to: " + overlayFile.toAbsolutePath());

        log.info("DeskPilot Window Capture Smoke Test DONE");
    }

    private static Rectangle clampToImage(Rectangle r, BufferedImage img) {
        int x = Math.max(0, r.x);
        int y = Math.max(0, r.y);

        int maxW = img.getWidth() - x;
        int maxH = img.getHeight() - y;

        int w = Math.max(1, Math.min(r.width, maxW));
        int h = Math.max(1, Math.min(r.height, maxH));

        return new Rectangle(x, y, w, h);
    }
}
