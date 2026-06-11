package control.algos;

import model.Pawn;
import model.TablutBoard;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.*;
import java.util.concurrent.*;

import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * Generated wih AI assistance with Claude Sonnet 4.6
 *
 *
 * drives an osarracino subprocess over a local tcp socket, implementing
 * just enough of the UniBo Tablut server protocol for a single-side client
 *
 * protocol recap (4-byte big-endian length prefix + utf8 json payload):
 *   – after connecting, osarracino sends its name as a json string: "osarracino"
 *   – the server (us) then loops:
 *       send B1 -> [WHITE sends move] -> send B2 -> [BLACK sends move]
 *   – WHITE only acts on B1; BLACK only acts on B2
 *
 * because decide() is called exactly once per engine turn, and the opponent's
 * turn is invisible to us, we exploit the fact that each side ignores the board
 * it does not act on:
 *   – WHITE: send board (B1) -> recv move -> send board again (dummy B2, ignored)
 *   – BLACK: send board (dummy B1, ignored) -> send board (B2) → recv move
 *
 * this lets requestMove() feed exactly one board per call into a blocking queue
 */
public class OsarracinoBridge {

    private static final int PORT_SWEDISH   = 5800;
    private static final int PORT_MOSCOVITE = 5801;

    private static volatile boolean initialized = false;
    private static int side;

    private static Process          osarracinoProcess;
    private static Socket           osarracinoSocket;
    private static DataInputStream  socketIn;
    private static DataOutputStream socketOut;


    private static final BlockingQueue<String>  boardQueue = new LinkedBlockingQueue<>(1);
    private static final BlockingQueue<Integer> moveQueue  = new LinkedBlockingQueue<>(1);
    private static Thread protocolThread;




