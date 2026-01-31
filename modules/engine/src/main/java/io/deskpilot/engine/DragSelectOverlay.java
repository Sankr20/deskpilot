package io.deskpilot.engine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.CountDownLatch;

public class DragSelectOverlay {

    /** Returns a Rectangle in SCREEN coordinates (Robot coordinates). */
    public static Rectangle selectRectRobot(String title, Rectangle constrainToRobot) throws Exception {
        CountDownLatch done = new CountDownLatch(1);

        final Rectangle[] result = new Rectangle[1];

        SwingUtilities.invokeLater(() -> {
            JWindow w = new JWindow();
            w.setAlwaysOnTop(true);
            w.setBackground(new Color(0, 0, 0, 0));

            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            w.setBounds(0, 0, screen.width, screen.height);

            OverlayPanel p = new OverlayPanel(title, constrainToRobot, rect -> {
                result[0] = rect;
                done.countDown();
                w.dispose();
            });

            w.setContentPane(p);
            w.setVisible(true);
        });

        done.await();

        if (result[0] == null) throw new RuntimeException("No selection made.");
        return result[0];
    }

    private static final class OverlayPanel extends JPanel {

        private final String title;
        private final Rectangle constrain;
        private final java.util.function.Consumer<Rectangle> onDone;

        private Point start;
        private Point end;

        OverlayPanel(String title, Rectangle constrain, java.util.function.Consumer<Rectangle> onDone) {
            this.title = title;
            this.constrain = constrain;
            this.onDone = onDone;

            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    start = e.getPoint();
                    end = e.getPoint();
                    repaint();
                }

                @Override public void mouseReleased(MouseEvent e) {
                    end = e.getPoint();
                    Rectangle r = rectFromPoints(start, end);
                    if (constrain != null) r = r.intersection(constrain);

                    // require a reasonable size
                    if (r.width < 5 || r.height < 5) {
                        start = null; end = null;
                        repaint();
                        return;
                    }
                    onDone.accept(r);
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseDragged(MouseEvent e) {
                    end = e.getPoint();
                    repaint();
                }
            });

            // ESC cancels
            registerKeyboardAction(
                    e -> { start = null; end = null; onDone.accept(null); },
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW
            );
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            // dim background
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // constrain outline
            if (constrain != null) {
                g2.setColor(new Color(255, 255, 255, 140));
                g2.setStroke(new BasicStroke(2));
                g2.drawRect(constrain.x, constrain.y, constrain.width, constrain.height);
            }

            // title text
            g2.setFont(new Font("Arial", Font.BOLD, 18));
            g2.setColor(new Color(255, 255, 255, 230));
            g2.drawString(title, 18, 30);
            g2.setFont(new Font("Arial", Font.PLAIN, 14));
            g2.drawString("Drag to capture icon/template. ESC to cancel.", 18, 52);

            // selection rect
            if (start != null && end != null) {
                Rectangle r = rectFromPoints(start, end);
                if (constrain != null) r = r.intersection(constrain);

                g2.setColor(new Color(255, 0, 0, 220));
                g2.setStroke(new BasicStroke(3));
                g2.drawRect(r.x, r.y, r.width, r.height);
            }
        }

        private static Rectangle rectFromPoints(Point a, Point b) {
            int x1 = Math.min(a.x, b.x);
            int y1 = Math.min(a.y, b.y);
            int x2 = Math.max(a.x, b.x);
            int y2 = Math.max(a.y, b.y);
            return new Rectangle(x1, y1, x2 - x1, y2 - y1);
        }
    }
}
