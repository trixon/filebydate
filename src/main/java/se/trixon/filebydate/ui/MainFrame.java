/*
 * Copyright 2017 Patrik Karlsson.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.trixon.filebydate.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import se.trixon.almond.util.AlmondAction;
import se.trixon.almond.util.AlmondOptions;
import se.trixon.almond.util.AlmondOptions.AlmondOptionsEvent;
import se.trixon.almond.util.AlmondUI;
import se.trixon.almond.util.BundleHelper;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.PomInfo;
import se.trixon.almond.util.SystemHelper;
import se.trixon.almond.util.icons.material.MaterialIcon;
import se.trixon.almond.util.swing.SwingHelper;
import se.trixon.almond.util.swing.dialogs.FileChooserPanel;
import se.trixon.almond.util.swing.dialogs.Message;
import se.trixon.filebydate.DateSource;
import se.trixon.filebydate.FileByDate;
import se.trixon.filebydate.NameCase;
import se.trixon.filebydate.Operation;
import se.trixon.filebydate.OperationListener;
import se.trixon.filebydate.Profile;
import se.trixon.filebydate.ProfileManager;

/**
 *
 * @author Patrik Karlsson
 */
public class MainFrame extends JFrame implements AlmondOptions.AlmondOptionsWatcher, OperationListener {

    private final ResourceBundle mBundle = BundleHelper.getBundle(FileByDate.class, "Bundle");
    private final ResourceBundle mBundleUI = BundleHelper.getBundle(MainFrame.class, "Bundle");
    private ActionManager mActionManager;
    private final AlmondUI mAlmondUI = AlmondUI.getInstance();
    private final LinkedList<AlmondAction> mBaseActions = new LinkedList<>();
    private final LinkedList<AlmondAction> mAllActions = new LinkedList<>();
    private final AlmondOptions mAlmondOptions = AlmondOptions.getInstance();
    private final ProfileManager mProfileManager = ProfileManager.getInstance();
    private final LinkedList<Profile> mProfiles = mProfileManager.getProfiles();
    private DefaultComboBoxModel mModel;

    /**
     * Creates new form MainFrame
     */
    public MainFrame() {
        initComponents();
        init();
    }

    @Override
    public void onAlmondOptions(AlmondOptionsEvent almondOptionsEvent) {
        switch (almondOptionsEvent) {
            case ICON_THEME:
                mAllActions.stream().forEach((almondAction) -> {
                    almondAction.updateIcon();
                });
                break;

            case LOOK_AND_FEEL:
                SwingUtilities.updateComponentTreeUI(this);
                SwingUtilities.updateComponentTreeUI(mPopupMenu);
                break;

            case MENU_ICONS:
                ActionMap actionMap = getRootPane().getActionMap();
                for (Object key : actionMap.allKeys()) {
                    Action action = actionMap.get(key);
                    Icon icon = null;
                    if (mAlmondOptions.isDisplayMenuIcons()) {
                        icon = (Icon) action.getValue(AlmondAction.ALMOND_SMALL_ICON_KEY);
                    }
                    action.putValue(Action.SMALL_ICON, icon);
                }
                break;

            default:
                throw new AssertionError();
        }
    }

    @Override
    public void onOperationFailed(String message) {
    }

    @Override
    public void onOperationFinished(String message) {
        logPanel.println(Dict.DONE.toString());
    }

    @Override
    public void onOperationInterrupted() {
        logPanel.println(Dict.OPERATION_INTERRUPTED.toString());
    }

    @Override
    public void onOperationLog(String message) {
        logPanel.println(message);
    }

    @Override
    public void onOperationProcessingStarted() {
    }

    @Override
    public void onOperationStarted() {
    }

