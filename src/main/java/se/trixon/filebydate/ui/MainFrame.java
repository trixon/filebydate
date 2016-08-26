/*
 * Copyright 2016 Patrik Karlsson.
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
import java.util.LinkedList;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.SystemUtils;
import se.trixon.almond.util.AlmondAction;
import se.trixon.almond.util.AlmondOptions;
import se.trixon.almond.util.AlmondOptions.AlmondOptionsEvent;
import se.trixon.almond.util.AlmondUI;
import se.trixon.almond.util.BundleHelper;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.SystemHelper;
import se.trixon.almond.util.icon.Pict;
import se.trixon.almond.util.icons.material.MaterialIcon;
import se.trixon.almond.util.swing.SwingHelper;
import se.trixon.almond.util.swing.dialogs.Message;
import se.trixon.filebydate.FileByDate;

/**
 *
 * @author Patrik Karlsson
 */
public class MainFrame extends JFrame implements AlmondOptions.AlmondOptionsWatcher {

    private final ResourceBundle mBundle = BundleHelper.getBundle(FileByDate.class, "Bundle");
    private ActionManager mActionManager;
    private final AlmondUI mAlmondUI = AlmondUI.getInstance();
    private final LinkedList<AlmondAction> mServerActions = new LinkedList<>();
    private final LinkedList<AlmondAction> mAllActions = new LinkedList<>();
    private final AlmondOptions mAlmondOptions = AlmondOptions.getInstance();

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

    private void init() {
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
        toolBar = new javax.swing.JToolBar();
        profileComboBox = new javax.swing.JComboBox<>();
        startButton = new javax.swing.JButton();
        addButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        menuButton = new javax.swing.JButton();
        mainPanel = new javax.swing.JPanel();
        sourceChooserPanel = new se.trixon.almond.util.swing.dialogs.FileChooserPanel();
        destChooserPanel = new se.trixon.almond.util.swing.dialogs.FileChooserPanel();
        options1Panel = new javax.swing.JPanel();
        patternPanel = new javax.swing.JPanel();
        patternLabel = new javax.swing.JLabel();
        patternTextField = new javax.swing.JTextField();
        dateSourcePanel = new javax.swing.JPanel();
        dateSourceLabel = new javax.swing.JLabel();
        dateSourceComboBox = new javax.swing.JComboBox();
        dateFormatPanel = new javax.swing.JPanel();
        dateFormatLabel = new javax.swing.JLabel();
        dateFormatTextField = new javax.swing.JTextField();
        options2Panel = new javax.swing.JPanel();
        dryRunCheckBox = new javax.swing.JCheckBox();
        followLinksCheckBox = new javax.swing.JCheckBox();
        opComboBox = new javax.swing.JComboBox();
        recursiveCheckBox = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        jComboBox2 = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        jComboBox3 = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        logPanel = new se.trixon.almond.util.swing.LogPanel();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("se/trixon/filebydate/ui/Bundle"); // NOI18N
        renameMenuItem.setText(bundle.getString("MainFrame.renameMenuItem.text")); // NOI18N
        mPopupMenu.add(renameMenuItem);

        cloneMenuItem.setText(bundle.getString("MainFrame.cloneMenuItem.text")); // NOI18N
        mPopupMenu.add(cloneMenuItem);
        mPopupMenu.add(removeAllProfilesMenuItem);
        mPopupMenu.add(jSeparator1);
        mPopupMenu.add(optionsMenuItem);
        mPopupMenu.add(jSeparator2);
        mPopupMenu.add(aboutMenuItem);
        mPopupMenu.add(jSeparator6);
        mPopupMenu.add(quitMenuItem);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle(bundle.getString("MainFrame.title")); // NOI18N

        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        profileComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        toolBar.add(profileComboBox);

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

        getContentPane().add(toolBar, java.awt.BorderLayout.PAGE_START);

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

        options1Panel.setLayout(new java.awt.GridLayout(1, 0));

        patternLabel.setText(Dict.FILE_PATTERN.getString());

        patternTextField.setText("*"); // NOI18N

        javax.swing.GroupLayout patternPanelLayout = new javax.swing.GroupLayout(patternPanel);
        patternPanel.setLayout(patternPanelLayout);
        patternPanelLayout.setHorizontalGroup(
            patternPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(patternPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(patternPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(patternPanelLayout.createSequentialGroup()
                        .addComponent(patternLabel)
                        .addGap(0, 113, Short.MAX_VALUE))
                    .addComponent(patternTextField))
                .addContainerGap())
        );
        patternPanelLayout.setVerticalGroup(
            patternPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(patternPanelLayout.createSequentialGroup()
                .addComponent(patternLabel)
                .addGap(0, 0, 0)
                .addComponent(patternTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 6, Short.MAX_VALUE))
        );

        options1Panel.add(patternPanel);

        dateSourceLabel.setText(bundle.getString("MainFrame.dateSourceLabel.text")); // NOI18N

        javax.swing.GroupLayout dateSourcePanelLayout = new javax.swing.GroupLayout(dateSourcePanel);
        dateSourcePanel.setLayout(dateSourcePanelLayout);
        dateSourcePanelLayout.setHorizontalGroup(
            dateSourcePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dateSourcePanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(dateSourcePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(dateSourceComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(dateSourcePanelLayout.createSequentialGroup()
                        .addComponent(dateSourceLabel)
                        .addGap(0, 121, Short.MAX_VALUE)))
                .addContainerGap())
        );
        dateSourcePanelLayout.setVerticalGroup(
            dateSourcePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dateSourcePanelLayout.createSequentialGroup()
                .addComponent(dateSourceLabel)
                .addGap(0, 0, 0)
                .addComponent(dateSourceComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 4, Short.MAX_VALUE))
        );

