import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;



/* =========================================================
   TICKET CLASS
   Represents a customer support ticket.
   ========================================================= */
class Ticket {

    private int ticketId;
    private String customerName;
    private String issue;
    private String status;
    private SupportRepresentative representative;

    private long startTime;
    private long endTime;


    // Constructor
    public Ticket(int ticketId, String customerName, String issue) {
        this.ticketId = ticketId;
        this.customerName = customerName;
        this.issue = issue;
        this.status = "Pending";
        this.representative = null;
    }


    // Getters
    public int getTicketId() {
        return ticketId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getIssue() {
        return issue;
    }

    public String getStatus() {
        return status;
    }

    public SupportRepresentative getRepresentative() {
        return representative;
    }


    // Setters
    public void setStatus(String status) {
        this.status = status;
    }

    public void setRepresentative(
            SupportRepresentative representative) {

        this.representative = representative;
    }


    // Start ticket processing
    public void startWork() {
        startTime = System.currentTimeMillis();
        status = "In Progress";
    }


    // Resolve ticket
    public void resolve() {
        endTime = System.currentTimeMillis();
        status = "Resolved";
    }


    // Calculate resolution time
    public long getResolutionTime() {

        if (startTime == 0 || endTime == 0) {
            return 0;
        }

        return endTime - startTime;
    }


    // Display ticket details
    public void display() {

        String representativeName;

        if (representative == null) {
            representativeName = "Not Assigned";
        } else {
            representativeName = representative.getName();
        }

        System.out.printf(
            "Ticket #%d | Customer: %s | Issue: %s | " +
            "Status: %s | Representative: %s%n",
            ticketId,
            customerName,
            issue,
            status,
            representativeName
        );
    }
}


/* =========================================================
   SUPPORT REPRESENTATIVE CLASS
   Represents a support representative.
   ========================================================= */
class SupportRepresentative {

    private int id;
    private String name;


    // Constructor
    public SupportRepresentative(int id, String name) {
        this.id = id;
        this.name = name;
    }


    // Getter
    public String getName() {
        return name;
    }


    // Display representative details
    public void display() {

        System.out.println(
            "Representative " + id + ": " + name
        );
    }
}


/* =========================================================
   TICKET SYSTEM CLASS
   Manages the queue, tickets, representatives,
   processing and reports.
   ========================================================= */
class TicketSystem {

    // FIFO queue
    private Queue<Ticket> ticketQueue =
            new LinkedList<>();

    // Stores every ticket created
    private ArrayList<Ticket> allTickets =
            new ArrayList<>();

    // Stores support representatives
    private ArrayList<SupportRepresentative> representatives =
            new ArrayList<>();

    // Used to generate ticket IDs
    private int nextTicketId = 1;


    // Constructor
    public TicketSystem() {

        // Neutral representative names
        representatives.add(
            new SupportRepresentative(1, "Representative1")
        );

        representatives.add(
            new SupportRepresentative(2, "Representative2")
        );
    }


    /* -----------------------------------------------------
       CREATE TICKET
       Adds a new ticket to the FIFO queue.
       ----------------------------------------------------- */
    public synchronized void createTicket(
            String customerName,
            String issue) {

        Ticket ticket = new Ticket(
            nextTicketId++,
            customerName,
            issue
        );

        // Add to FIFO queue
        ticketQueue.add(ticket);

        // Store in complete ticket history
        allTickets.add(ticket);


        System.out.println(
            "\nTicket created successfully!"
        );

        System.out.println(
            "Ticket ID: " + ticket.getTicketId()
        );

        System.out.println(
            "Added to queue at position: "
            + ticketQueue.size()
        );
    }


    /* -----------------------------------------------------
       GET NEXT TICKET
       Removes the first ticket from the FIFO queue.

       synchronized ensures that two threads cannot
       remove the same ticket at the same time.
       ----------------------------------------------------- */
    public synchronized Ticket getNextTicket(
            SupportRepresentative representative) {

        // FIFO operation
        Ticket ticket = ticketQueue.poll();


        if (ticket != null) {

            // Assign representative
            ticket.setRepresentative(representative);

            // Change status to In Progress
            ticket.startWork();
        }


        return ticket;
    }


