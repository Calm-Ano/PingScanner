package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.swing.table.*;

public class PScan extends JFrame implements ActionListener{
    public JMenuBar menu_bar;
    private JTextField ipaddr_text;
    private JTextField netmask_text;
    public JTable table;
    public JLabel progress_label;
    public JProgressBar progress;
    private PingScanner pingscanner;
    String[][] ips;
    DefaultTableModel model;
    String[] columns = {"Address", "Alive"};
    JScrollPane scrollPane;
    PingScanner scanner;
    private boolean display_all = false;
    JButton runButton;

//    class PingScanner extends Thread{;
    class PingScanner implements Runnable {
        // todo 実行中でもボタンを押したら rerun するように作る
        PScan win;
        String subnetmask;
        String ipaddr;
        PingScanner(PScan win, String ipaddr, String subnetmask) throws UnknownHostException {
            this.win = win;
            this.subnetmask = subnetmask;
            this.ipaddr = ipaddr;
//            ips = new String[calc_host_num(subnetmask)][2];
        }
        public int calc_host_num(String mask) throws UnknownHostException {
            // mask を使っていい感じに計算する;
            byte[] bNetAddr = InetAddress.getByName(mask).getAddress();
            for(int i = 0; i < bNetAddr.length; i++){
                bNetAddr[i] = (byte) ~bNetAddr[i];
            }
            return ((bNetAddr[0] & 0xFF) << 24) +
                    ((bNetAddr[1] & 0xFF) << 16) +
                    ((bNetAddr[2] & 0xFF) << 8) +
                    ((bNetAddr[3] & 0xFF)) - 1; // ブロードキャスト分引く
        }
        public String get_network_address(String ipAddr, String mask) throws UnknownHostException {
            byte[] bIP = InetAddress.getByName(ipAddr).getAddress();
            byte[] bSB = InetAddress.getByName(mask).getAddress();
            byte[] bNT = new byte[4];

            for(int i = 0;i<bIP.length;i++) {
                bNT[i] = (byte) (bIP[i] & bSB[i]);
            }
            System.out.println();

            return InetAddress.getByAddress(bNT).getHostAddress();
        }
        public String add_nwaddr_host(String nw_addr, int host_n) throws UnknownHostException {
            byte[] nwIP = InetAddress.getByName(nw_addr).getAddress();
            byte[] hostIP = new byte[4];

            hostIP[0] = (byte) (host_n >>> 24);
            host_n = host_n - ((host_n >>> 24) << 24);

            hostIP[1] = (byte) (host_n >>> 16);
            host_n = host_n - ((host_n >>> 16) << 16);

            hostIP[2] = (byte) (host_n >>> 8);
            host_n = host_n - ((host_n >>> 8) << 8);

            hostIP[3] = (byte) (host_n);

            hostIP[0] = (byte) (hostIP[0] | nwIP[0]);
            hostIP[1] = (byte) (hostIP[1] | nwIP[1]);
            hostIP[2] = (byte) (hostIP[2] | nwIP[2]);
            hostIP[3] = (byte) (hostIP[3] | nwIP[3]);

            return String.format("%d.%d.%d.%d", (hostIP[0] & 0xFF), (hostIP[1] & 0xFF),
                    (hostIP[2] & 0xFF), (hostIP[3] & 0xFF));
        }
        public void run(){
            String network_addr;
            try {
                ips = new String[calc_host_num(subnetmask)][2];
            } catch (UnknownHostException e) {
                System.out.print("Error");
            }
            try {
                int counts_alive = 0;
                int host_num = calc_host_num(subnetmask);
                ips = new String[host_num][2];
                System.out.println("---called--");
                network_addr = get_network_address(ipaddr, subnetmask);

                for (int i = 1; i <= host_num ; i++) {
                    String target_ip = add_nwaddr_host(network_addr, i);
                    boolean alive = new PingScan(target_ip, 1000).ping_to();
                    System.out.println(target_ip);
                    if (!display_all && alive){
                        ips[counts_alive][0] = target_ip;
                        ips[counts_alive][1] = "alive";
                        counts_alive++;
                    }else if(display_all){
                        ips[i - 1][0] = target_ip;
                        ips[i - 1][1] = alive ? "alive!" : "";
                    }
                    model = new DefaultTableModel(ips, columns);
                    table.setModel(model);
                    DefaultTableColumnModel columnModel
                            = (DefaultTableColumnModel) table.getColumnModel();

                    TableColumn column = null;
                    for (int j = 0; j < columnModel.getColumnCount(); j++) {
                        column = columnModel.getColumn(j);
                        column.setPreferredWidth(200 - j * 200 / 2);
                    }
                    // todo ここらへんうまくいけない。
                    progress.setValue((int)(i/host_num) * 100);
                    progress.setString(String.valueOf((int)(i/host_num) * 100) + "% 完了");
                    progress.repaint();
                }
            } catch (UnknownHostException unknownHostException) {
                unknownHostException.printStackTrace();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            runButton.setEnabled(true);
        }
    }

