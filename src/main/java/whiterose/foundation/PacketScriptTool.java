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
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PacketScriptTool {
    public static KeyBinding keyInspector = new KeyBinding("key.beenbtviewer.inspector", Keyboard.KEY_P, "key.categories.beenbtviewer");

    private static JTextArea console;
    private static JTree classTree;
    private static JList<String> memberList;
    private static JComboBox<ModContainer> modSelector;
    private static JComboBox<String> targetSelector;

    private static final Map<String, String> methodMappings = new HashMap<>();
    private static final Map<String, String> fieldMappings = new HashMap<>();
    private static final Map<String, String> paramMappings = new HashMap<>();
    private static final Map<String, Method> methodMap = new HashMap<>();

    public static void init() {
        ClientRegistry.registerKeyBinding(keyInspector);
        FMLCommonHandler.instance().bus().register(new PacketScriptTool());
        loadInternalMappings();
    }

    public static void log(String msg) {
        if (console != null) {
            SwingUtilities.invokeLater(() -> {
                console.append("\n" + msg);
                console.setCaretPosition(console.getDocument().getLength());
            });
        }
    }

    private static void loadInternalMappings() {
        methodMappings.clear();
        fieldMappings.clear();
        paramMappings.clear();
        loadCsvFromJar("/mappings/methods.csv", methodMappings);
        loadCsvFromJar("/mappings/fields.csv", fieldMappings);
        loadCsvFromJar("/mappings/params.csv", paramMappings);
    }

    private static void loadCsvFromJar(String path, Map<String, String> map) {
        try (InputStream is = PacketScriptTool.class.getResourceAsStream(path)) {
            if (is == null) return;
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) map.put(parts[0].trim(), parts[1].trim());
            }
        } catch (Exception ignored) {}
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (keyInspector.isPressed() && Minecraft.getMinecraft().currentScreen == null) {
            SwingUtilities.invokeLater(this::createUI);
        }
    }

    private void createUI() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        JFrame frame = new JFrame("Packet Tool & Xeno Inspector");
        frame.setSize(1100, 850);
        frame.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        modSelector = new JComboBox<>(Loader.instance().getActiveModList().toArray(new ModContainer[0]));
        JButton btnScan = new JButton("Scan Mod");
        JButton btnChanter = new JButton("Open Chanter");
        btnChanter.setBackground(new Color(180, 160, 255));

        topPanel.add(new JLabel("Mod:"));
        topPanel.add(modSelector);
        topPanel.add(btnScan);
        topPanel.add(btnChanter);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Select a Mod");
        classTree = new JTree(root);
        JScrollPane treeScroll = new JScrollPane(classTree);
        treeScroll.setBorder(new TitledBorder("Classes"));

        JPanel rightPanel = new JPanel(new BorderLayout());
        memberList = new JList<>();
        memberList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane memberScroll = new JScrollPane(memberList);
        memberScroll.setBorder(new TitledBorder("Members"));

        JPanel execPanel = new JPanel(new GridLayout(0, 1));
        execPanel.setBorder(new TitledBorder("Execution Target"));
        targetSelector = new JComboBox<>(new String[]{"Static", "Local Player", "New Instance", "Looking At"});
        JButton btnInvoke = new JButton("Invoke Selected");
        btnInvoke.setPreferredSize(new Dimension(0, 40));
        execPanel.add(targetSelector);
        execPanel.add(btnInvoke);

        rightPanel.add(memberScroll, BorderLayout.CENTER);
        rightPanel.add(execPanel, BorderLayout.SOUTH);

        mainSplit.setLeftComponent(treeScroll);
        mainSplit.setRightComponent(rightPanel);
        mainSplit.setDividerLocation(300);

        console = new JTextArea(12, 50);
        console.setEditable(false);
        console.setBackground(new Color(20, 20, 20));
        console.setForeground(new Color(0, 255, 100));
        JScrollPane consoleScroll = new JScrollPane(console);

        btnScan.addActionListener(e -> scanMod((ModContainer) modSelector.getSelectedItem()));
        btnChanter.addActionListener(e -> ChanterSystem.getInstance().show());
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
                model.addElement("F: " + f.getName() + " [" + fieldMappings.getOrDefault(f.getName(), "?") + "]");
            }
            model.addElement("");
            model.addElement("--- METHODS ---");
            for (Method m : clazz.getDeclaredMethods()) {
                m.setAccessible(true);
                String display = "M: " + m.getName() + " [" + methodMappings.getOrDefault(m.getName(), "?") + "]";
                model.addElement(display);
                methodMap.put(display, m);
            }
            memberList.setModel(model);
        } catch (Exception e) { log("Load error: " + e.getMessage()); }
    }

    private void showInvokePopup() {
        String selected = memberList.getSelectedValue();
        if (selected == null || !methodMap.containsKey(selected)) return;
        Method m = methodMap.get(selected);
        Class<?>[] pTypes = m.getParameterTypes();
        JTextField[] inputs = new JTextField[pTypes.length];
        JPanel p = new JPanel(new GridLayout(0, 1));

        for (int i = 0; i < pTypes.length; i++) {
            String label = pTypes[i].getSimpleName();
            if (pTypes[i] == ItemStack.class || pTypes[i] == NBTTagCompound.class) {
                label += " (Leave empty to use Chanter)";
            }
            p.add(new JLabel(label + ":"));
            inputs[i] = new JTextField();
            p.add(inputs[i]);
        }

        if (JOptionPane.showConfirmDialog(null, p, "Invoke", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
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
                if (mop != null) target = Minecraft.getMinecraft().theWorld.getTileEntity(mop.blockX, mop.blockY, mop.blockZ);
            }

            Object[] args = new Object[inputs.length];
            for (int i = 0; i < inputs.length; i++) {
                String txt = inputs[i].getText();
                Class<?> t = m.getParameterTypes()[i];
                // Injection logic from Chanter
                if (txt.isEmpty()) {
                    if (t == ItemStack.class) args[i] = ChanterSystem.getInstance().getCurrentItem();
                    else if (t == NBTTagCompound.class) args[i] = ChanterSystem.getInstance().getOutNBT();
                    else args[i] = parseArg(txt, t);
                } else {
                    args[i] = parseArg(txt, t);
                }
            }
            Object res = m.invoke(target, args);
            log("[Result] " + (res == null ? "void" : res.toString()));
        } catch (Exception e) { log("[Error] " + e.toString()); }
    }

    private Object parseArg(String s, Class<?> t) {
        if (t == int.class || t == Integer.class) return s.isEmpty() ? 0 : Integer.parseInt(s);
        if (t == boolean.class || t == Boolean.class) return Boolean.parseBoolean(s);
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
        } catch (Exception e) { log("Scan error: " + e.getMessage()); }
    }

    private void addNodeToTree(DefaultMutableTreeNode root, String className) {
        String[] parts = className.split("\\.");
        DefaultMutableTreeNode current = root;
        for (int i = 0; i < parts.length; i++) {
            DefaultMutableTreeNode found = null;
            for (int j = 0; j < current.getChildCount(); j++) {
                if (((DefaultMutableTreeNode) current.getChildAt(j)).getUserObject().equals(parts[i])) {
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