        options1Panel.add(dateSourcePanel);

        dateFormatLabel.setText(Dict.DATE_PATTERN.getString());

        dateFormatTextField.setToolTipText("<html>\n <h3>Date and Time Patterns</h3>\n <p>\n Date and time formats are specified by <em>date and time pattern</em>\n strings.\n Within date and time pattern strings, unquoted letters<br />from\n <code>'A'</code> to <code>'Z'</code> and from <code>'a'</code> to\n <code>'z'</code> are interpreted as pattern letters representing the\n components of a date or time string.<br />\n Text can be quoted using single quotes (<code>'</code>) to avoid\n interpretation.\n <code>\"''\"</code> represents a single quote.<br />\n All other characters are not interpreted; they're simply copied into the\n output string during formatting or matched against<br />the input string\n during parsing.\n <p>\nThe following pattern letters are defined (all other characters from\n <code>'A'</code> to <code>'Z'</code> and from <code>'a'</code> to\n <code>'z'</code> are reserved):\n <blockquote>\n <table border=0 cellspacing=3 cellpadding=0 summary=\"Chart shows pattern letters, date/time component, presentation, and examples.\">\n     <tr style=\"background-color: rgb(204, 204, 255);\">\n         <th align=left>Letter\n         <th align=left>Date or Time Component\n         <th align=left>Presentation\n         <th align=left>Examples\n     <tr>\n         <td><code>G</code>\n         <td>Era designator\n         <td><a href=\"#text\">Text</a>\n         <td><code>AD</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>y</code>\n         <td>Year\n         <td><a href=\"#year\">Year</a>\n         <td><code>1996</code>; <code>96</code>\n     <tr>\n         <td><code>Y</code>\n         <td>Week year\n         <td><a href=\"#year\">Year</a>\n         <td><code>2009</code>; <code>09</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>M</code>\n         <td>Month in year (context sensitive)\n         <td><a href=\"#month\">Month</a>\n         <td><code>July</code>; <code>Jul</code>; <code>07</code>\n     <tr>\n         <td><code>L</code>\n         <td>Month in year (standalone form)\n         <td><a href=\"#month\">Month</a>\n         <td><code>July</code>; <code>Jul</code>; <code>07</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>w</code>\n         <td>Week in year\n         <td><a href=\"#number\">Number</a>\n         <td><code>27</code>\n     <tr>\n         <td><code>W</code>\n         <td>Week in month\n         <td><a href=\"#number\">Number</a>\n         <td><code>2</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>D</code>\n         <td>Day in year\n         <td><a href=\"#number\">Number</a>\n         <td><code>189</code>\n     <tr>\n         <td><code>d</code>\n         <td>Day in month\n         <td><a href=\"#number\">Number</a>\n         <td><code>10</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>F</code>\n         <td>Day of week in month\n         <td><a href=\"#number\">Number</a>\n         <td><code>2</code>\n     <tr>\n         <td><code>E</code>\n         <td>Day name in week\n         <td><a href=\"#text\">Text</a>\n         <td><code>Tuesday</code>; <code>Tue</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>u</code>\n         <td>Day number of week (1 = Monday, ..., 7 = Sunday)\n         <td><a href=\"#number\">Number</a>\n         <td><code>1</code>\n     <tr>\n         <td><code>a</code>\n         <td>Am/pm marker\n         <td><a href=\"#text\">Text</a>\n         <td><code>PM</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>H</code>\n         <td>Hour in day (0-23)\n         <td><a href=\"#number\">Number</a>\n         <td><code>0</code>\n     <tr>\n         <td><code>k</code>\n         <td>Hour in day (1-24)\n         <td><a href=\"#number\">Number</a>\n         <td><code>24</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>K</code>\n         <td>Hour in am/pm (0-11)\n         <td><a href=\"#number\">Number</a>\n         <td><code>0</code>\n     <tr>\n         <td><code>h</code>\n         <td>Hour in am/pm (1-12)\n         <td><a href=\"#number\">Number</a>\n         <td><code>12</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>m</code>\n         <td>Minute in hour\n         <td><a href=\"#number\">Number</a>\n         <td><code>30</code>\n     <tr>\n         <td><code>s</code>\n         <td>Second in minute\n         <td><a href=\"#number\">Number</a>\n         <td><code>55</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>S</code>\n         <td>Millisecond\n         <td><a href=\"#number\">Number</a>\n         <td><code>978</code>\n     <tr>\n         <td><code>z</code>\n         <td>Time zone\n         <td><a href=\"#timezone\">General time zone</a>\n         <td><code>Pacific Standard Time</code>; <code>PST</code>; <code>GMT-08:00</code>\n     <tr style=\"background-color: rgb(238, 238, 255);\">\n         <td><code>Z</code>\n         <td>Time zone\n         <td><a href=\"#rfc822timezone\">RFC 822 time zone</a>\n         <td><code>-0800</code>\n     <tr>\n         <td><code>X</code>\n         <td>Time zone\n         <td><a href=\"#iso8601timezone\">ISO 8601 time zone</a>\n         <td><code>-08</code>; <code>-0800</code>;  <code>-08:00</code>\n </table>\n </blockquote>"); // NOI18N

