package io.deskpilot.demo;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * WinForms-style demo target app for DeskPilot:
 * - Toolbar actions
 * - Left navigation
 * - Form fields + dropdowns
 * - Grid (JTable) like UA-style list
 * - Status bar with deterministic state changes for OCR/template locators
 *
 * This is Swing (not WinForms), but deliberately mimics typical WinForms business UI structure.
 */
public class DeskPilotDemoApp {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DeskPilotDemoApp::start);
    }

    private static void start() {
        JFrame frame = new JFrame("DeskPilot Demo App - WinForms Style");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1080, 720));

        // Root
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        // ===== Top Toolbar (WinForms-ish) =====
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                new EmptyBorder(6, 6, 6, 6)
        ));

        JButton btnNew = toolBtn("New");
        btnNew.setName("BTN_NEW");

        JButton btnSave = toolBtn("Save");
        btnSave.setName("BTN_SAVE");

        JButton btnPost = toolBtn("Post");
        btnPost.setName("BTN_POST");

        JButton btnSearch = toolBtn("Search");
        btnSearch.setName("BTN_SEARCH");

        toolbar.add(btnNew);
        toolbar.add(btnSave);
        toolbar.add(btnPost);
        toolbar.addSeparator(new Dimension(12, 1));
        toolbar.add(btnSearch);

        // ===== Left Navigation (like UA sections) =====
        DefaultListModel<String> navModel = new DefaultListModel<>();
        navModel.addElement("Client & Matters");
        navModel.addElement("Transactions");
        navModel.addElement("Time & Fees");
        navModel.addElement("Billing");
        navModel.addElement("Reports");

        JList<String> navList = new JList<>(navModel);
        navList.setName("NAV_LIST");
        navList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        navList.setSelectedIndex(0);

        JScrollPane navScroll = new JScrollPane(navList);
        navScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                "Navigation",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12)
        ));

        JPanel left = new JPanel(new BorderLayout());
        left.add(navScroll, BorderLayout.CENTER);
        left.setPreferredSize(new Dimension(220, 10));

        // ===== Form Panel (like UA detail panel) =====
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(210, 210, 210)),
                        "Transaction Details",
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        new Font("Segoe UI", Font.BOLD, 12)
                ),
                new EmptyBorder(8, 8, 8, 8)
        ));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;

        JTextField txtClient = field("Client Name");
        txtClient.setName("FIELD_CLIENT");

        JTextField txtMatter = field("Matter ID");
        txtMatter.setName("FIELD_MATTER");

        JComboBox<String> cboActivity = new JComboBox<>(new String[]{
                "Consultation", "Drafting", "Court Appearance", "Research", "Admin"
        });
        cboActivity.setName("CBO_ACTIVITY");
        cboActivity.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JComboBox<String> cboRate = new JComboBox<>(new String[]{
                "Standard - $200/hr", "Senior - $350/hr", "Partner - $500/hr"
        });
        cboRate.setName("CBO_RATE");
        cboRate.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JTextField txtHours = field("Hours");
        txtHours.setName("FIELD_HOURS");

        JTextField txtAmount = field("Amount");
        txtAmount.setName("FIELD_AMOUNT");

        JCheckBox chkBillable = new JCheckBox("Billable");
        chkBillable.setName("CHK_BILLABLE");
        chkBillable.setSelected(true);
        chkBillable.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JRadioButton rbOpen = new JRadioButton("Open");
        rbOpen.setName("RB_OPEN");
        rbOpen.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JRadioButton rbClosed = new JRadioButton("Closed");
        rbClosed.setName("RB_CLOSED");
        rbClosed.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbOpen);
        bg.add(rbClosed);
        rbOpen.setSelected(true);

        JPanel statusRadio = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        statusRadio.setOpaque(false);
        statusRadio.add(rbOpen);
        statusRadio.add(rbClosed);

        addRow(form, gc, 0, "Client:", txtClient);
        addRow(form, gc, 1, "Matter:", txtMatter);
        addRow(form, gc, 2, "Activity:", cboActivity);
        addRow(form, gc, 3, "Rate:", cboRate);
        addRow(form, gc, 4, "Hours:", txtHours);
        addRow(form, gc, 5, "Amount:", txtAmount);

        // Billable + open/closed
        gc.gridy = 6;
        gc.gridx = 0;
        gc.weightx = 0;
        form.add(label("Flags:"), gc);

        JPanel flags = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        flags.setOpaque(false);
        flags.add(chkBillable);
        flags.add(new JSeparator(SwingConstants.VERTICAL));
        flags.add(statusRadio);

        gc.gridx = 1;
        gc.weightx = 1;
        form.add(flags, gc);

        // ===== Grid Panel (UA-like list/table) =====
        TxnTableModel tableModel = new TxnTableModel();
        JTable table = new JTable(tableModel);
        table.setName("GRID_TRANSACTIONS");
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(22);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane gridScroll = new JScrollPane(table);
        gridScroll.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(210, 210, 210)),
                        "Transactions Grid",
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        new Font("Segoe UI", Font.BOLD, 12)
                ),
                new EmptyBorder(4, 4, 4, 4)
        ));

        JButton btnAddRow = new JButton("Add to Grid");
        btnAddRow.setName("BTN_ADD_ROW");
        btnAddRow.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddRow.setFocusable(false);

        JButton btnUpdateRow = new JButton("Update Selected");
        btnUpdateRow.setName("BTN_UPDATE_ROW");
        btnUpdateRow.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnUpdateRow.setFocusable(false);

        JButton btnDeleteRow = new JButton("Delete Selected");
        btnDeleteRow.setName("BTN_DELETE_ROW");
        btnDeleteRow.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDeleteRow.setFocusable(false);

        JPanel gridActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        gridActions.add(btnAddRow);
        gridActions.add(btnUpdateRow);
        gridActions.add(btnDeleteRow);

        JPanel gridPanel = new JPanel(new BorderLayout(8, 8));
        gridPanel.add(gridActions, BorderLayout.NORTH);
        gridPanel.add(gridScroll, BorderLayout.CENTER);

        // ===== Status Bar (critical for OCR/template verification) =====
        JLabel status = new JLabel("Status: idle");
        status.setName("STATUS_LABEL");
        status.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel toast = new JLabel("Ready.");
        toast.setName("TOAST_LABEL");
        toast.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        toast.setForeground(new Color(80, 80, 80));

        JPanel statusBar = new JPanel(new BorderLayout(10, 0));
        statusBar.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                new EmptyBorder(6, 8, 6, 8)
        ));
        statusBar.add(status, BorderLayout.WEST);
        statusBar.add(toast, BorderLayout.CENTER);

        // ===== Split layout: left nav + right content =====
        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.add(form, BorderLayout.NORTH);
        right.add(gridPanel, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(240);
        split.setResizeWeight(0);

        root.add(toolbar, BorderLayout.NORTH);
        root.add(split, BorderLayout.CENTER);
        root.add(statusBar, BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.setLocationRelativeTo(null);

        // ===== Deterministic behaviors (for real automation reliability testing) =====

        Runnable updateAmountFromHoursRate = () -> {
            String hoursText = txtHours.getText().trim();
            double hours = parseDoubleSafe(hoursText, 0.0);

            int rate = switch (cboRate.getSelectedIndex()) {
                case 1 -> 350;
                case 2 -> 500;
                default -> 200;
            };

            double amount = Math.round(hours * rate * 100.0) / 100.0;
            txtAmount.setText(String.format("%.2f", amount));
        };

        txtHours.addActionListener(e -> updateAmountFromHoursRate.run());
        cboRate.addActionListener(e -> updateAmountFromHoursRate.run());

        // Search action: deterministic status + visual flips (good for pixel-based diffs too)
        Runnable doSearch = () -> {
            String now = LocalTime.now().format(TIME);
            String section = navList.getSelectedValue();

            // Deterministic state message
            status.setText("Status: searched");
            toast.setText("Search completed in [" + section + "] at " + now);

            // Flip statusbar background to guarantee a visible change (helps debugging and OCR artifacts)
           // Flip statusbar background to guarantee a visible change (helps debugging and OCR artifacts)
Color bgColor = statusBar.getBackground();
boolean isLight = bgColor.getRed() > 220;
statusBar.setBackground(isLight ? new Color(235, 245, 255) : new Color(245, 235, 255));

        };

        btnSearch.addActionListener(e -> doSearch.run());

        // New: clear form
        btnNew.addActionListener(e -> {
            txtClient.setText("");
            txtMatter.setText("");
            cboActivity.setSelectedIndex(0);
            cboRate.setSelectedIndex(0);
            txtHours.setText("");
            txtAmount.setText("");
            chkBillable.setSelected(true);
            rbOpen.setSelected(true);

            status.setText("Status: new");
            toast.setText("New transaction started at " + LocalTime.now().format(TIME));
        });

        // Save: show modal confirmation (WinForms-ish behavior)
        btnSave.addActionListener(e -> {
            String client = safe(txtClient.getText());
            if (client.isBlank()) {
                JOptionPane.showMessageDialog(frame, "Client is required.", "Validation", JOptionPane.WARNING_MESSAGE);
                status.setText("Status: validation_failed");
                toast.setText("Validation failed at " + LocalTime.now().format(TIME));
                return;
            }

            status.setText("Status: saved");
            toast.setText("Saved at " + LocalTime.now().format(TIME));

            JOptionPane.showMessageDialog(frame, "Saved successfully.", "Save", JOptionPane.INFORMATION_MESSAGE);
        });

        // Post: simulate business rule + status change
        btnPost.addActionListener(e -> {
            if (tableModel.rows.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Grid is empty. Add at least one row.", "Post", JOptionPane.WARNING_MESSAGE);
                status.setText("Status: post_failed");
                toast.setText("Post failed (no rows) at " + LocalTime.now().format(TIME));
                return;
            }
            status.setText("Status: posted");
            toast.setText("Posted " + tableModel.rows.size() + " row(s) at " + LocalTime.now().format(TIME));
        });

        // Grid actions: add/update/delete selected row
        btnAddRow.addActionListener(e -> {
            TxnRow r = rowFromForm(
                    txtClient.getText(), txtMatter.getText(),
                    (String) cboActivity.getSelectedItem(),
                    (String) cboRate.getSelectedItem(),
                    txtHours.getText(), txtAmount.getText(),
                    chkBillable.isSelected(),
                    rbOpen.isSelected() ? "Open" : "Closed"
            );
            tableModel.add(r);

            status.setText("Status: row_added");
            toast.setText("Row added at " + LocalTime.now().format(TIME));
        });

        btnUpdateRow.addActionListener(e -> {
            int idx = table.getSelectedRow();
            if (idx < 0) {
                JOptionPane.showMessageDialog(frame, "Select a row to update.", "Update", JOptionPane.WARNING_MESSAGE);
                return;
            }
            TxnRow r = rowFromForm(
                    txtClient.getText(), txtMatter.getText(),
                    (String) cboActivity.getSelectedItem(),
                    (String) cboRate.getSelectedItem(),
                    txtHours.getText(), txtAmount.getText(),
                    chkBillable.isSelected(),
                    rbOpen.isSelected() ? "Open" : "Closed"
            );
            tableModel.update(idx, r);

            status.setText("Status: row_updated");
            toast.setText("Row updated at " + LocalTime.now().format(TIME));
        });

        btnDeleteRow.addActionListener(e -> {
            int idx = table.getSelectedRow();
            if (idx < 0) {
                JOptionPane.showMessageDialog(frame, "Select a row to delete.", "Delete", JOptionPane.WARNING_MESSAGE);
                return;
            }
            tableModel.remove(idx);

            status.setText("Status: row_deleted");
            toast.setText("Row deleted at " + LocalTime.now().format(TIME));
        });

        // Navigation changes update status (mimic app context switch)
        navList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                status.setText("Status: section_changed");
                toast.setText("Section: " + navList.getSelectedValue());
            }
        });

        // Seed grid with a couple of rows so template matching has stable targets
        tableModel.add(new TxnRow("Acme Corp", "M-1001", "Consultation", "Standard - $200/hr", "1.00", "200.00", true, "Open"));
        tableModel.add(new TxnRow("Globex", "M-1002", "Research", "Senior - $350/hr", "0.50", "175.00", true, "Open"));

        // Known initial state
        status.setText("Status: idle");
        toast.setText("Ready. Use toolbar + grid actions to simulate UA-like flows.");

        frame.setVisible(true);
    }

    private static JButton toolBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setFocusable(false);
        return b;
    }

    private static JLabel label(String s) {
        JLabel l = new JLabel(s);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return l;
    }

    private static JTextField field(String placeholder) {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setColumns(18);
        f.putClientProperty("JTextField.placeholderText", placeholder); // harmless if LAF ignores
        return f;
    }

    private static void addRow(JPanel panel, GridBagConstraints gc, int row, String labelText, JComponent comp) {
        gc.gridy = row;

        gc.gridx = 0;
        gc.weightx = 0;
        panel.add(label(labelText), gc);

        gc.gridx = 1;
        gc.weightx = 1;
        panel.add(comp, gc);
    }

    private static double parseDoubleSafe(String s, double def) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception ignore) {
            return def;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static TxnRow rowFromForm(
            String client, String matter, String activity, String rate,
            String hours, String amount, boolean billable, String openClosed
    ) {
        String c = safe(client);
        if (c.isBlank()) c = "<client>";
        String m = safe(matter);
        if (m.isBlank()) m = "<matter>";
        String h = safe(hours);
        if (h.isBlank()) h = "0.00";
        String a = safe(amount);
        if (a.isBlank()) a = "0.00";

        return new TxnRow(c, m, activity, rate, h, a, billable, openClosed);
    }

    // ===== Table Model (grid-like behavior) =====

    private static final class TxnTableModel extends AbstractTableModel {
        private final String[] cols = {
                "Client", "Matter", "Activity", "Rate", "Hours", "Amount", "Billable", "Status"
        };
        private final List<TxnRow> rows = new ArrayList<>();

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            TxnRow r = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> r.client;
                case 1 -> r.matter;
                case 2 -> r.activity;
                case 3 -> r.rate;
                case 4 -> r.hours;
                case 5 -> r.amount;
                case 6 -> r.billable ? "Yes" : "No";
                case 7 -> r.openClosed;
                default -> "";
            };
        }

        public void add(TxnRow r) {
            rows.add(r);
            int idx = rows.size() - 1;
            fireTableRowsInserted(idx, idx);
        }

        public void update(int idx, TxnRow r) {
            if (idx < 0 || idx >= rows.size()) return;
            rows.set(idx, r);
            fireTableRowsUpdated(idx, idx);
        }

        public void remove(int idx) {
            if (idx < 0 || idx >= rows.size()) return;
            rows.remove(idx);
            fireTableRowsDeleted(idx, idx);
        }
    }

    private static final class TxnRow {
        final String client;
        final String matter;
        final String activity;
        final String rate;
        final String hours;
        final String amount;
        final boolean billable;
        final String openClosed;

        TxnRow(String client, String matter, String activity, String rate, String hours, String amount, boolean billable, String openClosed) {
            this.client = client;
            this.matter = matter;
            this.activity = activity;
            this.rate = rate;
            this.hours = hours;
            this.amount = amount;
            this.billable = billable;
            this.openClosed = openClosed;
        }
    }
}
