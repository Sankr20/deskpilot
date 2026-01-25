package io.deskpilot.engine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.CountDownLatch;

/**
 * Transparent full-screen overlay that lets you click-drag to select a
 * rectangle.
 * Returns the selected rect in *Win32 screen coords* (same as Java screen
 * coords when DPI aware).
 *
 * Usage:
 * Rectangle r = RegionPickerOverlay.pick("Drag to select region. ESC to
 * cancel.");
 */
public final class RegionPickerOverlay {

    private RegionPickerOverlay() {
    }

    public static Rectangle pick(String instruction) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final Rectangle[] result = new Rectangle[1];

        // Full virtual desktop bounds (supports multi-monitor)
        Rectangle screen = getVirtualBounds();

        JWindow w = new JWindow();
        w.setAlwaysOnTop(true);
        w.setBounds(screen);
        w.setBackground(new Color(0, 0, 0, 1)); // nearly transparent
        w.setFocusableWindowState(true);

        PickerPane pane = new PickerPane(instruction, screen, r -> {
            result[0] = r;
            latch.countDown();
            w.dispose();
        }, () -> {
            result[0] = null;
            latch.countDown();
            w.dispose();
        });

        w.setContentPane(pane);

        // Show
        SwingUtilities.invokeLater(() -> {
            w.setVisible(true);
            w.toFront();
            pane.requestFocusInWindow();
        });

        latch.await();
        return result[0];
    }

    private static Rectangle getVirtualBounds() {
        Rectangle all = new Rectangle();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (GraphicsDevice gd : ge.getScreenDevices()) {
            Rectangle b = gd.getDefaultConfiguration().getBounds();
            all = all.union(b);
        }
        return all;
    }

    // ----------------------------------------------------

    private static final class PickerPane extends JPanel {
        private final Rectangle screenBounds;
        private final String instruction;
        private final java.util.function.Consumer<Rectangle> onDone;
        private final Runnable onCancel;

        private Point start; // in screen coords
        private Point current; // in screen coords
        private boolean dragging;

        PickerPane(String instruction,
                Rectangle screenBounds,
                java.util.function.Consumer<Rectangle> onDone,
                Runnable onCancel) {
            this.instruction = instruction;
            this.screenBounds = screenBounds;
            this.onDone = onDone;
            this.onCancel = onCancel;

            setOpaque(false);
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        dragging = true;
                        start = toScreen(e.getPoint());
                        current = start;
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (!dragging)
                        return;
                    dragging = false;
                    current = toScreen(e.getPoint());
                    Rectangle r = rectFrom(start, current);
                    // Always return the rect; RecorderValidation will decide accept/reject.
                    onDone.accept(r);

                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (!dragging)
                        return;
                    current = toScreen(e.getPoint());
                    repaint();
                }
            });

            // ESC to cancel
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        onCancel.run();
                    }
                }
            });
        }

        private Point toScreen(Point pInComponent) {
            // Convert component coords -> screen coords
            return new Point(pInComponent.x + screenBounds.x, pInComponent.y + screenBounds.y);
        }

        private Rectangle rectFrom(Point a, Point b) {
            int x1 = Math.min(a.x, b.x);
            int y1 = Math.min(a.y, b.y);
            int x2 = Math.max(a.x, b.x);
            int y2 = Math.max(a.y, b.y);
            return new Rectangle(x1, y1, Math.max(1, x2 - x1), Math.max(1, y2 - y1));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                // dim background
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // instruction text
                g2.setFont(new Font("Arial", Font.BOLD, 18));
                g2.setColor(Color.WHITE);
                g2.drawString(instruction + "  (ESC to cancel)", 20, 30);

                if (start != null && current != null) {
                    Rectangle r = rectFrom(start, current);

                    // draw selection rectangle (in component coords)
                    int rx = r.x - screenBounds.x;
                    int ry = r.y - screenBounds.y;

                    g2.setStroke(new BasicStroke(2));
                    g2.setColor(new Color(255, 0, 0, 220));
                    g2.drawRect(rx, ry, r.width, r.height);

                    // fill light
                    g2.setColor(new Color(255, 0, 0, 50));
                    g2.fillRect(rx, ry, r.width, r.height);

                    // show size
                    g2.setColor(Color.WHITE);
                    g2.drawString(r.width + " x " + r.height, rx + 8, Math.max(ry - 8, 50));
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
