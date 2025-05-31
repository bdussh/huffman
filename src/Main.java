import java.io.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class Main {

    public static void main(String[] args) throws IOException {
        encodeMessage("src\\files");
        decodeMessage("src\\files\\exercise");
    }

    //calculates appearances of each character in text.txt in the given path
    private static Map<Integer, Integer> calculateFrequency(String path) throws IOException {
        Map<Integer, Integer> freq = new HashMap<>();

        try (FileReader fileReader = new FileReader(path + "\\text.txt");) {
            int ch = fileReader.read();
            while (ch != -1) {
                if (!freq.containsKey(ch)) {
                    freq.put(ch, 1);
                } else {
                    freq.put(ch, freq.get(ch) + 1);
                }
                ch = fileReader.read();
            }
        }
        return freq;
    }

    // creates tree, which defines huffman coding
    private static Node createTree(String path) throws IOException {
        Map<Integer, Integer> freq = calculateFrequency(path);
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(o -> o.frequency));

        for (Integer key : freq.keySet()) {
            pq.add(new Node(freq.get(key), key));
        }

        while (pq.size() > 1) {
            Node n1 = pq.poll();
            Node n2 = pq.poll();
            Node n3 = new Node(n1.frequency + n2.frequency);
            n3.right = n1;
            n3.left = n2;
            pq.add(n3);
        }
        return pq.poll();
    }

    // returns mapping of huffman coding for each char in the given tree
    // don't use this method
    private static Map<Integer, String> huffmanCoding(Node root, String prefix, Map<Integer, String> huffman) {
        if (root == null) {
            return huffman;
        }
        if (root.left == null && root.right == null) {
            huffman.put(root.ch, prefix);
            return huffman;
        }

        if (root.right != null) {
            huffmanCoding(root.right, prefix + "1", huffman);
        }
        if (root.left != null) {
            huffmanCoding(root.left, prefix + "0", huffman);
        }

        return huffman;
    }

    // use this
    private static Map<Integer, String> huffmanCoding(Node root) {
        return huffmanCoding(root, "", new HashMap<>());
    }

    // writes coding to dec_tab.txt
    private static void saveCoding(Map<Integer, String> huffman, String path) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path + "\\dec_tab.txt"));) {
            var iterator = huffman.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                bw.write(entry.getKey() + ":" + entry.getValue());
                if (iterator.hasNext()) {
                    bw.write("-");
                }
            }


        } catch (IOException e) {
            System.err.println("Error writing to file");
        }
    }

    // encodes text from text.txt and adds 1 and 0's till length%8==0
    private static String encodeText(Map<Integer, String> huffman, String path) throws IOException {
        String encoded = "";
        try (FileReader r = new FileReader(path + "\\text.txt");) {
            int c = r.read();
            while (c != -1) {
                encoded += huffman.get(c);
                c = r.read();
            }
        }

        encoded += "1";
        while (encoded.length() % 8 != 0) encoded += "0";

        return encoded;
    }

    //writes encoded message to output.dat
    private static void saveEncodedText(String encoded, String path) throws IOException {
        int length = encoded.length() / 8;
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            String byteString = encoded.substring(i * 8, (i + 1) * 8);
            bytes[i] = (byte) Integer.parseInt(byteString, 2);
        }
        try (var os = new FileOutputStream(path + "\\output.dat");) {
            os.write(bytes);
        }
    }

    //encodes text.txt in the given path
    public static void encodeMessage(String path) {
        try {
            Node root = createTree(path);
            Map<Integer, String> huffman = huffmanCoding(root);
            saveCoding(huffman, path);
            String encodedBitstring = encodeText(huffman, path);
            saveEncodedText(encodedBitstring, path);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // reads String from output.dat in the given path and get rid of redundant 0's and 1
    private static String readEncodedText(String path) throws IOException {
        File file = new File(path + "\\output.dat");
        byte[] bFile = new byte[(int) file.length()];
        FileInputStream fis = new FileInputStream(file);
        fis.read(bFile);
        fis.close();
        String encodedBitstring = "";
        for (int i = 0; i < bFile.length; i++) {
            String strByte = Integer.toBinaryString(bFile[i] & 0xFF);
            while (strByte.length() < 8) {
                strByte = "0" + strByte;
            }
            encodedBitstring += strByte;
        }
        int lastOne = encodedBitstring.lastIndexOf("1");
        encodedBitstring = encodedBitstring.substring(0, lastOne);
        return encodedBitstring;
    }
    // reads dec_tab.txt in the given path and return huffman table
    private static Map<Integer, String> readHuffmanTable(String path) throws IOException {
        Map<Integer, String> huffman = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path + "\\dec_tab.txt"));) {
            String line = br.readLine();
            String[] entries = line.split("-");
            for (String entry : entries) {
                String[] parts = entry.split(":");
                int ch = Integer.parseInt(parts[0]);
                String code = parts[1];
                huffman.put(ch, code);
            }
        }
        return huffman;
    }

    //reads encoded message from output.dat, decodes and writes message to text.txt
    private static void decodeMessage(String path) {
        try {
            String bitstring = readEncodedText(path);
            var huffman = readHuffmanTable(path);
            Map<String, Integer> invertedHuffman = new HashMap<>(); // to decode
            for (Map.Entry<Integer, String> entry : huffman.entrySet()) {
                invertedHuffman.put(entry.getValue(), entry.getKey());
            }

            String text = "";
            String cur = "";
            for (int i = 0; i < bitstring.length(); i++) {
                cur += bitstring.charAt(i);
                if (invertedHuffman.containsKey(cur)) {
                    text += (char) ((int) invertedHuffman.get(cur));
                    cur = "";
                }
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(path + "\\text.txt"))) {
                bw.write(text);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Node {
    Node left;
    Node right;
    int frequency;
    int ch;

    Node(int frequency, int ch) {
        this.frequency = frequency;
        this.ch = ch;
    }

    Node(int frequency) {
        this.frequency = frequency;
        this.ch = -1;
    }

    @Override
    public String toString() {
        return "{" + ch + ":" + frequency + "}";
    }
}