    /* -----------------------------------------------------
       VIEW PENDING TICKETS
       Shows only tickets currently waiting in the queue.
       ----------------------------------------------------- */
    public synchronized void displayPendingTickets() {

        if (ticketQueue.isEmpty()) {

            System.out.println(
                "\nNo pending tickets in the queue."
            );

            return;
        }


        System.out.println(
            "\n--- PENDING TICKETS (FIFO ORDER) ---"
        );


        for (Ticket ticket : ticketQueue) {
            ticket.display();
        }
    }


    /* -----------------------------------------------------
       VIEW ALL TICKETS
       Shows complete ticket history.
       ----------------------------------------------------- */
    public synchronized void displayAllTickets() {

        if (allTickets.isEmpty()) {

            System.out.println(
                "\nNo tickets have been created yet."
            );

            return;
        }


        System.out.println(
            "\n--- ALL TICKETS ---"
        );


        for (Ticket ticket : allTickets) {
            ticket.display();
        }
    }


    /* -----------------------------------------------------
       GET ALL TICKETS
       Returns a copy of the ticket list.
       ----------------------------------------------------- */
    public synchronized ArrayList<Ticket> getAllTickets() {

        return new ArrayList<>(allTickets);
    }


    /* -----------------------------------------------------
       PROCESS ALL TICKETS
       Creates two worker threads.

       Each thread represents a support representative
       and repeatedly takes the next ticket from the
       shared FIFO queue.
       ----------------------------------------------------- */
    public void processAllTickets() {

        // Check whether there are tickets to process
        synchronized (this) {

            if (ticketQueue.isEmpty()) {

                System.out.println(
                    "\nNo pending tickets to process."
                );

                return;
            }
        }


        System.out.println(
            "\nStarting multithreaded ticket processing..."
        );

        System.out.println(
            "Two support representatives are now "
            + "processing the queue.\n"
        );


        ArrayList<TicketProcessor> processors =
                new ArrayList<>();


        // Create one thread for each representative
        for (SupportRepresentative representative
                : representatives) {

            TicketProcessor processor =
                    new TicketProcessor(
                        this,
                        representative
                    );

            processors.add(processor);

            processor.start();
        }


        // Wait for all threads to finish
        for (TicketProcessor processor : processors) {

            try {

                processor.join();

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                System.out.println(
                    "Processing was interrupted."
                );
            }
        }


        System.out.println(
            "\nAll pending tickets have been processed."
        );
    }


    /* -----------------------------------------------------
       GENERATE REPORT
       Calculates ticket statistics.
       ----------------------------------------------------- */
    public void generateReport() {

        ArrayList<Ticket> tickets =
                getAllTickets();


        int resolved = 0;
        long totalTime = 0;


        for (Ticket ticket : tickets) {

            if (ticket.getStatus().equals("Resolved")) {

                resolved++;

                totalTime +=
                    ticket.getResolutionTime();
            }
        }


        System.out.println(
            "\n========== SUPPORT REPORT =========="
        );

        System.out.println(
            "Total tickets   : " + tickets.size()
        );

        System.out.println(
            "Resolved tickets: " + resolved
        );

        System.out.println(
            "Pending tickets : "
            + (tickets.size() - resolved)
        );


        if (resolved > 0) {

            System.out.println(
                "Average resolution time: "
                + (totalTime / resolved)
                + " ms"
            );

        } else {

            System.out.println(
                "Average resolution time: "
                + "No resolved tickets"
            );
        }


        System.out.println(
            "===================================="
        );


        // Save report to file
        saveReportToFile(
            tickets,
            resolved,
            totalTime
        );
    }


