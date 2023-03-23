import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;

public class StartNode {
	public static void main(String[] args) throws IOException
    {
		InetAddress lIp = InetAddress.getByName("localhost");
		Random r = new Random();
		int low = 1;
		int high = 100000000;
		String nickN = "" + r.nextInt(high-low) + low;
		if (args.length == 1) {
			Node uServer = new Node(lIp, nickN, args[0]);
			uServer.Start(uServer);
		} else if (args.length > 1){
			System.out.println("Please specify a chatroom to join (with no spaces) or leave blank for default");
		} else {
			Node uServer = new Node(lIp, nickN);
			uServer.Start(uServer);
		}
    }
}