    private void init() {
        String fileName = String.format("/%s/calendar-icon-1024px.png", getClass().getPackage().getName().replace(".", "/"));
        ImageIcon imageIcon = new ImageIcon(getClass().getResource(fileName));
        setIconImage(imageIcon.getImage());

        mModel = (DefaultComboBoxModel) profileComboBox.getModel();

        opComboBox.setModel(new DefaultComboBoxModel(mBundleUI.getString("operations").split("\\|")));
        dateSourceComboBox.setModel(new DefaultComboBoxModel(DateSource.values()));
        caseBaseComboBox.setModel(new DefaultComboBoxModel(NameCase.values()));
        caseSuffixComboBox.setModel(new DefaultComboBoxModel(NameCase.values()));
        followLinksCheckBox.setEnabled(!SystemUtils.IS_OS_WINDOWS);

        sourceChooserPanel.setDropMode(FileChooserPanel.DropMode.SINGLE);
        sourceChooserPanel.setMode(JFileChooser.DIRECTORIES_ONLY);

        destChooserPanel.setDropMode(FileChooserPanel.DropMode.SINGLE);
        destChooserPanel.setMode(JFileChooser.DIRECTORIES_ONLY);

        previewDateFormat();

        dateFormatTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                previewDateFormat();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                previewDateFormat();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                previewDateFormat();
            }
        });

        mActionManager = new ActionManager();
        mActionManager.initActions();

        mAlmondUI.addOptionsWatcher(this);
        mAlmondUI.addWindowWatcher(this);
        mAlmondUI.initoptions();

        InputMap inputMap = mPopupMenu.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = mPopupMenu.getActionMap();
        Action action = new AbstractAction("HideMenu") {

            @Override
            public void actionPerformed(ActionEvent e) {
                mPopupMenu.setVisible(false);
            }
        };

        String key = "HideMenu";
        actionMap.put(key, action);
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        inputMap.put(keyStroke, key);

        loadProfiles();
        populateProfiles(null);
        initListeners();
    }

    private Profile getSelectedProfile() {
        if (mModel.getSize() == 0) {
            return new Profile();
        } else {
            return (Profile) mModel.getSelectedItem();
        }
    }

    private void initListeners() {
        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                handle(e.getDocument());
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                handle(e.getDocument());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handle(e.getDocument());
            }

            private void handle(Document document) {

                Profile p = getSelectedProfile();
                if (document == patternTextField.getDocument()) {
                    p.setFilePattern(patternTextField.getText());
                } else if (document == dateFormatTextField.getDocument()) {
                    p.setDatePattern(dateFormatTextField.getText());
                } else if (document == sourceChooserPanel.getTextField().getDocument()) {
                    p.setSourceDir(new File(sourceChooserPanel.getPath()));
                } else if (document == destChooserPanel.getTextField().getDocument()) {
                    p.setDestDir(new File(destChooserPanel.getPath()));
                }
            }
        };

        sourceChooserPanel.getTextField().getDocument().addDocumentListener(documentListener);
        destChooserPanel.getTextField().getDocument().addDocumentListener(documentListener);
        patternTextField.getDocument().addDocumentListener(documentListener);
        dateFormatTextField.getDocument().addDocumentListener(documentListener);
    }

    private void populateProfiles(Profile profile) {
        mModel.removeAllElements();
        Collections.sort(mProfiles);

        mProfiles.stream().forEach((item) -> {
            mModel.addElement(item);
        });

        if (profile != null) {
            mModel.setSelectedItem(profile);
        }

        boolean hasProfiles = !mProfiles.isEmpty();
        SwingHelper.enableComponents(mainPanel, hasProfiles);
    }

    private void loadProfiles() {
        SwingHelper.enableComponents(mainPanel, false);

        try {
            mProfileManager.load();
        } catch (IOException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void previewDateFormat() {
        String datePreview;

        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormatTextField.getText());
            datePreview = simpleDateFormat.format(new Date(System.currentTimeMillis()));
        } catch (IllegalArgumentException ex) {
            datePreview = Dict.Dialog.ERROR.toString();
        }

        String dateLabel = String.format("%s (%s)", Dict.DATE_PATTERN.getString(), datePreview);
        dateFormatLabel.setText(dateLabel);
    }

    private String requestProfileName(String title, String value) {
        return (String) JOptionPane.showInputDialog(
                this,
                null,
                title,
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                value);
    }

    private void profileAdd(String defaultName) {
        String s = requestProfileName(Dict.Dialog.TITLE_PROFILE_ADD.toString(), defaultName);
        if (s != null) {
            Profile existingProfile = mProfileManager.getProfile(s);
            if (existingProfile == null) {
                Profile p = new Profile();
                p.setName(s);
                p.setSourceDir(FileUtils.getUserDirectory());
                p.setDestDir(FileUtils.getUserDirectory());
                p.setFilePattern("{*.jpg,*.JPG}");
                p.setDatePattern("yyyy/MM/yyyy-MM-dd");
                p.setOperation(0);
                p.setFollowLinks(true);
                p.setRecursive(true);
                p.setReplaceExisting(false);
                p.setBaseNameCase(NameCase.UNCHANGED);
                p.setExtNameCase(NameCase.UNCHANGED);

                mProfiles.add(p);
                populateProfiles(p);
            } else {
                Message.error(this, Dict.Dialog.ERROR.toString(), String.format(Dict.Dialog.ERROR_PROFILE_EXIST.toString(), s));
                profileAdd(s);
            }
        }
    }

    private void profileClone() throws CloneNotSupportedException {
        Profile original = getSelectedProfile();
        Profile p = original.clone();
        mProfiles.add(p);
        populateProfiles(p);
        String s = requestProfileName(Dict.Dialog.TITLE_PROFILE_CLONE.toString(), p.getName());
        if (s != null) {
            p.setName(s);
            populateProfiles(getSelectedProfile());
        } else {
            mProfiles.remove(p);
            populateProfiles(original);
        }
    }

    private void profileRename(String defaultName) {
        String s = requestProfileName(Dict.Dialog.TITLE_PROFILE_RENAME.toString(), defaultName);
        if (s != null) {
            Profile existingProfile = mProfileManager.getProfile(s);
            if (existingProfile == null) {
                getSelectedProfile().setName(s);
                populateProfiles(getSelectedProfile());
            } else if (existingProfile != getSelectedProfile()) {
                Message.error(this, Dict.Dialog.ERROR.toString(), String.format(Dict.Dialog.ERROR_PROFILE_EXIST.toString(), s));
                profileRename(s);
            }
        }
    }

    private void profileRemove() {
        if (!mProfiles.isEmpty()) {
            String message = String.format(Dict.Dialog.MESSAGE_PROFILE_REMOVE.toString(), getSelectedProfile().getName());
            int retval = JOptionPane.showConfirmDialog(this,
                    message,
                    Dict.Dialog.TITLE_PROFILE_REMOVE.toString(),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (retval == JOptionPane.OK_OPTION) {
                mProfiles.remove(getSelectedProfile());
                populateProfiles(null);
            }
        }
    }

    private void profileRemoveAll() {
        if (!mProfiles.isEmpty()) {
            String message = String.format(Dict.Dialog.MESSAGE_PROFILE_REMOVE_ALL.toString(), getSelectedProfile().getName());
            int retval = JOptionPane.showConfirmDialog(this,
                    message,
                    Dict.Dialog.TITLE_PROFILE_REMOVE_ALL.toString(),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (retval == JOptionPane.OK_OPTION) {
                mProfiles.clear();
                populateProfiles(null);
            }
        }
    }

    private void profileRun() {
        Profile profile = getSelectedProfile().clone();
        Object[] options = {Dict.RUN.toString(), Dict.DRY_RUN.toString(), Dict.CANCEL.toString()};
        String message = String.format(Dict.Dialog.TITLE_PROFILE_RUN.toString(), profile.getName());

        int result = JOptionPane.showOptionDialog(this,
                message,
                Dict.RUN.toString(),
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]);

        if (result > -1 && result < 2) {
            boolean dryRun = result == 1;
            saveProfiles();
            profile.setDryRun(dryRun);

            logPanel.clear();

            if (profile.isValid()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        logPanel.println(profile.toDebugString());
                        Operation operation = new Operation(MainFrame.this, profile);
                        operation.start();
                    }
                }).start();
            } else {
                logPanel.println(profile.toDebugString());
                logPanel.println(profile.getValidationError());
                logPanel.println(Dict.ABORTING.toString());
            }
        }
    }

    private void saveProfiles() {
        try {
            mProfileManager.save();
        } catch (IOException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void showOptions() {
        OptionsPanel optionsPanel = new OptionsPanel();
        SwingHelper.makeWindowResizable(optionsPanel);

        int retval = JOptionPane.showOptionDialog(this,
                optionsPanel,
                Dict.OPTIONS.toString(),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                null);

        if (retval == JOptionPane.OK_OPTION) {
            optionsPanel.save();
        }
    }

    private void quit() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        mPopupMenu = new javax.swing.JPopupMenu();
        renameMenuItem = new javax.swing.JMenuItem();
        cloneMenuItem = new javax.swing.JMenuItem();
        removeAllProfilesMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        optionsMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        aboutMenuItem = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        quitMenuItem = new javax.swing.JMenuItem();
        topPanel = new javax.swing.JPanel();
        profileComboBox = new javax.swing.JComboBox<>();
        toolBar = new javax.swing.JToolBar();
        startButton = new javax.swing.JButton();
        addButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        menuButton = new javax.swing.JButton();
        mainPanel = new javax.swing.JPanel();
        sourceChooserPanel = new se.trixon.almond.util.swing.dialogs.FileChooserPanel();
        destChooserPanel = new se.trixon.almond.util.swing.dialogs.FileChooserPanel();
        options1Panel = new javax.swing.JPanel();
        patternLabel = new javax.swing.JLabel();
        patternTextField = new javax.swing.JTextField();
        dateSourceLabel = new javax.swing.JLabel();
        dateSourceComboBox = new javax.swing.JComboBox<>();
        dateFormatLabel = new javax.swing.JLabel();
        dateFormatTextField = new javax.swing.JTextField();
        options2Panel = new javax.swing.JPanel();
        operationLabel = new javax.swing.JLabel();
        opComboBox = new javax.swing.JComboBox();
        followLinksCheckBox = new javax.swing.JCheckBox();
        recursiveCheckBox = new javax.swing.JCheckBox();
        overwriteCheckBox = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        caseBaseComboBox = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        caseSuffixComboBox = new javax.swing.JComboBox<>();
        jPanel1 = new javax.swing.JPanel();
        logPanel = new se.trixon.almond.util.swing.LogPanel();

        mPopupMenu.add(renameMenuItem);
        mPopupMenu.add(cloneMenuItem);
        mPopupMenu.add(removeAllProfilesMenuItem);
        mPopupMenu.add(jSeparator1);
        mPopupMenu.add(optionsMenuItem);
        mPopupMenu.add(jSeparator2);
        mPopupMenu.add(aboutMenuItem);
        mPopupMenu.add(jSeparator6);
        mPopupMenu.add(quitMenuItem);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("se/trixon/filebydate/ui/Bundle"); // NOI18N
        setTitle(bundle.getString("MainFrame.title")); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        topPanel.setLayout(new java.awt.GridBagLayout());

        profileComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                profileComboBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        topPanel.add(profileComboBox, gridBagConstraints);

        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        startButton.setFocusable(false);
        startButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        startButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolBar.add(startButton);

        addButton.setFocusable(false);
        addButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        addButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolBar.add(addButton);

        removeButton.setFocusable(false);
        removeButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        removeButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolBar.add(removeButton);

        menuButton.setFocusable(false);
        menuButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        menuButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        menuButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                menuButtonMousePressed(evt);
            }
        });
        toolBar.add(menuButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        topPanel.add(toolBar, gridBagConstraints);

        getContentPane().add(topPanel, java.awt.BorderLayout.PAGE_START);

        mainPanel.setLayout(new java.awt.GridBagLayout());

        sourceChooserPanel.setHeader(Dict.SOURCE.getString());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 0, 8);
        mainPanel.add(sourceChooserPanel, gridBagConstraints);

        destChooserPanel.setHeader(Dict.DESTINATION.getString());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 0, 8);
        mainPanel.add(destChooserPanel, gridBagConstraints);

        options1Panel.setLayout(new java.awt.GridBagLayout());

        patternLabel.setText(Dict.FILE_PATTERN.getString());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        options1Panel.add(patternLabel, gridBagConstraints);

        patternTextField.setText("*"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        options1Panel.add(patternTextField, gridBagConstraints);

        dateSourceLabel.setText(Dict.DATE_SOURCE.toString());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.25;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 8);
        options1Panel.add(dateSourceLabel, gridBagConstraints);

        dateSourceComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dateSourceComboBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.25;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 8);
        options1Panel.add(dateSourceComboBox, gridBagConstraints);

        dateFormatLabel.setText(Dict.DATE_PATTERN.getString());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        options1Panel.add(dateFormatLabel, gridBagConstraints);

        dateFormatTextField.setToolTipText("<html>\n <h3>Date and Time Patterns</h3>\n <p>\n Date and time formats are specified by <em>date and time pattern</em>\n strings.\n Within date and time pattern strings, unquoted letters<br />from\n <code>'A'</code> to <code>'Z'</code> and from <code>'a'</code> to\n <code>'z'</code> are interpreted as pattern letters representing the\n components of a date or time string.<br />\n Text can be quoted using single quotes (<code>'</code>) to avoid\n interpretation.\n <code>\"''\"</code> represents a single quote.<br />\n All other characters are not interpreted; they're simply copied into the\n output string during formatting or matched against<br />the input string\n during parsing.\n <p>\nThe following pattern letters are defined (all other characters from\n <code>'A'</code> to <code>'Z'</code> and from <code>'a'</code> to\n <code>'z'</code> are reserved):\n <blockquote>\n <table border=0 cellspacing=3 cellpadding=0 summary=\"Chart shows pattern letters, date/time component, presentation, and examples.\">\n     <tr style=\"background-color: rgb(204, 204, 255);\">\n         <th align=left>Letter\n         <th align=left>Date or Time Component\n         <th align=left>Presentation\n         <th align=left>Examples\n     <tr>\n         <td><code>G</code>\n         <td>Era designator\n         <td><a href=\"#text\">Text</a>\n         <td><code>AD</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>y</code>\n         <td>Year\n         <td><a href=\"#year\">Year</a>\n         <td><code>1996</code>; <code>96</code>\n     <tr>\n         <td><code>Y</code>\n         <td>Week year\n         <td><a href=\"#year\">Year</a>\n         <td><code>2009</code>; <code>09</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>M</code>\n         <td>Month in year (context sensitive)\n         <td><a href=\"#month\">Month</a>\n         <td><code>July</code>; <code>Jul</code>; <code>07</code>\n     <tr>\n         <td><code>L</code>\n         <td>Month in year (standalone form)\n         <td><a href=\"#month\">Month</a>\n         <td><code>July</code>; <code>Jul</code>; <code>07</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>w</code>\n         <td>Week in year\n         <td><a href=\"#number\">Number</a>\n         <td><code>27</code>\n     <tr>\n         <td><code>W</code>\n         <td>Week in month\n         <td><a href=\"#number\">Number</a>\n         <td><code>2</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>D</code>\n         <td>Day in year\n         <td><a href=\"#number\">Number</a>\n         <td><code>189</code>\n     <tr>\n         <td><code>d</code>\n         <td>Day in month\n         <td><a href=\"#number\">Number</a>\n         <td><code>10</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>F</code>\n         <td>Day of week in month\n         <td><a href=\"#number\">Number</a>\n         <td><code>2</code>\n     <tr>\n         <td><code>E</code>\n         <td>Day name in week\n         <td><a href=\"#text\">Text</a>\n         <td><code>Tuesday</code>; <code>Tue</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>u</code>\n         <td>Day number of week (1 = Monday, ..., 7 = Sunday)\n         <td><a href=\"#number\">Number</a>\n         <td><code>1</code>\n     <tr>\n         <td><code>a</code>\n         <td>Am/pm marker\n         <td><a href=\"#text\">Text</a>\n         <td><code>PM</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>H</code>\n         <td>Hour in day (0-23)\n         <td><a href=\"#number\">Number</a>\n         <td><code>0</code>\n     <tr>\n         <td><code>k</code>\n         <td>Hour in day (1-24)\n         <td><a href=\"#number\">Number</a>\n         <td><code>24</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>K</code>\n         <td>Hour in am/pm (0-11)\n         <td><a href=\"#number\">Number</a>\n         <td><code>0</code>\n     <tr>\n         <td><code>h</code>\n         <td>Hour in am/pm (1-12)\n         <td><a href=\"#number\">Number</a>\n         <td><code>12</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>m</code>\n         <td>Minute in hour\n         <td><a href=\"#number\">Number</a>\n         <td><code>30</code>\n     <tr>\n         <td><code>s</code>\n         <td>Second in minute\n         <td><a href=\"#number\">Number</a>\n         <td><code>55</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>S</code>\n         <td>Millisecond\n         <td><a href=\"#number\">Number</a>\n         <td><code>978</code>\n     <tr>\n         <td><code>z</code>\n         <td>Time zone\n         <td><a href=\"#timezone\">General time zone</a>\n         <td><code>Pacific Standard Time</code>; <code>PST</code>; <code>GMT-08:00</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>Z</code>\n         <td>Time zone\n         <td><a href=\"#rfc822timezone\">RFC 822 time zone</a>\n         <td><code>-0800</code>\n     <tr>\n         <td><code>X</code>\n         <td>Time zone\n         <td><a href=\"#iso8601timezone\">ISO 8601 time zone</a>\n         <td><code>-08</code>; <code>-0800</code>;  <code>-08:00</code>\n </table>\n </blockquote>"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        options1Panel.add(dateFormatTextField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 0, 8);
        mainPanel.add(options1Panel, gridBagConstraints);

        options2Panel.setLayout(new java.awt.GridBagLayout());

        operationLabel.setText(bundle.getString("MainFrame.operationLabel.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 0, 0);
        options2Panel.add(operationLabel, gridBagConstraints);

        opComboBox.setMaximumSize(new java.awt.Dimension(200, 32767));
        opComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                opComboBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        options2Panel.add(opComboBox, gridBagConstraints);

        followLinksCheckBox.setText(Dict.FOLLOW_LINKS.getString());
        followLinksCheckBox.setFocusable(false);
        followLinksCheckBox.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        followLinksCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                followLinksCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        options2Panel.add(followLinksCheckBox, gridBagConstraints);

        recursiveCheckBox.setText(Dict.RECURSIVE.getString());
        recursiveCheckBox.setFocusable(false);
        recursiveCheckBox.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        recursiveCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                recursiveCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        options2Panel.add(recursiveCheckBox, gridBagConstraints);

        overwriteCheckBox.setText(bundle.getString("MainFrame.overwriteCheckBox.text")); // NOI18N
        overwriteCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                overwriteCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        options2Panel.add(overwriteCheckBox, gridBagConstraints);

        jLabel1.setText(bundle.getString("MainFrame.jLabel1.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        options2Panel.add(jLabel1, gridBagConstraints);

        caseBaseComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                caseBaseComboBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        options2Panel.add(caseBaseComboBox, gridBagConstraints);

        jLabel2.setText(bundle.getString("MainFrame.jLabel2.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        options2Panel.add(jLabel2, gridBagConstraints);

        caseSuffixComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                caseSuffixComboBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        options2Panel.add(caseSuffixComboBox, gridBagConstraints);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 71, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 41, Short.MAX_VALUE)
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        options2Panel.add(jPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 0, 8);
        mainPanel.add(options2Panel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
        mainPanel.add(logPanel, gridBagConstraints);

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void menuButtonMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_menuButtonMousePressed
        if (evt == null || evt.getButton() == MouseEvent.BUTTON1) {
            if (mPopupMenu.isVisible()) {
                mPopupMenu.setVisible(false);
            } else {
                mPopupMenu.show(menuButton, menuButton.getWidth() - mPopupMenu.getWidth(), mPopupMenu.getHeight());

                int x = menuButton.getLocationOnScreen().x + menuButton.getWidth() - mPopupMenu.getWidth();
                int y = menuButton.getLocationOnScreen().y + menuButton.getHeight();

                mPopupMenu.setLocation(x, y);
            }
        }
    }//GEN-LAST:event_menuButtonMousePressed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        saveProfiles();
    }//GEN-LAST:event_formWindowClosing

    private void followLinksCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_followLinksCheckBoxActionPerformed
        getSelectedProfile().setFollowLinks(followLinksCheckBox.isSelected());
    }//GEN-LAST:event_followLinksCheckBoxActionPerformed

    private void recursiveCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_recursiveCheckBoxActionPerformed
        getSelectedProfile().setRecursive(recursiveCheckBox.isSelected());
    }//GEN-LAST:event_recursiveCheckBoxActionPerformed

    private void opComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_opComboBoxActionPerformed
        getSelectedProfile().setOperation(opComboBox.getSelectedIndex());
    }//GEN-LAST:event_opComboBoxActionPerformed

    private void caseBaseComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_caseBaseComboBoxActionPerformed
        getSelectedProfile().setBaseNameCase((NameCase) caseBaseComboBox.getSelectedItem());
    }//GEN-LAST:event_caseBaseComboBoxActionPerformed

    private void caseSuffixComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_caseSuffixComboBoxActionPerformed
        getSelectedProfile().setExtNameCase((NameCase) caseSuffixComboBox.getSelectedItem());
    }//GEN-LAST:event_caseSuffixComboBoxActionPerformed

    private void dateSourceComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dateSourceComboBoxActionPerformed
        getSelectedProfile().setDateSource((DateSource) dateSourceComboBox.getSelectedItem());
    }//GEN-LAST:event_dateSourceComboBoxActionPerformed

    private void profileComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_profileComboBoxActionPerformed
        Profile p = getSelectedProfile();
        if (p != null) {
            opComboBox.setSelectedIndex(p.getOperation());
            dateSourceComboBox.setSelectedItem(p.getDateSource());
            caseBaseComboBox.setSelectedItem(p.getBaseNameCase());
            caseSuffixComboBox.setSelectedItem(p.getExtNameCase());

            followLinksCheckBox.setSelected(p.isFollowLinks());
            recursiveCheckBox.setSelected(p.isRecursive());
            overwriteCheckBox.setSelected(p.isReplaceExisting());

            if (p.getSourceDir() != null) {
                sourceChooserPanel.setPath(p.getSourceDir().getAbsolutePath());
            }

            if (p.getDestDir() != null) {
                destChooserPanel.setPath(p.getDestDir().getAbsolutePath());
            }

            patternTextField.setText(p.getFilePattern());
            dateFormatTextField.setText(p.getDatePattern());
        }
    }//GEN-LAST:event_profileComboBoxActionPerformed

    private void overwriteCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_overwriteCheckBoxActionPerformed
        getSelectedProfile().setReplaceExisting(overwriteCheckBox.isSelected());
    }//GEN-LAST:event_overwriteCheckBoxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JButton addButton;
    private javax.swing.JComboBox<NameCase> caseBaseComboBox;
    private javax.swing.JComboBox<NameCase> caseSuffixComboBox;
    private javax.swing.JMenuItem cloneMenuItem;
    private javax.swing.JLabel dateFormatLabel;
    private javax.swing.JTextField dateFormatTextField;
    private javax.swing.JComboBox<DateSource> dateSourceComboBox;
    private javax.swing.JLabel dateSourceLabel;
    private se.trixon.almond.util.swing.dialogs.FileChooserPanel destChooserPanel;
    private javax.swing.JCheckBox followLinksCheckBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private se.trixon.almond.util.swing.LogPanel logPanel;
    private javax.swing.JPopupMenu mPopupMenu;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JButton menuButton;
    private javax.swing.JComboBox opComboBox;
    private javax.swing.JLabel operationLabel;
    private javax.swing.JPanel options1Panel;
    private javax.swing.JPanel options2Panel;
    private javax.swing.JMenuItem optionsMenuItem;
    private javax.swing.JCheckBox overwriteCheckBox;
    private javax.swing.JLabel patternLabel;
    private javax.swing.JTextField patternTextField;
    private javax.swing.JComboBox<Profile> profileComboBox;
    private javax.swing.JMenuItem quitMenuItem;
    private javax.swing.JCheckBox recursiveCheckBox;
    private javax.swing.JMenuItem removeAllProfilesMenuItem;
    private javax.swing.JButton removeButton;
    private javax.swing.JMenuItem renameMenuItem;
    private se.trixon.almond.util.swing.dialogs.FileChooserPanel sourceChooserPanel;
    private javax.swing.JButton startButton;
    private javax.swing.JToolBar toolBar;
    private javax.swing.JPanel topPanel;
    // End of variables declaration//GEN-END:variables

    class ActionManager {

        static final String ABOUT = "about";
        static final String ADD = "add";
        static final String CLONE = "clone";
        static final String MENU = "menu";
        static final String OPTIONS = "options";
        static final String QUIT = "shutdownServerAndWindow";
        static final String REMOVE = "remove";
        static final String REMOVE_ALL = "remove_all";
        static final String RENAME = "rename";
        static final String START = "start";

        private ActionManager() {
            initActions();
        }

        Action getAction(String key) {
            return getRootPane().getActionMap().get(key);
        }

        private void initAction(AlmondAction action, String key, KeyStroke keyStroke, Enum iconEnum, boolean baseAction) {
            InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap actionMap = getRootPane().getActionMap();

            action.putValue(Action.ACCELERATOR_KEY, keyStroke);
            action.putValue(Action.SHORT_DESCRIPTION, action.getValue(Action.NAME));
            action.putValue("hideActionText", true);
            action.setIconEnum(iconEnum);
            action.updateIcon();

            inputMap.put(keyStroke, key);
            actionMap.put(key, action);

            if (baseAction) {
                mBaseActions.add(action);
            }

            mAllActions.add(action);
        }

        private void initActions() {
            AlmondAction action;
            KeyStroke keyStroke;
            int commandMask = SystemHelper.getCommandMask();

            //menu
            int menuKey = KeyEvent.VK_M;
            keyStroke = KeyStroke.getKeyStroke(menuKey, commandMask);
            action = new AlmondAction(Dict.MENU.toString()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() != menuButton) {
                        menuButtonMousePressed(null);
                    }
                }
            };

            initAction(action, MENU, keyStroke, MaterialIcon._Navigation.MENU, true);
            menuButton.setAction(action);

            //options
            int optionsKey = SystemUtils.IS_OS_MAC ? KeyEvent.VK_COMMA : KeyEvent.VK_P;
            keyStroke = KeyStroke.getKeyStroke(optionsKey, commandMask);
            action = new AlmondAction(Dict.OPTIONS.toString()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    showOptions();
                }
            };

            initAction(action, OPTIONS, keyStroke, MaterialIcon._Action.SETTINGS, true);
            optionsMenuItem.setAction(action);

            //start
            keyStroke = null;
            action = new AlmondAction(Dict.START.toString()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!mProfiles.isEmpty()) {
                        profileRun();
                    }
                }
            };

            initAction(action, START, keyStroke, MaterialIcon._Av.PLAY_ARROW, false);
            startButton.setAction(action);

            //add
            keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0);

            action = new AlmondAction(Dict.ADD.toString()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    profileAdd(null);
                }
            };

            initAction(action, ADD, keyStroke, MaterialIcon._Content.ADD, true);
            addButton.setAction(action);

            //clone
            keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, commandMask);
            action = new AlmondAction(Dict.CLONE.toString()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        profileClone();
                    } catch (CloneNotSupportedException ex) {
                        Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            };

            initAction(action, CLONE, keyStroke, MaterialIcon._Content.CONTENT_COPY, false);
            cloneMenuItem.setAction(action);

            //rename
            keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_R, commandMask);
            action = new AlmondAction(Dict.RENAME.toString()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    profileRename(getSelectedProfile().getName());
                }
            };

            initAction(action, RENAME, keyStroke, MaterialIcon._Editor.MODE_EDIT, false);
            renameMenuItem.setAction(action);

            //remove
            keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
            action = new AlmondAction(Dict.REMOVE.toString()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    profileRemove();
                }
            };

            initAction(action, REMOVE, keyStroke, MaterialIcon._Content.REMOVE, false);
            removeButton.setAction(action);

            //remove all
            keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.SHIFT_DOWN_MASK);
            action = new AlmondAction(Dict.REMOVE_ALL.toString()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    profileRemoveAll();
                }
            };

            initAction(action, REMOVE_ALL, keyStroke, MaterialIcon._Content.CLEAR, false);
            removeAllProfilesMenuItem.setAction(action);

            //about
            keyStroke = null;
            action = new AlmondAction(Dict.ABOUT.toString()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    PomInfo pomInfo = new PomInfo(FileByDate.class, "se.trixon", "filebydate");
                    String versionInfo = String.format(mBundle.getString("version_info"), pomInfo.getVersion());
                    Message.information(MainFrame.this, Dict.ABOUT.toString(), versionInfo);
                }
            };

            initAction(action, ABOUT, keyStroke, null, true);
            aboutMenuItem.setAction(action);

            //quit
            keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Q, commandMask);
            action = new AlmondAction(Dict.QUIT.toString()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    quit();
                }
            };

            initAction(action, QUIT, keyStroke, MaterialIcon._Content.CLEAR, true);
            quitMenuItem.setAction(action);

            for (Component component : mPopupMenu.getComponents()) {
                if (component instanceof AbstractButton) {
                    ((AbstractButton) component).setToolTipText(null);
                }
            }

            for (Component component : toolBar.getComponents()) {
                if (component instanceof AbstractButton) {
                    ((AbstractButton) component).setText(null);
                }
            }
        }
    }
}
