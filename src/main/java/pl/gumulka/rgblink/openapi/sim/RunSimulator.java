package pl.gumulka.rgblink.openapi.sim;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class RunSimulator {
  private DatagramSocket socket;
  private boolean running;
  private byte[] buf = new byte[256];

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


    toSend = addTao1ProDatablocks((toSend));

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

  private String addTao1ProDatablocks(String msg) {
    if (msg.matches("<F....F1B3.*")) {
      msg = msg.substring(0, 12) + "0700" + "00>" + "01 80 07 38 04 3c 00";
    } else if (msg.matches("<F....F1B4.*")) {
      msg = msg.substring(0, 12) + "2100" + "00>" + "00 22 72 74 6d 70 3a 2f 2f 31 39 32 2e 31 36 38 2e 30 2e 37 36 2f 6c 69 76 65 2f 74 65 73 74 22 cf";
    }
    return msg;
  }
}
