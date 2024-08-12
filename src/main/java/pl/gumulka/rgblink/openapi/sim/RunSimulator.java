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
      for(String chunk : chunks) {
        packet = sendResponse(packet, chunk);
      }


    }
    socket.close();
  }

  private DatagramPacket sendResponse(DatagramPacket packet, String received) throws IOException {
    InetAddress address = packet.getAddress();
    int port = packet.getPort();
    String toSend = received.replaceFirst("T", "F");
    if(toSend.matches("<F....F1.*")){
      toSend = toSend + "01 80 07 38 04 3c 00";
    }
    byte[] bytes = toSend.getBytes(StandardCharsets.UTF_8);
    packet = new DatagramPacket(bytes, bytes.length, address, port);
    socket.send(packet);
    System.out.println("==> " + toSend);
    return packet;
  }
}