    /* -----------------------------------------------------
       SAVE REPORT TO FILE
       Demonstrates file handling.
       ----------------------------------------------------- */
    private void saveReportToFile(
            ArrayList<Ticket> tickets,
            int resolved,
            long totalTime) {

        try {

            FileWriter writer =
                    new FileWriter(
                        "support_report.txt"
                    );


            writer.write(
                "CUSTOMER SUPPORT TICKETING SYSTEM REPORT\n"
            );

            writer.write(
                "=========================================\n"
            );

            writer.write(
                "Total tickets: "
                + tickets.size()
                + "\n"
            );

            writer.write(
                "Resolved tickets: "
                + resolved
                + "\n"
            );

            writer.write(
                "Pending tickets: "
                + (tickets.size() - resolved)
                + "\n"
            );


            if (resolved > 0) {

                writer.write(
                    "Average resolution time: "
                    + (totalTime / resolved)
                    + " ms\n"
                );
            }


            writer.write(
                "\nTICKET DETAILS\n"
            );


            for (Ticket ticket : tickets) {

                String representativeName;

                if (ticket.getRepresentative() == null) {

                    representativeName =
                            "Not Assigned";

                } else {

                    representativeName =
                            ticket.getRepresentative()
                                  .getName();
                }


                writer.write(
                    "Ticket #"
                    + ticket.getTicketId()
                    + " | Customer: "
                    + ticket.getCustomerName()
                    + " | Status: "
                    + ticket.getStatus()
                    + " | Representative: "
                    + representativeName
                    + " | Resolution: "
                    + ticket.getResolutionTime()
                    + " ms\n"
                );
            }


            writer.close();


            System.out.println(
                "Report saved to support_report.txt"
            );


        } catch (IOException e) {

            System.out.println(
                "Could not save report: "
                + e.getMessage()
            );
        }
    }
}


/* =========================================================
   TICKET PROCESSOR CLASS
   Extends Thread.

   Each TicketProcessor represents a worker thread
   associated with one support representative.
   ========================================================= */
class TicketProcessor extends Thread {

    private TicketSystem system;
    private SupportRepresentative representative;


    // Constructor
    public TicketProcessor(
            TicketSystem system,
            SupportRepresentative representative) {

        this.system = system;
        this.representative = representative;
    }


    /* -----------------------------------------------------
       RUN METHOD
       Executed automatically when start() is called.
       ----------------------------------------------------- */
    @Override
    public void run() {

        while (true) {

            // Get next ticket from FIFO queue
            Ticket ticket =
                    system.getNextTicket(
                        representative
                    );


            // Stop when queue becomes empty
            if (ticket == null) {
                break;
            }


            System.out.println(
                Thread.currentThread().getName()
                + " | "
                + representative.getName()
                + " assigned Ticket #"
                + ticket.getTicketId()
                + " | "
                + ticket.getIssue()
            );


            try {

                // Simulate ticket processing
                Thread.sleep(1000);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                System.out.println(
                    "Ticket processing interrupted."
                );

                return;
            }


            // Resolve ticket
            ticket.resolve();


            System.out.println(
                Thread.currentThread().getName()
                + " | Ticket #"
                + ticket.getTicketId()
                + " RESOLVED in "
                + ticket.getResolutionTime()
                + " ms\n"
            );
        }
    }
}


/* =========================================================
   MAIN CLASS
   CustomerSupport.java
   ========================================================= */
public class CustomerSupport {

    private static Scanner scanner =
            new Scanner(System.in);

    private static TicketSystem system =
            new TicketSystem();


