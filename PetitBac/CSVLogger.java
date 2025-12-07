package PetitBac;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CSVLogger {
    private final File file;

    public CSVLogger(String path) {
        this.file = new File(path);
        try {
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                try (FileWriter fw = new FileWriter(file, false)) {
                    fw.write("round,player,algorithm,nodesExpanded,timeMs,score,chosenWords\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void append(int round, String player, String algorithm, long nodes, double timeMs, int score, String chosenWords) {
        try (FileWriter fw = new FileWriter(file, true)) {
            String line = String.format("%d,%s,%s,%d,%.3f,%d,\"%s\"\n", round, player, algorithm, nodes, timeMs, score, chosenWords.replace("\"", "\'"));
            fw.write(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
