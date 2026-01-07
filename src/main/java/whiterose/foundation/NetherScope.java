package whiterose.foundation;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class NetherScope {
    public static KeyBinding keyScope = new KeyBinding("NetherScope", Keyboard.KEY_O, "Whiterose Foundation");

    private static JFrame frame;
    private static JTable packetTable;
    private static DefaultTableModel tableModel;
    private static JTextArea detailsArea;
    private static boolean isSniffing = false;

    public static void init() {
        FMLCommonHandler.instance().bus().register(new NetherScope());
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (keyScope.isPressed() && Minecraft.getMinecraft().currentScreen == null) {
            SwingUtilities.invokeLater(this::createUI);
        }
    }

    private void createUI() {
        if (frame != null) {
            frame.setVisible(true);
            frame.toFront();
            return;
        }

        frame = new JFrame("NetherScope | GC Packet Sniffer");
        frame.setSize(1000, 700);
        frame.setLayout(new BorderLayout());

        // Top Control Bar
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnToggle = new JButton("Start Sniffing");
        JButton btnClear = new JButton("Clear Log");
        controls.add(btnToggle);
        controls.add(btnClear);
        controls.add(new JLabel("Target: Outbound (C->S) FML Channels"));

        // Packet Table
        String[] columns = {"ID", "Channel", "Size (Bytes)", "Time"};
        tableModel = new DefaultTableModel(columns, 0);
        packetTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(packetTable);
        tableScroll.setBorder(new TitledBorder("Outbound Pipeline"));

        // Details Area
        detailsArea = new JTextArea();
        detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        detailsArea.setEditable(false);
        JScrollPane detailsScroll = new JScrollPane(detailsArea);
        detailsScroll.setBorder(new TitledBorder("Packet Inspection & Interpretation"));

        packetTable.getSelectionModel().addListSelectionListener(e -> {
            int row = packetTable.getSelectedRow();
            if (row != -1 && row < tableModel.getRowCount()) {
                detailsArea.setText((String) tableModel.getValueAt(row, 4));
                detailsArea.setCaretPosition(0);
            }
        });

        btnToggle.addActionListener(e -> {
            isSniffing = !isSniffing;
            btnToggle.setText(isSniffing ? "Stop Sniffing" : "Start Sniffing");
            if (isSniffing) injectHook();
        });

        btnClear.addActionListener(e -> tableModel.setRowCount(0));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, detailsScroll);
        split.setDividerLocation(300);

        frame.add(controls, BorderLayout.NORTH);
        frame.add(split, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setVisible(true);
    }

    private void injectHook() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.getNetHandler() == null || mc.getNetHandler().getNetworkManager() == null) return;

            // Avoid double injection
            if (mc.getNetHandler().getNetworkManager().channel().pipeline().get("netherscope_hook") != null) {
                return;
            }

            mc.getNetHandler().getNetworkManager().channel().pipeline().addBefore("packet_handler", "netherscope_hook", new ChannelOutboundHandlerAdapter() {
                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                    if (isSniffing) {
                        capturePacket(msg);
                    }
                    super.write(ctx, msg, promise);
                }
            });
        } catch (Exception e) {
            System.err.println("[NetherScope] Hook failed: " + e.getMessage());
        }
    }

    private void capturePacket(Object msg) {
        if (!(msg instanceof FMLProxyPacket)) return;

        FMLProxyPacket p = (FMLProxyPacket) msg;
        String channel = p.channel();
        ByteBuf data = p.payload().copy(); // Copy to avoid affecting the original stream
        int size = data.readableBytes();

        String analysis = analyzePacket(channel, data);

        SwingUtilities.invokeLater(() -> {
            tableModel.insertRow(0, new Object[]{
                    tableModel.getRowCount() + 1,
                    channel,
                    size,
                    System.currentTimeMillis() % 10000,
                    analysis
            });
            // Keep log size sane
            if (tableModel.getRowCount() > 100) tableModel.setRowCount(100);
        });
    }

    private String analyzePacket(String channel, ByteBuf buf) {
        StringBuilder sb = new StringBuilder();
        sb.append("Channel: ").append(channel).append("\n");
        sb.append("Size: ").append(buf.readableBytes()).append(" bytes\n");
        sb.append("------------------------------------------\n");

        // Interpretation for Galacticraft PacketDynamic
        if (channel.contains("Galacticraft")) {
            try {
                int type = buf.readInt();
                int dim = buf.readInt();
                sb.append("[GC PacketDynamic Detected]\n");
                sb.append("Object Type: ").append(type == 0 ? "Entity" : "TileEntity").append("\n");
                sb.append("Dimension: ").append(dim).append("\n");

                if (type == 1) { // TileEntity
                    int x = buf.readInt();
                    int y = buf.readInt();
                    int z = buf.readInt();
                    sb.append("Coordinates: ").append(x).append(", ").append(y).append(", ").append(z).append("\n");
                } else {
                    int entityID = buf.readInt();
                    sb.append("Entity ID: ").append(entityID).append("\n");
                }
                sb.append("Remaining Data (NetworkedFields): ").append(buf.readableBytes()).append(" bytes\n");
            } catch (Exception e) {
                sb.append("Interpret Error: ").append(e.getMessage()).append("\n");
            }
            buf.resetReaderIndex();
        }

        sb.append("\n--- HEX DUMP ---\n");
        sb.append(manualHexDump(buf));
        return sb.toString();
    }

    private String manualHexDump(ByteBuf buf) {
        StringBuilder result = new StringBuilder();
        int length = buf.readableBytes();
        for (int i = 0; i < length; i += 16) {
            // Address
            result.append(String.format("%04X: ", i));

            // Hex
            for (int j = 0; j < 16; j++) {
                if (i + j < length) {
                    result.append(String.format("%02X ", buf.getByte(i + j)));
                } else {
                    result.append("   ");
                }
            }

            result.append(" | ");

            // ASCII
            for (int j = 0; j < 16; j++) {
                if (i + j < length) {
                    byte b = buf.getByte(i + j);
                    if (b >= 32 && b <= 126) result.append((char) b);
                    else result.append(".");
                }
            }
            result.append("\n");
        }
        return result.toString();
    }
}