    /* -----------------------------------------------------
       MAIN METHOD
       ----------------------------------------------------- */
    public static void main(String[] args) {
        if (args.length > 0 && "--console".equalsIgnoreCase(args[0])) {
            runConsole();
            return;
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        SwingUtilities.invokeLater(() ->
            new CustomerSupportDashboard(system).setVisible(true)
        );
    }

    /** Original console menu remains available with: java CustomerSupport --console */
    private static void runConsole() {

        int choice;


        System.out.println(
            "================================================"
        );

        System.out.println(
            "       CUSTOMER SUPPORT TICKETING SYSTEM"
        );

        System.out.println(
            "================================================"
        );

        System.out.println(
            "Java | Queue + FIFO | OOP | Multithreading"
        );


        do {

            showMenu();

            choice =
                readInt("Enter your choice: ");


            switch (choice) {

                case 1:
                    createTicket();
                    break;


                case 2:
                    system.displayPendingTickets();
                    break;


                case 3:
                    system.processAllTickets();
                    break;


                case 4:
                    system.displayAllTickets();
                    break;


                case 5:
                    system.generateReport();
                    break;


                case 6:

                    System.out.println(
                        "\nThank you for using the "
                        + "Customer Support Ticketing System!"
                    );

                    break;


                default:

                    System.out.println(
                        "\nInvalid choice. "
                        + "Please select 1-6."
                    );
            }


        } while (choice != 6);


        scanner.close();
    }


    /* -----------------------------------------------------
       DISPLAY MAIN MENU
       ----------------------------------------------------- */
    private static void showMenu() {

        System.out.println(
            "\n---------------- MAIN MENU ----------------"
        );

        System.out.println(
            "1. Create Support Ticket"
        );

        System.out.println(
            "2. View Pending Tickets (FIFO Queue)"
        );

        System.out.println(
            "3. Process All Tickets (Multithreaded)"
        );

        System.out.println(
            "4. View All Tickets (Complete History)"
        );

        System.out.println(
            "5. Generate Support Report"
        );

        System.out.println(
            "6. Exit"
        );

        System.out.println(
            "--------------------------------------------"
        );
    }


    /* -----------------------------------------------------
       CREATE TICKET INPUT
       ----------------------------------------------------- */
    private static void createTicket() {

        String name;
        String issue;


        // Customer name validation
        do {

            System.out.print(
                "Enter customer name: "
            );

            name =
                scanner.nextLine().trim();


            if (name.isEmpty()) {

                System.out.println(
                    "Customer name cannot be empty."
                );
            }

        } while (name.isEmpty());


        // Issue validation
        do {

            System.out.print(
                "Enter issue description: "
            );

            issue =
                scanner.nextLine().trim();


            if (issue.isEmpty()) {

                System.out.println(
                    "Issue description cannot be empty."
                );
            }

        } while (issue.isEmpty());


        // Create ticket
        system.createTicket(
            name,
            issue
        );
    }


    /* -----------------------------------------------------
       INTEGER INPUT VALIDATION
       Handles NumberFormatException.
       ----------------------------------------------------- */
    private static int readInt(String message) {

        while (true) {

            try {

                System.out.print(message);

                return Integer.parseInt(
                    scanner.nextLine().trim()
                );


            } catch (NumberFormatException e) {

                System.out.println(
                    "Please enter a valid number."
                );
            }
        }
    }
}


/* =========================================================
   SWING DASHBOARD UI (additive presentation layer)
   Uses the existing Ticket, TicketSystem and representative
   classes above; ticket business logic remains in backend.
   ========================================================= */

/** Additive Swing dashboard for the existing TicketSystem backend. */
class CustomerSupportDashboard extends JFrame {
    // Soft Clean Minimalist design tokens
    private static final Color BG = new Color(248, 249, 250);       // #F8F9FA
    private static final Color SURFACE = Color.WHITE;               // #FFFFFF
    private static final Color PRIMARY = new Color(79, 70, 229);     // #4F46E5
    private static final Color SECONDARY = new Color(113, 128, 150);// #718096
    private static final Color ACCENT = new Color(16, 185, 129);     // #10B981
    private static final Color TEXT = new Color(30, 41, 59);         // #1E293B
    private static final Color MUTED = new Color(100, 116, 139);    // #64748B
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color PALE_BLUE = new Color(238, 242, 255);
    private static final Color PALE_GREEN = new Color(236, 253, 245);
    private static final Color PALE_AMBER = new Color(255, 251, 235);