        javax.swing.GroupLayout dateFormatPanelLayout = new javax.swing.GroupLayout(dateFormatPanel);
        dateFormatPanel.setLayout(dateFormatPanelLayout);
        dateFormatPanelLayout.setHorizontalGroup(
            dateFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dateFormatPanelLayout.createSequentialGroup()
                .addComponent(dateFormatLabel)
                .addGap(0, 119, Short.MAX_VALUE))
            .addComponent(dateFormatTextField)
        );
        dateFormatPanelLayout.setVerticalGroup(
            dateFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dateFormatPanelLayout.createSequentialGroup()
                .addComponent(dateFormatLabel)
                .addGap(0, 0, 0)
                .addComponent(dateFormatTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        options1Panel.add(dateFormatPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 0, 8);
        mainPanel.add(options1Panel, gridBagConstraints);

        dryRunCheckBox.setText(Dict.DRY_RUN.getString());
        dryRunCheckBox.setFocusable(false);
        dryRunCheckBox.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        followLinksCheckBox.setText(Dict.FOLLOW_LINKS.getString());
        followLinksCheckBox.setFocusable(false);
        followLinksCheckBox.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        opComboBox.setMaximumSize(new java.awt.Dimension(200, 32767));

        recursiveCheckBox.setText(Dict.RECURSIVE.getString());
        recursiveCheckBox.setFocusable(false);
        recursiveCheckBox.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        jLabel1.setText(bundle.getString("MainFrame.jLabel1.text")); // NOI18N

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel2.setText(bundle.getString("MainFrame.jLabel2.text")); // NOI18N

        jComboBox3.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel3.setText(bundle.getString("MainFrame.jLabel3.text")); // NOI18N

        javax.swing.GroupLayout options2PanelLayout = new javax.swing.GroupLayout(options2Panel);
        options2Panel.setLayout(options2PanelLayout);
        options2PanelLayout.setHorizontalGroup(
            options2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(options2PanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(options2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addGroup(options2PanelLayout.createSequentialGroup()
                        .addComponent(opComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(recursiveCheckBox)
                        .addGap(18, 18, 18)
                        .addComponent(followLinksCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(dryRunCheckBox)))
                .addGap(33, 33, 33)
                .addGroup(options2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(options2PanelLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(42, 42, 42)
                        .addComponent(jLabel2))
                    .addGroup(options2PanelLayout.createSequentialGroup()
                        .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        options2PanelLayout.setVerticalGroup(
            options2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(options2PanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(options2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(options2PanelLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addGroup(options2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(options2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(opComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(recursiveCheckBox))
                            .addGroup(options2PanelLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(followLinksCheckBox))))
                    .addGroup(options2PanelLayout.createSequentialGroup()
                        .addGroup(options2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(options2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(dryRunCheckBox))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JButton addButton;
    private javax.swing.JMenuItem cloneMenuItem;
    private javax.swing.JLabel dateFormatLabel;
    private javax.swing.JPanel dateFormatPanel;
    private javax.swing.JTextField dateFormatTextField;
    private javax.swing.JComboBox dateSourceComboBox;
    private javax.swing.JLabel dateSourceLabel;
    private javax.swing.JPanel dateSourcePanel;
    private se.trixon.almond.util.swing.dialogs.FileChooserPanel destChooserPanel;
    private javax.swing.JCheckBox dryRunCheckBox;
    private javax.swing.JCheckBox followLinksCheckBox;
    private javax.swing.JComboBox<String> jComboBox2;
    private javax.swing.JComboBox<String> jComboBox3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private se.trixon.almond.util.swing.LogPanel logPanel;
    private javax.swing.JPopupMenu mPopupMenu;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JButton menuButton;
    private javax.swing.JComboBox opComboBox;
    private javax.swing.JPanel options1Panel;
    private javax.swing.JPanel options2Panel;
    private javax.swing.JMenuItem optionsMenuItem;
    private javax.swing.JLabel patternLabel;
    private javax.swing.JPanel patternPanel;
    private javax.swing.JTextField patternTextField;
    private javax.swing.JComboBox<String> profileComboBox;
    private javax.swing.JMenuItem quitMenuItem;
    private javax.swing.JCheckBox recursiveCheckBox;
    private javax.swing.JMenuItem removeAllProfilesMenuItem;
    private javax.swing.JButton removeButton;
    private javax.swing.JMenuItem renameMenuItem;
    private se.trixon.almond.util.swing.dialogs.FileChooserPanel sourceChooserPanel;
    private javax.swing.JButton startButton;
    private javax.swing.JToolBar toolBar;
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

        private void initAction(AlmondAction action, String key, KeyStroke keyStroke, Enum iconEnum, boolean serverAction) {
            InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap actionMap = getRootPane().getActionMap();

            action.putValue(Action.ACCELERATOR_KEY, keyStroke);
            action.putValue(Action.SHORT_DESCRIPTION, action.getValue(Action.NAME));
            action.putValue("hideActionText", true);
            action.setIconEnum(iconEnum);
            action.updateIcon();

            inputMap.put(keyStroke, key);
            actionMap.put(key, action);

            if (serverAction) {
                mServerActions.add(action);
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

            initAction(action, MENU, keyStroke, MaterialIcon.Navigation.MENU, false);
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

            initAction(action, OPTIONS, keyStroke, MaterialIcon.Action.SETTINGS, false);
            optionsMenuItem.setAction(action);

            //start
            keyStroke = null;
            action = new AlmondAction(Dict.START.toString()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    System.out.println("start");
                }
            };

            initAction(action, START, keyStroke, MaterialIcon.Av.PLAY_ARROW, false);
            startButton.setAction(action);

            //add
            keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0);

            action = new AlmondAction(Dict.ADD.toString()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    System.out.println("add");
                }
            };

            initAction(action, ADD, keyStroke, MaterialIcon.Content.ADD, false);
            addButton.setAction(action);

            //clone
            keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, commandMask);
            action = new AlmondAction(Dict.CLONE.toString()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    System.out.println("clone");
                }
            };

            initAction(action, CLONE, keyStroke, MaterialIcon.Content.CONTENT_COPY, false);
            cloneMenuItem.setAction(action);

            //rename
            keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_R, commandMask);
            action = new AlmondAction(Dict.RENAME.toString()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    System.out.println("rename");
                }
            };

            initAction(action, RENAME, keyStroke, MaterialIcon.Editor.MODE_EDIT, false);
            renameMenuItem.setAction(action);

            //remove
            keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
            action = new AlmondAction(Dict.REMOVE.toString()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    System.out.println("remove");
                }
            };

            initAction(action, REMOVE, keyStroke, MaterialIcon.Content.REMOVE, false);
            removeButton.setAction(action);

            //remove all
            keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.SHIFT_DOWN_MASK);
            action = new AlmondAction(Dict.REMOVE_ALL.toString()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    System.out.println("remove all");
                }
            };

            initAction(action, REMOVE_ALL, keyStroke, MaterialIcon.Content.CLEAR, false);
            removeAllProfilesMenuItem.setAction(action);

            //about
            keyStroke = null;
            action = new AlmondAction(Dict.ABOUT.toString()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    String versionInfo = String.format(mBundle.getString("version_info"), SystemHelper.getJarVersion(FileByDate.class));
                    Message.information(MainFrame.this, Dict.ABOUT.toString(), versionInfo);
                }
            };

            initAction(action, ABOUT, keyStroke, null, false);
            aboutMenuItem.setAction(action);

            //quit
            keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Q, commandMask);
            action = new AlmondAction(Dict.QUIT.toString()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    quit();
                }
            };

            initAction(action, QUIT, keyStroke, Pict.Actions.APPLICATION_EXIT, false);
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
