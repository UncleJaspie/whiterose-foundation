package whiterose.foundation;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Keyboard;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PacketScriptTool {
    public static KeyBinding keyInspector = new KeyBinding("Scripting", Keyboard.KEY_P, "Whiterose Foundation");

    private static JTextArea console;
    private static JTree classTree;
    private static JList<String> memberList;
    private static JComboBox<ModContainer> modSelector;
    private static JComboBox<String> targetSelector;

    // Mapping storage inspired by the JS Deep Mapper
    private static final Map<String, String> mcpMap = new HashMap<>();
    private static final Map<String, Method> methodMap = new HashMap<>();

    public static void init() {
        ClientRegistry.registerKeyBinding(keyInspector);
        FMLCommonHandler.instance().bus().register(new PacketScriptTool());
        loadMappings();
    }

    private static void loadMappings() {
        mcpMap.clear();
        // Try internal jar assets first
        loadCsv("/assets/mappings/methods.csv");
        loadCsv("/assets/mappings/fields.csv");
        loadCsv("/assets/mappings/params.csv");

        // Try external desktop path as a fallback (like your JS script)
        File externalDir = new File(System.getProperty("user.home") + "/Desktop/Essentials/Tools/mappings/");
        if (externalDir.exists()) {
            loadCsvFile(new File(externalDir, "methods.csv"));
            loadCsvFile(new File(externalDir, "fields.csv"));
            loadCsvFile(new File(externalDir, "params.csv"));
        }
    }

    private static void loadCsv(String path) {
        try (InputStream is = PacketScriptTool.class.getResourceAsStream(path)) {
            if (is == null) return;
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            parseReader(br);
        } catch (Exception ignored) {}
    }

    private static void loadCsvFile(File file) {
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            parseReader(br);
        } catch (Exception ignored) {}
    }

    private static void parseReader(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",");
            if (parts.length >= 2) {
                String searge = parts[0].trim();
                String readable = parts[1].trim();
                if (!readable.isEmpty()) mcpMap.put(searge, readable);
            }
        }
    }

    public static void log(String msg) {
        if (console != null) {
            SwingUtilities.invokeLater(() -> {
                console.append("\n" + msg);
                console.setCaretPosition(console.getDocument().getLength());
            });
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (keyInspector.isPressed() && Minecraft.getMinecraft().currentScreen == null) {
            SwingUtilities.invokeLater(this::createUI);
        }
    }

    private void createUI() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        JFrame frame = new JFrame("Packet Tool & Deep Inspector");
        frame.setSize(1200, 900);
        frame.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        modSelector = new JComboBox<>(Loader.instance().getActiveModList().toArray(new ModContainer[0]));
        JButton btnScan = new JButton("Scan Mod");
        JButton btnReloadMaps = new JButton("Reload Mappings");

        topPanel.add(new JLabel("Mod:"));
        topPanel.add(modSelector);
        topPanel.add(btnScan);
        topPanel.add(btnReloadMaps);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Select a Mod");
        classTree = new JTree(root);
        JScrollPane treeScroll = new JScrollPane(classTree);
        treeScroll.setBorder(new TitledBorder("Explorer"));

        JPanel rightPanel = new JPanel(new BorderLayout());
        memberList = new JList<>();
        memberList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        memberList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) showMethodSource();
            }
        });

        JScrollPane memberScroll = new JScrollPane(memberList);
        memberScroll.setBorder(new TitledBorder("Members (Mapped Names in Brackets)"));

        JPanel execPanel = new JPanel(new GridLayout(0, 1));
        execPanel.setBorder(new TitledBorder("Invocation Settings"));
        targetSelector = new JComboBox<>(new String[]{"Static", "Local Player", "New Instance", "Looking At"});
        JButton btnInvoke = new JButton("Invoke Selected Method");
        btnInvoke.setPreferredSize(new Dimension(0, 45));
        btnInvoke.setBackground(new Color(100, 150, 255));
        btnInvoke.setForeground(Color.WHITE);
        execPanel.add(targetSelector);
        execPanel.add(btnInvoke);

        rightPanel.add(memberScroll, BorderLayout.CENTER);
        rightPanel.add(execPanel, BorderLayout.SOUTH);

        mainSplit.setLeftComponent(treeScroll);
        mainSplit.setRightComponent(rightPanel);
        mainSplit.setDividerLocation(350);

        console = new JTextArea(10, 50);
        console.setEditable(false);
        console.setBackground(new Color(15, 15, 15));
        console.setForeground(new Color(0, 255, 120));
        console.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane consoleScroll = new JScrollPane(console);

        btnScan.addActionListener(e -> scanMod((ModContainer) modSelector.getSelectedItem()));
        btnReloadMaps.addActionListener(e -> { loadMappings(); log("Mappings reloaded."); });
        classTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) classTree.getLastSelectedPathComponent();
            if (node != null && node.isLeaf()) loadClassMembers(node.getUserObject().toString());
        });
        btnInvoke.addActionListener(e -> showInvokePopup());

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(mainSplit, BorderLayout.CENTER);
        frame.add(consoleScroll, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void loadClassMembers(String className) {
        DefaultListModel<String> model = new DefaultListModel<>();
        methodMap.clear();
        try {
            Class<?> clazz = Class.forName(className);
            model.addElement("--- FIELDS ---");
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                String mapped = mcpMap.getOrDefault(f.getName(), "No Mapping");
                model.addElement(String.format("F: %-20s | %s (%s)", f.getName(), mapped, f.getType().getSimpleName()));
            }
            model.addElement("");
            model.addElement("--- METHODS ---");
            for (Method m : clazz.getDeclaredMethods()) {
                m.setAccessible(true);

                // Construct readable signature
                StringBuilder params = new StringBuilder();
                Class<?>[] pTypes = m.getParameterTypes();
                for (int i = 0; i < pTypes.length; i++) {
                    params.append(pTypes[i].getSimpleName());
                    if (i < pTypes.length - 1) params.append(", ");
                }

                String mappedName = mcpMap.getOrDefault(m.getName(), m.getName());
                String display = String.format("M: %s(%s) -> %s [%s]",
                        m.getName(), params.toString(), m.getReturnType().getSimpleName(), mappedName);

                model.addElement(display);
                methodMap.put(display, m);
            }
            memberList.setModel(model);
        } catch (Exception e) { log("Reflection Error: " + e.getMessage()); }
    }

    private void showMethodSource() {
        String selected = memberList.getSelectedValue();
        if (selected == null || !methodMap.containsKey(selected)) return;
        Method m = methodMap.get(selected);

        JFrame srcFrame = new JFrame("Runtime Inspection: " + m.getName());
        srcFrame.setSize(700, 500);
        JTextArea srcArea = new JTextArea();
        srcArea.setBackground(new Color(30, 30, 30));
        srcArea.setForeground(new Color(220, 220, 220));
        srcArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        srcArea.setEditable(false);

        StringBuilder sb = new StringBuilder();
        sb.append("// Metadata Analysis for ").append(m.getName()).append("\n");
        sb.append("// Mapped Name: ").append(mcpMap.getOrDefault(m.getName(), "???")).append("\n\n");

        sb.append(Modifier.toString(m.getModifiers())).append(" ")
                .append(m.getReturnType().getSimpleName()).append(" ")
                .append(m.getName()).append("(");

        Class<?>[] params = m.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            // Check for param mappings (p_xxxxx_x -> name)
            String paramName = "arg" + i;
            // Note: JVM doesn't keep param names usually, but we can guess if we had params.csv
            sb.append(params[i].getSimpleName()).append(" ").append(paramName);
            if (i < params.length - 1) sb.append(", ");
        }
        sb.append(") {\n");
        sb.append("    // Parameters detected:\n");
        for(Class<?> pt : params) {
            sb.append("    // -> ").append(pt.getName()).append("\n");
            if (pt.getName().contains("net.minecraft")) sb.append("    //    [Vanilla Object detected]\n");
        }
        sb.append("\n    /* Use 'Invoke' to see return values or side effects. */\n");
        sb.append("}");

        srcArea.setText(sb.toString());
        srcFrame.add(new JScrollPane(srcArea));
        srcFrame.setLocationRelativeTo(null);
        srcFrame.setVisible(true);
    }

    private void showInvokePopup() {
        String selected = memberList.getSelectedValue();
        if (selected == null || !methodMap.containsKey(selected)) return;
        Method m = methodMap.get(selected);
        Class<?>[] pTypes = m.getParameterTypes();

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel inputGrid = new JPanel(new GridLayout(0, 2, 5, 5));
        JTextField[] inputs = new JTextField[pTypes.length];

        for (int i = 0; i < pTypes.length; i++) {
            String label = pTypes[i].getSimpleName();
            inputGrid.add(new JLabel(" " + label + " (arg" + i + "):"));
            inputs[i] = new JTextField();
            // Hinting for complex objects
            if (pTypes[i] == ItemStack.class) inputs[i].setToolTipText("Leave empty to use item from Chanter");
            inputGrid.add(inputs[i]);
        }

        panel.add(new JLabel("Invoking: " + m.getName()), BorderLayout.NORTH);
        panel.add(new JScrollPane(inputGrid), BorderLayout.CENTER);

        if (JOptionPane.showConfirmDialog(null, panel, "Invoke Method", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            executeInvoke(m, inputs);
        }
    }

    private void executeInvoke(Method m, JTextField[] inputs) {
        try {
            Object target = null;
            String mode = (String) targetSelector.getSelectedItem();
            if (mode.equals("Local Player")) target = Minecraft.getMinecraft().thePlayer;
            else if (mode.equals("New Instance")) target = m.getDeclaringClass().newInstance();
            else if (mode.equals("Looking At")) {
                MovingObjectPosition mop = Minecraft.getMinecraft().objectMouseOver;
                if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    target = Minecraft.getMinecraft().theWorld.getTileEntity(mop.blockX, mop.blockY, mop.blockZ);
                }
            }

            Object[] args = new Object[inputs.length];
            for (int i = 0; i < inputs.length; i++) {
                args[i] = parseArg(inputs[i].getText(), m.getParameterTypes()[i]);
            }

            long start = System.currentTimeMillis();
            Object res = m.invoke(target, args);
            long end = System.currentTimeMillis();

            log(String.format("[Invoke] %s completed in %dms. Result: %s",
                    m.getName(), (end-start), (res == null ? "void" : res.toString())));
        } catch (Exception e) {
            log("[Invoke Error] " + (e.getCause() != null ? e.getCause().toString() : e.getMessage()));
        }
    }

    private Object parseArg(String s, Class<?> t) {
        if (s.isEmpty()) {
            if (t == ItemStack.class) return ChanterSystem.getInstance().getCurrentItem();
            if (t == NBTTagCompound.class) return ChanterSystem.getInstance().getOutNBT();
            if (t == int.class || t == Integer.class) return 0;
            if (t == boolean.class || t == Boolean.class) return false;
            return null;
        }
        if (t == int.class || t == Integer.class) return Integer.parseInt(s);
        if (t == float.class || t == Float.class) return Float.parseFloat(s);
        if (t == double.class || t == Double.class) return Double.parseDouble(s);
        if (t == boolean.class || t == Boolean.class) return s.equalsIgnoreCase("true") || s.equals("1");
        if (t == String.class) return s;
        return null;
    }

    private void scanMod(ModContainer mod) {
        if (mod == null || mod.getSource() == null) return;
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(mod.getName());
        try (JarFile jar = new JarFile(mod.getSource())) {
            Enumeration<JarEntry> en = jar.entries();
            while (en.hasMoreElements()) {
                String entry = en.nextElement().getName();
                if (entry.endsWith(".class") && !entry.contains("$")) {
                    addNodeToTree(rootNode, entry.replace(".class", "").replace("/", "."));
                }
            }
            classTree.setModel(new DefaultTreeModel(rootNode));
            log("Scan complete for " + mod.getModId());
        } catch (Exception e) { log("Scan error: " + e.getMessage()); }
    }

    private void addNodeToTree(DefaultMutableTreeNode root, String className) {
        String[] parts = className.split("\\.");
        DefaultMutableTreeNode current = root;
        for (int i = 0; i < parts.length; i++) {
            DefaultMutableTreeNode found = null;
            for (int j = 0; j < current.getChildCount(); j++) {
                if (((DefaultMutableTreeNode) current.getChildAt(j)).getUserObject().toString().equals(parts[i])) {
                    found = (DefaultMutableTreeNode) current.getChildAt(j);
                    break;
                }
            }
            if (found == null) {
                found = new DefaultMutableTreeNode(parts[i]);
                current.add(found);
            }
            current = found;
            if (i == parts.length - 1) current.setUserObject(className);
        }
    }
}