    private final TicketSystem backend;
    private final JTextField customerField = new JTextField();
    private final JTextField issueField = new JTextField();
    private final JLabel totalValue = new JLabel("0");
    private final JLabel pendingValue = new JLabel("0");
    private final JLabel progressValue = new JLabel("0");
    private final JLabel resolvedValue = new JLabel("0");
    private final JLabel statusLabel = new JLabel("Ready — create a ticket to get started.");
    private final DefaultTableModel model = new DefaultTableModel(
        new Object[]{"Ticket ID", "Customer", "Issue", "Status", "Representative", "Resolution (ms)"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JButton createButton = button("＋  Create ticket", PRIMARY, Color.WHITE);
    private final JButton processButton = button("▶  Process all", ACCENT, Color.WHITE);
    private final JButton refreshButton = button("↻  Refresh", PALE_BLUE, PRIMARY);
    private final JButton reportButton = button("Generate report", SURFACE, TEXT);
    private final JButton openReportButton = button("Open report file", SURFACE, PRIMARY);
    private boolean processing;

    public CustomerSupportDashboard(TicketSystem backend) {
        super("Customer Support Ticketing System");
        if (backend == null) throw new IllegalArgumentException("TicketSystem cannot be null.");
        this.backend = backend;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 680));
        setSize(1200, 780);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        configureTable();
        bindActions();
        refreshDashboard();
        new Timer(500, e -> refreshDashboard()).start();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 8));
        header.setBackground(SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
            BorderFactory.createEmptyBorder(24, 32, 24, 32)));
        JPanel titles = new JPanel(new GridLayout(0, 1, 0, 6));
        titles.setOpaque(false);
        JLabel eyebrow = label("SUPPORT OPERATIONS  /  OVERVIEW", 11, PRIMARY, true);
        JLabel title = label("Customer Support Ticketing System", 25, TEXT, true);
        JLabel subtitle = label("Manage incoming tickets, track progress, and generate reports.", 13, MUTED, false);
        titles.add(eyebrow); titles.add(title); titles.add(subtitle);
        JLabel badge = label("  FIFO  •  MULTITHREADING  ", 12, PRIMARY, true);
        badge.setOpaque(true); badge.setBackground(PALE_BLUE);
        badge.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        header.add(titles, BorderLayout.CENTER);
        header.add(badge, BorderLayout.EAST);
        return header;
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBackground(BG);
        content.setBorder(BorderFactory.createEmptyBorder(24, 32, 20, 32));
        JPanel upper = new JPanel(new BorderLayout(0, 18));
        upper.setOpaque(false);
        JPanel metrics = new JPanel(new GridLayout(1, 4, 16, 0));
        metrics.setOpaque(false);
        metrics.add(metric("TOTAL TICKETS", totalValue, PRIMARY, PALE_BLUE));
        metrics.add(metric("PENDING", pendingValue, new Color(180, 120, 20), PALE_AMBER));
        metrics.add(metric("IN PROGRESS", progressValue, PRIMARY, PALE_BLUE));
        metrics.add(metric("RESOLVED", resolvedValue, ACCENT, PALE_GREEN));
        upper.add(metrics, BorderLayout.NORTH);
        upper.add(buildCreateCard(), BorderLayout.CENTER);
        content.add(upper, BorderLayout.NORTH);
        content.add(buildTableCard(), BorderLayout.CENTER);
        return content;
    }

    private JPanel metric(String caption, JLabel value, Color numberColor, Color tint) {
        JPanel card = cardPanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        card.add(label(caption, 11, MUTED, true), BorderLayout.NORTH);
        value.setFont(new Font("SansSerif", Font.BOLD, 28)); value.setForeground(numberColor);
        JPanel row = new JPanel(new BorderLayout(12, 0)); row.setOpaque(false);
        JPanel bar = new JPanel(); bar.setBackground(tint); bar.setPreferredSize(new Dimension(5, 42));
        row.add(bar, BorderLayout.WEST); row.add(value, BorderLayout.CENTER);
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildCreateCard() {
        JPanel card = cardPanel(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(18, 20, 18, 20)));
        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 5)); heading.setOpaque(false);
        heading.add(label("Create a support ticket", 17, TEXT, true));
        heading.add(label("New tickets enter the queue as Pending.", 12, MUTED, false));
        JPanel fields = new JPanel(new GridLayout(1, 2, 16, 0)); fields.setOpaque(false);
        fields.add(fieldGroup("CUSTOMER NAME", customerField, "Enter customer name"));
        fields.add(fieldGroup("ISSUE DESCRIPTION", issueField, "Briefly describe the issue"));
        JPanel bottom = new JPanel(new BorderLayout(12, 0)); bottom.setOpaque(false);
        bottom.add(fields, BorderLayout.CENTER); bottom.add(createButton, BorderLayout.EAST);
        card.add(heading, BorderLayout.NORTH); card.add(bottom, BorderLayout.CENTER);
        return card;
    }

    private JPanel fieldGroup(String caption, JTextField field, String hint) {
        JPanel p = new JPanel(new BorderLayout(0, 7)); p.setOpaque(false);
        p.add(label(caption, 10, SECONDARY, true), BorderLayout.NORTH);
        field.setToolTipText(hint); field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setForeground(TEXT); field.setCaretColor(PRIMARY); field.setPreferredSize(new Dimension(200, 40));
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        p.add(field, BorderLayout.CENTER); return p;
    }

    private JPanel buildTableCard() {
        JPanel card = cardPanel(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(20, 20, 16, 20)));
        JPanel heading = new JPanel(new BorderLayout(12, 8)); heading.setOpaque(false);
        JPanel labels = new JPanel(new GridLayout(0, 1, 0, 5)); labels.setOpaque(false);
        labels.add(label("Ticket management", 17, TEXT, true));
        labels.add(label("All tickets and their latest status", 12, MUTED, false));
        JPanel actions = new JPanel(); actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
        actions.add(refreshButton); actions.add(Box.createHorizontalStrut(8));
        actions.add(reportButton); actions.add(Box.createHorizontalStrut(8));
        actions.add(openReportButton); actions.add(Box.createHorizontalStrut(8)); actions.add(processButton);
        heading.add(labels, BorderLayout.WEST); heading.add(actions, BorderLayout.EAST);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER)); scroll.getViewport().setBackground(SURFACE);
        card.add(heading, BorderLayout.NORTH); card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout()); footer.setBackground(SURFACE);
        footer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
            BorderFactory.createEmptyBorder(10, 32, 10, 32)));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12)); statusLabel.setForeground(MUTED);
        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(label("Customer Support  •  Java Swing", 11, SECONDARY, false), BorderLayout.EAST);
        return footer;
    }

    private void configureTable() {
        table.setFont(new Font("SansSerif", Font.PLAIN, 12)); table.setForeground(TEXT);
        table.setBackground(SURFACE); table.setGridColor(new Color(241, 245, 249));
        table.setRowHeight(34); table.setShowVerticalLines(false); table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1)); table.setFillsViewportHeight(true);
        table.setSelectionBackground(PALE_BLUE); table.setSelectionForeground(TEXT);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        table.getTableHeader().setForeground(SECONDARY); table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean selected,
                    boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, selected, focus, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                setFont(new Font("SansSerif", Font.BOLD, 11));
                if (!selected) {
                    String s = v == null ? "" : v.toString();
                    setForeground("Resolved".equals(s) ? ACCENT : "In Progress".equals(s) ? PRIMARY : new Color(180, 120, 20));
                }
                return c;
            }
        });
    }

    private void bindActions() {
        createButton.addActionListener(this::createTicket);
        issueField.addActionListener(this::createTicket);
        customerField.addActionListener(e -> issueField.requestFocusInWindow());
        refreshButton.addActionListener(e -> { refreshDashboard(); statusLabel.setText("Dashboard refreshed."); });
        processButton.addActionListener(e -> processTickets());
        reportButton.addActionListener(e -> generateReport());
        openReportButton.addActionListener(e -> openReport());
    }

    private void createTicket(ActionEvent event) {
        String customer = customerField.getText().trim();
        String issue = issueField.getText().trim();
        if (customer.isEmpty() || issue.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter both a customer name and issue description.",
                "Missing ticket details", JOptionPane.WARNING_MESSAGE); return;
        }
        try {
            // Calls existing backend method; no ticket model or business logic is duplicated.
            backend.createTicket(customer, issue);
            customerField.setText(""); issueField.setText("");
            refreshDashboard(); statusLabel.setText("Ticket created and added to the FIFO queue.");
        } catch (RuntimeException ex) { showError("Could not create ticket: " + ex.getMessage()); }
    }

    private void processTickets() {
        if (processing) { statusLabel.setText("Ticket processing is already running."); return; }
        boolean hasPending = false;
        for (Ticket ticket : backend.getAllTickets()) if ("Pending".equals(ticket.getStatus())) { hasPending = true; break; }
        if (!hasPending) {
            JOptionPane.showMessageDialog(this, "There are no pending tickets to process.",
                "Queue is empty", JOptionPane.INFORMATION_MESSAGE); return;
        }
        processing = true; processButton.setEnabled(false); statusLabel.setText("Processing tickets in the background…");
        // Backend joins its worker threads, so call it off Swing's Event Dispatch Thread.
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() { backend.processAllTickets(); return null; }
            @Override protected void done() {
                processing = false; processButton.setEnabled(true); refreshDashboard();
                statusLabel.setText("Ticket processing finished.");
            }
        }.execute();
    }

    private void generateReport() {
        try {
            backend.generateReport(); // Existing report logic writes support_report.txt.
            statusLabel.setText("Report generated: support_report.txt");
            JOptionPane.showMessageDialog(this,
                "Report generated as support_report.txt in the program's working directory.",
                "Report generated", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException ex) { showError("Could not generate report: " + ex.getMessage()); }
    }

    private void openReport() {
        File report = new File("support_report.txt");
        if (!report.exists()) {
            JOptionPane.showMessageDialog(this, "Generate the report first.", "Report not found", JOptionPane.INFORMATION_MESSAGE); return;
        }
        if (!Desktop.isDesktopSupported()) {
            JOptionPane.showMessageDialog(this, "Report location:\n" + report.getAbsolutePath(),
                "Open report manually", JOptionPane.INFORMATION_MESSAGE); return;
        }
        try { Desktop.getDesktop().open(report.getAbsoluteFile()); }
        catch (IOException ex) { showError("Could not open report: " + ex.getMessage()); }
    }

    private void refreshDashboard() {
        ArrayList<Ticket> tickets = backend.getAllTickets();
        int pending = 0, inProgress = 0, resolved = 0;
        model.setRowCount(0);
        for (Ticket ticket : tickets) {
            String status = ticket.getStatus();
            if ("Pending".equals(status)) pending++;
            else if ("In Progress".equals(status)) inProgress++;
            else if ("Resolved".equals(status)) resolved++;
            SupportRepresentative rep = ticket.getRepresentative();
            model.addRow(new Object[]{ticket.getTicketId(), ticket.getCustomerName(), ticket.getIssue(), status,
                rep == null ? "Not Assigned" : rep.getName(),
                "Resolved".equals(status) ? ticket.getResolutionTime() : "—"});
        }
        totalValue.setText(String.valueOf(tickets.size())); pendingValue.setText(String.valueOf(pending));
        progressValue.setText(String.valueOf(inProgress)); resolvedValue.setText(String.valueOf(resolved));
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Operation failed", JOptionPane.ERROR_MESSAGE);
        statusLabel.setText("An operation could not be completed.");
    }

    private static JPanel cardPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout); panel.setBackground(SURFACE); return panel;
    }

    private static JLabel label(String text, int size, Color color, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color); return label;
    }

    private static JButton button(String text, Color background, Color foreground) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 12)); b.setForeground(foreground); b.setBackground(background);
        b.setFocusPainted(false); b.setBorderPainted(false); b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(140, 40)); return b;
    }

}
