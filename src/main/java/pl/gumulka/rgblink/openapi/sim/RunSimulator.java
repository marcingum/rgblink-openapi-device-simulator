package pl.gumulka.rgblink.openapi.sim;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class RunSimulator {
  private DatagramSocket socket;
  private boolean running;
  private byte[] buf = new byte[256];

  private static final List<String> KNOWN_DEVICE_MODEL_KEYS = Arrays.asList(
      "220000", "320000", "700000", "A00000", "800000", "230100",
      "240100", "240000", "241000", "250000", "629000", "2C0100",
      "2C0106", "2C0107", "271000", "271001", "271100", "271101",
      "353200"
  );

  private static final Random RANDOM = new Random();

  public static void main(String args[]) {
    try {
      RunSimulator s = new RunSimulator(5560);
      s.run();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public RunSimulator(int port) throws SocketException {
    socket = new DatagramSocket(port);
  }

  public void run() throws IOException {
    running = true;

    while (running) {
      //clearBuf();

      DatagramPacket packet = new DatagramPacket(buf, buf.length);
      socket.receive(packet);
      String received = new String(packet.getData(), 0, packet.getLength());
      System.out.println("<== " + received);

      if (received.equals("end")) {
        running = false;
        continue;
      }

      String[] chunks = received.split("(?<=\\G.{" + 19 + "})");
      for (String chunk : chunks) {
        packet = sendResponse(packet, chunk);
      }


    }
    socket.close();
  }

  private DatagramPacket sendResponse(DatagramPacket packet, String received) throws IOException, NumberFormatException {
    InetAddress address = packet.getAddress();
    int port = packet.getPort();
    String toSend = received.replaceFirst("T", "F");


    //toSend = addTao1ProDatablocks((toSend));
    toSend = randomDeviceModelKey(toSend);
    toSend = doSpecial7502stupidResponse(toSend);

    // fix checksum
    int sum = 0;
    for (int i = 2; i < 16; i += 2) {
      sum += Integer.parseInt(toSend.substring(i, i + 2), 16);
    }
    int checksum = sum % 256;
    String checksum2 = String.format("%02X", checksum);
    toSend = toSend.substring(0, 16) + checksum2 + toSend.substring(18);

    byte[] bytes = toSend.getBytes(StandardCharsets.UTF_8);
    packet = new DatagramPacket(bytes, bytes.length, address, port);
    socket.send(packet);
    System.out.println("==> " + toSend);
    return packet;
  }

  private String doSpecial7502stupidResponse(String toSend) {
    if (toSend.indexOf("7502") == 6) {
      toSend = toSend.substring(0, 10) + "000000" + toSend.substring(16);
    }
    return toSend;
  }

  private String addTao1ProDatablocks(String msg) {
    if (msg.matches("<F....F1B3.*")) {
      msg = msg.substring(0, 12) + "0700" + "00>" + "01 80 07 38 04 3c 00";
    } else if (msg.matches("<F....F1B4.*")) {
      msg = msg.substring(0, 12) + "2100" + "00>" + "00 22 72 74 6d 70 3a 2f 2f 31 39 32 2e 31 36 38 2e 30 2e 37 36 2f 6c 69 76 65 2f 74 65 73 74 22 cf";
    } else if (msg.matches("<F....F1B5.*")) {
      msg = msg.substring(0, 12) + "0500" + "00>" + "00 18 70 17 9f";
    } else if (msg.matches("<F....F145.*")) {
      msg = msg.substring(0, 12) + "2600" + "00>" + "2F 6D 65 64 69 61 2F 75 73 62 30 2F 72 65 63 6F 72 64 5F 32 30 31 33 30 31 31 38 30 39 35 37 34 35 2E 6D 70 34 F2";
    }
    return msg;
  }

  private String randomDeviceModelKey(String toSend) {
    String target = "6801";
    int index = toSend.indexOf(target);
    if (index != -1 && index + 4 + 6 <= toSend.length()) {
      int startReplace = index + 4;
      int endReplace = startReplace + 6;
      String randomKey = KNOWN_DEVICE_MODEL_KEYS.get(RANDOM.nextInt(KNOWN_DEVICE_MODEL_KEYS.size()));
      return toSend.substring(0, startReplace) + randomKey + toSend.substring(endReplace);
    }
    return toSend;
  }
}