    public static void main(String[] args) {
        PScan w = new PScan("Host Scanning Tool v1.0");
        w.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        w.setSize( 700, 600 );
        w.setLocationRelativeTo(null);
        w.setVisible( true );
    }

    /**
     * Create the frame.
     */
    public PScan(String title) {
        super(title);

        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());

        menu_bar = new JMenuBar();
        setJMenuBar(menu_bar);

        JMenu operation = new JMenu("表示");
        menu_bar.add(operation);

        operation.add(new DisplayAll());
        operation.add(new HideFalse());

        JPanel header = addHeader();
        p.add(header, BorderLayout.NORTH);

        JPanel body = addBody();
        p.add(body, BorderLayout.CENTER);

//        todo :　progress barを導入して進捗を視覚化
//        実装がうまくできない....
        JPanel footer = addFooter();
        p.add(footer, BorderLayout.SOUTH);

        getContentPane().add(p, BorderLayout.CENTER);
    }

    private JPanel addHeader(){
        /*
         * GUIヘッダ部分
         */
        // todo Enterキーで ip_panel -> mask_panel -> button 実効できるようにする。
        JPanel header = new JPanel();
        header.setLayout(new FlowLayout());

        JPanel innerheader = new JPanel();
        innerheader.setLayout(new BorderLayout());

        JPanel ip_panel = new JPanel();
        ip_panel.setLayout(new BorderLayout());

        JPanel mask_panel = new JPanel();
        mask_panel.setLayout(new BorderLayout());


        JLabel ip_label = new JLabel("this hosts ipaddr  ");
        ip_panel.add(ip_label, BorderLayout.WEST);

        ipaddr_text = new JTextField(35);
        ipaddr_text.setText("192.168.1.0");
        ip_panel.add(ipaddr_text, BorderLayout.EAST);
        innerheader.add(ip_panel, BorderLayout.NORTH);

        JLabel mask_label = new JLabel("subnet mask");
        mask_panel.add(mask_label, BorderLayout.WEST);
        netmask_text = new JTextField(35);
        netmask_text.setText("255.255.255.0");
        mask_panel.add(netmask_text, BorderLayout.EAST);
        innerheader.add(mask_panel, BorderLayout.SOUTH);

        header.add(innerheader);

        runButton = new JButton("Run");
        runButton.addActionListener(this);
        header.add(runButton);

        return header;
    }

    private JPanel addBody(){
        /*
         * GUIテーブル部分
         */
        JPanel body = new JPanel();
        body.setLayout(new FlowLayout());
        String[][] data = new String[32][2];

        model = new DefaultTableModel(columns, 30);
        table = new JTable();
        table.setModel(model);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        DefaultTableColumnModel columnModel
                = (DefaultTableColumnModel)table.getColumnModel();

        TableColumn column = null;
        for (int i = 0 ; i < columnModel.getColumnCount() ; i++){
            column = columnModel.getColumn(i);
            column.setPreferredWidth(200 - i * 100);
        }

        scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(318, 400));

        body.add(scrollPane);
        return body;
    }

    private JPanel addFooter(){
        /*
         * プログレスバー
         */
        JPanel footer = new JPanel();
        footer.setLayout(new FlowLayout());

        this.progress = new JProgressBar(0, 100);

//        progress.setBorderPainted(false);
        progress.setStringPainted(true);
        progress.setString("");

        footer.add(progress);
        return footer;
    }
    public void actionPerformed(ActionEvent e){
        try {
            runButton.setEnabled(false);
            scanner = new PingScanner(this, ipaddr_text.getText(), netmask_text.getText());

            Thread thread = new Thread(scanner);
            thread.start();
        } catch (Exception ee) {
            ee.printStackTrace(System.err);
        }
    }
    class DisplayAll extends AbstractAction{
        DisplayAll(){
            putValue( Action.NAME, "全表示" );
            putValue( Action.SHORT_DESCRIPTION, "IPアドレス順にすべての結果を表示します" );
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            display_all = true;
        }
    }
    class HideFalse extends AbstractAction{
        HideFalse(){
            putValue( Action.NAME, "一部表示" );
            putValue( Action.SHORT_DESCRIPTION, "IPアドレス順に応答のあったもののみを表示します" );
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            display_all = false;
        }
    }
}
