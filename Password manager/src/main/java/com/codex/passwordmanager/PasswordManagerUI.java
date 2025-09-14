package com.codex.passwordmanager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class PasswordManagerUI extends JFrame {
    // Enhanced Color palette with better contrast
    private static final Color PRIMARY_COLOR = new Color(41, 128, 185);     // More vibrant blue
    private static final Color SECONDARY_COLOR = new Color(52, 152, 219);   // Bright blue
    private static final Color ACCENT_COLOR = new Color(46, 204, 113);      // Success green
    private static final Color WARNING_COLOR = new Color(241, 196, 15);     // Warning yellow
    private static final Color ERROR_COLOR = new Color(231, 76, 60);        // Error red
    
    private static final Color BACKGROUND_COLOR = new Color(245, 247, 250); // Light gray-blue
    private static final Color CARD_BACKGROUND = Color.WHITE;               // White for cards
    private static final Color TEXT_PRIMARY = new Color(44, 62, 80);        // Darker gray for better contrast
    private static final Color TEXT_SECONDARY = new Color(127, 140, 141);   // Medium gray for secondary text
    private static final Color BORDER_COLOR = new Color(189, 195, 199);     // More visible border
    
    // New colors for better visibility
    private static final Color TABLE_HEADER_BG = new Color(52, 73, 94);
    private static final Color TABLE_SELECTION_BG = new Color(236, 240, 241);
    private static final Color TABLE_SELECTION_FG = new Color(44, 62, 80);
    
    private JTextField websiteField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField searchField;
    private JLabel statusLabel;
    private JTable passwordTable;
    private DefaultTableModel tableModel;
    private File pendriveRoot;
    private List<PasswordEntry> passwordEntries;
    // Strength meter UI references
    private JLabel[] strengthBars = new JLabel[4];
    private JLabel strengthTextLabel;

    public PasswordManagerUI() {
        passwordEntries = new ArrayList<>();
        initializeUI();
        setupComponents();
        detectPendrive();
        loadPasswords();
    }

    private void initializeUI() {
        setTitle("🔐 Pendrive Password Manager");
        setSize(1200, 750); // Default size
        setMinimumSize(new Dimension(1100, 650));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
        
        // Set modern look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // Customize UI defaults for better appearance
            UIManager.put("Button.foreground", Color.WHITE);
            UIManager.put("Button.background", SECONDARY_COLOR);
            UIManager.put("Button.focus", new Color(0, 0, 0, 0));
            UIManager.put("Table.selectionBackground", TABLE_SELECTION_BG);
            UIManager.put("Table.selectionForeground", TABLE_SELECTION_FG);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Set application icon
        setIconImage(createAppIcon());
        
        // Main container with modern styling
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BACKGROUND_COLOR);
        
        // Add padding to the main container
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        mainPanel.setBackground(BACKGROUND_COLOR);
        add(mainPanel, BorderLayout.CENTER);
        
        // Create header
        createHeader(mainPanel);
        
        // Create main content area
        createMainContent(mainPanel);
        
        // Create status bar
        createStatusBar(mainPanel);
        
        // Add keyboard shortcuts
        setupKeyboardShortcuts();
    }

    private void createHeader(JPanel parent) {
        JPanel headerPanel = new GradientPanel(PRIMARY_COLOR, new Color(39, 55, 70));
        headerPanel.setBorder(new EmptyBorder(20, 25, 20, 25));
        
        JLabel titleLabel = new JLabel("CryptoShield Pendrive Password Manager");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
        
        JLabel subtitleLabel = new JLabel("Secure password storage on your USB drive");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(220, 220, 220));
        
        // Add status indicator
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusPanel.setOpaque(false);
        
        JLabel statusIndicator = new JLabel(" Online");
        statusIndicator.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusIndicator.setForeground(new Color(230, 230, 230));
        statusIndicator.setToolTipText("Pendrive connection status");
        
        statusPanel.add(statusIndicator);
        
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(subtitleLabel, BorderLayout.SOUTH);
        
        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(statusPanel, BorderLayout.EAST);
        parent.add(headerPanel, BorderLayout.NORTH);
    }

    private void createMainContent(JPanel parent) {
        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBackground(BACKGROUND_COLOR);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Left panel for password entry
        createPasswordEntryPanel(contentPanel);
        
        // Right panel for password list
        createPasswordListPanel(contentPanel);
        
        parent.add(contentPanel, BorderLayout.CENTER);
    }

    private void createPasswordEntryPanel(JPanel parent) {
        JPanel entryPanel = new JPanel();
        entryPanel.setLayout(new BoxLayout(entryPanel, BoxLayout.Y_AXIS));
        entryPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        entryPanel.setBackground(CARD_BACKGROUND);
        entryPanel.setPreferredSize(new Dimension(420, 0));
        
        // Panel title
        JLabel titleLabel = new JLabel("add new password", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        titleLabel.setForeground(PRIMARY_COLOR);
        titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        entryPanel.add(titleLabel);
        entryPanel.add(Box.createVerticalStrut(5));
        
        // Website field
        JPanel websitePanel = createFieldPanel("Website:", websiteField = new JTextField());
        entryPanel.add(websitePanel);
        entryPanel.add(Box.createVerticalStrut(12));
        
        // Username field
        JPanel usernamePanel = createFieldPanel("Username:", usernameField = new JTextField());
        entryPanel.add(usernamePanel);
        entryPanel.add(Box.createVerticalStrut(12));
        
        // Password field with show/hide toggle
        JPanel passwordPanel = createPasswordFieldPanel();
        entryPanel.add(passwordPanel);
        entryPanel.add(Box.createVerticalStrut(8));
        
        // Password strength indicator
        JPanel strengthPanel = createPasswordStrengthPanel();
        entryPanel.add(strengthPanel);
        entryPanel.add(Box.createVerticalStrut(25));
        
        // Buttons panel
        JPanel buttonPanel = createButtonPanel();
        entryPanel.add(buttonPanel);
        
        parent.add(entryPanel, BorderLayout.WEST);
    }

    private JPanel createFieldPanel(String labelText, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setPreferredSize(new Dimension(80, 26));
        label.setForeground(TEXT_PRIMARY);
        
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
            new javax.swing.border.LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        field.setBackground(new Color(250, 250, 250));
        field.setForeground(TEXT_PRIMARY);
        field.setPreferredSize(new Dimension(0, 28));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        
        // Add focus effect
        // Restore theme focus style
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    new javax.swing.border.LineBorder(SECONDARY_COLOR, 2, true),
                    new EmptyBorder(5, 9, 5, 9)
                ));
                field.setBackground(Color.WHITE);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    new javax.swing.border.LineBorder(BORDER_COLOR, 1, true),
                    new EmptyBorder(6, 10, 6, 10)
                ));
                field.setBackground(new Color(250, 250, 250));
            }
        });
        
        panel.add(label, BorderLayout.WEST);
        panel.add(field, BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createPasswordFieldPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        
        JLabel label = new JLabel("Password:");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setPreferredSize(new Dimension(80, 26));
        label.setForeground(TEXT_PRIMARY);
        
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            new javax.swing.border.LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        passwordField.setBackground(new Color(250, 250, 250));
        passwordField.setForeground(TEXT_PRIMARY);
        passwordField.setPreferredSize(new Dimension(0, 28));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        passwordField.setEchoChar('\u2022');
        
        // Restore theme focus style
        passwordField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                passwordField.setBorder(BorderFactory.createCompoundBorder(
                    new javax.swing.border.LineBorder(SECONDARY_COLOR, 2, true),
                    new EmptyBorder(5, 9, 5, 9)
                ));
                passwordField.setBackground(Color.WHITE);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                passwordField.setBorder(BorderFactory.createCompoundBorder(
                    new javax.swing.border.LineBorder(BORDER_COLOR, 1, true),
                    new EmptyBorder(6, 10, 6, 10)
                ));
                passwordField.setBackground(new Color(250, 250, 250));
            }
        });
        
        // Show/Hide toggle inside the field
        JButton showHideBtn = createIconButton("Show", new Color(100, 110, 120));
        showHideBtn.setPreferredSize(new Dimension(56, 28));
        showHideBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        showHideBtn.setToolTipText("Show/Hide Password");
        showHideBtn.addActionListener(e -> {
            boolean currentlyHidden = passwordField.getEchoChar() != 0;
            togglePasswordVisibility();
            ((JButton)e.getSource()).setText(currentlyHidden ? "Hide" : "Show");
        });
        
        JPanel fieldPanel = new JPanel(new BorderLayout());
        fieldPanel.add(passwordField, BorderLayout.CENTER);
        fieldPanel.add(showHideBtn, BorderLayout.EAST);
        
        panel.add(label, BorderLayout.WEST);
        panel.add(fieldPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createPasswordStrengthPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.setOpaque(false);
        
        JLabel strengthLabel = new JLabel("Password Strength:");
        strengthLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        strengthLabel.setForeground(TEXT_SECONDARY);
        
        JPanel strengthBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        strengthBar.setOpaque(false);
        
        // Wireframe dashed line indicator
        // Create strength indicators (4 bars)
        for (int i = 0; i < 4; i++) {
            JLabel bar = new JLabel("█");
            bar.setFont(new Font("Arial", Font.BOLD, 14));
            bar.setForeground(new Color(220, 220, 220));
            bar.setName("strengthBar" + i);
            strengthBars[i] = bar;
            strengthBar.add(bar);
        }
        
        JLabel strengthText = new JLabel("Enter password");
        strengthText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        strengthText.setForeground(TEXT_SECONDARY);
        strengthText.setName("strengthText");
        strengthTextLabel = strengthText;
        
        panel.add(strengthLabel);
        panel.add(strengthBar);
        panel.add(strengthText);
        
        // Listen for changes to update real-time strength
        passwordField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updatePasswordStrength(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updatePasswordStrength(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePasswordStrength(); }
        });
        
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(5, 0, 0, 0));
        
        JButton saveButton = createStyledButton("Save", ACCENT_COLOR);
        JButton editButton = createStyledButton("Edit", SECONDARY_COLOR);
        JButton deleteButton = createStyledButton("Delete", ERROR_COLOR);
        JButton clearButton = createStyledButton("Clear", new Color(120, 134, 150));
        
        // Add tooltips
        saveButton.setToolTipText("Save password (Ctrl+S)");
        editButton.setToolTipText("Edit selected password (Ctrl+E)");
        deleteButton.setToolTipText("Delete selected password (Delete)");
        clearButton.setToolTipText("Clear all fields (Ctrl+L)");
        
        saveButton.addActionListener(this::savePassword);
        editButton.addActionListener(this::editPassword);
        deleteButton.addActionListener(this::deletePassword);
        clearButton.addActionListener(e -> clearFields());
        
        panel.add(saveButton);
        panel.add(editButton);
        panel.add(deleteButton);
        panel.add(clearButton);
        
        return panel;
    }

    // simple button variant removed; using themed createStyledButton

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(0, 40));
        button.setBorder(new EmptyBorder(10, 15, 10, 15));
        
        // Add hover effect with smooth transition
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(brighter(color, 0.8f));
                button.setBorder(new EmptyBorder(9, 14, 9, 14));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
                button.setBorder(new EmptyBorder(10, 15, 10, 15));
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                button.setBackground(darker(color, 0.7f));
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                button.setBackground(brighter(color, 0.8f));
            }
        });
        
        return button;
    }
    
    private JButton createIconButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setForeground(TEXT_SECONDARY);
        button.setBackground(new Color(245, 245, 245));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            new EmptyBorder(5, 8, 5, 8)
        ));
        
        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(235, 235, 235));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(245, 245, 245));
            }
        });
        
        return button;
    }

    private void createPasswordListPanel(JPanel parent) {
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        listPanel.setBackground(CARD_BACKGROUND);
        
        // Panel title
        JLabel titleLabel = new JLabel("Stored Passwords");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(PRIMARY_COLOR);
        titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        listPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Search panel - Improved visibility
        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchLabel.setForeground(TEXT_PRIMARY); // Better contrast
        
        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            new EmptyBorder(10, 12, 10, 12)
        ));
        searchField.setBackground(new Color(250, 250, 250));
        searchField.setForeground(TEXT_PRIMARY); // Better contrast
        searchField.setToolTipText("Search by website or username");
        searchField.addActionListener(e -> filterPasswords());
        
        // Add search button for better UX
        JButton searchButton = createIconButton("Search", TEXT_SECONDARY);
        searchButton.setPreferredSize(new Dimension(70, 36));
        searchButton.addActionListener(e -> filterPasswords());
        
        JPanel searchFieldPanel = new JPanel(new BorderLayout());
        searchFieldPanel.add(searchField, BorderLayout.CENTER);
        searchFieldPanel.add(searchButton, BorderLayout.EAST);
        
        JButton refreshButton = createStyledButton("Refresh", SECONDARY_COLOR);
        refreshButton.setPreferredSize(new Dimension(120, 40));
        refreshButton.setToolTipText("Refresh password list");
        refreshButton.addActionListener(e -> loadPasswords());
        
        searchPanel.add(searchLabel, BorderLayout.WEST);
        searchPanel.add(searchFieldPanel, BorderLayout.CENTER);
        searchPanel.add(refreshButton, BorderLayout.EAST);
        
        // Table setup
        String[] columnNames = {"Website", "Username", "Password", "Actions"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3; // Only actions column is editable
            }
        };
        
        passwordTable = new JTable(tableModel);
        passwordTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        passwordTable.setRowHeight(40); // Increased row height for better visibility
        passwordTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        passwordTable.setShowGrid(false);
        passwordTable.setIntercellSpacing(new Dimension(0, 0));
        passwordTable.setSelectionBackground(TABLE_SELECTION_BG);
        passwordTable.setSelectionForeground(TABLE_SELECTION_FG);
        passwordTable.setForeground(TEXT_PRIMARY); // Better contrast
        passwordTable.setBackground(Color.WHITE);
        
        // Table header styling - Improved visibility
        passwordTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        passwordTable.getTableHeader().setBackground(TABLE_HEADER_BG);
        passwordTable.getTableHeader().setForeground(Color.WHITE);
        passwordTable.getTableHeader().setPreferredSize(new Dimension(0, 45));
        passwordTable.getTableHeader().setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY_COLOR),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        // Force a solid, opaque header with consistent background color
        passwordTable.getTableHeader().setOpaque(true);
        passwordTable.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        passwordTable.getTableHeader().setReorderingAllowed(false);
        
        // Hide password column by default
        passwordTable.getColumnModel().getColumn(2).setMinWidth(0);
        passwordTable.getColumnModel().getColumn(2).setMaxWidth(0);
        passwordTable.getColumnModel().getColumn(2).setWidth(0);
        
        // Add action buttons to each row - Improved visibility
        passwordTable.getColumn("Actions").setCellRenderer(new ButtonRenderer());
        passwordTable.getColumn("Actions").setCellEditor(new ButtonEditor(new JCheckBox()));
        
        // Ensure sensible column widths for visibility
        passwordTable.getColumnModel().getColumn(0).setPreferredWidth(260); // Website
        passwordTable.getColumnModel().getColumn(1).setPreferredWidth(220); // Username
        javax.swing.table.TableColumn actionsCol = passwordTable.getColumnModel().getColumn(3);
        actionsCol.setMinWidth(110);
        actionsCol.setPreferredWidth(130);
        actionsCol.setMaxWidth(170);
        
        JScrollPane scrollPane = new JScrollPane(passwordTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getViewport().setBackground(Color.WHITE);
        
        // Create a container for the search panel and table
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(searchPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.setOpaque(false);
        
        listPanel.add(contentPanel, BorderLayout.CENTER);
        
        parent.add(listPanel, BorderLayout.CENTER);
    }

    private void createStatusBar(JPanel parent) {
        JPanel statusPanel = new GradientPanel(new Color(52, 73, 94), new Color(44, 62, 80));
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
            new EmptyBorder(12, 20, 12, 20)
        ));
        
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(Color.WHITE);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);
        
        JLabel versionLabel = new JLabel("v2.0");
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        versionLabel.setForeground(new Color(200, 200, 200));
        
        JLabel timeLabel = new JLabel();
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        timeLabel.setForeground(new Color(200, 200, 200));
        timeLabel.setName("timeLabel");
        
        rightPanel.add(versionLabel);
        rightPanel.add(new JLabel("•"));
        rightPanel.add(timeLabel);
        
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(rightPanel, BorderLayout.EAST);
        
        parent.add(statusPanel, BorderLayout.SOUTH);
        
        // Start time updater
        startTimeUpdater();
    }

    private Image createAppIcon() {
        // Create a simple icon with the new color scheme
        BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw background
        GradientPaint gradient = new GradientPaint(0, 0, PRIMARY_COLOR, 32, 32, SECONDARY_COLOR);
        g2d.setPaint(gradient);
        g2d.fillRoundRect(0, 0, 32, 32, 8, 8);
        
        // Draw lock icon
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(8, 10, 16, 12, 4, 4);
        g2d.fillRect(12, 6, 8, 6);
        
        g2d.dispose();
        return icon;
    }

    // Helper methods for color manipulation
    private Color brighter(Color color, float factor) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        
        return new Color(
            Math.min((int)(r / factor), 255),
            Math.min((int)(g / factor), 255),
            Math.min((int)(b / factor), 255)
        );
    }
    
    private Color darker(Color color, float factor) {
        return new Color(
            Math.max((int)(color.getRed() * factor), 0),
            Math.max((int)(color.getGreen() * factor), 0),
            Math.max((int)(color.getBlue() * factor), 0)
        );
    }

    private void setupComponents() {
        // Add window listener for cleanup and save confirmation
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (hasUnsavedChanges()) {
                    int result = JOptionPane.showConfirmDialog(
                        PasswordManagerUI.this,
                        "You have unsaved changes. Do you want to save before closing?",
                        "Save Changes",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                    );
                    
                    if (result == JOptionPane.YES_OPTION) {
                        // Save changes
                        ActionEvent saveEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "save");
                        savePassword(saveEvent);
                        System.exit(0);
                    } else if (result == JOptionPane.NO_OPTION) {
                        // Don't save, just exit
                        System.exit(0);
                    }
                    // If CANCEL, do nothing (don't close)
                } else {
                    System.exit(0);
                }
            }
        });
        
        // Start pendrive monitoring
        startPendriveMonitoring();
    }

    private void detectPendrive() {
        List<String> authorized = PendriveDetector.getAuthorizedPendrivePaths();
        if (!authorized.isEmpty()) {
            pendriveRoot = new File(authorized.get(0));
            statusLabel.setText("✅ Authorized pendrive detected: " + pendriveRoot.getAbsolutePath());
        } else {
            // If there are pendrives but none authorized, guide the user
            List<String> any = PendriveDetector.getAllPendrivePaths();
            if (!any.isEmpty()) {
                statusLabel.setText("⚠️ Unauthorized USB detected.");

                int choice = JOptionPane.showConfirmDialog(this,
                    "A USB drive was detected but it is not authorized.\n\n" +
                    "If this is your first time, do you want to set up this USB now?\n\n" +
                    "This will create a small hidden key file on the USB.",
                    "First-time Setup",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

                if (choice == JOptionPane.YES_OPTION) {
                    // Attempt to create token on the first detected removable drive
                    File candidate = new File(any.get(0));
                    boolean created = PendriveDetector.createAuthorizationToken(candidate);
                    if (created) {
                        pendriveRoot = candidate;
                        statusLabel.setText("✅ USB set up and authorized: " + pendriveRoot.getAbsolutePath());
                    } else {
                        statusLabel.setText("❌ Failed to set up USB. Please try again.");
                        JOptionPane.showMessageDialog(this,
                            "Could not complete first-time setup.\n\n" +
                            "Ensure the USB is writable and not write-protected.",
                            "Setup Failed",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                statusLabel.setText("❌ No pendrive detected! Please insert your USB.");
                JOptionPane.showMessageDialog(this, 
                    "No pendrive detected!\n\nPlease ensure:\n" +
                    "1. Your USB drive is connected to your computer\n" +
                    "2. The USB is properly recognized by Windows\n" +
                    "3. The USB has a drive letter assigned", 
                    "Pendrive Not Detected", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Password management methods
    private void savePassword(ActionEvent e) {
        if (pendriveRoot == null) {
            statusLabel.setText("❌ No pendrive detected!");
            return;
        }
        
        String website = websiteField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (website.isEmpty() || username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("⚠️ All fields are required!");
            showErrorDialog("All fields are required!", "Validation Error");
            return;
        }
        
        try {
            PasswordEntry entry = new PasswordEntry(website, username, password);
            
            // Check if entry already exists
            if (passwordExists(website, username)) {
                int result = JOptionPane.showConfirmDialog(this, 
                    "A password for this website and username already exists.\nDo you want to update it?",
                    "Password Exists", 
                    JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    updatePassword(entry);
                } else {
                    return;
                }
            } else {
                addPassword(entry);
            }
            
            clearFields();
            loadPasswords();
            statusLabel.setText("✅ Password saved successfully!");
            
        } catch (Exception ex) {
            statusLabel.setText("❌ Error saving password!");
            showErrorDialog("Error saving password: " + ex.getMessage(), "Save Error");
        }
    }

    private void editPassword(ActionEvent e) {
        int selectedRow = passwordTable.getSelectedRow();
        if (selectedRow == -1) {
            statusLabel.setText("⚠️ Please select a password to edit!");
            showErrorDialog("Please select a password from the table to edit.", "No Selection");
            return;
        }
        
        PasswordEntry entry = passwordEntries.get(selectedRow);
        websiteField.setText(entry.getWebsite());
        usernameField.setText(entry.getUsername());
        passwordField.setText(entry.getPassword());
        
        statusLabel.setText("✏️ Password loaded for editing. Make changes and click Save.");
    }

    private void deletePassword(ActionEvent e) {
        int selectedRow = passwordTable.getSelectedRow();
        if (selectedRow == -1) {
            statusLabel.setText("⚠️ Please select a password to delete!");
            showErrorDialog("Please select a password from the table to delete.", "No Selection");
            return;
        }
        
        PasswordEntry entry = passwordEntries.get(selectedRow);
        int result = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete the password for:\n" +
            "Website: " + entry.getWebsite() + "\n" +
            "Username: " + entry.getUsername(),
            "Confirm Delete", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
            
        if (result == JOptionPane.YES_OPTION) {
            try {
                removePassword(entry);
                loadPasswords();
                clearFields();
                statusLabel.setText("✅ Password deleted successfully!");
            } catch (Exception ex) {
                statusLabel.setText("❌ Error deleting password!");
                showErrorDialog("Error deleting password: " + ex.getMessage(), "Delete Error");
            }
        }
    }

    private void clearFields() {
        websiteField.setText("");
        usernameField.setText("");
        passwordField.setText("");
        passwordTable.clearSelection();
    }

    private void togglePasswordVisibility() {
        if (passwordField.getEchoChar() == 0) {
            passwordField.setEchoChar('\u2022');
        } else {
            passwordField.setEchoChar((char)0);
        }
    }

    private void filterPasswords() {
        String searchText = searchField.getText().toLowerCase().trim();
        if (searchText.isEmpty()) {
            loadPasswords();
            return;
        }
        
        tableModel.setRowCount(0);
        for (PasswordEntry entry : passwordEntries) {
            if (entry.getWebsite().toLowerCase().contains(searchText) ||
                entry.getUsername().toLowerCase().contains(searchText)) {
                addRowToTable(entry);
            }
        }
        statusLabel.setText("🔍 Filtered " + tableModel.getRowCount() + " passwords");
    }

    private void loadPasswords() {
        if (pendriveRoot == null) {
            statusLabel.setText("❌ No pendrive detected!");
            return;
        }
        
        passwordEntries.clear();
        tableModel.setRowCount(0);
        
        try {
            File file = new File(pendriveRoot, "passwords.txt");
            if (!file.exists()) {
                statusLabel.setText("📝 No passwords stored yet");
                return;
            }
            
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    try {
                String decrypted = new String(Base64.getDecoder().decode(line), StandardCharsets.UTF_8);
                        String[] parts = decrypted.split(",", 3);
                        if (parts.length == 3) {
                            PasswordEntry entry = new PasswordEntry(parts[0], parts[1], parts[2]);
                            passwordEntries.add(entry);
                            addRowToTable(entry);
                        }
                    } catch (Exception e) {
                        // Skip invalid entries
                        continue;
                    }
                }
            }
            
            statusLabel.setText("✅ Loaded " + passwordEntries.size() + " passwords");
            
        } catch (Exception ex) {
            statusLabel.setText("❌ Error loading passwords!");
            showErrorDialog("Error loading passwords: " + ex.getMessage(), "Load Error");
        }
    }

    private void addRowToTable(PasswordEntry entry) {
        Object[] row = {
            entry.getWebsite(),
            entry.getUsername(),
            "••••••••",
            "Actions"
        };
        tableModel.addRow(row);
    }

    private void addPassword(PasswordEntry entry) throws Exception {
        String encrypted = Base64.getEncoder().encodeToString(
            (entry.getWebsite() + "," + entry.getUsername() + "," + entry.getPassword())
            .getBytes(StandardCharsets.UTF_8)
        );
        
        File file = new File(pendriveRoot, "passwords.txt");
        try (FileWriter fw = new FileWriter(file, true)) {
            fw.write(encrypted + "\n");
        }
    }

    private void updatePassword(PasswordEntry newEntry) throws Exception {
        // Remove old entry and add new one
        removePasswordFromFile(newEntry.getWebsite(), newEntry.getUsername());
        addPassword(newEntry);
    }

    private void removePassword(PasswordEntry entry) throws Exception {
        removePasswordFromFile(entry.getWebsite(), entry.getUsername());
    }

    private void removePasswordFromFile(String website, String username) throws Exception {
        File file = new File(pendriveRoot, "passwords.txt");
        if (!file.exists()) return;
        
        List<String> lines = Files.readAllLines(file.toPath());
        List<String> newLines = new ArrayList<>();
        
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                try {
                    String decrypted = new String(Base64.getDecoder().decode(line), StandardCharsets.UTF_8);
                    String[] parts = decrypted.split(",", 3);
                    if (parts.length == 3 && !(parts[0].equals(website) && parts[1].equals(username))) {
                        newLines.add(line);
                    }
                } catch (Exception e) {
                    newLines.add(line); // Keep invalid lines as they are
                }
            }
        }
        
        try (FileWriter fw = new FileWriter(file)) {
            for (String line : newLines) {
                fw.write(line + "\n");
            }
        }
    }

    private boolean passwordExists(String website, String username) {
        return passwordEntries.stream()
            .anyMatch(entry -> entry.getWebsite().equals(website) && entry.getUsername().equals(username));
    }

    private void showErrorDialog(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    // New methods for enhanced UI
    private void setupKeyboardShortcuts() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke("ctrl S"), "save");
        getRootPane().getActionMap().put("save", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                savePassword(e);
            }
        });
        
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke("ctrl E"), "edit");
        getRootPane().getActionMap().put("edit", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                editPassword(e);
            }
        });
        
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke("DELETE"), "delete");
        getRootPane().getActionMap().put("delete", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                deletePassword(e);
            }
        });
        
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke("ctrl L"), "clear");
        getRootPane().getActionMap().put("clear", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                clearFields();
            }
        });
    }

    private void updatePasswordStrength() {
        String password = new String(passwordField.getPassword());
        int strength = calculatePasswordStrength(password);
        
        // Update strength bars directly
        for (int i = 0; i < strengthBars.length; i++) {
            if (strengthBars[i] != null) {
                strengthBars[i].setForeground(i < strength ? getStrengthColor(strength) : new Color(220, 220, 220));
            }
        }
        
        // Update strength text directly
        if (strengthTextLabel != null) {
            strengthTextLabel.setText(getStrengthText(strength));
            strengthTextLabel.setForeground(getStrengthColor(strength));
        }
    }

    private int calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) return 0;

        // Quick reject of very common passwords
        String[] common = {
            "password","123456","123456789","qwerty","111111","12345678","abc123",
            "password1","12345","iloveyou","admin","welcome","monkey","dragon"
        };
        String lower = password.toLowerCase();
        for (String s : common) {
            if (lower.equals(s)) return 1; // Very weak
        }

        // Character pool size
        int pool = 0;
        if (password.matches(".*[a-z].*")) pool += 26;
        if (password.matches(".*[A-Z].*")) pool += 26;
        if (password.matches(".*[0-9].*")) pool += 10;
        if (password.matches(".*[^a-zA-Z0-9].*")) pool += 33; // approx printable symbols
        if (pool == 0) return 0;

        double bits = calculateEntropyBits(password, pool);

        // Penalties for repeats and sequences
        if (hasManyRepeats(password)) bits -= 10;
        if (hasSequentialRun(password)) bits -= 10;
        if (lower.contains("password") || lower.contains("qwerty")) bits -= 10;

        // Map entropy to 0..4 scale
        if (bits < 28) return 1;        // Very weak
        if (bits < 36) return 2;        // Weak
        if (bits < 60) return 3;        // Good
        return 4;                       // Strong
    }

    private double calculateEntropyBits(String password, int pool) {
        // Shannon-style approximation: log2(pool^len) = len * log2(pool)
        double bits = password.length() * (Math.log(pool) / Math.log(2));
        return Math.max(bits, 0);
    }

    private boolean hasManyRepeats(String password) {
        // Penalize if more than half of the characters are repeats of the same char class
        int repeats = 0;
        for (int i = 1; i < password.length(); i++) {
            if (password.charAt(i) == password.charAt(i - 1)) repeats++;
        }
        return repeats >= password.length() / 2;
    }

    private boolean hasSequentialRun(String password) {
        // Detect ascending or descending runs of length >= 4 (e.g., 1234, abcd)
        int up = 1, down = 1;
        for (int i = 1; i < password.length(); i++) {
            int diff = password.charAt(i) - password.charAt(i - 1);
            if (diff == 1) { up++; down = 1; } 
            else if (diff == -1) { down++; up = 1; } 
            else { up = 1; down = 1; }
            if (up >= 4 || down >= 4) return true;
        }
        return false;
    }

    private Color getStrengthColor(int strength) {
        switch (strength) {
            case 0: return new Color(220, 220, 220);
            case 1: return new Color(231, 76, 60);
            case 2: return new Color(230, 126, 34);
            case 3: return new Color(241, 196, 15);
            case 4: return new Color(46, 204, 113);
            default: return new Color(220, 220, 220);
        }
    }

    private String getStrengthText(int strength) {
        switch (strength) {
            case 0: return "Enter password";
            case 1: return "Very Weak";
            case 2: return "Weak";
            case 3: return "Good";
            case 4: return "Strong";
            default: return "Enter password";
        }
    }

    private void startTimeUpdater() {
        Timer timer = new Timer(1000, e -> {
            java.time.LocalTime now = java.time.LocalTime.now();
            String timeString = now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            
            // Find and update time label
            Component[] components = getContentPane().getComponents();
            for (Component comp : components) {
                if (comp instanceof JPanel) {
                    updateTimeLabel((JPanel) comp);
                }
            }
        });
        timer.start();
    }

    private void updateTimeLabel(JPanel panel) {
        Component[] components = panel.getComponents();
        for (Component comp : components) {
            if (comp instanceof JLabel && comp.getName() != null && 
                comp.getName().equals("timeLabel")) {
                JLabel timeLabel = (JLabel) comp;
                java.time.LocalTime now = java.time.LocalTime.now();
                String timeString = now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                timeLabel.setText(timeString);
            } else if (comp instanceof JPanel) {
                updateTimeLabel((JPanel) comp);
            }
        }
    }

    private boolean hasUnsavedChanges() {
        return !websiteField.getText().trim().isEmpty() || 
               !usernameField.getText().trim().isEmpty() || 
               passwordField.getPassword().length > 0;
    }

    private void startPendriveMonitoring() {
        Timer pendriveMonitor = new Timer(2000, e -> {
            boolean pendriveStillAuthorized = false;
            if (pendriveRoot != null) {
                // Check that drive still exists and still has the token
                if (pendriveRoot.exists() && PendriveDetector.isAuthorizedDrive(pendriveRoot)) {
                    // also confirm it remains listed as a removable drive
                    List<String> current = PendriveDetector.getAllPendrivePaths();
                    for (String path : current) {
                        if (path.equals(pendriveRoot.getAbsolutePath())) {
                            pendriveStillAuthorized = true;
                            break;
                        }
                    }
                }
            }

            if (!pendriveStillAuthorized && pendriveRoot != null) {
                JOptionPane.showMessageDialog(this,
                    "Authorized USB was removed or authorization token missing!\n\n" +
                    "The application will now close to protect your data.",
                    "USB Removed",
                    JOptionPane.WARNING_MESSAGE);
                System.exit(0);
            }
        });
        pendriveMonitor.start();
    }

    // Inner classes for table functionality with improved visibility
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setFocusPainted(false);
            setBorderPainted(false);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText("View");
            setBackground(SECONDARY_COLOR);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            if (isSelected) {
                setBackground(darker(SECONDARY_COLOR, 0.9f));
            }
            return this;
        }
    }

    private class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;
        private int row;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                }
            });
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText("View");
            button.setBackground(ACCENT_COLOR);
            button.setForeground(Color.WHITE);
            button.setFont(new Font("Segoe UI", Font.BOLD, 13));
            button.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            isPushed = true;
            this.row = row;
            return button;
        }

        public Object getCellEditorValue() {
            if (isPushed) {
                // Show password dialog
                if (row < passwordEntries.size()) {
                    PasswordEntry entry = passwordEntries.get(row);
                    showPasswordDialog(entry);
                }
            }
            isPushed = false;
            return label;
        }

        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }

        protected void fireEditingStopped() {
            super.fireEditingStopped();
        }
    }

    // High-contrast table header renderer
    private class HeaderRenderer extends javax.swing.table.DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setOpaque(true);
            setBackground(TABLE_HEADER_BG);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setHorizontalAlignment(LEFT);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY_COLOR),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            return this;
        }
    }

    private void showPasswordDialog(PasswordEntry entry) {
        JDialog dialog = new JDialog(this, "Password Details", true);
        dialog.setSize(450, 250);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(BACKGROUND_COLOR);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(CARD_BACKGROUND);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        JLabel websiteLabelTitle = new JLabel("Website:");
        websiteLabelTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        websiteLabelTitle.setForeground(TEXT_PRIMARY);
        panel.add(websiteLabelTitle, gbc);
        gbc.gridx = 1;
        JLabel websiteLabel = new JLabel(entry.getWebsite());
        websiteLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        websiteLabel.setForeground(TEXT_PRIMARY);
        panel.add(websiteLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel usernameLabelTitle = new JLabel("Username:");
        usernameLabelTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        usernameLabelTitle.setForeground(TEXT_PRIMARY);
        panel.add(usernameLabelTitle, gbc);
        gbc.gridx = 1;
        JLabel usernameLabel = new JLabel(entry.getUsername());
        usernameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        usernameLabel.setForeground(TEXT_PRIMARY);
        panel.add(usernameLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel passwordLabelTitle = new JLabel("Password:");
        passwordLabelTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        passwordLabelTitle.setForeground(TEXT_PRIMARY);
        panel.add(passwordLabelTitle, gbc);
        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField(entry.getPassword());
        passwordField.setEditable(false);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        passwordField.setForeground(TEXT_PRIMARY);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            new EmptyBorder(5, 8, 5, 8)
        ));
        panel.add(passwordField, gbc);
        
        JButton copyButton = createStyledButton("Copy Password", SECONDARY_COLOR);
        copyButton.addActionListener(e -> {
            StringSelection stringSelection = new StringSelection(entry.getPassword());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
            statusLabel.setText("✅ Password copied to clipboard!");
            dialog.dispose();
        });
        
        JButton closeButton = createStyledButton("Close", new Color(120, 134, 150));
        closeButton.addActionListener(e -> dialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(CARD_BACKGROUND);
        buttonPanel.add(copyButton);
        buttonPanel.add(closeButton);
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}

// Password entry class
class PasswordEntry {
    private String website;
    private String username;
    private String password;

    public PasswordEntry(String website, String username, String password) {
        this.website = website;
        this.username = username;
        this.password = password;
    }

    public String getWebsite() { return website; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }

    public void setWebsite(String website) { this.website = website; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
}

// Updated Gradient Panel for modern header and status bar
class GradientPanel extends JPanel {
    private Color startColor;
    private Color endColor;
    
    public GradientPanel(Color startColor, Color endColor) {
        this.startColor = startColor;
        this.endColor = endColor;
    }
    
    public GradientPanel(Color solidColor) {
        this(solidColor, solidColor);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (startColor.equals(endColor)) {
            // Solid color
            g.setColor(startColor);
            g.fillRect(0, 0, getWidth(), getHeight());
        } else {
            // Gradient
            Graphics2D g2d = (Graphics2D) g.create();
            int w = getWidth();
            int h = getHeight();
            
            GradientPaint gradient = new GradientPaint(0, 0, startColor, 0, h, endColor);
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, w, h);
            g2d.dispose();
        }
    }
}