    /**
     * starts the osarracino subprocess and establishes the tcp connection
     * if already running for the same side, does nothing.
     * if already running for a different side, stops and restart
     */
    public static synchronized void startEngine(int side, int level) {
        if (initialized && OsarracinoBridge.side == side) return;
        if (initialized) stop();

        OsarracinoBridge.side = side;

        int timeoutS = level*2;
        String color = side == 0 ? "white" : "black";
        int port = side == 0 ? PORT_SWEDISH : PORT_MOSCOVITE;

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            serverSocket.setSoTimeout(8000);

            Path binary = findBinary();
            ProcessBuilder pb = new ProcessBuilder(
                    binary.toAbsolutePath().toString(),
                    color,
                    "-t", String.valueOf(timeoutS),
                    "-f", "off"
            );
            pb.redirectErrorStream(true);    // merge stderr into stdout
            osarracinoProcess = pb.start();


            Thread logger = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(osarracinoProcess.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null)
                        System.out.println("[osarracino] " + line);
                } catch (IOException ignored) {}
            }, "osarracino-stdout");
            logger.setDaemon(true);
            logger.start();  // uncomment this line to have osarracino's output in the terminal (spoiler : it's insanely verbose)


            osarracinoSocket = serverSocket.accept();
            serverSocket.close();

            socketIn  = new DataInputStream(
                    new BufferedInputStream(osarracinoSocket.getInputStream()));
            socketOut = new DataOutputStream(
                    new BufferedOutputStream(osarracinoSocket.getOutputStream()));

            String name = recv();
            System.out.printf("[OsarracinoBridge] %s connected as %s (search time ~%ds)%n",
                    name, color.toUpperCase(), timeoutS);

            boardQueue.clear();
            moveQueue.clear();
            protocolThread = new Thread(OsarracinoBridge::protocolLoop, "osarracino-protocol");
            protocolThread.setDaemon(true);
            protocolThread.start();

            initialized = true;

        } catch (Exception e) {
            throw new RuntimeException("[OsarracinoBridge] startup failed: " + e.getMessage(), e);
        }
    }


    /**
     * feeds the current board to osarracino and blocks until it replies with
     * a move
     * must be called only on the engine's own turn
     */
    public static int requestMove(TablutBoard board, int turn) {
        String json = boardToJson(board, turn);
        try {
            boardQueue.put(json);
            return moveQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("[OsarracinoBridge] interrupted while waiting for move", e);
        }
    }


    /**
     * kills the subprocess and resets all state
     * call this when the game ends so the next game gets a fresh engine
     */
    public static synchronized void stop() {
        initialized = false;
        if (protocolThread != null) {
            protocolThread.interrupt();
            protocolThread = null;
        }
        if (osarracinoProcess != null) {
            osarracinoProcess.destroyForcibly();
            osarracinoProcess = null;
        }
        try {
            if (osarracinoSocket != null) {
                osarracinoSocket.close();
                osarracinoSocket = null;
            }
        } catch (IOException ignored) {}
        boardQueue.clear();
        moveQueue.clear();
    }

    public static boolean isInitialized() { return initialized; }


    /**
     * mirrors osarracino's internal game_loop() structure:
     *
     *   while (true) {
     *       recv B1;  if WHITE -> send move;
     *       recv B2;  if BLACK -> send move;
     *   }
     *
     *   WHITE path:  send board (B1) -> recv move -> send board (dummy B2)
     *   BLACK path:  send board (dummy B1) -> send board (B2) -> recv move
     */
    private static void protocolLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String board = boardQueue.take();   // blocks until requestMove() delivers

                if (side == 0) {
                    send(board);
                    String moveJson = recv();
                    moveQueue.put(parseMoveInt(moveJson));
                    send(board);

                } else {
                    send(board);
                    send(board);
                    String moveJson = recv();
                    moveQueue.put(parseMoveInt(moveJson));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            if (initialized)
                System.err.println("[OsarracinoBridge] protocol error: " + e.getMessage());
        }
    }



    //  wire I/O   (4-byte big-endian length prefix + utf- payload)
    private static void send(String payload) throws IOException {
        byte[] bytes = payload.getBytes(UTF_8);
        socketOut.writeInt(bytes.length); // big endian
        socketOut.write(bytes);
        socketOut.flush();
    }

    private static String recv() throws IOException {
        int    len   = socketIn.readInt();  // big-endian — matches C++ ntohl()
        byte[] bytes = new byte[len];
        socketIn.readFully(bytes);
        return new String(bytes, UTF_8);
    }


    /**
     * converts the game board to the JSON format osarracino expects:
     *
     *   { "board": [[row0col0, ...], ..., [row8col0, ...]], "turn": "WHITE" }
     *
     * piece mapping:
     *   PAWN_MOSCOVITE (1) → "BLACK"
     *   PAWN_SOLDIER   (2) → "WHITE"
     *   PAWN_KING      (3) → "KING"
     *   empty center (4,4) → "THRONE"   (osarracino treats THRONE == EMPTY but
     *                                     flags the cell as an obstacle correctly)
     *   other empty        → "EMPTY"
     */
    private static String boardToJson(TablutBoard board, int turn) {
        String turnStr = (turn == 0) ? "WHITE" : "BLACK";
        StringBuilder sb = new StringBuilder(512);
        sb.append("{\"board\":[");

        for (int row = 0; row < 9; row++) {
            sb.append('[');
            for (int col = 0; col < 9; col++) {
                if (col > 0) sb.append(',');

                if (board.getElement(row, col) instanceof Pawn p) {
                    sb.append(switch (p.getColor()) {
                        case Pawn.PAWN_MOSCOVITE -> "\"BLACK\"";
                        case Pawn.PAWN_SOLDIER -> "\"WHITE\"";
                        case Pawn.PAWN_KING -> "\"KING\"";
                        default -> "\"EMPTY\"";
                    });
                } else {
                    sb.append((row == 4 && col == 4) ? "\"THRONE\"" : "\"EMPTY\"");
                }
            }
            sb.append(row < 8 ? "]," : "]");
        }

        sb.append("],\"turn\":\"").append(turnStr).append("\"}");
        return sb.toString();
    }



    /**
     * parses osarracino's move reply:
     *   {"from":"e5","to":"e3","turn":"WHITE"}
     *
     * position encoding:  col letter ('a'–'i') + row digit ('1'–'9')
     *   "e5"  →  col = 'e'-'a' = 4,  row = '5'-'1' = 4  →  raster = 4*9+4 = 40
     *
     * returns the packed move int used in all other engines:  src | (dst << 7)
     */
    private static int parseMoveInt(String json) {
        String from = extractJsonString(json, "from");
        String to = extractJsonString(json, "to");
        return posToRaster(from) | (posToRaster(to) << 7);
    }


    private static String extractJsonString(String json, String key) {
        int ki = json.indexOf('"' + key + '"');
        int colon = json.indexOf(':', ki);
        int q1 = json.indexOf('"', colon + 1);
        int q2 = json.indexOf('"', q1 + 1);
        return json.substring(q1 + 1, q2);
    }

    private static int posToRaster(String pos) {
        int col = pos.charAt(0) - 'a';
        int row = pos.charAt(1) - '1';
        return row * 9 + col;
    }


    /**
     * locates the osarracino binary using three strategies:
     *  1. class-resource lookup - works from ide / source layout (most common)
     *  2. relative path  control/algos/osarracino  from the working directory
     *  3. system property  -Dosarracino.path=<path>  for custom installations
     */
    private static Path findBinary() throws Exception {
        // 1: class resource (handles ide, source-layout, and jar)
        URL url = OsarracinoBridge.class.getResource("osarracino");
        if (url != null) {
            if ("file".equals(url.getProtocol())) {
                Path p = Path.of(url.toURI());
                p.toFile().setExecutable(true);
                return p;
            }

            Path tmp = Files.createTempFile("osarracino_", "");
            try (InputStream is = url.openStream()) {
                Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            tmp.toFile().setExecutable(true);
            tmp.toFile().deleteOnExit();
            return tmp;
        }

        // 2: relative to working directory
        Path rel = Path.of("control", "algos", "osarracino");
        if (rel.toFile().canExecute()) return rel;

        // 3: system property override
        String prop = System.getProperty("osarracino.path");
        if (prop != null) {
            Path p = Path.of(prop);
            if (p.toFile().canExecute()) return p;
        }

        throw new FileNotFoundException(
                "osarracino binary not found. " +
                        "Place it beside OsarracinoBridge.class or pass -Dosarracino.path=<path>");
    }
}