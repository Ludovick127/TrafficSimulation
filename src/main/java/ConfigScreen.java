import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Ecran de configuration : choix du circuit et nombre de voitures par voie
 */
public class ConfigScreen extends JFrame {

    private final JComboBox<String> circuitCombo;
    private final JPanel lanesPanel;
    private final Map<String, Circuit> circuits = new HashMap<>();
    private final Map<Integer, JSpinner> laneSpinners = new HashMap<>();
    private final JButton startButton;

    public ConfigScreen() {
        setTitle("Traffic Simulation - Configuration");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 420);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10,10));
        getContentPane().setBackground(new Color(245,245,240));

        circuits.put("Silverstone Circuit", new SilverstoneCircuit());
        circuits.put("Manhattan - Midtown Loop", new ManhattanCircuit());

        // Header
        JPanel header = new JPanel();
        header.setBackground(new Color(30,30,30));
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
        JLabel title = new JLabel("Traffic Simulation");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = new JLabel("Choisissez le circuit et le nombre de voitures par voie");
        sub.setForeground(new Color(200,200,200));
        sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(5));
        header.add(sub);
        add(header, BorderLayout.NORTH);

        // Center form
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        // Circuit choice
        JPanel circuitPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        circuitPanel.setBackground(Color.WHITE);
        circuitPanel.add(new JLabel("Circuit :"));
        circuitCombo = new JComboBox<>(new String[]{"Silverstone Circuit", "Manhattan - Midtown Loop"});
        circuitCombo.setPreferredSize(new Dimension(260,28));
        circuitPanel.add(circuitCombo);
        form.add(circuitPanel);
        form.add(Box.createVerticalStrut(10));

        // Lanes panel
        lanesPanel = new JPanel();
        lanesPanel.setBackground(Color.WHITE);
        lanesPanel.setLayout(new BoxLayout(lanesPanel, BoxLayout.Y_AXIS));
        lanesPanel.setBorder(BorderFactory.createTitledBorder("Nombre de voitures par voie (2 voies)"));
        form.add(lanesPanel);

        // Info label
        JLabel info = new JLabel("<html><i>Astuce : 2 voies pour Manhattan, 3 voies pour Silverstone.<br>La configuration s'adapte automatiquement au circuit choisi.</i></html>");
        info.setFont(new Font("SansSerif", Font.ITALIC, 11));
        info.setForeground(new Color(100,100,100));
        info.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
        form.add(info);

        add(form, BorderLayout.CENTER);

        // Footer button
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(new Color(245,245,240));
        startButton = new JButton("▶ Lancer la simulation");
        startButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        startButton.setBackground(new Color(34,139,34));
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        startButton.setPreferredSize(new Dimension(220,40));
        footer.add(startButton);
        add(footer, BorderLayout.SOUTH);

        // listeners
        circuitCombo.addActionListener(e -> updateLanesPanel());
        startButton.addActionListener(e -> launchSimulation());

        // initial
        updateLanesPanel();
    }

    private void updateLanesPanel() {
        lanesPanel.removeAll();
        laneSpinners.clear();
        String selected = (String) circuitCombo.getSelectedItem();
        Circuit c = circuits.get(selected);
        int laneCount = c != null ? c.getLaneCount() : 2;
        // For Silverstone we show 3, for Manhattan 2, but spec says 2 voies -> we show laneCount
        // To respect spec "2 voies", we can show 2 spinners even for Silverstone as inner/outer, but better show laneCount
        // We'll show laneCount spinners with labels Voie 1, Voie 2, (Voie 3 if needed)
        lanesPanel.setBorder(BorderFactory.createTitledBorder("Nombre de voitures par voie ("+laneCount+" voies)"));
        for (int i=0;i<laneCount;i++) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
            row.setBackground(Color.WHITE);
            String laneName = "Voie " + (i+1) + " :";
            if (c instanceof SilverstoneCircuit) {
                if (i==0) laneName = "Voie 1 (intérieur) :";
                else if (i==1) laneName = "Voie 2 (centre) :";
                else laneName = "Voie 3 (extérieur) :";
            } else {
                if (i==0) laneName = "Voie 1 (ouest) :";
                else laneName = "Voie 2 (est) :";
            }
            row.add(new JLabel(laneName));
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(8, 0, 30, 1));
            // defaults: Silverstone 8,10,8 ; Manhattan 8,8
            if (c instanceof SilverstoneCircuit) {
                int def = (i==1 ? 10 : 8);
                spinner.setValue(def);
            } else {
                spinner.setValue(8);
            }
            spinner.setPreferredSize(new Dimension(70,26));
            row.add(spinner);
            // add small color indicator for lane
            JPanel colorBox = new JPanel();
            colorBox.setPreferredSize(new Dimension(16,16));
            colorBox.setBackground(i==0 ? new Color(0,180,0) : i==1 ? new Color(50,100,255) : Color.RED);
            colorBox.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            row.add(colorBox);
            laneSpinners.put(i, spinner);
            lanesPanel.add(row);
        }
        lanesPanel.revalidate();
        lanesPanel.repaint();
    }

    private void launchSimulation() {
        String selected = (String) circuitCombo.getSelectedItem();
        Circuit circuit = circuits.get(selected);
        if (circuit == null) return;
        int laneCount = circuit.getLaneCount();
        int[] counts = new int[laneCount];
        for (int i=0;i<laneCount;i++) {
            JSpinner sp = laneSpinners.get(i);
            counts[i] = (Integer) sp.getValue();
        }
        // need fresh circuit instance for new window
        Circuit fresh;
        if (circuit instanceof SilverstoneCircuit) fresh = new SilverstoneCircuit();
        else fresh = new ManhattanCircuit();

        // dispose config and open simulation
        dispose();
        TrafficSimulation.launchWithConfig(fresh, counts);
    }

    public static void showConfig() {
        SwingUtilities.invokeLater(() -> {
            ConfigScreen cs = new ConfigScreen();
            cs.setVisible(true);
        });
    }
}
