/**
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.undo.*;
import java.util.List;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import jnafilechooser.api.*;
import java.util.concurrent.*;
import java.util.Scanner;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.Font;
import java.awt.FontFormatException;
import java.nio.file.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.*;
import javax.imageio.*;
import java.awt.*;
import java.util.StringTokenizer;

public class Notes extends JFrame {
    private static final AtomicInteger windowCount = new AtomicInteger(0);
    private static TextArea textArea;
    private static Font font = null;
    private static File currentFile = null;
    private static boolean isModified = false;
    private static UndoManager undoManager;
    private static JLabel statusLabel;
    private static Boolean firstTimeCompile = false;
    private static float defaultFontSize = 16.0f;
    private static JnaFileChooser ch = new JnaFileChooser();
    private static logWindow log = new logWindow();
    private static boolean encrypt = false;// false
    private static boolean backup = false;//false
    private static boolean config = true;
    private static String img_path = null;
    private static Color bgColor = null;
    private static Color textColor = null;
    //private static Color universalBgColor = new Color(255,255,255);
    //private static Color universalTextColor = new Color(0,0,0);
    private static SimpleDateFormat formatter = new SimpleDateFormat("dd_MM_yyyy__HH_mm_ss");
    //private String Ei = "(X):E{]>";
    private static JMenuItem encryptItem,backupItem,configItem;
    private static final String backupDir = System.getenv("LOCALAPPDATA")+"\\Notes\\Backup";
    private static final String ei = "(X):.,E{]>";// org = "(X):"+"\u200C"+"E{]>"
    public Notes(){
        windowCount.incrementAndGet(); // Increment the window count when a new window is created
        undoManager = new UndoManager();
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, new File(System.getProperty("user.dir") + "\\res\\Consolas.TTF"));
            font = font.deriveFont(defaultFontSize);
        } catch (FontFormatException | IOException ffe) {
            ffe.printStackTrace();
        }
        if(!((new File(System.getenv("LOCALAPPDATA")+"\\Notes")).exists())){
            try{
                Files.createDirectories(Paths.get(System.getenv("LOCALAPPDATA")+"\\Notes"));
            }catch (IOException ioe){
                ioe.printStackTrace();
            }
        }
        if(!((new File(System.getenv("LOCALAPPDATA")+"\\Notes\\Backup")).exists())){
            try{
                Files.createDirectories(Paths.get(System.getenv("LOCALAPPDATA")+"\\Notes\\Backup"));
            }catch (IOException ioe){
                ioe.printStackTrace();
            }
        }
        setTitle("*untitled.txt - Notes");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Handle closing manually
        setIconImage(new ImageIcon(System.getProperty("user.dir")+"\\res\\icon.png").getImage());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if(promptForUnsavedChanges()){
                    if(encrypt || backup){
                        saveFile();
                    }
                    saveSettings();
                }
                if(encrypt || backup){
                    saveFile();
                }
                saveSettings();
            }
        });

        textArea = new TextArea();
        textArea.setFont(font);
        textArea.getDocument().addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));
        textArea.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (!isModified) {
                    if(Character.isDefined(e.getKeyChar()) && (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == 0){
                        isModified = true;
                        updateTitle();
                    }
                }
                updateStatusBar();
            }
        });
        textArea.addCaretListener(e -> updateStatusBar());
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem newItem = createMenuItem("New", KeyEvent.VK_N, KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        JMenuItem newWindowItem = createMenuItem("New Window", KeyEvent.VK_W, KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        JMenuItem openItem = createMenuItem("Open", KeyEvent.VK_O, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        JMenuItem saveItem = createMenuItem("Save", KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        JMenuItem saveAsItem = createMenuItem("Save As", KeyEvent.VK_A, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        JMenuItem exitItem = createMenuItem("Exit", KeyEvent.VK_E, null);

        newItem.addActionListener(e -> newFile());
        newWindowItem.addActionListener(e -> newWindow());
        openItem.addActionListener(e -> openFile());
        saveItem.addActionListener(e -> saveFile());
        saveAsItem.addActionListener(e -> saveFileAs());
        exitItem.addActionListener(e -> {
            if(promptForUnsavedChanges()){
                if(encrypt || backup){
                    saveFile();
                }
                if (windowCount.decrementAndGet() == 0){
                    saveSettings();// Decrement window count and check if it's zero
                    System.exit(0); // Exit program if no more windows are open
                }
                saveSettings();
            }
            if(encrypt || backup){
                saveFile();
            }
            saveSettings();
        });

        fileMenu.add(newItem);
        fileMenu.add(newWindowItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        //fileMenu.addSeparator();
        //fileMenu.add(printItem);
        //fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        JMenuItem undoItem = createMenuItem("Undo", KeyEvent.VK_U, KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        JMenuItem redoItem = createMenuItem("Redo", KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        JMenuItem cutItem = createMenuItem("Cut", KeyEvent.VK_T, KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        JMenuItem copyItem = createMenuItem("Copy", KeyEvent.VK_C, KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        JMenuItem pasteItem = createMenuItem("Paste", KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
        JMenuItem findItem = createMenuItem("Find", KeyEvent.VK_F, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        JMenuItem replaceItem = createMenuItem("Replace", KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK));
        JMenuItem goToItem = createMenuItem("Go To", KeyEvent.VK_G, KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK));
        JMenuItem selectAllItem = createMenuItem("Select All", KeyEvent.VK_A, KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
        JMenuItem timeDateItem = createMenuItem("Time/Date", KeyEvent.VK_D, KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));

        undoItem.addActionListener(e -> undoManager.undo());
        redoItem.addActionListener(e -> undoManager.redo());
        cutItem.addActionListener(e -> textArea.cut());
        copyItem.addActionListener(e -> textArea.copy());
        pasteItem.addActionListener(e -> textArea.paste());
        findItem.addActionListener(e -> showFindDialog());
        replaceItem.addActionListener(e -> showReplaceDialog());
        goToItem.addActionListener(e -> goToLine());
        selectAllItem.addActionListener(e -> textArea.selectAll());
        timeDateItem.addActionListener(e -> insertTimeDate());

        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.add(cutItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);
        editMenu.addSeparator();
        editMenu.add(findItem);
        editMenu.add(replaceItem);
        editMenu.add(goToItem);
        editMenu.add(selectAllItem);
        editMenu.add(timeDateItem);
        //editMenu.add(changeFontItem);
        menuBar.add(editMenu);

        // View Menu
        JMenu viewMenu = new JMenu("View");
        JMenuItem increaseFontSizeItem = createMenuItem("Increase Font Size", KeyEvent.VK_I, KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK));
        JMenuItem decreaseFontSizeItem = createMenuItem("Decrease Font Size", KeyEvent.VK_D, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));
        JMenuItem resetFontSizeItem = createMenuItem("Reset Font Size", KeyEvent.VK_R, null);

        increaseFontSizeItem.addActionListener(e -> changeFontSize(1));
        decreaseFontSizeItem.addActionListener(e -> changeFontSize(-1));
        resetFontSizeItem.addActionListener(e -> resetFontSize());

        viewMenu.add(increaseFontSizeItem);
        viewMenu.add(decreaseFontSizeItem);
        viewMenu.add(resetFontSizeItem);
        menuBar.add(viewMenu);

        // Design Menu
        JMenu designMenu = new JMenu("Design");
        JMenuItem changeBackgroundItem = createMenuItem("Change Current File Background", KeyEvent.VK_B, null);
        JMenuItem changeTextColorItem = createMenuItem("Change Current File Text Color", KeyEvent.VK_T, null);
        JMenuItem changeUniversalBackgroundItem = createMenuItem("Change Universal Background", KeyEvent.VK_B, null);
        JMenuItem changeUniversalTextColorItem = createMenuItem("Change Universal Text Color", KeyEvent.VK_T, null);
        JMenuItem setImageAsBackgroundItem = createMenuItem("Set Image As Background", KeyEvent.VK_I, null);
        JMenuItem changeFontItem = createMenuItem("Change Font", KeyEvent.VK_F, null);

        changeBackgroundItem.addActionListener(e -> changeBackgroundColor());
        changeUniversalBackgroundItem.addActionListener(e -> changeUniversalBackgroundColor());
        changeTextColorItem.addActionListener(e -> changeTextColor());
        changeUniversalTextColorItem.addActionListener(e -> changeUniversalTextColor());
        setImageAsBackgroundItem.addActionListener(e -> setImageAsBackground());
        changeFontItem.addActionListener(e -> changeFont());

        designMenu.add(changeBackgroundItem);
        designMenu.add(changeUniversalBackgroundItem);
        designMenu.add(setImageAsBackgroundItem);
        designMenu.add(changeTextColorItem);
        designMenu.add(changeUniversalTextColorItem);
        designMenu.add(changeFontItem);
        menuBar.add(designMenu);

        //privacy menu
        JMenu privacyMenu = new JMenu("Privacy");
        encryptItem = createMenuItem("Enable Encryption", KeyEvent.VK_E, null);
        if(encrypt){
            encryptItem.setText("Disable Encryption");
        }
        backupItem = createMenuItem("Enable Backup", KeyEvent.VK_E, null);
        if(backup){
            backupItem.setText("Disable Backup");
        }
        configItem = createMenuItem("Don't Save Configuration", KeyEvent.VK_C, null);
        if(!config){
            configItem.setText("Save Configuration");
        }
        JMenuItem backupDirItem = createMenuItem("Backup Directory", KeyEvent.VK_E, null);
        JMenuItem decryptCurrentItem = createMenuItem("Decrypt Current File", KeyEvent.VK_E, null);

        encryptItem.addActionListener(e -> {
            encrypt = !encrypt;
            if(encrypt){
                encryptItem.setText("Disable Encryption");
            }else{
                encryptItem.setText("Enable Encryption");
            }
        });
        backupItem.addActionListener(e -> {
            backup = !backup;
            if(backup){
                backupItem.setText("Disable Backup");
            }else{
                backupItem.setText("Enable Backup");
            }
        });
        configItem.addActionListener(e -> {
            config = !config;
            if(config){
                configItem.setText("Don't Save Configuration");
                isModified = false;
                updateTitle();
            }else{
                configItem.setText("Save Configuration");
                isModified = false;
                updateTitle();
            }
        });
        decryptCurrentItem.addActionListener(e -> {
            Decrypter d = new Decrypter();
            String lineStr = JOptionPane.showInputDialog(this, "Enter seed:", "Decrypt Current File", JOptionPane.PLAIN_MESSAGE);
            byte seed = Byte.parseByte(lineStr);
            String s = d.decrypt(textArea.getText(),seed);
            textArea.setText(s);
        });
        backupDirItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, ("Backups Are Located In: "+backupDir), "Backup Directory Path", JOptionPane.PLAIN_MESSAGE);
        });

        privacyMenu.add(encryptItem);
        privacyMenu.add(backupItem);
        privacyMenu.add(configItem);
        privacyMenu.add(decryptCurrentItem);
        privacyMenu.add(backupDirItem);
        menuBar.add(privacyMenu);

        //dev menu
        JMenu devMenu = new JMenu("Dev");
        JMenuItem compileItem = new JMenuItem("Compile Java Code");
        JMenuItem runItem = new JMenuItem("Run Java Code");

        //compileItem.setAccelerator(KeyStroke.getKeyStroke("F9"));
        //runItem.setAccelerator(KeyStroke.getKeyStroke("F10"));

        //compileItem.addActionListener(e -> compileFile());
        //runItem.addActionListener(e -> runFile());
        //devMenu.add(compileItem);
        //devMenu.add(runItem);
        //menuBar.add(devMenu);

        setJMenuBar(menuBar);

        textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), "increaseFont");
        textArea.getActionMap().put("increaseFont", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeFontSize(1);
            }
        });

        textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "decreaseFont");
        textArea.getActionMap().put("decreaseFont", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeFontSize(-1);
            }
        });

        add(new JScrollPane(textArea), BorderLayout.CENTER);

        // Status Bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Ln: 1, Col: 1, Font Size: " + defaultFontSize + "pt");
        statusBar.add(statusLabel, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        loadSettings();//load the settings
        updateStatusBar();
    }
    static String path = null;
    private void setImageAsBackground(){
        JFrame f = new JFrame("Select Image To Set As Background");
        f.setSize(400,250);
        f.setLocationRelativeTo(null);
        f.setBackground(new Color(240,240,240));
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.getContentPane().setLayout(null);

        JButton b1 = new JButton("Choose Image From PC");
        JButton okb = new JButton("Okay");
        JButton canb = new JButton("cancel");
        b1.setSize(300,30);
        b1.setFont(new Font("",Font.BOLD,20));
        b1.setLocation(50,70);
        b1.setFocusable(false);
        b1.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                boolean comp = ch.showOpenDialog(f);
                if((comp) && ((ch.getSelectedFile().getAbsolutePath()).endsWith(".png") || (ch.getSelectedFile().getAbsolutePath()).endsWith(".jpg") || (ch.getSelectedFile().getAbsolutePath()).endsWith(".jpeg") || (ch.getSelectedFile().getAbsolutePath()).endsWith(".bmp") || (ch.getSelectedFile().getAbsolutePath()).endsWith(".wbmp"))){
                    path = ch.getSelectedFile().getAbsolutePath();
                    okb.setEnabled(true);
                }else if(comp){
                    JOptionPane.showMessageDialog(null, "\tThe File Type \""+ch.getSelectedFile().getAbsolutePath().substring((ch.getSelectedFile().getAbsolutePath().lastIndexOf(".")))+"\" Is Not Supported \nOnly \"png\",\"bmp\",\"wbmp\",\"jpg\" And \"jpeg\" Type Of Files Are Supported", "Notes", JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        okb.setSize(130,30);
        okb.setFont(new Font("",Font.BOLD,20));
        okb.setLocation(50,170);
        okb.setFocusable(false);
        okb.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(path != null){
                    textArea.setImage(path);
                    img_path = path;
                    textArea.repaint();
                    f.dispose();
                }
            }
        });
        okb.setEnabled(false);
        canb.setSize(130,30);
        canb.setFont(new Font("",Font.BOLD,20));
        canb.setLocation(220,170);
        canb.setFocusable(false);
        canb.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                f.dispose();
            }
        });
        f.getContentPane().add(canb);
        f.getContentPane().add(b1);
        f.getContentPane().add(okb);
        f.setVisible(true);
    }

    private JMenuItem createMenuItem(String text, int mnemonic, KeyStroke accelerator) {
        JMenuItem menuItem = new JMenuItem(text+"         ");
        menuItem.setMnemonic(mnemonic);
        menuItem.setAccelerator(accelerator);
        menuItem.setHorizontalTextPosition(SwingConstants.LEFT);
        menuItem.setHorizontalAlignment(SwingConstants.LEFT);
        return menuItem;
    }

    private void saveSettings() {
        try(BufferedWriter writer = new BufferedWriter(new FileWriter("saves.txt"))) {
            //Color bgColor = textArea.getBackground();
            //Color textColor = textArea.getForeground();
            Font font = textArea.getFont();
            writer.write("background:" + String.format("#%06X", universalBgColor.getRGB() & 0xFFFFFF) + "\n");
            writer.write("textColor:" + String.format("#%06X", universalTextColor.getRGB() & 0xFFFFFF) + "\n");
            writer.write("font:" + font.getFontName() + "," + font.getStyle() + "," + font.getSize() + "\n");
            writer.write("BG_Image:"+img_path+"\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSettings() {
        File file = new File("saves.txt");
        if (!file.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("background:")) {
                    String colorStr = line.substring("background:".length());
                    Color color = Color.decode(colorStr);
                    universalBgColor = color;
                    textArea.setBackground(color);
                } else if (line.startsWith("textColor:")) {
                    String colorStr = line.substring("textColor:".length());
                    Color color = Color.decode(colorStr);
                    universalTextColor = color;
                    textArea.setForeground(color);
                } else if (line.startsWith("font:")) {
                    String fontStr = line.substring("font:".length());
                    String[] parts = fontStr.split(",");
                    String fontName = parts[0];
                    int fontStyle = Integer.parseInt(parts[1].trim());
                    int fontSize = Integer.parseInt(parts[2].trim());
                    Font font = new Font(fontName, fontStyle, fontSize);
                    textArea.setFont(font);
                } else if (line.startsWith("BG_Image:")) {
                    String path = line.substring("BG_Image:".length()).trim();
                    img_path = path;
                    textArea.setImage(img_path);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format in settings file.");
            e.printStackTrace();
        }
    }

    public void compileFile(){
        if(firstTimeCompile || log.isOpen == false){
            log.setVisible(true);
            log.isLog = true;
            log.isOpen = true;
            firstTimeCompile = true;
        }
        try{
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if(compiler == null){
                log.print("compiler not available");
                return;
            }
            int exitCode = compiler.run(null, null, null, currentFile.getAbsolutePath());
            log.print(exitCode == 0 ? "Compilation succeeded.\n" : "Compilation failed with exit code "+exitCode+".\n");
        }catch(Exception e){
            log.print("Exception: "+e.getMessage());
        }
    }

    private void runFile(){
        compileFile();
        String className = new File(currentFile.getAbsolutePath()).getName().replace(".java", "");
        try{
            ProcessBuilder process = new ProcessBuilder("java -cp \""+currentFile.getParentFile().getAbsolutePath()+"\" "+className);
            process.redirectErrorStream(true);
            Process p = process.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.print(line+"\n");
                }
            }
            p.waitFor();
        }catch(Exception e){
            log.print("Exception: "+e.getMessage());
        }
        log.print(className);
    }

    private void changeBackgroundColor() {
        Color newColor = JColorChooser.showDialog(this, "Choose Background Color", textArea.getBackground());
        if (newColor != null) {
            bgColor = newColor;
            textArea.removeImage();
            img_path = null;
            textArea.setBackground(newColor);
            textArea.repaint();
        }
    }

    private void changeUniversalBackgroundColor() {
        Color newColor = JColorChooser.showDialog(this, "Choose Universal Background Color", textArea.getBackground());
        if (newColor != null) {
            universalBgColor = newColor;
            textArea.removeImage();
            img_path = null;
            textArea.setBackground(newColor);
            textArea.repaint();
        }
    }

    private void changeTextColor() {
        Color newColor = JColorChooser.showDialog(this, "Choose Text Color", textArea.getForeground());
        if (newColor != null) {
            textColor = newColor;
            textArea.setForeground(newColor);
        }
    }

    private void changeUniversalTextColor() {
        Color newColor = JColorChooser.showDialog(this, "Choose Text Color", textArea.getForeground());
        if (newColor != null) {
            universalTextColor = newColor;
            textArea.setForeground(newColor);
        }
    }

    private void changeFont() {
        JDialog fontDialog = new JDialog(this, "Change Font", true);
        fontDialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel fontLabel = new JLabel("Select Font: ");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        fontDialog.add(fontLabel, gbc);

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();
        JComboBox<String> fontBox = new JComboBox<>(fontNames);
        fontBox.setEditable(true);
        fontBox.setSelectedItem(textArea.getFont().getFamily());
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        fontDialog.add(fontBox, gbc);

        JLabel styleLabel = new JLabel("Font Style: ");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        fontDialog.add(styleLabel, gbc);

        String[] styles = {"Regular", "Bold", "Italic", "Bold Italic"};
        JComboBox<String> styleBox = new JComboBox<>(styles);
        styleBox.setEditable(true);
        styleBox.setSelectedIndex(textArea.getFont().getStyle());
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        fontDialog.add(styleBox, gbc);

        JLabel sizeLabel = new JLabel("Font Size: ");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        fontDialog.add(sizeLabel, gbc);

        Integer[] sizes = {8, 10, 12, 14, 16, 18, 20, 24, 28, 32, 36, 40, 48, 56, 64, 72};
        JComboBox<Integer> sizeBox = new JComboBox<>(sizes);
        sizeBox.setEditable(true);
        sizeBox.setSelectedItem(textArea.getFont().getSize());
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        fontDialog.add(sizeBox, gbc);

        JLabel sampleLabel = new JLabel("Sample Text");
        sampleLabel.setFont(font);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        fontDialog.add(sampleLabel, gbc);

        fontBox.addActionListener(e -> updateSampleFont(fontBox, styleBox, sizeBox, sampleLabel));
        styleBox.addActionListener(e -> updateSampleFont(fontBox, styleBox, sizeBox, sampleLabel));
        sizeBox.addActionListener(e -> updateSampleFont(fontBox, styleBox, sizeBox, sampleLabel));

        JPanel buttonPanel = new JPanel();
        JButton chooseFontButton = new JButton("Choose Font from PC");
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        chooseFontButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Font Files", "ttf", "otf");
            fileChooser.setFileFilter(filter);
            int returnValue = fileChooser.showOpenDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File fontFile = fileChooser.getSelectedFile();
                try {
                    Font customFont = Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(Font.PLAIN, 12); // Default size
                    GraphicsEnvironment ge1 = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    ge1.registerFont(customFont);
                    fontBox.addItem(customFont.getName()); // Add the loaded font to the JComboBox
                    fontBox.setSelectedItem(customFont.getName());
                } catch (IOException | FontFormatException ex) {
                    ex.printStackTrace();
                }
            }
        });

        okButton.addActionListener(e -> {
            String selectedFont = (String) fontBox.getSelectedItem();
            int selectedStyle = styleBox.getSelectedIndex();
            int selectedSize = (int) sizeBox.getSelectedItem();
            Font newFont = new Font(selectedFont, selectedStyle, selectedSize);
            textArea.setFont(newFont);
            updateStatusBar();
            fontDialog.dispose();
        });

        cancelButton.addActionListener(e -> fontDialog.dispose());

        buttonPanel.add(chooseFontButton);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        fontDialog.add(buttonPanel, gbc);

        fontDialog.pack();
        fontDialog.setLocationRelativeTo(this);
        fontDialog.setVisible(true);
    }

    private void updateSampleFont(JComboBox<String> fontBox, JComboBox<String> styleBox, JComboBox<Integer> sizeBox, JLabel sampleLabel) {
        String selectedFont = (String) fontBox.getSelectedItem();
        int selectedStyle = styleBox.getSelectedIndex();
        int selectedSize = (int) sizeBox.getSelectedItem();
        sampleLabel.setFont(new Font(selectedFont, selectedStyle, selectedSize));
    }

    private void newFile() {
        textArea.setText("");
        currentFile = null;
        isModified = false;
        updateTitle();
    }

    private void newWindow() {
        SwingUtilities.invokeLater(() -> {
            Notes newEditor = new Notes();
            newEditor.setVisible(true);
        });
    }

    private void openFile() {
        boolean a = ch.showOpenDialog(this);
        if(a){
            currentFile = ch.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))){
                textArea.read(reader, null);
                String s = textArea.getText();
                if(s.indexOf("\u200F") == -1/* || s.indexOf("\u200B") > 50*//**){
                    return;
                }
                StringTokenizer st = new StringTokenizer(s.substring(0,s.indexOf("\u200F")),"\u200D");
                s = s.substring(s.indexOf("\u200F")+1);
                String fs = s;
                String[] k = new String[st.countTokens()];
                for(int i = 0;st.hasMoreTokens();i++){
                    k[i] = st.nextToken();
                }
                if(k[2].equals("<\uFEFF>")){
                    backup = true;
                    backupItem.setText("Disable Backup");
                    textArea.setText(s);
                }
                if(!(k[3].equals("<null>"))){
                    String c = k[3];
                    c = c.substring(1,c.length()-1);
                    bgColor = Color.decode(c);
                    textArea.setBackground(bgColor);
                }else{
                    bgColor = null;
                    textArea.setBackground(universalBgColor);
                }
                if(!(k[4].equals("<null>"))){
                    String c = k[4];
                    c = c.substring(1,c.length()-1);
                    textColor = Color.decode(c);
                    textArea.setForeground(textColor);
                }else{
                    textColor = null;
                    textArea.setForeground(universalTextColor);
                }
                if(!(k[0].equals("<null>"))){
                    encrypt = true;
                    encryptItem.setText("Disable Encryption");
                    byte seed = Byte.parseByte(k[1]);
                    Decrypter d = new Decrypter();
                    String sd = d.decrypt(s,seed);
                    fs = sd;
                }
                textArea.setText(fs);
                /*if(s.indexOf("(X):"+"\u200C"+"E{]>") == 0){
                encrypt = true;
                encryptItem.setText("Disable Encryption");
                s = s.substring(("(X):"+"\u200C"+"E{]>").length());
                byte seed = Byte.parseByte(s.substring(0,2));
                s = s.substring(2);
                Decrypter d = new Decrypter();
                String sd = d.decrypt(s,seed);
                textArea.setText(sd);
                }else if(s.indexOf("<\uFEFF>"+"(X):"+"\u200C"+"E{]>") == 0){
                backup = true;
                encrypt = true;
                encryptItem.setText("Disable Encryption");
                backupItem.setText("Disable Backup");
                s = s.substring(("<\uFEFF>"+"(X):"+"\u200C"+"E{]>").length());
                byte seed = Byte.parseByte(s.substring(0,2));
                s = s.substring(2);
                Decrypter d = new Decrypter();
                String sd = d.decrypt(s,seed);
                textArea.setText(sd);
                }else if(s.indexOf("<\uFEFF>") == 0){
                backup = true;
                backupItem.setText("Disable Backup");
                s = s.substring(1);
                textArea.setText(s);
                }*//**
            } catch (IOException e) {
                e.printStackTrace();
            }
            isModified = false;
            updateTitle();
        }
    }

    private void openFile(String filePath) {
        currentFile = new File(filePath);
        try(BufferedReader reader = new BufferedReader(new FileReader(currentFile))){
            textArea.read(reader, null);
            String s = textArea.getText();
            if(s.indexOf("\u200F") == -1/* || s.indexOf("\u200B") > 50*//**){
                return;
            }
            StringTokenizer st = new StringTokenizer(s.substring(0,s.indexOf("\u200F")),"\u200D");
            s = s.substring(s.indexOf("\u200F")+1);
            String fs = s;
            String[] k = new String[st.countTokens()];
            for(int i = 0;st.hasMoreTokens();i++){
                k[i] = st.nextToken();
            }
            if(!(k[2].equals("<null>"))){
                backup = true;
                backupItem.setText("Disable Backup");
                textArea.setText(s);
            }
            if(!(k[3].equals("<null>"))){
                String c = k[3];
                c = c.substring(1,c.length()-1);
                bgColor = Color.decode(c);
                textArea.setBackground(bgColor);
            }else{
                bgColor = null;
                textArea.setBackground(universalBgColor);
            }
            if(!(k[4].equals("<null>"))){
                String c = k[4];
                c = c.substring(1,c.length()-1);
                textColor = Color.decode(c);
                textArea.setForeground(textColor);
            }else{
                textColor = null;
                textArea.setForeground(universalTextColor);
            }
            if(!(k[0].equals("<null>"))){
                encrypt = true;
                encryptItem.setText("Disable Encryption");
                byte seed = Byte.parseByte(k[1]);
                Decrypter d = new Decrypter();
                String sd = d.decrypt(s,seed);
                fs = sd;
            }
            textArea.setText(fs);
            /*if(s.indexOf("(X):"+"\u200C"+"E{]>") == 0){
            encrypt = true;
            encryptItem.setText("Disable Encryption");
            s = s.substring(("(X):"+"\u200C"+"E{]>").length());
            byte seed = Byte.parseByte(s.substring(0,2));
            s = s.substring(2);
            Decrypter d = new Decrypter();
            String sd = d.decrypt(s,seed);
            textArea.setText(sd);
            }else if(s.indexOf("<\uFEFF>"+"(X):"+"\u200C"+"E{]>") == 0){
            backup = true;
            encrypt = true;
            encryptItem.setText("Disable Encryption");
            backupItem.setText("Disable Backup");
            s = s.substring(("<\uFEFF>"+"(X):"+"\u200C"+"E{]>").length());
            byte seed = Byte.parseByte(s.substring(0,2));
            s = s.substring(2);
            Decrypter d = new Decrypter();
            String sd = d.decrypt(s,seed);
            textArea.setText(sd);
            }else if(s.indexOf("<\uFEFF>") == 0){
            backup = true;
            backupItem.setText("Disable Backup");
            s = s.substring(1);
            textArea.setText(s);
            }*//**
        } catch (IOException e) {
            e.printStackTrace();
        }
        isModified = false;
        updateTitle();
    }

    private void saveFile() {
        // init line prototype <encrypted><backup><bgColor><TextColor>
        if (currentFile != null) {
            String sp = "\u200D";
            String s = "";
            if(!config){
                try(BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))){
                    String s1 = textArea.getText();
                    if(encrypt){
                        Encrypter e = new Encrypter();
                        Byte seed = e.seed;
                        textArea.setText(e.encrypt(textArea.getText()));
                        JOptionPane.showMessageDialog(this,"The Seed For The Encrypted Text Is: "+seed, "Encrypted Text Seed Info", JOptionPane.PLAIN_MESSAGE);
                    }
                    textArea.write(writer);
                    textArea.setText(s1);
                }catch(Exception e){
                    e.printStackTrace();
                }

                if(backup){
                    SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yyyy__HH_mm_ss");
                    String currentTimeDate = "["+sdf.format(new Date())+"]";
                    File backupFile = new File(backupDir+"\\"+currentTimeDate+currentFile.getAbsolutePath().substring(currentFile.getAbsolutePath().lastIndexOf("\\")+1));
                    if(!(backupFile.exists())){
                        try{
                            Files.createFile(Paths.get(backupDir+"\\"+currentTimeDate+currentFile.getAbsolutePath().substring(currentFile.getAbsolutePath().lastIndexOf("\\")+1)));
                        }catch (IOException ioe){
                            ioe.printStackTrace();
                        }
                    }
                    try(BufferedWriter writer = new BufferedWriter(new FileWriter(backupFile))){
                        if(encrypt){
                            //"(X):"+"\u200C"+"E{]>" , this is the encryption identifier
                            s = ei+sp;// <\uFEFF> is the identification ghost character for backup determining
                            Encrypter e = new Encrypter();
                            s = s+e.seed+sp;
                            s = s+"<"+"\uFEFF"+">"+sp;
                            if(bgColor != null){
                                s = s+"<"+String.format("#%06X", bgColor.getRGB() & 0xFFFFFF)+">"+sp;
                            }else{
                                s = s+"<null>"+sp;
                            }
                            if(textColor != null){
                                s = s+"<"+String.format("#%06X", textColor.getRGB() & 0xFFFFFF)+">"+sp;
                            }else{
                                s = s+"<null>"+sp;
                            }
                            s = s+"\u200F"+e.encrypt(textArea.getText());
                            String s1 = textArea.getText();
                            textArea.setText(s);
                            textArea.write(writer);
                            textArea.setText(s1);
                        }else{
                            s = "<null>"+sp+"<null>"+sp;
                            s = s+"<\uFEFF>"+sp;
                            if(bgColor != null){
                                s = s+"<"+String.format("#%06X", bgColor.getRGB() & 0xFFFFFF)+">"+sp;
                            }else{
                                s = s+"<null>"+sp;
                            }
                            if(textColor != null){
                                s = s+"<"+String.format("#%06X", textColor.getRGB() & 0xFFFFFF)+">"+sp;
                            }else{
                                s = s+"<null>"+sp;
                            }
                            s = s+"\u200F"+textArea.getText();
                            String s1 = textArea.getText();
                            textArea.setText(s);
                            textArea.write(writer);
                            textArea.setText(s1);
                        }
                        isModified = false;
                        updateTitle();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                isModified = false;
                updateTitle();
                return;
            }
            try(BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))){
                if(encrypt){
                    s = ei+sp;
                    Encrypter e = new Encrypter();
                    s = s+(e.seed)+sp;
                    if(backup){
                        s = s+"<\uFEFF>"+sp;
                    }else{
                        s = s+"<null>"+sp;
                    }
                    if(bgColor != null){
                        s = s+"<"+String.format("#%06X", bgColor.getRGB() & 0xFFFFFF)+">"+sp;
                    }else{
                        s = s+"<null>"+sp;
                    }
                    if(textColor != null){
                        s = s+"<"+String.format("#%06X", textColor.getRGB() & 0xFFFFFF)+">"+sp;
                    }else{
                        s = s+"<null>"+sp;
                    }
                    s = s+"\u200F"+e.encrypt(textArea.getText());
                    String s1 = textArea.getText();
                    textArea.setText(s);
                    textArea.write(writer);
                    textArea.setText(s1);
                }else{
                    s = "<null>"+sp+"<null>"+sp;
                    if(backup){
                        s = s+"<\uFEFF>"+sp;
                    }else{
                        s = s+"<null>"+sp;
                    }
                    if(bgColor != null){
                        s = s+"<"+String.format("#%06X", bgColor.getRGB() & 0xFFFFFF)+">"+sp;
                    }else{
                        s = s+"<null>"+sp;
                    }
                    if(textColor != null){
                        s = s+"<"+String.format("#%06X", textColor.getRGB() & 0xFFFFFF)+">"+sp;
                    }else{
                        s = s+"<null>"+sp;
                    }
                    s = s+"\u200F"+textArea.getText();
                    String s1 = textArea.getText();
                    textArea.setText(s);
                    textArea.write(writer);
                    textArea.setText(s1);
                }
                isModified = false;
                updateTitle();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(backup){
                SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yyyy__HH_mm_ss");
                String currentTimeDate = "["+sdf.format(new Date())+"]";
                File backupFile = new File(backupDir+"\\"+currentTimeDate+currentFile.getAbsolutePath().substring(currentFile.getAbsolutePath().lastIndexOf("\\")+1));
                if(!(backupFile.exists())){
                    try{
                        Files.createFile(Paths.get(backupDir+"\\"+currentTimeDate+currentFile.getAbsolutePath().substring(currentFile.getAbsolutePath().lastIndexOf("\\")+1)));
                    }catch (IOException ioe){
                        ioe.printStackTrace();
                    }
                }
                try(BufferedWriter writer = new BufferedWriter(new FileWriter(backupFile))){
                    if(encrypt){
                        //"(X):"+"\u200C"+"E{]>" , this is the encryption identifier
                        s = ei+sp;// <\uFEFF> is the identification ghost character for backup determining
                        Encrypter e = new Encrypter();
                        s = s+e.seed+sp;
                        s = s+"<"+"\uFEFF"+">"+sp;
                        if(bgColor != null){
                            s = s+"<"+String.format("#%06X", bgColor.getRGB() & 0xFFFFFF)+">"+sp;
                        }else{
                            s = s+"<null>"+sp;
                        }
                        if(textColor != null){
                            s = s+"<"+String.format("#%06X", textColor.getRGB() & 0xFFFFFF)+">"+sp;
                        }else{
                            s = s+"<null>"+sp;
                        }
                        s = s+"\u200F"+e.encrypt(textArea.getText());
                        String s1 = textArea.getText();
                        textArea.setText(s);
                        textArea.write(writer);
                        textArea.setText(s1);
                    }else{
                        s = "<null>"+sp+"<null>"+sp;
                        s = s+"<\uFEFF>"+sp;
                        if(bgColor != null){
                            s = s+"<"+String.format("#%06X", bgColor.getRGB() & 0xFFFFFF)+">"+sp;
                        }else{
                            s = s+"<null>"+sp;
                        }
                        if(textColor != null){
                            s = s+"<"+String.format("#%06X", textColor.getRGB() & 0xFFFFFF)+">"+sp;
                        }else{
                            s = s+"<null>"+sp;
                        }
                        s = s+"\u200F"+textArea.getText();
                        String s1 = textArea.getText();
                        textArea.setText(s);
                        textArea.write(writer);
                        textArea.setText(s1);
                    }
                    isModified = false;
                    updateTitle();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            saveFileAs();
        }
    }

    private void saveFileAs() {
        boolean c = ch.showSaveDialog(this);
        if(c) {
            currentFile = ch.getSelectedFile();
            char sp = '\u200D';
            String s = "";
            if(!config){
                try(BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))){
                    String s1 = textArea.getText();
                    if(encrypt){
                        Encrypter e = new Encrypter();
                        Byte seed = e.seed;
                        textArea.setText(e.encrypt(textArea.getText()));
                        JOptionPane.showMessageDialog(this,"The Seed For The Encrypted Text Is: "+seed, "Encrypted Text Seed Info", JOptionPane.PLAIN_MESSAGE);
                    }
                    textArea.write(writer);
                    textArea.setText(s1);
                }catch(Exception e){
                    e.printStackTrace();
                }

                if(backup){
                    SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yyyy__HH_mm_ss");
                    String currentTimeDate = "["+sdf.format(new Date())+"]";
                    File backupFile = new File(backupDir+"\\"+currentTimeDate+currentFile.getAbsolutePath().substring(currentFile.getAbsolutePath().lastIndexOf("\\")+1));
                    if(!(backupFile.exists())){
                        try{
                            Files.createFile(Paths.get(backupDir+"\\"+currentTimeDate+currentFile.getAbsolutePath().substring(currentFile.getAbsolutePath().lastIndexOf("\\")+1)));
                        }catch (IOException ioe){
                            ioe.printStackTrace();
                        }
                    }
                    try(BufferedWriter writer = new BufferedWriter(new FileWriter(backupFile))){
                        if(encrypt){
                            //"(X):"+"\u200C"+"E{]>" , this is the encryption identifier
                            s = ei+sp;// <\uFEFF> is the identification ghost character for backup determining
                            Encrypter e = new Encrypter();
                            s = s+e.seed+sp;
                            s = s+"<"+"\uFEFF"+">"+sp;
                            if(bgColor != null){
                                s = s+"<"+String.format("#%06X", bgColor.getRGB() & 0xFFFFFF)+">"+sp;
                            }else{
                                s = s+"<null>"+sp;
                            }
                            if(textColor != null){
                                s = s+"<"+String.format("#%06X", textColor.getRGB() & 0xFFFFFF)+">"+sp;
                            }else{
                                s = s+"<null>"+sp;
                            }
                            s = s+"\u200F"+e.encrypt(textArea.getText());
                            String s1 = textArea.getText();
                            textArea.setText(s);
                            textArea.write(writer);
                            textArea.setText(s1);
                        }else{
                            s = "<null>"+sp+"<null>"+sp;
                            s = s+"<\uFEFF>"+sp;
                            if(bgColor != null){
                                s = s+"<"+String.format("#%06X", bgColor.getRGB() & 0xFFFFFF)+">"+sp;
                            }else{
                                s = s+"<null>"+sp;
                            }
                            if(textColor != null){
                                s = s+"<"+String.format("#%06X", textColor.getRGB() & 0xFFFFFF)+">"+sp;
                            }else{
                                s = s+"<null>"+sp;
                            }
                            s = s+"\u200F"+textArea.getText();
                            String s1 = textArea.getText();
                            textArea.setText(s);
                            textArea.write(writer);
                            textArea.setText(s1);
                        }
                        isModified = false;
                        updateTitle();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                isModified = false;
                updateTitle();
                return;
            }
            try(BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))){
                if(encrypt){

                    //"(X):"+"\u200C"+"E{]>" , this is the encryption identifier
                    s = ei+sp;
                    Encrypter e = new Encrypter();
                    s = s+(e.seed)+sp;
                    if(backup){
                        s = s+"<\uFEFF>"+sp;
                    }else{
                        s = s+"<null>"+sp;
                    }
                    if(bgColor != null){
                        s = s+"<"+String.format("#%06X", bgColor.getRGB() & 0xFFFFFF)+">"+sp;
                    }else{
                        s = s+"<null>"+sp;
                    }
                    if(textColor != null){
                        s = s+"<"+String.format("#%06X", textColor.getRGB() & 0xFFFFFF)+">"+sp;
                    }else{
                        s = s+"<null>"+sp;
                    }
                    s = s+"\u200F"+e.encrypt(textArea.getText());
                    String s1 = textArea.getText();
                    textArea.setText(s);
                    textArea.write(writer);
                    textArea.setText(s1);
                }else{
                    s = "<null>"+sp+"<null>"+sp;
                    if(backup){
                        s = s+"<\uFEFF>"+sp;
                    }else{
                        s = s+"<null>"+sp;
                    }
                    if(bgColor != null){
                        s = s+"<"+String.format("#%06X", bgColor.getRGB() & 0xFFFFFF)+">"+sp;
                    }else{
                        s = s+"<null>"+sp;
                    }
                    if(textColor != null){
                        s = s+"<"+String.format("#%06X", textColor.getRGB() & 0xFFFFFF)+">"+sp;
                    }else{
                        s = s+"<null>"+sp;
                    }
                    s = s+"\u200F"+textArea.getText();
                    String s1 = textArea.getText();
                    textArea.setText(s);
                    textArea.write(writer);
                    textArea.setText(s1);
                }
                isModified = false;
                updateTitle();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(backup){
                SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yyyy__HH_mm_ss");
                String currentTimeDate = "["+sdf.format(new Date())+"]";
                File backupFile = new File(backupDir+"\\"+currentTimeDate+currentFile.getAbsolutePath().substring(currentFile.getAbsolutePath().lastIndexOf("\\")+1));
                if(!(backupFile.exists())){
                    try{
                        Files.createFile(Paths.get(backupDir+"\\"+currentTimeDate+currentFile.getAbsolutePath().substring(currentFile.getAbsolutePath().lastIndexOf("\\")+1)));
                    }catch (IOException ioe){
                        ioe.printStackTrace();
                    }
                }
                try(BufferedWriter writer = new BufferedWriter(new FileWriter(backupFile))){
                    if(encrypt){
                        //"(X):"+"\u200C"+"E{]>" , this is the encryption identifier
                        s = ei+sp;// <\uFEFF> is the identification ghost character for backup determining
                        Encrypter e = new Encrypter();
                        s = s+e.seed+sp;
                        s = s+"<"+"\uFEFF"+">"+sp;
                        if(bgColor != null){
                            s = s+"<"+String.format("#%06X", bgColor.getRGB() & 0xFFFFFF)+">"+sp;
                        }else{
                            s = s+"<null>"+sp;
                        }
                        if(textColor != null){
                            s = s+"<"+String.format("#%06X", textColor.getRGB() & 0xFFFFFF)+">"+sp;
                        }else{
                            s = s+"<null>"+sp;
                        }
                        s = s+"\u200F"+e.encrypt(textArea.getText());
                        String s1 = textArea.getText();
                        textArea.setText(s);
                        textArea.write(writer);
                        textArea.setText(s1);
                    }else{
                        s = "<null>"+sp+"<null>"+sp;
                        s = s+"<\uFEFF>"+sp;
                        if(bgColor != null){
                            s = s+"<"+String.format("#%06X", bgColor.getRGB() & 0xFFFFFF)+">"+sp;
                        }else{
                            s = s+"<null>"+sp;
                        }
                        if(textColor != null){
                            s = s+"<"+String.format("#%06X", textColor.getRGB() & 0xFFFFFF)+">"+sp;
                        }else{
                            s = s+"<null>"+sp;
                        }
                        s = s+"\u200F"+textArea.getText();
                        String s1 = textArea.getText();
                        textArea.setText(s);
                        textArea.write(writer);
                        textArea.setText(s1);
                    }
                    isModified = false;
                    updateTitle();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String currentDateAndTime(){
        Date date = new Date();
        String formattedDate = formatter.format(date);
        return formattedDate;
    }

    private void updateTitle() {
        if(currentFile != null) {
            String title = currentFile.getName() + " - Notes";
            if (isModified) {
                title = "*" + title;
            }
            setTitle(title);
        } else {
            setTitle("*untitled.txt - Notes");
        }
    }

    private void changeFontSize(int delta) {
        Font currentFont = textArea.getFont();
        float newSize = currentFont.getSize() + delta;
        if (newSize > 0) {
            textArea.setFont(currentFont.deriveFont(newSize));
            updateStatusBar();
        }
    }

    private void resetFontSize() {
        textArea.setFont(font.deriveFont(defaultFontSize));
        updateStatusBar();
    }

    private void updateStatusBar() {
        try {
            int line = textArea.getLineOfOffset(textArea.getCaretPosition()) + 1;
            int column = textArea.getCaretPosition() - textArea.getLineStartOffset(textArea.getLineOfOffset(textArea.getCaretPosition())) + 1;
            float fontSize = textArea.getFont().getSize();
            String statusText = String.format("Ln: %d, Col: %d, Font Size: %.0fpt",
                    line, column, fontSize);
            statusLabel.setText(statusText);
        } catch (javax.swing.text.BadLocationException e) {
            e.printStackTrace();
        }
    }

    private boolean promptForUnsavedChanges() {
        if(isModified) {
            // Custom options for the JOptionPane
            Object[] options = {"Save","Save Without Config", "Don't Save", "Cancel"};
            int choice = JOptionPane.showOptionDialog(this,
                    "You have unsaved changes. Do you want to save them before exiting?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);

            switch (choice) {
                case 0:
                    saveFile();
                    return !isModified; // If save was successful and isModified is false, then proceed to exit
                case 1:
                    config = false;
                    saveFile();
                    return !isModified; // If save was successful and isModified is false, then proceed to exit
                case 2:
                    return true; // Proceed to exit without saving
                case 3:
                default:
                    return false; // Cancel exit
            }
        }
        return true; // No unsaved changes, proceed to exit
    }

    private void showFindDialog() {
        JDialog findDialog = new JDialog(this, "Find", false);
        findDialog.setLayout(new BorderLayout());
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        JLabel findLabel = new JLabel("Find: ");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(findLabel, gbc);
        JTextField findText = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(findText, gbc);
        JCheckBox matchCase = new JCheckBox("Match Case");
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(matchCase, gbc);
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        JButton findNextButton = new JButton("Find Next");
        JButton findPrevButton = new JButton("Find Previous");
        JButton cancelButton = new JButton("Cancel");
        findNextButton.addActionListener(e -> findText(findText.getText(), matchCase.isSelected(), true));
        findPrevButton.addActionListener(e -> findText(findText.getText(), matchCase.isSelected(), false));
        cancelButton.addActionListener(e -> findDialog.dispose());
        buttonPanel.add(findNextButton);
        buttonPanel.add(findPrevButton);
        buttonPanel.add(cancelButton);
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(buttonPanel, gbc);
        findDialog.add(panel, BorderLayout.CENTER);
        findDialog.pack();
        findDialog.setLocationRelativeTo(this);
        findDialog.setVisible(true);
    }

    private void showReplaceDialog() {
        JDialog replaceDialog = new JDialog(this, "Replace", false);
        replaceDialog.setLayout(new BorderLayout());
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        JLabel findLabel = new JLabel("Find: ");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(findLabel, gbc);
        JTextField findText = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(findText, gbc);

        JLabel replaceLabel = new JLabel("Replace: ");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(replaceLabel, gbc);

        JTextField replaceText = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(replaceText, gbc);

        JCheckBox matchCase = new JCheckBox("Match Case");
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(matchCase, gbc);

        JPanel buttonPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        JButton findNextButton = new JButton("Find Next");
        JButton findPrevButton = new JButton("Find Previous");
        JButton replaceButton = new JButton("Replace");
        JButton replaceAllButton = new JButton("Replace All");
        JButton cancelButton = new JButton("Cancel");

        findNextButton.addActionListener(e -> findText(findText.getText(), matchCase.isSelected(), true));
        findPrevButton.addActionListener(e -> findText(findText.getText(), matchCase.isSelected(), false));
        replaceButton.addActionListener(e -> replaceText(findText.getText(), replaceText.getText(), matchCase.isSelected()));
        replaceAllButton.addActionListener(e -> replaceAllText(findText.getText(), replaceText.getText(), matchCase.isSelected()));
        cancelButton.addActionListener(e -> replaceDialog.dispose());

        buttonPanel.add(findNextButton);
        buttonPanel.add(findPrevButton);
        buttonPanel.add(replaceButton);
        buttonPanel.add(replaceAllButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 3;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(buttonPanel, gbc);

        replaceDialog.add(panel, BorderLayout.CENTER);
        replaceDialog.pack();
        replaceDialog.setLocationRelativeTo(this);
        replaceDialog.setVisible(true);
    }

    private void findText(String text, boolean matchCase, boolean forward) {
        String content = textArea.getText();
        String searchText = text;
        if (!matchCase) {
            content = content.toLowerCase();
            searchText = searchText.toLowerCase();
        }
        int startPos = textArea.getCaretPosition();
        int searchPos;
        if (forward) {
            searchPos = content.indexOf(searchText, startPos);
            if (searchPos == -1) {
                searchPos = content.indexOf(searchText, 0);
            }
        } else {
            searchPos = content.lastIndexOf(searchText, startPos - searchText.length() - 1);
            if (searchPos == -1) {
                searchPos = content.lastIndexOf(searchText);
            }
        }
        if (searchPos != -1) {
            textArea.setCaretPosition(searchPos);
            textArea.moveCaretPosition(searchPos + searchText.length());
        } else {
            JOptionPane.showMessageDialog(this, "Text not found.");
        }
    }

    private void replaceText(String findText, String replaceText, boolean matchCase) {
        String content = textArea.getText();
        String searchText = findText;

        if (!matchCase) {
            content = content.toLowerCase();
            searchText = searchText.toLowerCase();
        }

        int startPos = textArea.getCaretPosition();
        int searchPos = content.indexOf(searchText, startPos);

        if (searchPos != -1) {
            textArea.setCaretPosition(searchPos);
            textArea.moveCaretPosition(searchPos + searchText.length());
            textArea.replaceSelection(replaceText);
        } else {
            JOptionPane.showMessageDialog(this, "Text not found.");
        }
    }

    private void replaceAllText(String findText, String replaceText, boolean matchCase) {
        String content = textArea.getText();
        String searchText = findText;

        if (!matchCase) {
            content = content.toLowerCase();
            searchText = searchText.toLowerCase();
        }

        int startPos = 0;
        int searchPos = content.indexOf(searchText, startPos);

        while (searchPos != -1) {
            textArea.setCaretPosition(searchPos);
            textArea.moveCaretPosition(searchPos + searchText.length());
            textArea.replaceSelection(replaceText);
            startPos = searchPos + replaceText.length();
            content = textArea.getText();
            searchPos = content.indexOf(searchText, startPos);
        }
    }

    private void goToLine() {
        String lineStr = JOptionPane.showInputDialog(this, "Enter line number:", "Go To Line", JOptionPane.PLAIN_MESSAGE);
        if (lineStr != null) {
            try {
                int line = Integer.parseInt(lineStr);
                int pos = textArea.getLineStartOffset(line - 1);
                textArea.setCaretPosition(pos);
            } catch (NumberFormatException | javax.swing.text.BadLocationException e) {
                JOptionPane.showMessageDialog(this, "Invalid line number.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void insertTimeDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd-MM-yyyy");
        String currentTimeDate = sdf.format(new Date());
        textArea.insert("[" + currentTimeDate + "]", textArea.getCaretPosition());
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> {
            Notes editor = new Notes();
            editor.setVisible(true);
            if(args.length > 0){
                editor.openFile(args[0]);
            }
        });
    }
}
class TextArea extends JTextArea{
    Image img = null;
    TextArea(){
        //setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        if(!(img == null)){
            g.drawImage(img, 0, 0,getWidth(),getHeight(), null);
        }
    }

    public void setImage(String path){
        img = new ImageIcon(path).getImage();
        repaint();
    }

    public void removeImage(){
        img = null;
        repaint();
    }
}*/